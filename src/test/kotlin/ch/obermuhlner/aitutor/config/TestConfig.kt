package ch.obermuhlner.aitutor.config

import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.core.model.ConversationResponse
import ch.obermuhlner.aitutor.core.model.ConversationState
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun mockAiChatService(): AiChatService {
        val mock = mockk<AiChatService>()

        // Default mock behavior for AI chat service
        every { mock.call(any(), any()) } returns AiChatService.AiChatResponse(
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
}
