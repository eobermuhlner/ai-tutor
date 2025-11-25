package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

/**
 * Configuration for metadata evaluation intervals.
 * Controls how often different metadata fields are re-evaluated.
 */
@ConfigurationProperties(prefix = "ai-tutor.metadata-evaluation")
class MetadataEvaluationConfig {
    var cefrLevelInterval: Int = 10      // Evaluate CEFR level every N turns
    var topicInterval: Int = 5           // Evaluate topic every N turns
    var phaseInterval: Int = 5           // Evaluate phase every N turns
    var lessonCheckInterval: Int = 3     // Check lesson progression every N turns (also evaluates goals)
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
    private val lessonGoalsEvaluationService: ch.obermuhlner.aitutor.lesson.service.LessonGoalsEvaluationService,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val objectMapper: ObjectMapper
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
            lessonGoalsEvaluationService.evaluateLessonGoals(session, messages)
            lessonProgressionService.checkAndProgressLesson(sessionId)

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
    @Suppress("UNUSED_PARAMETER")
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

        @Suppress("UNUSED_VARIABLE")
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
