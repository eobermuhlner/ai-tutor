package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.dto.CorrectionResponse
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service focused on generating error corrections for learner messages.
 * Separated from main tutoring logic to reduce LLM context and cognitive burden.
 * Uses ChatModel directly (no streaming) with strict schema enforcement.
 */
@Service
class CorrectionService(
    private val chatModel: ChatModel,
    private val languageService: LanguageService,
    @Value("\${ai-tutor.prompts.correction-analysis}") private val correctionAnalysisPrompt: String,
    @Value("\${ai-tutor.chat.strict-schema-enforcement:true}") private val strictSchemaEnforcement: Boolean
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generate corrections for a user's message using ChatModel directly.
     *
     * @param userText The learner's message to analyze
     * @param sourceLanguageCode The learner's native language
     * @param targetLanguageCode The language being learned
     * @param estimatedCEFRLevel The learner's estimated proficiency level
     * @param phase Current conversation phase (Free phase skips corrections)
     * @param chatModel Optional per-user ChatModel for BYOK support
     * @return List of corrections found (empty if no errors)
     */
    fun generateCorrections(
        userText: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        estimatedCEFRLevel: CEFRLevel,
        phase: ConversationPhase,
        chatModel: ChatModel? = null
    ): List<Correction> {
        // Skip correction for empty or very short messages
        if (userText.isBlank() || userText.length < 3) {
            logger.debug("Skipping correction for short message: $userText")
            return emptyList()
        }

        val sourceLanguage = languageService.getLanguageName(sourceLanguageCode)
        val targetLanguage = languageService.getLanguageName(targetLanguageCode)

        logger.debug("Generating corrections for: $targetLanguage message (CEFR: ${estimatedCEFRLevel.name}, phase: ${phase.name})")

        val systemPrompt = PromptTemplate(correctionAnalysisPrompt).render(mapOf(
            "targetLanguage" to targetLanguage,
            "targetLanguageCode" to targetLanguageCode,
            "sourceLanguage" to sourceLanguage,
            "sourceLanguageCode" to sourceLanguageCode,
        ))

        val messages = listOf(
            SystemMessage(systemPrompt),
            UserMessage(userText)
        )

        try {
            // Use provided ChatModel or system default
            val effectiveChatModel = chatModel ?: this.chatModel

            val correctionResponse = if (strictSchemaEnforcement && isStrictEnforcementSupported(effectiveChatModel)) {
                callWithStrictEnforcement(messages, effectiveChatModel)
            } else {
                callWithSoftEnforcement(messages, effectiveChatModel)
            }

            logger.info("Generated ${correctionResponse.corrections.size} corrections")
            return correctionResponse.corrections

        } catch (e: Exception) {
            logger.error("Failed to generate corrections for message", e)
            return emptyList() // Graceful degradation
        }
    }

    private fun isStrictEnforcementSupported(model: ChatModel): Boolean {
        val className = model.javaClass.name
        return className.contains("OpenAi", ignoreCase = true) ||
               className.contains("Ollama", ignoreCase = true)
    }

    private fun callWithStrictEnforcement(messages: List<org.springframework.ai.chat.messages.Message>, model: ChatModel): CorrectionResponse {
        val outputConverter = BeanOutputConverter(CorrectionResponse::class.java)
        val jsonSchema = outputConverter.jsonSchema

        val chatOptions = when {
            model.javaClass.name.contains("OpenAi", ignoreCase = true) -> {
                logger.debug("Using OpenAI strict JSON schema enforcement for corrections")
                OpenAiChatOptions.builder()
                    .responseFormat(
                        ResponseFormat.builder()
                            .type(ResponseFormat.Type.JSON_SCHEMA)
                            .jsonSchema(
                                ResponseFormat.JsonSchema.builder()
                                    .name("CorrectionResponse")
                                    .schema(jsonSchema)
                                    .strict(true)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }
            else -> {
                logger.warn("Unknown provider for strict enforcement, falling back to soft")
                return callWithSoftEnforcement(messages, model)
            }
        }

        val prompt = Prompt(messages, chatOptions)
        val response = model.call(prompt)
        val content = response.result.output.text ?: ""

        logger.debug("Correction response received (${content.length} chars)")
        return (outputConverter.convert(content) as CorrectionResponse?) ?: CorrectionResponse(emptyList())
    }

    private fun callWithSoftEnforcement(messages: List<org.springframework.ai.chat.messages.Message>, model: ChatModel): CorrectionResponse {
        logger.debug("Using soft JSON schema enforcement for corrections")

        val outputConverter = BeanOutputConverter(CorrectionResponse::class.java)
        val schemaInstruction = """

            IMPORTANT: Return your response as a JSON object with this exact structure:
            ${outputConverter.jsonSchema}

            Wrap the JSON in triple backticks with 'json' language marker:
            ```json
            {your json here}
            ```
        """.trimIndent()

        val enhancedMessages = messages + SystemMessage(schemaInstruction)

        val prompt = Prompt(enhancedMessages)
        val response = model.call(prompt)
        val content = response.result.output.text ?: ""

        logger.debug("Correction response received (${content.length} chars)")

        // Extract JSON from markdown fence if present
        val jsonContent = if (content.contains("```json")) {
            val match = Regex("```json\\s*\\R(.*?)\\R?```", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .find(content)
            match?.groupValues?.getOrNull(1)?.trim() ?: content
        } else {
            content
        }

        return try {
            (outputConverter.convert(jsonContent) as CorrectionResponse?) ?: CorrectionResponse(emptyList())
        } catch (e: Exception) {
            logger.error("Failed to parse correction response", e)
            CorrectionResponse(emptyList())
        }
    }
}
