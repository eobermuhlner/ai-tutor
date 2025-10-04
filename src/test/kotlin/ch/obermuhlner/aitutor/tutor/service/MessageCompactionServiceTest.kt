package ch.obermuhlner.aitutor.tutor.service

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.messages.AssistantMessage
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageCompactionServiceTest {

    private val service = MessageCompactionService(
        maxTokens = 100000,
        recentMessageCount = 15
    )

    @Test
    fun `compactMessages should keep all messages when under limit`() {
        val systemMessages = listOf(
            SystemMessage("System prompt 1"),
            SystemMessage("System prompt 2")
        )

        val conversationMessages = listOf(
            UserMessage("Hello"),
            AssistantMessage("Hi there"),
            UserMessage("How are you?"),
            AssistantMessage("I'm doing well, thank you!")
        )

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should have all system messages + all conversation messages
        assertEquals(6, result.size)
        assertTrue(result.take(2).all { it is SystemMessage })
    }

    @Test
    fun `compactMessages should keep only recent messages when limit exceeded`() {
        val systemMessages = listOf(
            SystemMessage("System prompt")
        )

        // Create 20 conversation messages (10 user + 10 assistant)
        val conversationMessages = (1..20).map { i ->
            if (i % 2 == 1) {
                UserMessage("User message $i")
            } else {
                AssistantMessage("Assistant message $i")
            }
        }

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should have 1 system message + up to 15 recent conversation messages
        assertTrue(result.size <= 16)
        assertTrue(result.first() is SystemMessage)

        // Most recent messages should be preserved
        val lastConversationMessage = result.last()
        assertTrue(lastConversationMessage is AssistantMessage)
    }

    @Test
    fun `compactMessages should handle empty conversation`() {
        val systemMessages = listOf(
            SystemMessage("System prompt")
        )

        val conversationMessages = emptyList<org.springframework.ai.chat.messages.Message>()

        val result = service.compactMessages(systemMessages, conversationMessages)

        assertEquals(1, result.size)
        assertTrue(result.first() is SystemMessage)
    }

    @Test
    fun `compactMessages should preserve message order`() {
        val systemMessages = listOf(
            SystemMessage("System 1"),
            SystemMessage("System 2")
        )

        val conversationMessages = listOf(
            UserMessage("First user message"),
            AssistantMessage("First assistant message"),
            UserMessage("Second user message"),
            AssistantMessage("Second assistant message")
        )

        val result = service.compactMessages(systemMessages, conversationMessages)

        // System messages should come first
        assertEquals(6, result.size)
        assertTrue(result[0] is SystemMessage)
        assertTrue(result[1] is SystemMessage)

        // Then conversation messages in order
        assertTrue(result[2] is UserMessage)
        assertTrue(result[3] is AssistantMessage)
        assertTrue(result[4] is UserMessage)
        assertTrue(result[5] is AssistantMessage)
    }

    @Test
    fun `compactMessages should handle very large messages`() {
        val service = MessageCompactionService(
            maxTokens = 1000, // Small limit
            recentMessageCount = 5
        )

        val systemMessages = listOf(
            SystemMessage("Short system prompt")
        )

        // Create messages with lots of text
        val conversationMessages = (1..10).map { i ->
            if (i % 2 == 1) {
                UserMessage("User message $i ".repeat(50)) // ~1000 chars
            } else {
                AssistantMessage("Assistant message $i ".repeat(50)) // ~1000 chars
            }
        }

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should have system message + some subset of recent messages
        assertTrue(result.size >= 1) // At least system message
        assertTrue(result.size <= 6) // System + up to 5 recent
        assertTrue(result.first() is SystemMessage)
    }

    @Test
    fun `compactMessages should keep exactly recentMessageCount when possible`() {
        val service = MessageCompactionService(
            maxTokens = 100000,
            recentMessageCount = 3 // Very small window
        )

        val systemMessages = listOf(
            SystemMessage("System")
        )

        val conversationMessages = (1..10).map { i ->
            UserMessage("Message $i")
        }

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should have 1 system + 3 most recent conversation messages
        assertEquals(4, result.size)

        // Check that we have the last 3 messages
        val conversationPart = result.drop(1)
        assertEquals(3, conversationPart.size)
    }

    @Test
    fun `compactMessages should handle system messages exceeding budget`() {
        val service = MessageCompactionService(
            maxTokens = 10, // Very small limit (about 40 chars = 10 tokens)
            recentMessageCount = 5
        )

        // Create very large system messages that exceed the token budget
        val systemMessages = listOf(
            SystemMessage("This is a very long system prompt that exceeds the token budget ".repeat(5)), // ~300 chars
            SystemMessage("Another long system message ".repeat(5)) // ~150 chars
        )

        val conversationMessages = listOf(
            UserMessage("Short message")
        )

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should only have system messages, no conversation messages
        assertEquals(2, result.size)
        assertTrue(result.all { it is SystemMessage })
    }

    @Test
    fun `compactMessages should handle messages with null text`() {
        val service = MessageCompactionService(
            maxTokens = 100000,
            recentMessageCount = 10
        )

        val systemMessages = listOf(
            SystemMessage("System prompt")
        )

        // Create messages that might have null text (simulated by empty strings)
        val conversationMessages = listOf(
            UserMessage(""),
            AssistantMessage(""),
            UserMessage("Valid message")
        )

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should handle empty messages gracefully
        assertEquals(4, result.size) // 1 system + 3 conversation
        assertTrue(result.first() is SystemMessage)
    }

    @Test
    fun `compactMessages should preserve message types in order`() {
        val service = MessageCompactionService(
            maxTokens = 100000,
            recentMessageCount = 10
        )

        val systemMessages = listOf(
            SystemMessage("System 1"),
            SystemMessage("System 2"),
            SystemMessage("System 3")
        )

        val conversationMessages = listOf(
            UserMessage("User 1"),
            AssistantMessage("Assistant 1"),
            UserMessage("User 2"),
            AssistantMessage("Assistant 2")
        )

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Verify order: all system messages first, then conversation messages
        assertEquals(7, result.size)
        assertTrue(result[0] is SystemMessage)
        assertTrue(result[1] is SystemMessage)
        assertTrue(result[2] is SystemMessage)
        assertTrue(result[3] is UserMessage)
        assertTrue(result[4] is AssistantMessage)
        assertTrue(result[5] is UserMessage)
        assertTrue(result[6] is AssistantMessage)
    }

    @Test
    fun `compactMessages should handle edge case with exactly budget-sized messages`() {
        val service = MessageCompactionService(
            maxTokens = 100, // 100 tokens = ~400 chars
            recentMessageCount = 5
        )

        val systemMessages = listOf(
            SystemMessage("Short system") // ~12 chars = ~3 tokens
        )

        // Create messages that are exactly at the budget boundary
        // Each message ~80 chars = ~20 tokens, 5 messages = ~100 tokens
        val conversationMessages = (1..5).map { i ->
            UserMessage("Message number $i with some additional text to reach the size limit here now")
        }

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should have system message + some conversation messages
        assertTrue(result.size >= 1) // At least system message
        assertTrue(result.size <= 6) // System + up to 5 conversation
        assertTrue(result.first() is SystemMessage)
    }
}
