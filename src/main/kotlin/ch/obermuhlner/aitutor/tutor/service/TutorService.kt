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
    private val supportedLanguages: Map<String, LanguageMetadata>,
    @Value("\${ai-tutor.prompts.system}") private val systemPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-free}") private val phaseFreePromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-correction}") private val phaseCorrectionPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-drill}") private val phaseDrillPromptTemplate: String,
    @Value("\${ai-tutor.prompts.developer}") private val developerPromptTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.no-tracking}") private val vocabularyNoTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.with-tracking}") private val vocabularyWithTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.reactive}") private val teachingStyleReactiveTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.guided}") private val teachingStyleGuidedTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.directive}") private val teachingStyleDirectiveTemplate: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
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
        onReplyChunk: (String) -> Unit = { print(it) }
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
        val teachingStyleGuidance = buildTeachingStyleGuidance(tutor.teachingStyle, targetLanguage)

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
            teachingStyleGuidance = teachingStyleGuidance
        )

        val systemMessages = listOf(SystemMessage(consolidatedSystemPrompt))

        // Log metrics for monitoring
        val estimatedTokens = consolidatedSystemPrompt.length / 4
        logger.info("System prompt assembled: 1 message, ~$estimatedTokens tokens (estimated)")
        logger.debug("Phase: ${conversationState.phase.name} ($phaseReason)")
        logger.debug("Topic: ${conversationState.currentTopic ?: "free conversation"} ($topicEligibilityStatus)")

        val compactedMessages = messageCompactionService.compactMessages(systemMessages, messages, sessionId)

        val response = aiChatService.call(AiChatRequest(compactedMessages), onReplyChunk)

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

    private fun buildLanguageMetadataPrompt(languageCode: String): String {
        val metadata = supportedLanguages[languageCode]
        return if (metadata != null) {
            "Target Language Difficulty: ${metadata.difficulty.name}"
        } else {
            "" // Fallback for languages not in catalog
        }
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
        teachingStyleGuidance: String
    ): String = buildString {
        // Base system prompt (role, persona, languages)
        append(PromptTemplate(systemPromptTemplate).render(mapOf(
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

        // Developer rules (JSON schema)
        append(PromptTemplate(developerPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        )))

        append("\n\n")

        // Session Context (structured, not toString())
        append("=== Current Session Context ===\n")
        append("Phase: ${conversationState.phase.name} ($phaseReason)\n")
        append("CEFR Level: ${conversationState.estimatedCEFRLevel.name}\n")
        append("Topic: ${conversationState.currentTopic ?: "Free conversation"} ($topicEligibilityStatus)\n")
        if (pastTopics.isNotEmpty()) {
            append("Recent Topics: ${pastTopics.takeLast(3).joinToString(", ")}\n")
        }

        append("\n")

        // Language metadata
        append(buildLanguageMetadataPrompt(targetLanguageCode))
    }
}