package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("!test")  // Exclude from test profile
class SingleJsonEntityAiChatService(
    val chatModel: ChatModel,
    private val chatOptionsFactory: ChatOptionsFactory,
    @Value("\${ai-tutor.chat.strict-schema-enforcement:true}") private val strictSchemaEnforcement: Boolean
) : AiChatService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit,
        chatModel: ChatModel?
    ): AiChatResponse? {
        // Use provided ChatModel or fall back to system default
        val effectiveChatModel = chatModel ?: this.chatModel

        logger.debug("Request: ${request.messages.size} messages")
        if (logger.isTraceEnabled) {
            request.messages.forEach { message ->
                logger.trace("Message: $message")
            }
        }

        val result = if (strictSchemaEnforcement) {
            callWithStrictEnforcement(request, effectiveChatModel)
        } else {
            callWithSoftEnforcement(request, effectiveChatModel)
        }

        onReplyText(result?.reply ?: "")

        logger.trace("Response: {}", result)
        return result
    }

    private fun callWithStrictEnforcement(request: AiChatRequest, effectiveChatModel: ChatModel): AiChatResponse? {
        val outputConverter = BeanOutputConverter(AiChatResponse::class.java)
        val jsonSchema = outputConverter.jsonSchema

        // Use the factory to get provider-specific options
        val chatOptions = chatOptionsFactory.createOptions(effectiveChatModel, jsonSchema)

        if (chatOptions == null) {
            logger.warn("No supported provider found, falling back to soft enforcement")
            return callWithSoftEnforcement(request, effectiveChatModel)
        }

        val prompt = Prompt(request.messages, chatOptions)
        val response = effectiveChatModel.call(prompt)
        logger.debug("Response metadata: {}", response.metadata)
        logger.debug("Response usage: {}", response.metadata.usage)
        val content = response.result.output.text ?: ""

        val parsedResponse = outputConverter.convert(content)

        // Extract token usage from response metadata
        val tokenUsage = try {
            val usage = response.metadata?.usage
            if (usage != null) {
                val promptTokens = usage.promptTokens?.toLong() ?: 0L
                val completionTokens = (usage.totalTokens?.toLong() ?: 0L) - promptTokens
                ch.obermuhlner.aitutor.conversation.dto.TokenUsage(
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = usage.totalTokens?.toLong() ?: 0L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Could not extract token usage from response metadata", e)
            null
        }

        return parsedResponse?.copy(tokenUsage = tokenUsage)
    }

    private fun callWithSoftEnforcement(request: AiChatRequest, effectiveChatModel: ChatModel): AiChatResponse? {
        val response = ChatClient.create(effectiveChatModel)
            .prompt(Prompt(request.messages))
            .call()
            .chatResponse()

        val parsedResponse = response?.let { chatResponse ->
            // Parse the entity from response
            val content = chatResponse.result.output.text ?: ""
            try {
                val outputConverter = BeanOutputConverter(AiChatResponse::class.java)
                outputConverter.convert(content)
            } catch (e: Exception) {
                logger.error("Failed to parse AI response", e)
                null
            }
        }

        // Extract token usage
        val tokenUsage = response?.let { chatResponse ->
            try {
                val usage = chatResponse.metadata?.usage
                if (usage != null) {
                    val promptTokens = usage.promptTokens?.toLong() ?: 0L
                    val completionTokens = (usage.totalTokens?.toLong() ?: 0L) - promptTokens
                    ch.obermuhlner.aitutor.conversation.dto.TokenUsage(
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = usage.totalTokens?.toLong() ?: 0L
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.debug("Could not extract token usage from response metadata", e)
                null
            }
        }

        return parsedResponse?.copy(tokenUsage = tokenUsage)
    }

}
