package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class SingleJsonEntityAiChatService(
    val chatModel: ChatModel
) : AiChatService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        logger.debug("Request: ${request.messages.size} messages")
        if (logger.isTraceEnabled) {
            request.messages.forEach { message ->
                logger.trace("Message: $message")
            }
        }
        val result = ChatClient.create(chatModel)
            .prompt(Prompt(request.messages))
            .call()
            .entity(AiChatResponse::class.java)

        onReplyText(result?.reply ?: "")

        logger.trace("Response: {}", result)
        return result
    }
}
