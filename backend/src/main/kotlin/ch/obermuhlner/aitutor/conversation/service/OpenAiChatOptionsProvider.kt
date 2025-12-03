package ch.obermuhlner.aitutor.conversation.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value

/**
 * ChatOptionsProvider implementation for OpenAI models with strict JSON schema enforcement.
 */
class OpenAiChatOptionsProvider(
    @Value("\${spring.ai.openai.chat.options.reasoning-effort:}") // Default blank for backwards compatibility
    private val reasoningEffort: String = ""
) : ChatOptionsProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(chatModel: ChatModel): Boolean {
        return chatModel.javaClass.name.contains("OpenAi", ignoreCase = true)
    }

    override fun createOptions(jsonSchema: String): ChatOptions {
        logger.debug("Creating OpenAI strict JSON schema options with reasoning effort: $reasoningEffort")
        val builder = OpenAiChatOptions.builder()
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

        // Apply reasoning effort if it's supported by the OpenAI API version
        if (reasoningEffort.isNotBlank()) {
            builder.reasoningEffort(reasoningEffort)
        }

        return builder.build()
    }
}