package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

/**
 * Decision metadata returned by phase decision logic.
 * Contains the selected phase plus context for LLM prompts.
 */
data class PhaseDecision(
    val phase: ConversationPhase,
    val reason: String,
    val severityScore: Double,
    val fossilizationDetected: Boolean = false,
    val repeatedErrorTypes: List<String> = emptyList()
)

@Service
class PhaseDecisionService(
    private val objectMapper: ObjectMapper
) {
    /**
     * Decides the conversation phase based on recent message history.
     *
     * Three-phase logic using severity-weighted scoring with fossilization detection:
     * - Free: Pure fluency, no error tracking (very low severity score)
     * - Correction: Balanced default - track errors for UI hover but don't mention them
     * - Drill: Explicit error work (high severity score, critical errors, or fossilization)
     *
     * Severity Weights (with fossilization escalation):
     * - Critical: 3.0 (blocks comprehension entirely)
     * - High: 2.0 (global errors, significant barrier)
     * - Medium: 1.0 (grammar issues, meaning clear)
     * - Low: 0.3 (minor/chat-acceptable issues)
     * - Fossilization multiplier: 1.5x for 2nd occurrence, 2.0x for 3rd+ occurrence
     *
     * Logic:
     * - Start with Correction phase (balanced default)
     * - Switch to Drill if:
     *   * 2+ Critical errors in last 3 messages, OR
     *   * 3+ High-severity repeated errors (fossilization risk), OR
     *   * Weighted severity score >= 4.5 in last 5 messages (lowered for repeated errors)
     * - Switch to Free if:
     *   * Only Low-severity errors in last 3 messages, AND
     *   * Weighted severity score < 1.0 in last 5 messages
     * - Default: Correction (balanced middle ground)
     */
    fun decidePhase(
        currentPhase: ConversationPhase,
        recentMessages: List<ChatMessageEntity>
    ): PhaseDecision {
        // If manually set to Free, Correction, or Drill, return as-is
        if (currentPhase != ConversationPhase.Auto) {
            return PhaseDecision(
                phase = currentPhase,
                reason = "User-selected phase",
                severityScore = 0.0
            )
        }

        val userMessages = recentMessages.filter { it.role == MessageRole.USER }

        // Start with Correction phase (balanced default)
        if (userMessages.size < 3) {
            return PhaseDecision(
                phase = ConversationPhase.Correction,
                reason = "Balanced default - insufficient history",
                severityScore = 0.0
            )
        }

        // Look at last 5 user messages
        val recentUserMessages = userMessages.takeLast(5)
        val lastThreeMessages = recentUserMessages.takeLast(3)

        // Detect fossilization: count how many times each error type repeats
        val errorTypeCounts = countErrorTypeOccurrences(recentUserMessages, recentMessages)
        val fossilizedErrors = errorTypeCounts.filter { it.value >= 2 }
        val fossilizationDetected = fossilizedErrors.isNotEmpty()

        // Calculate severity-weighted scores WITH fossilization escalation
        val totalSeverityScore = calculateSeverityScoreWithFossilization(recentUserMessages, recentMessages)

        // Count critical and high-severity errors
        val criticalErrorsInLastThree = countErrorsBySeverity(lastThreeMessages, recentMessages, ch.obermuhlner.aitutor.core.model.ErrorSeverity.Critical)
        val highSeverityRepeatedErrors = countRepeatedErrorsBySeverity(recentUserMessages, recentMessages, ch.obermuhlner.aitutor.core.model.ErrorSeverity.High)

        // Check if only low-severity errors in last 3
        val onlyLowSeverityInLastThree = hasOnlyLowSeverityErrors(lastThreeMessages, recentMessages)

        // Switch to Drill if serious comprehension issues or fossilization risk
        // Lowered threshold from 6.0 to 4.5 to catch fossilization earlier
        if (criticalErrorsInLastThree >= 2 || highSeverityRepeatedErrors >= 3 || totalSeverityScore >= 4.5) {
            val reasonParts = mutableListOf<String>()
            if (fossilizationDetected) {
                reasonParts.add("Fossilization risk detected (${fossilizedErrors.keys.joinToString(", ")})")
            }
            reasonParts.add("severity score: %.1f".format(totalSeverityScore))

            return PhaseDecision(
                phase = ConversationPhase.Drill,
                reason = "${reasonParts.joinToString(", ")} - explicit practice needed",
                severityScore = totalSeverityScore,
                fossilizationDetected = fossilizationDetected,
                repeatedErrorTypes = fossilizedErrors.keys.toList()
            )
        }

        // Switch to Free if very low severity consistently (confidence building)
        if (onlyLowSeverityInLastThree && totalSeverityScore < 1.0) {
            return PhaseDecision(
                phase = ConversationPhase.Free,
                reason = "Low error rate (score: %.1f) - building fluency confidence".format(totalSeverityScore),
                severityScore = totalSeverityScore
            )
        }

        // Default to Correction (balanced middle ground)
        return PhaseDecision(
            phase = ConversationPhase.Correction,
            reason = "Balanced approach - tracking errors for learner awareness",
            severityScore = totalSeverityScore
        )
    }

    private fun countErrorsInMessage(
        userMessage: ChatMessageEntity,
        allMessages: List<ChatMessageEntity>
    ): Int {
        // Find the assistant's response to this user message
        val userIndex = allMessages.indexOf(userMessage)
        if (userIndex == -1 || userIndex >= allMessages.size - 1) {
            return 0
        }

        val assistantResponse = allMessages[userIndex + 1]
        if (assistantResponse.role != MessageRole.ASSISTANT) {
            return 0
        }

        // Parse corrections from assistant message
        val correctionsJson = assistantResponse.correctionsJson ?: return 0

        return try {
            val corrections: List<Correction> = objectMapper.readValue(correctionsJson)
            corrections.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Detects repeated errors across messages (fossilization risk).
     * Returns count of error types that appear in multiple messages.
     */
    private fun countRepeatedErrors(
        recentUserMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>
    ): Int {
        // Collect all error types from recent messages
        val errorTypesByMessage = recentUserMessages.map { userMessage ->
            getErrorTypes(userMessage, allMessages)
        }

        // Find error types that appear in multiple messages
        val allErrorTypes = errorTypesByMessage.flatten()
        val errorTypeCounts = allErrorTypes.groupingBy { it }.eachCount()

        // Count how many error types appear 2+ times (repeated)
        return errorTypeCounts.values.count { it >= 2 }
    }

    private fun getErrorTypes(
        userMessage: ChatMessageEntity,
        allMessages: List<ChatMessageEntity>
    ): List<String> {
        val userIndex = allMessages.indexOf(userMessage)
        if (userIndex == -1 || userIndex >= allMessages.size - 1) {
            return emptyList()
        }

        val assistantResponse = allMessages[userIndex + 1]
        if (assistantResponse.role != MessageRole.ASSISTANT) {
            return emptyList()
        }

        val correctionsJson = assistantResponse.correctionsJson ?: return emptyList()

        return try {
            val corrections: List<Correction> = objectMapper.readValue(correctionsJson)
            corrections.map { it.errorType.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculate weighted severity score for user messages.
     * Critical: 3.0, High: 2.0, Medium: 1.0, Low: 0.3
     */
    private fun calculateSeverityScore(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>
    ): Double {
        return userMessages.sumOf { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections.sumOf { correction ->
                when (correction.severity) {
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Critical -> 3.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.High -> 2.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Medium -> 1.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Low -> 0.3
                }
            }
        }
    }

    /**
     * Count how many times each error type appears across recent messages.
     * Returns a map of errorType -> occurrence count.
     */
    private fun countErrorTypeOccurrences(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>
    ): Map<String, Int> {
        val errorTypes = mutableListOf<String>()

        userMessages.forEach { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections.forEach { correction ->
                errorTypes.add(correction.errorType.name)
            }
        }

        return errorTypes.groupingBy { it }.eachCount()
    }

    /**
     * Calculate weighted severity score WITH fossilization escalation.
     * - 1st occurrence: base weight (1.0x)
     * - 2nd occurrence: 1.5x multiplier
     * - 3rd+ occurrence: 2.0x multiplier
     */
    private fun calculateSeverityScoreWithFossilization(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>
    ): Double {
        // Track which errors we've already seen in this iteration
        val errorTypesSeen = mutableMapOf<String, Int>()

        return userMessages.sumOf { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections.sumOf { correction ->
                val errorType = correction.errorType.name

                // Determine fossilization multiplier based on how many times we've seen this error
                errorTypesSeen[errorType] = (errorTypesSeen[errorType] ?: 0) + 1
                val occurrenceNumber = errorTypesSeen[errorType]!!

                val fossilizationMultiplier = when {
                    occurrenceNumber == 1 -> 1.0      // First time: normal weight
                    occurrenceNumber == 2 -> 1.5      // Second time: escalate
                    else -> 2.0                        // Third+ time: strong escalation
                }

                // Base severity weight
                val baseWeight = when (correction.severity) {
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Critical -> 3.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.High -> 2.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Medium -> 1.0
                    ch.obermuhlner.aitutor.core.model.ErrorSeverity.Low -> 0.3
                }

                baseWeight * fossilizationMultiplier
            }
        }
    }

    /**
     * Count errors of a specific severity level.
     */
    private fun countErrorsBySeverity(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>,
        severity: ch.obermuhlner.aitutor.core.model.ErrorSeverity
    ): Int {
        return userMessages.sumOf { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections.count { it.severity == severity }
        }
    }

    /**
     * Count repeated errors of a specific severity level.
     */
    private fun countRepeatedErrorsBySeverity(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>,
        severity: ch.obermuhlner.aitutor.core.model.ErrorSeverity
    ): Int {
        val errorTypesByMessage = userMessages.map { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections
                .filter { it.severity == severity }
                .map { it.errorType.name }
        }

        val allErrorTypes = errorTypesByMessage.flatten()
        val errorTypeCounts = allErrorTypes.groupingBy { it }.eachCount()

        return errorTypeCounts.values.count { it >= 2 }
    }

    /**
     * Check if only low-severity errors exist in messages.
     */
    private fun hasOnlyLowSeverityErrors(
        userMessages: List<ChatMessageEntity>,
        allMessages: List<ChatMessageEntity>
    ): Boolean {
        return userMessages.all { userMessage ->
            val corrections = getCorrections(userMessage, allMessages)
            corrections.isEmpty() || corrections.all {
                it.severity == ch.obermuhlner.aitutor.core.model.ErrorSeverity.Low
            }
        }
    }

    /**
     * Get all corrections for a user message.
     */
    private fun getCorrections(
        userMessage: ChatMessageEntity,
        allMessages: List<ChatMessageEntity>
    ): List<Correction> {
        val userIndex = allMessages.indexOf(userMessage)
        if (userIndex == -1 || userIndex >= allMessages.size - 1) {
            return emptyList()
        }

        val assistantResponse = allMessages[userIndex + 1]
        if (assistantResponse.role != MessageRole.ASSISTANT) {
            return emptyList()
        }

        val correctionsJson = assistantResponse.correctionsJson ?: return emptyList()

        return try {
            objectMapper.readValue(correctionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
