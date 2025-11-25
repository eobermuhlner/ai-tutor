package ch.obermuhlner.aitutor.conversation.config

import ch.obermuhlner.aitutor.conversation.service.ChatOptionsProvider
import ch.obermuhlner.aitutor.conversation.service.OllamaChatOptionsProvider
import ch.obermuhlner.aitutor.conversation.service.OpenAiChatOptionsProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatOptionsConfig {
    
    @Bean
    fun openAiChatOptionsProvider(): ChatOptionsProvider {
        return OpenAiChatOptionsProvider()
    }
    
    @Bean
    fun ollamaChatOptionsProvider(objectMapper: ObjectMapper): ChatOptionsProvider {
        return OllamaChatOptionsProvider(objectMapper)
    }
}