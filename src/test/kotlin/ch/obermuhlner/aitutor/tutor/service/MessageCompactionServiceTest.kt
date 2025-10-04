package ch.obermuhlner.aitutor.tutor.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage

class MessageCompactionServiceTest {

    private val mockSummarizationService = mockk<ConversationSummarizationService>()

    private val serviceWithSummarization = MessageCompactionService(
        maxTokens = 100000,
        recentMessageCount = 15,
        summarizationEnabled = true,
        summaryPrefixPrompt = "Previous conversation summary: {summary}",
        summarizationService = mockSummarizationService
    )

    private val serviceWithoutSummarization = MessageCompactionService(
        maxTokens = 100000,
        recentMessageCount = 15,
        summarizationEnabled = false,
        summaryPrefixPrompt = "Previous conversation summary: {summary}",
        summarizationService = mockSummarizationService
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

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

        // Should have all system messages + all conversation messages
        assertEquals(6, result.size)
        assertTrue(result.take(2).all { it is SystemMessage })
    }

    @Test
    fun `compactMessages should keep only recent messages when limit exceeded (no summarization)`() {
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

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

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

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

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

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

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
            recentMessageCount = 5,
            summarizationEnabled = false,
            summaryPrefixPrompt = "Previous conversation summary: {summary}",
            summarizationService = mockSummarizationService
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
            recentMessageCount = 3, // Very small window
            summarizationEnabled = false,
            summaryPrefixPrompt = "Previous conversation summary: {summary}",
            summarizationService = mockSummarizationService
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
            recentMessageCount = 5,
            summarizationEnabled = false,
            summaryPrefixPrompt = "Previous conversation summary: {summary}",
            summarizationService = mockSummarizationService
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

        // Should have system messages + last conversation message as fallback
        assertEquals(3, result.size)
        assertTrue(result[0] is SystemMessage)
        assertTrue(result[1] is SystemMessage)
        assertTrue(result[2] is UserMessage)
    }

    @Test
    fun `compactMessages should handle messages with null text`() {
        val systemMessages = listOf(
            SystemMessage("System prompt")
        )

        // Create messages that might have null text (simulated by empty strings)
        val conversationMessages = listOf(
            UserMessage(""),
            AssistantMessage(""),
            UserMessage("Valid message")
        )

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

        // Should handle empty messages gracefully
        assertEquals(4, result.size) // 1 system + 3 conversation
        assertTrue(result.first() is SystemMessage)
    }

    @Test
    fun `compactMessages should preserve message types in order`() {
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

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

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
        val systemMessages = listOf(
            SystemMessage("Short system") // ~12 chars = ~3 tokens
        )

        // Create messages that are exactly at the budget boundary
        // Each message ~80 chars = ~20 tokens, 5 messages = ~100 tokens
        val conversationMessages = (1..5).map { _ ->
            UserMessage("Message number with some additional text to reach the size limit here now")
        }

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

        // Should have system message + some conversation messages
        assertTrue(result.size >= 1) // At least system message
        assertTrue(result.size <= 6) // System + up to 5 conversation
        assertTrue(result.first() is SystemMessage)
    }

    // === Summarization Tests ===

    @Test
    fun `compactMessages with summarization should summarize old messages and keep recent`() {
        val systemMessages = listOf(SystemMessage("System"))

        // Create 20 messages: 5 old + 15 recent
        val conversationMessages = (1..20).map { i ->
            if (i % 2 == 1) UserMessage("User $i") else AssistantMessage("Assistant $i")
        }

        // Mock summarization service
        every { mockSummarizationService.summarizeMessages(any()) } returns "Summary of old conversation"

        val result = serviceWithSummarization.compactMessages(systemMessages, conversationMessages)

        // Should have: system + summary + recent messages
        verify(exactly = 1) { mockSummarizationService.summarizeMessages(any()) }
        assertTrue(result.size >= 2) // At least system + summary
        assertTrue(result.first() is SystemMessage)
        // Second message should be the summary
        assertTrue(result[1] is SystemMessage)
        assertTrue((result[1] as SystemMessage).text.contains("Previous conversation summary"))
    }

    @Test
    fun `compactMessages with summarization disabled should not call summarization service`() {
        val systemMessages = listOf(SystemMessage("System"))
        val conversationMessages = (1..20).map { i ->
            UserMessage("Message $i")
        }

        val result = serviceWithoutSummarization.compactMessages(systemMessages, conversationMessages)

        // Should NOT call summarization service
        verify(exactly = 0) { mockSummarizationService.summarizeMessages(any()) }
        assertTrue(result.size <= 16) // System + up to 15 recent
    }

    @Test
    fun `compactMessages with summarization should handle no old messages`() {
        val systemMessages = listOf(SystemMessage("System"))

        // Only 10 messages, all will be "recent"
        val conversationMessages = (1..10).map { i ->
            UserMessage("Message $i")
        }

        val result = serviceWithSummarization.compactMessages(systemMessages, conversationMessages)

        // Should NOT call summarization service (no old messages)
        verify(exactly = 0) { mockSummarizationService.summarizeMessages(any()) }
        assertEquals(11, result.size) // System + 10 messages
    }

    @Test
    fun `compactMessages with summarization should trim recent if summary too large`() {
        val service = MessageCompactionService(
            maxTokens = 100, // Very small limit
            recentMessageCount = 10,
            summarizationEnabled = true,
            summaryPrefixPrompt = "Previous conversation summary: {summary}",
            summarizationService = mockSummarizationService
        )

        val systemMessages = listOf(SystemMessage("System"))

        // Create many large messages
        val conversationMessages = (1..20).map { i ->
            UserMessage("This is a very long message ".repeat(20)) // ~540 chars each
        }

        // Mock a large summary
        every { mockSummarizationService.summarizeMessages(any()) } returns "Large summary ".repeat(50)

        val result = service.compactMessages(systemMessages, conversationMessages)

        // Should call summarization and trim recent messages to fit
        verify(exactly = 1) { mockSummarizationService.summarizeMessages(any()) }
        assertTrue(result.size >= 2) // At least system + summary
        assertTrue(result.first() is SystemMessage)
    }
}
