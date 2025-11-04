package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import ch.obermuhlner.aitutor.lesson.service.LessonContentService
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TutorService(
    private val aiChatService: AiChatService,
    private val languageService: LanguageService,
    private val vocabularyContextService: VocabularyContextService,
    private val messageCompactionService: MessageCompactionService,
    private val lessonProgressionService: LessonProgressionService,
    private val lessonContentService: LessonContentService,
    private val catalogService: CatalogService,
    private val objectMapper: ObjectMapper,
    private val supportedLanguages: Map<String, LanguageMetadata>,
    @Value("\${ai-tutor.prompts.system}") private val systemPromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-none}") private val levelNonePromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-a1}") private val levelA1PromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-a2}") private val levelA2PromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-b1}") private val levelB1PromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-b2}") private val levelB2PromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-c1}") private val levelC1PromptTemplate: String,
    @Value("\${ai-tutor.prompts.level-c2}") private val levelC2PromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-free}") private val phaseFreePromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-correction}") private val phaseCorrectionPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-drill}") private val phaseDrillPromptTemplate: String,
    @Value("\${ai-tutor.prompts.developer}") private val developerPromptTemplate: String,
    @Value("\${ai-tutor.prompts.error-classification-guidance}") private val errorClassificationGuidance: String,
    @Value("\${ai-tutor.prompts.vocabulary.no-tracking}") private val vocabularyNoTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.with-tracking}") private val vocabularyWithTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.course-teaching-style.reactive}") private val courseTeachingStyleReactiveTemplate: String,
    @Value("\${ai-tutor.prompts.course-teaching-style.guided}") private val courseTeachingStyleGuidedTemplate: String,
    @Value("\${ai-tutor.prompts.course-teaching-style.directive}") private val courseTeachingStyleDirectiveTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.reactive}") private val teachingStyleReactiveTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.guided}") private val teachingStyleGuidedTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.directive}") private val teachingStyleDirectiveTemplate: String,
    @Value("\${ai-tutor.prompts.lesson}") private val lessonPrompt: String,
    @Value("\${ai-tutor.prompts.initiate-welcome}") private val initiateWelcomeTemplate: String,
    @Value("\${ai-tutor.prompts.initiate-reengage}") private val initiateReengageTemplate: String,
    @Value("\${ai-tutor.prompts.initiate-reengage-light}") private val initiateReengageLightTemplate: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val levelToPromptTemplateMap = mapOf(
        CEFRLevel.None to levelNonePromptTemplate,
        CEFRLevel.A1 to levelA1PromptTemplate,
        CEFRLevel.A2 to levelA2PromptTemplate,
        CEFRLevel.B1 to levelB1PromptTemplate,
        CEFRLevel.B2 to levelB2PromptTemplate,
        CEFRLevel.C1 to levelC1PromptTemplate,
        CEFRLevel.C2 to levelC2PromptTemplate,
    )

    data class TutorResponse(
        val reply: String,
        val conversationResponse: ConversationResponse
    )

    fun respond(
        tutor: Tutor,
        conversationState: ConversationState,
        userId: UUID,
        messages: List<Message>,
        sessionId: UUID? = null,
        session: ChatSessionEntity? = null,
        onReplyChunk: (String) -> Unit = { print(it) },
        chatModel: org.springframework.ai.chat.model.ChatModel? = null // Optional per-user ChatModel
    ): TutorResponse? {
        logger.debug("Tutor respond: user=$userId, session=$sessionId, phase=${conversationState.phase}, topic=${conversationState.currentTopic}")

        val sourceLanguageCode = tutor.sourceLanguageCode
        val targetLanguageCode = tutor.targetLanguageCode

        val sourceLanguage = languageService.getLanguageName(sourceLanguageCode)
        val targetLanguage = languageService.getLanguageName(targetLanguageCode)

        logger.debug("Language pair: $sourceLanguage -> $targetLanguage")

        // Get vocabulary context for the user
        val vocabContext = vocabularyContextService.getVocabularyContext(userId, targetLanguageCode)

        val vocabularyGuidance = buildVocabularyGuidance(vocabContext)

        // Get current lesson and curriculum if session is course-based
        val currentLesson = if (session != null && session.courseTemplateId != null) {
            lessonProgressionService.checkAndProgressLesson(session.id)
        } else {
            null
        }

        // Get curriculum for course overview
        val curriculum = if (session != null && session.courseTemplateId != null && currentLesson != null) {
            val courseSlug = getCourseSlug(session)
            courseSlug?.let { lessonContentService.getCurriculum(it) }
        } else {
            null
        }

        val teachingStyleGuidance = buildTeachingStyleGuidance(tutor.teachingStyle, targetLanguage)

        val courseTeachingStyleGuidance = buildCourseTeachingStyleGuidance(tutor.teachingStyle, targetLanguage)

        // Extract decision metadata with safe defaults for backward compatibility
        val phaseReason = conversationState.phaseReason ?: "Balanced default phase"
        val topicEligibilityStatus = conversationState.topicEligibilityStatus ?: "Active conversation"
        val pastTopics = conversationState.pastTopics

        // Build consolidated system prompt
        val consolidatedSystemPrompt = buildConsolidatedSystemPrompt(
            tutor = tutor,
            conversationState = conversationState,
            phaseReason = phaseReason,
            topicEligibilityStatus = topicEligibilityStatus,
            pastTopics = pastTopics,
            targetLanguage = targetLanguage,
            targetLanguageCode = targetLanguageCode,
            sourceLanguage = sourceLanguage,
            sourceLanguageCode = sourceLanguageCode,
            vocabularyGuidance = vocabularyGuidance,
            teachingStyleGuidance = teachingStyleGuidance,
            courseTeachingStyleGuidance = courseTeachingStyleGuidance,
            currentLesson = currentLesson,
            curriculum = curriculum
        )

        val systemMessages = listOf(SystemMessage(consolidatedSystemPrompt))

        // Log metrics for monitoring
        val estimatedTokens = consolidatedSystemPrompt.length / 4
        logger.info("System prompt assembled: 1 message, ~$estimatedTokens tokens (estimated)")
        logger.debug("Phase: ${conversationState.phase.name} ($phaseReason)")
        logger.debug("Topic: ${conversationState.currentTopic ?: "free conversation"} ($topicEligibilityStatus)")

        val compactedMessages = messageCompactionService.compactMessages(systemMessages, messages, sessionId)

        val response = aiChatService.call(AiChatRequest(compactedMessages), onReplyChunk, chatModel)

        return response?.let {
            TutorResponse(
                reply = it.reply,
                conversationResponse = it.conversationResponse
            )
        }
    }

    private fun buildVocabularyGuidance(vocabContext: ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext): String {
        if (vocabContext.totalWordCount == 0) {
            return PromptTemplate(vocabularyNoTrackingTemplate).render(emptyMap())
        }

        val reinforcementList = if (vocabContext.wordsForReinforcement.isNotEmpty()) {
            vocabContext.wordsForReinforcement.joinToString(", ")
        } else {
            "(none)"
        }

        val recentList = if (vocabContext.recentNewWords.isNotEmpty()) {
            vocabContext.recentNewWords.joinToString(", ")
        } else {
            "(none)"
        }

        val masteredList = if (vocabContext.masteredWords.isNotEmpty()) {
            vocabContext.masteredWords.take(20).joinToString(", ")
        } else {
            "(none)"
        }

        return PromptTemplate(vocabularyWithTrackingTemplate).render(mapOf(
            "totalWordCount" to vocabContext.totalWordCount.toString(),
            "wordsForReinforcement" to reinforcementList,
            "recentNewWords" to recentList,
            "masteredWords" to masteredList
        ))
    }

    private fun buildTeachingStyleGuidance(teachingStyle: TeachingStyle, targetLanguage: String): String {
        val template = when (teachingStyle) {
            TeachingStyle.Reactive -> teachingStyleReactiveTemplate
            TeachingStyle.Guided -> teachingStyleGuidedTemplate
            TeachingStyle.Directive -> teachingStyleDirectiveTemplate
        }
        return PromptTemplate(template).render(mapOf("targetLanguage" to targetLanguage))
    }

    private fun buildCourseTeachingStyleGuidance(teachingStyle: TeachingStyle, targetLanguage: String): String {
        val template = when (teachingStyle) {
            TeachingStyle.Reactive -> courseTeachingStyleReactiveTemplate
            TeachingStyle.Guided -> courseTeachingStyleGuidedTemplate
            TeachingStyle.Directive -> courseTeachingStyleDirectiveTemplate
        }
        return PromptTemplate(template).render(mapOf("targetLanguage" to targetLanguage))
    }

    private fun getTeachingStyleDescription(teachingStyle: TeachingStyle): String {
        return when (teachingStyle) {
            TeachingStyle.Reactive -> "Follow the learner's conversational lead within lesson content, allowing natural topic flow while staying within lesson scope"
            TeachingStyle.Guided -> "Provide strategic prompts and discovery questions related to lesson content, guiding learners to notice patterns"
            TeachingStyle.Directive -> "Give explicit instruction and structured lessons, with clear guidance on lesson objectives and exercises"
        }
    }

    private fun buildLanguageMetadataPrompt(languageCode: String): String {
        val metadata = supportedLanguages[languageCode]
        return if (metadata != null) {
            "Target Language Difficulty: ${metadata.difficulty.name}"
        } else {
            "" // Fallback for languages not in catalog
        }
    }

    private fun buildLessonContextPrompt(lesson: LessonContent, curriculum: CourseCurriculum?): String = buildString {
        // Course Overview (if curriculum available)
        if (curriculum != null) {
            append("=== Course Overview ===\n")
            append("You are teaching from a structured course with ${curriculum.lessons.size} lessons.\n\n")
            append("Full Course Curriculum:\n")
            curriculum.lessons.forEachIndexed { index, lessonMeta ->
                // Load lesson title for each lesson in the curriculum
                val lessonContent = lessonContentService.getLesson(curriculum.courseId, lessonMeta.id)
                val lessonTitle = lessonContent?.title ?: lessonMeta.id
                val marker = if (lessonMeta.id == lesson.id) " <-- CURRENT LESSON" else ""
                append("${index + 1}. $lessonTitle$marker\n")
            }
            append("\n")
        }

        append("=== This Week's Lesson ===\n")
        append("Lesson: ${lesson.title}\n")
        if (lesson.weekNumber != null) {
            append("Week: ${lesson.weekNumber}\n")
        }
        append("CEFR Level: ${lesson.targetCEFR.name}\n")
        append("Focus Areas: ${lesson.focusAreas.joinToString(", ")}\n\n")

        append("Goals:\n")
        lesson.goals.forEach { goal ->
            append("- $goal\n")
        }
        append("\n")

        if (lesson.grammarPoints.isNotEmpty()) {
            append("Grammar Focus:\n")
            lesson.grammarPoints.forEach { grammar ->
                append("- ${grammar.title}: ${grammar.rule}\n")
                if (grammar.examples.isNotEmpty()) {
                    append("  Examples: ${grammar.examples.joinToString("; ")}\n")
                }
            }
            append("\n")
        }

        if (lesson.essentialVocabulary.isNotEmpty()) {
            append("Essential Vocabulary:\n")
            lesson.essentialVocabulary.take(20).forEach { vocab ->
                append("- ${vocab.word} (${vocab.translation})\n")
            }
            if (lesson.essentialVocabulary.size > 20) {
                append("... and ${lesson.essentialVocabulary.size - 20} more\n")
            }
            append("\n")
        }

        if (lesson.practicePatterns.isNotEmpty()) {
            append("Practice Patterns:\n")
            lesson.practicePatterns.forEach { pattern ->
                append("- $pattern\n")
            }
            append("\n")
        }

        if (lesson.commonMistakes.isNotEmpty()) {
            append("Common Mistakes to Watch:\n")
            lesson.commonMistakes.forEach { mistake ->
                append("- $mistake\n")
            }
            append("\n")
        }

        append(lessonPrompt)
    }

    internal fun buildConsolidatedSystemPrompt(
        tutor: Tutor,
        conversationState: ConversationState,
        phaseReason: String,
        topicEligibilityStatus: String,
        pastTopics: List<String>,
        targetLanguage: String,
        targetLanguageCode: String,
        sourceLanguage: String,
        sourceLanguageCode: String,
        vocabularyGuidance: String,
        teachingStyleGuidance: String,
        courseTeachingStyleGuidance: String,
        currentLesson: LessonContent? = null,
        curriculum: CourseCurriculum? = null
    ): String = buildString {
        // Base system prompt (role, persona, languages)
        append(PromptTemplate(systemPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "targetLanguageCode" to targetLanguageCode,
            "sourceLanguage" to sourceLanguage,
            "sourceLanguageCode" to sourceLanguageCode,
            "tutorName" to tutor.name,
            "tutorGender" to (tutor.gender?.name ?: "Neutral"),
            "tutorPersona" to tutor.persona,
            "tutorDomain" to tutor.domain,
            "vocabularyGuidance" to vocabularyGuidance,
            "teachingStyleGuidance" to teachingStyleGuidance,
            "courseTeachingStyleGuidance" to courseTeachingStyleGuidance,
        )))

        val levelPromptTemplate = levelToPromptTemplateMap[conversationState.estimatedCEFRLevel]
        append(PromptTemplate(levelPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "targetLanguageCode" to targetLanguageCode,
            "sourceLanguage" to sourceLanguage,
            "sourceLanguageCode" to sourceLanguageCode,
            "tutorName" to tutor.name,
            "tutorPersona" to tutor.persona,
            "tutorDomain" to tutor.domain,
            "vocabularyGuidance" to vocabularyGuidance,
            "teachingStyleGuidance" to teachingStyleGuidance
        )))

        append("\n\n")

        // Phase-specific behavior
        val phasePrompt = when (conversationState.phase) {
            ConversationPhase.Free -> phaseFreePromptTemplate
            ConversationPhase.Correction -> phaseCorrectionPromptTemplate
            ConversationPhase.Drill -> phaseDrillPromptTemplate
            ConversationPhase.Auto -> phaseCorrectionPromptTemplate // Should never happen (resolved in ChatService)
        }
        append(PromptTemplate(phasePrompt).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        )))

        append("\n\n")

        // Error classification decision tree (mandatory algorithm)
        append(errorClassificationGuidance)

        append("\n\n")

        // Developer rules (JSON schema)
        append(PromptTemplate(developerPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        )))

        append("\n\n")

        // Lesson Context (if course-based session)
        if (currentLesson != null) {
            append(buildLessonContextPrompt(currentLesson, curriculum))
            append("\n\n")
        }

        // Session Context (structured, not toString())
        append("=== Current Session Context ===\n")
        append("Phase: ${conversationState.phase.name} ($phaseReason)\n")
        append("CEFR Level: ${conversationState.estimatedCEFRLevel.name}\n")
        append("Topic: ${conversationState.currentTopic ?: "Free conversation"} ($topicEligibilityStatus)\n")
        if (pastTopics.isNotEmpty()) {
            append("Recent Topics: ${pastTopics.takeLast(3).joinToString(", ")}\n")
        }

        // Vocabulary review mode guidance
        if (conversationState.vocabularyReviewMode && conversationState.dueVocabularyCount != null && conversationState.dueVocabularyCount > 0) {
            append("\nVocabulary Review Mode: ACTIVE\n")
            append("Due for Review: ${conversationState.dueVocabularyCount} words\n")
            append("Guidance: Naturally integrate 2-3 due vocabulary words into the conversation. Ask the learner to use them, or prompt recall (e.g., 'Do you remember the word for...'). Keep it conversational, not quiz-like.\n")
        }

        // Tutor-initiated message guidance
        if (conversationState.initiationContext != null) {
            append("\n=== Tutor-Initiated Message ===\n")
            when (conversationState.initiationContext) {
                "welcome" -> {
                    val learningContext = if (currentLesson != null) {
                        "the course"
                    } else {
                        "this conversation session"
                    }
                    val lessonContext = if (currentLesson != null) {
                        "First Lesson: ${currentLesson.title}\nFocus Areas: ${currentLesson.focusAreas.joinToString(", ")}"
                    } else {
                        "Free conversation - no structured lesson"
                    }
                    append(PromptTemplate(initiateWelcomeTemplate).render(mapOf(
                        "learningContext" to learningContext,
                        "lessonContext" to lessonContext,
                        "tutorName" to tutor.name,
                        "tutorPersona" to tutor.persona,
                        "targetLanguage" to targetLanguage,
                        "sourceLanguage" to sourceLanguage
                    )))
                }
                "reengage" -> {
                    val topicContext = if (conversationState.currentTopic != null) {
                        "Previous topic: ${conversationState.currentTopic}"
                    } else {
                        "No previous topic recorded"
                    }
                    append(PromptTemplate(initiateReengageTemplate).render(mapOf(
                        "topicContext" to topicContext,
                        "targetLanguage" to targetLanguage,
                        "sourceLanguage" to sourceLanguage
                    )))
                }
                "reengage-light" -> {
                    append(PromptTemplate(initiateReengageLightTemplate).render(mapOf(
                        "targetLanguage" to targetLanguage
                    )))
                }
                else -> {
                    append("Context: Tutor-initiated message (type: ${conversationState.initiationContext})\n")
                    append("Task: Start the conversation naturally and engage the learner.\n")
                }
            }
        }

        append("\n")

        // Language metadata
        append(buildLanguageMetadataPrompt(targetLanguageCode))
    }

    // Helper: Map session to course slug for file system lookup
    private fun getCourseSlug(session: ChatSessionEntity): String? {
        val courseTemplateId = session.courseTemplateId ?: return null
        val course = catalogService.getCourseById(courseTemplateId) ?: return null

        // Parse English name from JSON
        val nameEnglish = try {
            val nameMap = objectMapper.readValue<Map<String, String>>(course.nameJson)
            nameMap["en"] ?: "unknown"
        } catch (e: Exception) {
            logger.warn("Failed to parse course name JSON: ${course.nameJson}", e)
            "unknown"
        }

        // Generate slug from language code (ISO part only) and course name
        // Example: "es-ES" + "Conversational Spanish" -> "es-conversational-spanish"
        // Extract language part (before hyphen) to match filesystem structure
        val languageOnly = course.languageCode.lowercase().substringBefore("-")
        return "$languageOnly-${nameEnglish.lowercase().replace(" ", "-")}"
    }
}