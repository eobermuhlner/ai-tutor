package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.core.model.Correction
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class PhaseDecisionService(
    private val objectMapper: ObjectMapper
) {
    /**
     * Decides the conversation phase based on recent message history.
     *
     * Three-phase logic:
     * - Free: Pure fluency, no error tracking (first few messages or very low errors)
     * - Correction: Balanced default - track errors for UI hover but don't mention them
     * - Drill: Explicit error work (high error frequency or repeated patterns)
     *
     * Logic:
     * - Start with Correction phase (balanced default)
     * - Switch to Drill if 3+ repeated errors or 5+ total errors in last 5 messages
     * - Switch to Free if consistently low errors (0-1 in last 3 messages)
     * - Return to Correction after successful drill work (2+ low-error messages)
     */
    fun decidePhase(
        currentPhase: ConversationPhase,
        recentMessages: List<ChatMessageEntity>
    ): ConversationPhase {
        // If manually set to Free, Correction, or Drill, this shouldn't be called
        // But handle gracefully anyway
        if (currentPhase != ConversationPhase.Auto) {
            return currentPhase
        }

        val userMessages = recentMessages.filter { it.role == MessageRole.USER }

        // Start with Correction phase (balanced default)
        if (userMessages.size < 3) {
            return ConversationPhase.Correction
        }

        // Look at last 5 user messages
        val recentUserMessages = userMessages.takeLast(5)

        // Count errors in each message
        val errorCounts = recentUserMessages.map { message ->
            countErrorsInMessage(message, recentMessages)
        }

        val totalErrors = errorCounts.sum()
        val lastThreeErrors = errorCounts.takeLast(3).sum()
        val repeatedErrors = countRepeatedErrors(recentUserMessages, recentMessages)

        // Switch to Drill if high error frequency or fossilization risk
        if (repeatedErrors >= 3 || totalErrors >= 5) {
            return ConversationPhase.Drill
        }

        // Switch to Free if very low errors consistently (confidence building)
        if (lastThreeErrors <= 1 && totalErrors <= 2) {
            return ConversationPhase.Free
        }

        // Default to Correction (balanced middle ground)
        return ConversationPhase.Correction
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
}
