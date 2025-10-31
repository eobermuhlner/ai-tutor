package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("!test")  // Exclude from test profile
class SingleJsonEntityAiChatService(
    val chatModel: ChatModel,
    @Value("\${ai-tutor.chat.strict-schema-enforcement:true}") private val strictSchemaEnforcement: Boolean
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

        val result = if (strictSchemaEnforcement) {
            callWithStrictEnforcement(request)
        } else {
            callWithSoftEnforcement(request)
        }

        onReplyText(result?.reply ?: "")

        logger.trace("Response: {}", result)
        return result
    }

    private fun callWithStrictEnforcement(request: AiChatRequest): AiChatResponse? {
        val outputConverter = BeanOutputConverter(AiChatResponse::class.java)
        val jsonSchema = outputConverter.jsonSchema

        // Detect provider and use appropriate strict enforcement
        val chatOptions = when {
            isOpenAiProvider() -> {
                logger.debug("Using OpenAI strict JSON schema enforcement")
                OpenAiChatOptions.builder()
                    .responseFormat(
                        ResponseFormat.builder()
                            .type(ResponseFormat.Type.JSON_SCHEMA)
                            .jsonSchema(
                                ResponseFormat.JsonSchema.builder()
                                    .name("AiChatResponse")
                                    .schema(jsonSchema)
                                    .strict(true)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }
            isOllamaProvider() -> {
                logger.debug("Using Ollama format-based JSON schema enforcement")
                OllamaOptions.builder()
                    .format(outputConverter.jsonSchemaMap)
                    .temperature(0.0)  // Recommended for deterministic output
                    .build()
            }
            else -> {
                logger.warn("Unknown provider, falling back to soft enforcement")
                return callWithSoftEnforcement(request)
            }
        }

        val prompt = Prompt(request.messages, chatOptions)
        val response = chatModel.call(prompt)
        val content = response.result.output.text ?: ""

        return outputConverter.convert(content)
    }

    private fun callWithSoftEnforcement(request: AiChatRequest): AiChatResponse? {
        return ChatClient.create(chatModel)
            .prompt(Prompt(request.messages))
            .call()
            .entity(AiChatResponse::class.java)
    }

    private fun isOpenAiProvider(): Boolean {
        return chatModel.javaClass.name.contains("OpenAi", ignoreCase = true)
    }

    private fun isOllamaProvider(): Boolean {
        return chatModel.javaClass.name.contains("Ollama", ignoreCase = true)
    }
}
