package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
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

    override fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        val result = ChatClient.create(chatModel)
            .prompt(Prompt(request.messages))
            .call()
            .entity(AiChatResponse::class.java)

        onReplyText(result?.reply ?: "")

        return result
    }
}
