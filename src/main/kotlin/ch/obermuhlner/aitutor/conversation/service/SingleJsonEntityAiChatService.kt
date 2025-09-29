package ch.obermuhlner.aitutor.conversation.service

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
        request: AiChatService.AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatService.AiChatResponse? {
        val result = ChatClient.create(chatModel)
            .prompt(Prompt(request.messages))
            .call()
            .entity(AiChatService.AiChatResponse::class.java)

        onReplyText(result?.reply ?: "")

        return result
    }
}
