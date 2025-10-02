package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import org.springframework.stereotype.Service

@Service
class TopicDecisionService {
    /**
     * Decides the current conversation topic based on recent message history.
     *
     * Topic detection logic:
     * - Analyzes recent messages to detect topic patterns
     * - Returns null for free/unstructured conversation
     * - Returns topic string when a clear topic is sustained
     * - Detects topic transitions based on content shifts
     *
     * Strategy:
     * - Look at last 5-10 messages for context
     * - Detect keywords, themes, and semantic coherence
     * - Topic typically spans 3-10 message exchanges
     * - Major vocabulary/context shift signals topic change
     * - User explicitly stating new topic overrides
     *
     * For now, this is a placeholder implementation.
     * Future: Could use LLM-based topic extraction or semantic analysis
     */
    fun decideTopic(
        currentTopic: String?,
        recentMessages: List<ChatMessageEntity>
    ): String? {
        // Placeholder implementation - returns current topic unchanged
        // In a real implementation, we would:
        // 1. Analyze message content for topic indicators
        // 2. Detect topic transitions (semantic shift)
        // 3. Extract topic names from conversation context
        // 4. Consider message frequency and engagement
        // 5. Use NLP or LLM to classify/extract topics

        if (recentMessages.size < 3) {
            // Not enough context to determine topic
            return null
        }

        // For now, maintain current topic
        // This allows manual topic setting to work
        return currentTopic
    }

    /**
     * Determines if a topic should be archived to past topics.
     * Called when topic changes.
     */
    fun shouldArchiveTopic(topic: String?, messagesSinceTopicStart: Int): Boolean {
        if (topic == null) return false

        // Archive if topic lasted at least 3 message exchanges
        return messagesSinceTopicStart >= 6 // 3 exchanges = 6 messages (user + assistant)
    }

    /**
     * Extracts topic from explicit user intent.
     * E.g., "Let's talk about cooking" -> "cooking"
     */
    fun extractExplicitTopic(userMessage: String): String? {
        // Placeholder: Simple pattern matching
        // Future: Use NLP/LLM for better extraction

        val talkAboutPattern = """(?i)let'?s?\s+talk\s+about\s+(\w+)""".toRegex()
        val matchResult = talkAboutPattern.find(userMessage)

        return matchResult?.groupValues?.getOrNull(1)
    }
}
