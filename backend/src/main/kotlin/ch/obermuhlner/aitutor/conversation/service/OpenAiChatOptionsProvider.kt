package ch.obermuhlner.aitutor.conversation.service

import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.slf4j.LoggerFactory

/**
 * ChatOptionsProvider implementation for OpenAI models with strict JSON schema enforcement.
 */
class OpenAiChatOptionsProvider : ChatOptionsProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(chatModel: ChatModel): Boolean {
        return chatModel.javaClass.name.contains("OpenAi", ignoreCase = true)
    }

    override fun createOptions(jsonSchema: String): ChatOptions {
        logger.debug("Creating OpenAI strict JSON schema options")
        return OpenAiChatOptions.builder()
            .responseFormat(
                ResponseFormat.builder()
                    .type(ResponseFormat.Type.JSON_SCHEMA)
                    .jsonSchema(
                        ResponseFormat.JsonSchema.builder()
                            .name("StructuredResponse")
                            .schema(jsonSchema)
                            .strict(true)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}