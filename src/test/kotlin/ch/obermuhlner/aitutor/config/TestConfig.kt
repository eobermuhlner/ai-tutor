package ch.obermuhlner.aitutor.config

import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import io.mockk.every
import io.mockk.mockk
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")  // Only active during tests
class TestConfig {

    @Bean
    @Primary
    fun mockAiChatService(): AiChatService {
        val mock = mockk<AiChatService>()

        // Default mock behavior for AI chat service
        every { mock.call(any(), any()) } returns AiChatResponse(
            reply = "Test reply from AI",
            conversationResponse = ConversationResponse(
                conversationState = ConversationState(
                    phase = ConversationPhase.Free,
                    estimatedCEFRLevel = CEFRLevel.A1
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
            )
        )

        return mock
    }

    /**
     * Mock ChatModel bean to prevent NoUniqueBeanDefinitionException.
     * This is marked as @Primary to take precedence over any autoconfigured ChatModel beans
     * from OpenAI, Azure OpenAI, or Ollama providers during tests.
     */
    @Bean
    @Primary
    fun mockChatModel(): ChatModel {
        val mock = mockk<ChatModel>()

        // Default mock behavior
        every { mock.call(any<Prompt>()) } returns ChatResponse(
            listOf(Generation(AssistantMessage("Test response")))
        )

        return mock
    }
}
