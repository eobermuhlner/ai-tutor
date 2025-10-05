package ch.obermuhlner.aitutor.tutor.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

class ConversationSummarizationServiceTest {

    private val mockChatModel = mockk<ChatModel>()

    private val service = ConversationSummarizationService(
        chatModel = mockChatModel,
        batchSizeTokens = 100, // Small batch size for testing
        compressionRatio = 0.4,
        minSummaryTokens = 100,
        maxSummaryTokens = 2000,
        summarizationPrompt = "Summarize this conversation. Target length: approximately {targetWords} words."
    )

    @Test
    fun `summarizeMessages should return empty string for empty input`() {
        val result = service.summarizeMessages(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `summarizeMessages should handle single batch`() {
        val messages = listOf(
            UserMessage("Hello"),
            AssistantMessage("Hi there"),
            UserMessage("How are you?"),
            AssistantMessage("I'm good")
        )

        // Mock ChatModel response
        val mockResponse = mockk<ChatResponse>()
        val mockGeneration = mockk<Generation>()
        val mockOutput = mockk<org.springframework.ai.chat.messages.AssistantMessage>()

        every { mockOutput.text } returns "Summary: User greeted and asked about wellbeing"
        every { mockGeneration.output } returns mockOutput
        every { mockResponse.result } returns mockGeneration
        every { mockChatModel.call(any<Prompt>()) } returns mockResponse

        val result = service.summarizeMessages(messages)

        assertEquals("Summary: User greeted and asked about wellbeing", result)
        verify(exactly = 1) { mockChatModel.call(any<Prompt>()) }
    }

    @Test
    fun `summarizeMessages should handle multiple batches with recursion`() {
        // Create messages that exceed batch size (100 tokens = ~400 chars)
        // Each message ~200 chars = ~50 tokens, so 3 messages = ~150 tokens > 100 batch size
        val messages = listOf(
            UserMessage("This is a long message ".repeat(10)), // ~230 chars
            AssistantMessage("This is another long message ".repeat(10)), // ~290 chars
            UserMessage("And one more long message ".repeat(10)) // ~250 chars
        )

        // Mock responses for batch summaries
        val mockResponse1 = mockk<ChatResponse>()
        val mockGeneration1 = mockk<Generation>()
        val mockOutput1 = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutput1.text } returns "Batch 1 summary"
        every { mockGeneration1.output } returns mockOutput1
        every { mockResponse1.result } returns mockGeneration1

        val mockResponse2 = mockk<ChatResponse>()
        val mockGeneration2 = mockk<Generation>()
        val mockOutput2 = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutput2.text } returns "Batch 2 summary"
        every { mockGeneration2.output } returns mockOutput2
        every { mockResponse2.result } returns mockGeneration2

        val mockResponseFinal = mockk<ChatResponse>()
        val mockGenerationFinal = mockk<Generation>()
        val mockOutputFinal = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutputFinal.text } returns "Final summary"
        every { mockGenerationFinal.output } returns mockOutputFinal
        every { mockResponseFinal.result } returns mockGenerationFinal

        // Return different responses for each call
        every { mockChatModel.call(any<Prompt>()) } returnsMany listOf(
            mockResponse1,
            mockResponse2,
            mockResponseFinal
        )

        val result = service.summarizeMessages(messages)

        assertEquals("Final summary", result)
        // Should call ChatModel at least twice (once per batch + once for final)
        verify(atLeast = 2) { mockChatModel.call(any<Prompt>()) }
    }

    @Test
    fun `summarizeMessages should handle null text from ChatModel`() {
        val messages = listOf(
            UserMessage("Test message")
        )

        val mockResponse = mockk<ChatResponse>()
        val mockGeneration = mockk<Generation>()
        val mockOutput = mockk<org.springframework.ai.chat.messages.AssistantMessage>()

        every { mockOutput.text } returns null
        every { mockGeneration.output } returns mockOutput
        every { mockResponse.result } returns mockGeneration
        every { mockChatModel.call(any<Prompt>()) } returns mockResponse

        val result = service.summarizeMessages(messages)

        assertEquals("", result)
    }

    @Test
    fun `summarizeMessages should format conversation correctly`() {
        val messages = listOf(
            UserMessage("Hello tutor"),
            AssistantMessage("Hello learner")
        )

        // Capture the prompt sent to ChatModel
        val promptSlot = slot<Prompt>()

        val mockResponse = mockk<ChatResponse>()
        val mockGeneration = mockk<Generation>()
        val mockOutput = mockk<org.springframework.ai.chat.messages.AssistantMessage>()

        every { mockOutput.text } returns "Summary"
        every { mockGeneration.output } returns mockOutput
        every { mockResponse.result } returns mockGeneration
        every { mockChatModel.call(capture(promptSlot)) } returns mockResponse

        service.summarizeMessages(messages)

        // Verify prompt contains formatted conversation
        val promptText = promptSlot.captured.instructions.joinToString("\n") { it.text }
        assertTrue(promptText.contains("Learner: Hello tutor"))
        assertTrue(promptText.contains("Tutor: Hello learner"))
    }

    @Test
    fun `summarizeMessages should split batches correctly based on token estimation`() {
        // Create exactly 2 batches worth of messages
        // Batch size: 100 tokens = ~400 chars
        val messages = listOf(
            UserMessage("x".repeat(200)), // ~50 tokens
            AssistantMessage("x".repeat(200)), // ~50 tokens (total 100, fits in batch 1)
            UserMessage("x".repeat(200)), // ~50 tokens (starts batch 2)
            AssistantMessage("x".repeat(200)) // ~50 tokens (total 100 in batch 2)
        )

        val mockResponse1 = mockk<ChatResponse>()
        val mockGeneration1 = mockk<Generation>()
        val mockOutput1 = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutput1.text } returns "Batch 1"
        every { mockGeneration1.output } returns mockOutput1
        every { mockResponse1.result } returns mockGeneration1

        val mockResponse2 = mockk<ChatResponse>()
        val mockGeneration2 = mockk<Generation>()
        val mockOutput2 = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutput2.text } returns "Batch 2"
        every { mockGeneration2.output } returns mockOutput2
        every { mockResponse2.result } returns mockGeneration2

        val mockResponseFinal = mockk<ChatResponse>()
        val mockGenerationFinal = mockk<Generation>()
        val mockOutputFinal = mockk<org.springframework.ai.chat.messages.AssistantMessage>()
        every { mockOutputFinal.text } returns "Final"
        every { mockGenerationFinal.output } returns mockOutputFinal
        every { mockResponseFinal.result } returns mockGenerationFinal

        every { mockChatModel.call(any<Prompt>()) } returnsMany listOf(
            mockResponse1,
            mockResponse2,
            mockResponseFinal
        )

        val result = service.summarizeMessages(messages)

        assertEquals("Final", result)
        verify(exactly = 3) { mockChatModel.call(any<Prompt>()) }
    }
}
