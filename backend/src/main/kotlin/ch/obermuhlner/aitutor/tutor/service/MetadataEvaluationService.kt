package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.util.*

/**
 * Configuration for metadata evaluation intervals.
 * Controls how often different metadata fields are re-evaluated.
 */
@ConfigurationProperties(prefix = "ai-tutor.metadata-evaluation")
class MetadataEvaluationConfig {
    var cefrLevelInterval: Int = 10      // Evaluate CEFR level every N turns
    var topicInterval: Int = 5           // Evaluate topic every N turns
    var phaseInterval: Int = 5           // Evaluate phase every N turns
    var lessonCheckInterval: Int = 3     // Check lesson progression every N turns
}

/**
 * Service for periodic background evaluation of session metadata.
 *
 * This service decouples metadata updates from the LLM conversation flow,
 * evaluating fields like CEFR level, topic, and phase at configurable intervals
 * rather than on every message.
 *
 * Benefits:
 * - Simplified system prompts (LLM doesn't manage metadata)
 * - Automatic hysteresis (periodic evaluation prevents rapid changes)
 * - Reduced token costs (smaller LLM response schema)
 * - Clearer separation of concerns (teaching vs. session management)
 *
 * Similar to how corrections were refactored into CorrectionService, this service
 * manages session state independently from the main tutoring conversation.
 */
@Service
class MetadataEvaluationService(
    private val config: MetadataEvaluationConfig,
    private val phaseDecisionService: PhaseDecisionService,
    private val topicDecisionService: TopicDecisionService,
    private val lessonProgressionService: LessonProgressionService,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val lessonContentService: ch.obermuhlner.aitutor.lesson.service.LessonContentService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val userChatModelFactory: ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory,
    private val languageService: ch.obermuhlner.aitutor.language.service.LanguageService,
    private val objectMapper: ObjectMapper,
    @Value("\${ai-tutor.prompts.lesson-goals-evaluation}") private val lessonGoalsEvaluationPrompt: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluates session metadata if appropriate intervals have been reached.
     * Called after each message is sent to check if any metadata should be updated.
     *
     * @param sessionId The chat session to evaluate
     * @param messageHistory Recent messages for context (optional, will be fetched if null)
     */
    fun evaluateIfNeeded(
        sessionId: UUID,
        messageHistory: List<ChatMessageEntity>? = null
    ) {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: run {
            logger.warn("Session $sessionId not found for metadata evaluation")
            return
        }

        val messages = messageHistory ?: chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        val turnCount = messages.count { it.role == MessageRole.USER }

        var sessionUpdated = false

        // Evaluate phase if interval reached
        if (shouldEvaluatePhase(turnCount)) {
            sessionUpdated = evaluatePhase(session, messages) || sessionUpdated
        }

        // Evaluate CEFR level if interval reached
        if (shouldEvaluateCEFRLevel(turnCount)) {
            sessionUpdated = evaluateCEFRLevel(session, messages, turnCount) || sessionUpdated
        }

        // Evaluate topic if interval reached
        if (shouldEvaluateTopic(turnCount)) {
            sessionUpdated = evaluateTopic(session, messages) || sessionUpdated
        }

        // Check lesson progression if interval reached and session is course-based
        if (session.courseTemplateId != null && shouldCheckLessonProgression(turnCount)) {
            // LessonProgressionService handles its own persistence
            lessonProgressionService.checkAndProgressLesson(sessionId)

            // After checking progression, evaluate lesson goals completion if we're still on the same lesson
            // (if lesson didn't advance, we should update goalsCompleted field)
            if (turnCount >= 5 && turnCount % 5 == 0) { // Evaluate every 5 turns, starting at turn 5
                evaluateLessonGoals(session, messages)
            }
        }

        // Save session if any metadata was updated
        if (sessionUpdated) {
            chatSessionRepository.save(session)
            logger.info("Session $sessionId metadata updated at turn $turnCount")
        }
    }

    /**
     * Evaluates and updates the conversation phase based on error patterns.
     * Only updates if conversationPhase is Auto mode.
     */
    private fun evaluatePhase(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>
    ): Boolean {
        // Only auto-decide if user has selected Auto mode
        if (session.conversationPhase != ConversationPhase.Auto) {
            // Keep effective phase in sync with user preference
            if (session.effectivePhase != session.conversationPhase) {
                session.effectivePhase = session.conversationPhase
                return true
            }
            return false
        }

        val phaseDecision = phaseDecisionService.decidePhase(
            currentPhase = ConversationPhase.Auto,
            recentMessages = messages
        )

        // Update effective phase and reason if changed
        var updated = false
        if (session.effectivePhase != phaseDecision.phase) {
            logger.info(
                "Session ${session.id} phase changed: ${session.effectivePhase} -> ${phaseDecision.phase} " +
                "(reason: ${phaseDecision.reason})"
            )
            session.effectivePhase = phaseDecision.phase
            updated = true
        }

        // Always update phase reason (context may change even if phase stays same)
        if (session.phaseReason != phaseDecision.reason) {
            session.phaseReason = phaseDecision.reason
            updated = true
        }

        return updated
    }

    /**
     * Evaluates and updates the estimated CEFR level.
     * Uses a simple heuristic based on error patterns and conversation complexity.
     */
    private fun evaluateCEFRLevel(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>,
        turnCount: Int
    ): Boolean {
        // Simple heuristic: analyze recent performance
        // In a real implementation, this could use more sophisticated analysis
        // For now, we'll keep the current level stable unless there's clear evidence of change

        // TODO: Implement CEFR level evaluation logic
        // This could analyze:
        // - Error frequency and severity
        // - Vocabulary complexity
        // - Sentence structure complexity
        // - Comprehension demonstrated in responses

        // For now, just log that evaluation occurred
        logger.debug("CEFR level evaluation at turn $turnCount for session ${session.id} (current: ${session.estimatedCEFRLevel})")

        return false // No changes for now
    }

    /**
     * Evaluates and updates the current conversation topic.
     * Uses TopicDecisionService to validate topic changes with hysteresis.
     */
    private fun evaluateTopic(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>
    ): Boolean {
        // For topic evaluation, we need to extract the proposed topic from conversation content
        // This is a simplified version - in practice, might use LLM to extract topic

        val recentUserMessages = messages
            .filter { it.role == MessageRole.USER }
            .takeLast(5)

        // Simple heuristic: keep current topic for now
        // In full implementation, could analyze message content to detect topic shifts
        val proposedTopic = session.currentTopic

        val topicDecision = topicDecisionService.decideTopic(
            currentTopic = session.currentTopic,
            llmProposedTopic = proposedTopic,
            recentMessages = messages,
            pastTopicsJson = session.pastTopicsJson
        )

        var updated = false

        // Update topic if changed
        if (session.currentTopic != topicDecision.topic) {
            // Archive old topic if it lasted long enough
            if (topicDecisionService.shouldArchiveTopic(session.currentTopic, topicDecision.turnCount)) {
                val pastTopics = topicDecision.pastTopics.toMutableList()
                session.currentTopic?.let { pastTopics.add(it) }
                session.pastTopicsJson = objectMapper.writeValueAsString(pastTopics)
            }

            logger.info(
                "Session ${session.id} topic changed: '${session.currentTopic}' -> '${topicDecision.topic}' " +
                "(status: ${topicDecision.eligibilityStatus})"
            )
            session.currentTopic = topicDecision.topic
            updated = true
        }

        // Always update topic eligibility status (context may change even if topic stays same)
        if (session.topicEligibilityStatus != topicDecision.eligibilityStatus) {
            session.topicEligibilityStatus = topicDecision.eligibilityStatus
            updated = true
        }

        return updated
    }

    /**
     * Evaluates lesson goals completion using LLM analysis.
     * Updates the goalsCompleted field in lessonProgressJson.
     */
    private fun evaluateLessonGoals(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>
    ) {
        // Only evaluate if there's an active lesson
        if (session.currentLessonId == null || session.courseTemplateId == null) {
            return
        }

        try {
            // Get course slug for lesson lookup
            val course = catalogService.getCourseById(session.courseTemplateId!!) ?: return
            val nameMap = objectMapper.readValue<Map<String, String>>(course.nameJson)
            val courseNameEn = nameMap["en"] ?: "unknown"
            val languageOnly = course.languageCode.lowercase().substringBefore("-")
            val courseSlug = "$languageOnly-${courseNameEn.lowercase().replace(" ", "-")}"

            // Get lesson content to extract goals
            val lessonContent = lessonContentService.getLesson(courseSlug, session.currentLessonId!!) ?: return

            // Extract goals section from lesson markdown
            val goalsSection = extractLessonGoals(lessonContent.fullMarkdown)
            if (goalsSection.isBlank()) {
                logger.debug("No goals section found in lesson ${session.currentLessonId}")
                return
            }

            // Get recent conversation context (last 10 messages)
            val recentMessages = messages.takeLast(10)
            val conversationContext = recentMessages.joinToString("\n") { msg ->
                "${msg.role.name}: ${msg.content}"
            }

            // Get language names
            val targetLanguage = languageService.getLanguageName(session.targetLanguageCode)
            val sourceLanguage = languageService.getLanguageName(session.sourceLanguageCode)

            // Render prompt template with placeholders
            val renderedPrompt = org.springframework.ai.chat.prompt.PromptTemplate(lessonGoalsEvaluationPrompt).render(mapOf(
                "targetLanguage" to targetLanguage,
                "targetLanguageCode" to session.targetLanguageCode,
                "sourceLanguage" to sourceLanguage,
                "sourceLanguageCode" to session.sourceLanguageCode,
                "cefrLevel" to session.estimatedCEFRLevel.name,
                "lessonGoals" to goalsSection,
                "conversationContext" to conversationContext
            ))

            // Get chat model for this user
            val chatModel = userChatModelFactory.getChatModelForUser(session.userId)

            // Call LLM
            val response = chatModel.call(renderedPrompt)

            // Parse response
            val jsonResponse = extractJsonFromResponse(response)
            val resultMap = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            val goalsCompleted = resultMap["goalsCompleted"] as? Boolean ?: false
            val reasoning = resultMap["reasoning"] as? String ?: "No reasoning provided"

            // Update session lessonProgressJson
            val progressJson = session.lessonProgressJson ?: """{"turnCount": 0, "goalsCompleted": false}"""
            val progressMap = objectMapper.readValue<MutableMap<String, Any>>(progressJson)
            progressMap["goalsCompleted"] = goalsCompleted
            session.lessonProgressJson = objectMapper.writeValueAsString(progressMap)
            chatSessionRepository.save(session)

            logger.info("Session ${session.id} lesson goals evaluated: $goalsCompleted - $reasoning")

        } catch (e: Exception) {
            logger.error("Failed to evaluate lesson goals for session ${session.id}", e)
        }
    }

    /**
     * Extracts the "This Week's Goals" or "Lesson Goals" section from lesson markdown.
     */
    private fun extractLessonGoals(markdown: String): String {
        val lines = markdown.lines()
        val goalsSectionStart = lines.indexOfFirst {
            it.trim().startsWith("##") &&
            (it.contains("Goal", ignoreCase = true) || it.contains("Objective", ignoreCase = true))
        }

        if (goalsSectionStart == -1) {
            return ""
        }

        // Find next section (starting with ##)
        val nextSectionStart = lines.subList(goalsSectionStart + 1, lines.size)
            .indexOfFirst { it.trim().startsWith("##") }

        val goalsSectionEnd = if (nextSectionStart == -1) {
            lines.size
        } else {
            goalsSectionStart + 1 + nextSectionStart
        }

        return lines.subList(goalsSectionStart, goalsSectionEnd).joinToString("\n").trim()
    }

    /**
     * Extracts JSON from LLM response, handling markdown code blocks.
     */
    private fun extractJsonFromResponse(response: String): String {
        val trimmed = response.trim()

        // Check if response is wrapped in markdown code block
        if (trimmed.startsWith("```")) {
            val lines = trimmed.lines()
            val contentLines = lines.drop(1).dropLast(1) // Remove first and last lines
            return contentLines.joinToString("\n").trim()
        }

        return trimmed
    }

    private fun shouldEvaluatePhase(turnCount: Int): Boolean {
        return turnCount > 0 && turnCount % config.phaseInterval == 0
    }

    private fun shouldEvaluateCEFRLevel(turnCount: Int): Boolean {
        return turnCount >= config.cefrLevelInterval && turnCount % config.cefrLevelInterval == 0
    }

    private fun shouldEvaluateTopic(turnCount: Int): Boolean {
        return turnCount > 0 && turnCount % config.topicInterval == 0
    }

    private fun shouldCheckLessonProgression(turnCount: Int): Boolean {
        return turnCount > 0 && turnCount % config.lessonCheckInterval == 0
    }
}
