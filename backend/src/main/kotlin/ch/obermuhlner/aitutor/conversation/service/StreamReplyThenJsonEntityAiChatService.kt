package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.core.util.LlmJson
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.ai.util.json.schema.JsonSchemaGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!test")  // Exclude from test profile
class StreamReplyThenJsonEntityAiChatService(
    val chatModel: ChatModel,
    @Value("\${ai-tutor.prompts.json-response-format}") private val jsonResponseFormatPrompt: String,
    @Value("\${ai-tutor.chat.strict-schema-enforcement:true}") private val strictSchemaEnforcement: Boolean
) : AiChatService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        logger.debug("AI chat request with ${request.messages.size} messages")

        if (strictSchemaEnforcement && (isOpenAiProvider() || isOllamaProvider())) {
            return callWithStrictEnforcement(request, onReplyText)
        } else {
            return callWithSoftEnforcement(request, onReplyText)
        }
    }

    private fun callWithStrictEnforcement(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        val outputConverter = BeanOutputConverter(AiChatResponse::class.java)
        val jsonSchema = outputConverter.jsonSchema

        // Detect provider and use appropriate strict enforcement
        val chatOptions = when {
            isOpenAiProvider() -> {
                logger.debug("Using OpenAI strict JSON schema enforcement for streaming")
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
                logger.debug("Using Ollama format-based JSON schema enforcement for streaming")
                OllamaOptions.builder()
                    .format(outputConverter.jsonSchemaMap)
                    .temperature(0.0)  // Recommended for deterministic output
                    .build()
            }
            else -> {
                logger.warn("Unknown provider, falling back to soft enforcement")
                return callWithSoftEnforcement(request, onReplyText)
            }
        }

        logger.debug("Request: ${request.messages.size} messages")
        if (logger.isTraceEnabled) {
            request.messages.forEach { message ->
                logger.trace("Message: $message")
            }
        }

        val json = streamUntilJsonAndParse(
            chatModel,
            request.messages,
            chatOptions
        ) { chunk ->
            onReplyText(chunk)
        }

        if (json == null) {
            logger.warn("No JSON found in AI response")
            return null
        }

        logger.debug("AI response JSON received (${json.length} chars)")
        logger.trace("Response: $json")
        return outputConverter.convert(json)
    }

    private fun callWithSoftEnforcement(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        val schema = JsonSchemaGenerator.generateForType(AiChatResponse::class.java)

        val promptText = PromptTemplate(jsonResponseFormatPrompt).render(mapOf(
            "schema" to schema
        ))

        val allMessages = request.messages + SystemMessage(promptText)

        logger.debug("Request: ${allMessages.size} messages")
        if (logger.isTraceEnabled) {
            allMessages.forEach { message ->
                logger.trace("Message: $message")
            }
        }

        val json = streamUntilJsonAndParse(
            chatModel,
            allMessages,
        ) { chunk ->
            onReplyText(chunk)
        }

        if (json == null) {
            logger.warn("No JSON found in AI response")
            return null
        }

        logger.debug("AI response JSON received (${json.length} chars)")
        logger.trace("Response: $json")
        return LlmJson.parseAs<AiChatResponse>(json)
    }

    private fun streamUntilJsonAndParse(
        chatModel: ChatModel,
        allMessages: List<Message>,
        chatOptions: org.springframework.ai.chat.prompt.ChatOptions? = null,
        overlap: Int = 32,
        onText: (String) -> Unit
    ): String? {
        val startFence = Regex("```\\s*json\\s*\\R?", setOf(RegexOption.IGNORE_CASE))
        val endFence   = Regex("```")

        val acc = StringBuilder()
        var printed = 0
        var jsonStart = -1
        var done = false
        var parsed: String? = null

        val prompt = if (chatOptions != null) {
            Prompt(allMessages, chatOptions)
        } else {
            Prompt(allMessages)
        }
        chatModel.stream(prompt)
            .doOnNext { resp ->
                if (done) return@doOnNext
                val chunk = resp.result.output.text.orEmpty()
                if (chunk.isEmpty()) return@doOnNext

                acc.append(chunk)

                if (jsonStart < 0) {
                    val searchFrom = maxOf(0, printed - overlap)
                    val m = startFence.find(acc, searchFrom)
                    if (m != null) {
                        val s = m.range.first
                        if (s > printed) {
                            onText(acc.substring(printed, s))
                            printed = s
                        }
                        jsonStart = s
                    } else {
                        val safeEnd = (acc.length - overlap).coerceAtLeast(printed)
                        if (safeEnd > printed) {
                            onText(acc.substring(printed, safeEnd))
                            printed = safeEnd
                        }
                    }
                }

                if (jsonStart >= 0) {
                    val m2 = endFence.find(acc, startIndex = jsonStart + 3)
                    if (m2 != null) {
                        val endIdx = m2.range.first
                        val fenced = acc.substring(jsonStart, endIdx + 3)

                        // Normalize: force a newline after ```json so LlmJson can extract.
                        val normalized = Regex("```\\s*json\\s*", setOf(RegexOption.IGNORE_CASE))
                            .replace(fenced, "```json\n")

                        parsed = normalized
                        done = true
                    }
                }
            }
            .takeUntil { done }
            .then()
            .block()

        if (!done && printed < acc.length) {
            onText(acc.substring(printed))
        }

        return parsed
    }

    private fun isOpenAiProvider(): Boolean {
        return chatModel.javaClass.name.contains("OpenAi", ignoreCase = true)
    }

    private fun isOllamaProvider(): Boolean {
        return chatModel.javaClass.name.contains("Ollama", ignoreCase = true)
    }
}
