package ch.obermuhlner.aitutor.language.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel

class OpenAITranslationServiceTest {

    private val chatModel = mockk<ChatModel>()
    private val chatClient = mockk<ChatClient>()
    private val translationService = OpenAITranslationService(chatModel)

    @BeforeEach
    fun setup() {
        mockkStatic(ChatClient::class)
        every { ChatClient.create(chatModel) } returns chatClient
    }

    @Test
    fun `translate should return translated text`() {
        val chatClientRequest = mockk<ChatClient.ChatClientRequestSpec>()
        val callSpec = mockk<ChatClient.CallResponseSpec>()

        every { chatClient.prompt() } returns chatClientRequest
        every { chatClientRequest.user(any<String>()) } returns chatClientRequest
        every { chatClientRequest.call() } returns callSpec
        every { callSpec.content() } returns "Hola"

        val result = translationService.translate("Hello", "en", "es")

        assertEquals("Hola", result)
    }

    @Test
    fun `translate should return original text when translation returns null`() {
        val chatClientRequest = mockk<ChatClient.ChatClientRequestSpec>()
        val callSpec = mockk<ChatClient.CallResponseSpec>()

        every { chatClient.prompt() } returns chatClientRequest
        every { chatClientRequest.user(any<String>()) } returns chatClientRequest
        every { chatClientRequest.call() } returns callSpec
        every { callSpec.content() } returns null

        val result = translationService.translate("Hello", "en", "es")

        assertEquals("Hello", result)
    }
}
