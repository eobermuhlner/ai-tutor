package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

/**
 * Configuration for topic decision hysteresis.
 * Hysteresis prevents topic thrashing by requiring different thresholds for changes.
 */
data class TopicDecisionConfig(
    val minTurnsToEstablish: Int = 2,      // Must engage with topic this long before establishing
    val minTurnsBeforeChange: Int = 3,     // Can't change topic before this many turns
    val maxTurnsForStagnation: Int = 12,   // Encourage change after this many turns
    val recentTopicWindowSize: Int = 3     // Don't revisit topics from last N entries
)

@Service
class TopicDecisionService(
    private val objectMapper: ObjectMapper
) {
    private val config = TopicDecisionConfig()

    /**
     * Validates and decides the current conversation topic based on LLM proposal and message history.
     *
     * This service acts as a filter/validator for the LLM's topic decisions, implementing
     * hysteresis to prevent topic thrashing and ensure stability.
     *
     * The LLM proposes topics via ConversationState.currentTopic in its response.
     * This service validates the proposal against turn counts and past topic history.
     *
     * Hysteresis Rules:
     * 1. Same topic → always accept (no change)
     * 2. Too early to change → reject LLM proposal (enforce stability)
     * 3. Recently discussed topic → reject to prevent repetition
     * 4. Stale topic → accept any change (encourage variety)
     * 5. Establishing new topic → require minimum engagement
     * 6. Return to free conversation → allow if sufficient time
     *
     * @param currentTopic The topic currently stored in the session
     * @param llmProposedTopic The topic the LLM wants to use (from ConversationResponse)
     * @param recentMessages All messages in the session for turn counting
     * @param pastTopicsJson JSON array of past topics from session
     * @return The validated topic to use (may differ from LLM proposal)
     */
    fun decideTopic(
        currentTopic: String?,
        llmProposedTopic: String?,
        recentMessages: List<ChatMessageEntity>,
        pastTopicsJson: String? = null
    ): String? {
        val turnCount = countTurnsInRecentMessages(recentMessages)
        val pastTopics = getPastTopicsFromJson(pastTopicsJson)

        // Rule 1: LLM keeps same topic → always accept
        if (llmProposedTopic == currentTopic) {
            return currentTopic
        }

        // Rule 2: Too early to change topic → reject LLM proposal (hysteresis)
        if (currentTopic != null && turnCount < config.minTurnsBeforeChange) {
            return currentTopic // Enforce stability
        }

        // Rule 3: LLM proposes recently discussed topic → reject to prevent repetition
        if (llmProposedTopic != null &&
            pastTopics.takeLast(config.recentTopicWindowSize).contains(llmProposedTopic)) {
            return currentTopic // Prevent repetition
        }

        // Rule 4: Current topic is getting stale → accept any change from LLM
        if (currentTopic != null && turnCount >= config.maxTurnsForStagnation) {
            return llmProposedTopic // Encourage variety
        }

        // Rule 5: Starting new topic from null → require minimum engagement
        if (currentTopic == null && llmProposedTopic != null) {
            return if (turnCount >= config.minTurnsToEstablish) {
                llmProposedTopic // Establish new topic
            } else {
                null // Stay in free conversation
            }
        }

        // Rule 6: LLM wants to return to free conversation → allow if sufficient time
        if (llmProposedTopic == null && currentTopic != null) {
            return if (turnCount >= config.minTurnsBeforeChange) {
                null // Natural conclusion
            } else {
                currentTopic // Keep topic alive
            }
        }

        // Default: accept LLM proposal
        return llmProposedTopic
    }

    /**
     * Counts the number of conversation turns (user-assistant pairs) in recent messages.
     *
     * @param messages All messages in the conversation
     * @return Number of user messages (approximates turn count)
     */
    fun countTurnsInRecentMessages(messages: List<ChatMessageEntity>): Int {
        // Count user messages as a proxy for turns
        // Each user message typically gets an assistant response (1 turn)
        val recentMessages = messages.takeLast(30) // Look at last 30 messages max
        return recentMessages.count { it.role == MessageRole.USER }
    }

    /**
     * Determines if a topic should be archived to past topics.
     * Called when topic changes.
     *
     * @param topic The topic to potentially archive
     * @param turnCount Number of turns the topic was active
     * @return true if topic should be archived (lasted long enough)
     */
    fun shouldArchiveTopic(topic: String?, turnCount: Int): Boolean {
        if (topic == null) return false

        // Archive if topic lasted at least 3 turns (meaningful engagement)
        return turnCount >= 3
    }

    /**
     * Parses past topics from JSON array string.
     */
    private fun getPastTopicsFromJson(pastTopicsJson: String?): List<String> {
        if (pastTopicsJson == null) return emptyList()

        return try {
            objectMapper.readValue<List<String>>(pastTopicsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Extracts topic from explicit user intent.
     * E.g., "Let's talk about cooking" -> "cooking"
     *
     * Note: This is kept for potential future use but not currently used in main logic.
     * The LLM is responsible for topic detection.
     */
    fun extractExplicitTopic(userMessage: String): String? {
        val talkAboutPattern = """(?i)let'?s?\s+talk\s+about\s+(\w+)""".toRegex()
        val matchResult = talkAboutPattern.find(userMessage)

        return matchResult?.groupValues?.getOrNull(1)
    }
}
