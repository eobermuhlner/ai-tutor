package ch.obermuhlner.aitutor.language.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class OpenAITranslationService(
    private val chatClient: ChatClient
) : TranslationService {

    override fun translate(text: String, from: String, to: String): String {
        val prompt = """
            Translate the following text from $from to $to.
            Preserve tone, style, and formality.
            Return ONLY the translation, no explanations.

            Text: $text
        """.trimIndent()

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content() ?: text  // Fallback to original if translation fails
    }

    override fun batchTranslate(texts: List<String>, from: String, to: String): List<String> {
        if (texts.isEmpty()) return emptyList()

        val prompt = """
            Translate each line from $from to $to.
            Preserve tone, style, and formality.
            Return translations in the same order, one per line.

            ${texts.joinToString("\n")}
        """.trimIndent()

        val response = chatClient.prompt()
            .user(prompt)
            .call()
            .content() ?: return texts

        return response.lines().take(texts.size)
    }
}
