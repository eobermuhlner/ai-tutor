package ch.obermuhlner.aitutor.config

import ch.obermuhlner.aitutor.auth.filter.JwtAuthenticationFilter
import ch.obermuhlner.aitutor.auth.handler.OAuth2AuthenticationFailureHandler
import ch.obermuhlner.aitutor.auth.handler.OAuth2AuthenticationSuccessHandler
import ch.obermuhlner.aitutor.auth.service.JwtTokenService
import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.conversation.service.AiAudioService
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.user.service.CustomUserDetailsService
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
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.web.cors.CorsConfigurationSource

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

    /**
     * Mock AudioProperties bean to prevent SpringAiAudioService initialization errors.
     * This prevents the service from trying to resolve OPENAI_API_KEY environment variable.
     */
    @Bean
    @Primary
    fun mockAudioProperties(): AudioProperties {
        return AudioProperties(
            enabled = false,
            defaultModel = "tts-1",
            defaultVoice = "alloy",
            defaultSpeed = 1.0,
            voiceMappings = emptyMap()
        )
    }

    /**
     * Mock AiAudioService to prevent TTS-related initialization errors during tests.
     */
    @Bean
    @Primary
    fun mockAiAudioService(): AiAudioService {
        val mock = mockk<AiAudioService>()

        every { mock.isAvailable() } returns false
        every { mock.getVoiceMappings() } returns emptyMap()
        every { mock.synthesizeSpeech(any(), any(), any(), any(), any()) } throws UnsupportedOperationException("TTS not available in tests")

        return mock
    }

    /**
     * Mock JwtAuthenticationFilter bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockJwtAuthenticationFilter(): JwtAuthenticationFilter {
        val mock = mockk<JwtAuthenticationFilter>()
        return mock
    }

    /**
     * Mock CorsConfigurationSource bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockCorsConfigurationSource(): CorsConfigurationSource {
        val mock = mockk<CorsConfigurationSource>()
        return mock
    }

    /**
     * Mock Environment bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockEnvironment(): Environment {
        val mock = mockk<Environment>()
        return mock
    }

    /**
     * Mock OAuth2AuthenticationSuccessHandler bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockOAuth2AuthenticationSuccessHandler(): OAuth2AuthenticationSuccessHandler {
        val mock = mockk<OAuth2AuthenticationSuccessHandler>()
        return mock
    }

    /**
     * Mock OAuth2AuthenticationFailureHandler bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockOAuth2AuthenticationFailureHandler(): OAuth2AuthenticationFailureHandler {
        val mock = mockk<OAuth2AuthenticationFailureHandler>()
        return mock
    }



    /**
     * Mock AuthenticationManager bean to prevent SecurityConfig dependency injection issues.
     */
    @Bean
    @Primary
    fun mockAuthenticationManager(): AuthenticationManager {
        val mock = mockk<AuthenticationManager>()
        every { mock.authenticate(any()) } returns org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "testuser", 
            "testpassword",
            listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        )
        return mock
    }
}
