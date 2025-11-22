package ch.obermuhlner.aitutor.testharness.conversation

import ch.obermuhlner.aitutor.testharness.ai.AiProviderFactory
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.LearnerMessage
import ch.obermuhlner.aitutor.testharness.domain.LlmConversationConfig
import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import org.slf4j.LoggerFactory

/**
 * Generates conversation messages using an LLM based on scenario configuration.
 */
class LlmConversationGenerator(private val config: TestHarnessConfig) {
    private val logger = LoggerFactory.getLogger(LlmConversationGenerator::class.java)
    private val aiProvider = AiProviderFactory.create(config.getAiProviderConfig())

    /**
     * Generates conversation messages based on the LLM conversation configuration.
     */
    fun generateConversationMessages(
        scenario: TestScenario,
        llmConfig: LlmConversationConfig
    ): List<LearnerMessage> {
        logger.info("Generating ${llmConfig.messageCount} messages using LLM for scenario: ${scenario.name}")

        val prompt = buildConversationGenerationPrompt(scenario, llmConfig)
        logger.debug("LLM conversation generation prompt: $prompt")

        val response = aiProvider.chat(
            prompt = prompt,
            model = config.judgeModel,
            temperature = config.judgeTemperature
        )

        logger.debug("LLM response: $response")
        
        // Parse the response to extract conversation messages
        return parseConversationMessages(response, llmConfig)
    }

    /**
     * Generates a single conversation message, potentially with context from the previous tutor response.
     */
    fun generateSingleConversationMessage(
        scenario: TestScenario,
        llmConfig: LlmConversationConfig,
        previousTutorResponse: String? = null
    ): LearnerMessage {
        logger.debug("Generating single message using LLM with previous tutor response context: ${previousTutorResponse?.take(50)}...")

        val prompt = buildSingleMessageGenerationPrompt(scenario, llmConfig, previousTutorResponse)
        logger.debug("Single message generation prompt: $prompt")

        val response = aiProvider.chat(
            prompt = prompt,
            model = config.judgeModel,
            temperature = config.judgeTemperature
        )

        logger.debug("LLM response: $response")
        
        // Parse the single response to extract one conversation message
        val messages = parseConversationMessages(response, llmConfig)
        return if (messages.isNotEmpty()) {
            messages.first()
        } else {
            // Fallback if parsing fails
            LearnerMessage(content = "Hello")
        }
    }

    private fun buildSingleMessageGenerationPrompt(
        scenario: TestScenario,
        llmConfig: LlmConversationConfig,
        previousTutorResponse: String?
    ): String {
        val contextSection = if (previousTutorResponse != null) {
            """
            
            The tutor just responded: "$previousTutorResponse"
            
            Generate an appropriate follow-up message that continues the conversation naturally.
            """
        } else {
            """
            
            This is the beginning of the conversation. Generate an appropriate opening message.
            """
        }

        return """
            You are tasked with generating a single conversation message for an AI language tutor test scenario.
            
            Scenario details:
            - Learner: ${scenario.learnerPersona.name} (${scenario.learnerPersona.cefrLevel})
            - Target language: ${scenario.learnerPersona.targetLanguage}
            - Source language: ${scenario.learnerPersona.sourceLanguage}
            - Common errors: ${scenario.learnerPersona.commonErrors.joinToString(", ")}
            - Learning goals: ${scenario.learnerPersona.learningGoals.joinToString(", ")}
            
            LLM Conversation Configuration:
            - Target error types (if any): ${if (llmConfig.errorTypes.isEmpty()) "None (learner should be accurate)" else llmConfig.errorTypes.joinToString(", ")}
            - Target passiveness: ${llmConfig.targetPassiveness} (0.0 = completely active, 1.0 = completely passive)
            - Target error frequency: ${llmConfig.targetErrorFrequency} (0.0 = no errors, 1.0 = many errors)
            - Course: ${llmConfig.course ?: "None specified"}
            - Lesson: ${llmConfig.lesson ?: "None specified"}
            - Additional notes: ${llmConfig.notes ?: "None"}
            
            $contextSection
            
            Generate exactly one message that the human learner would send to the AI tutor.
            The message should be appropriate for a ${scenario.learnerPersona.cefrLevel} level learner of ${scenario.learnerPersona.targetLanguage}.
            
            The message should reflect the target passiveness and error frequency as specified. 
            If target error frequency is high, include the specified error types in the message.
            
            Respond with the message in this exact format, without any additional text:
            MESSAGE: [message content]
            
            The message should be a natural part of a conversation with an AI language tutor.
        """.trimIndent()
    }

    private fun buildConversationGenerationPrompt(scenario: TestScenario, llmConfig: LlmConversationConfig): String {
        return """
            You are tasked with generating a conversation script for an AI language tutor test scenario.
            
            Scenario details:
            - Learner: ${scenario.learnerPersona.name} (${scenario.learnerPersona.cefrLevel})
            - Target language: ${scenario.learnerPersona.targetLanguage}
            - Source language: ${scenario.learnerPersona.sourceLanguage}
            - Common errors: ${scenario.learnerPersona.commonErrors.joinToString(", ")}
            - Learning goals: ${scenario.learnerPersona.learningGoals.joinToString(", ")}
            
            LLM Conversation Configuration:
            - Target message count: ${llmConfig.messageCount}
            - Target topic (if specified): ${llmConfig.topic ?: "Any relevant topic for the learner's level"}
            - Target error types: ${if (llmConfig.errorTypes.isEmpty()) "None (learner should be accurate)" else llmConfig.errorTypes.joinToString(", ")}
            - Target passiveness: ${llmConfig.targetPassiveness} (0.0 = completely active, 1.0 = completely passive)
            - Target error frequency: ${llmConfig.targetErrorFrequency} (0.0 = no errors, 1.0 = many errors)
            - Course: ${llmConfig.course ?: "None specified"}
            - Lesson: ${llmConfig.lesson ?: "None specified"}
            - Additional notes: ${llmConfig.notes ?: "None"}
            
            Generate exactly ${llmConfig.messageCount} messages that the human learner would send to the AI tutor.
            The messages should be appropriate for a ${scenario.learnerPersona.cefrLevel} level learner of ${scenario.learnerPersona.targetLanguage}.
            
            The messages should reflect the target passiveness and error frequency as specified. 
            If target error frequency is high, include the specified error types in the messages.
            
            Respond with the messages in this exact format, without any additional text:
            MESSAGE 1: [first message content]
            MESSAGE 2: [second message content]
            MESSAGE 3: [third message content]
            ...
            MESSAGE N: [Nth message content]
            
            Each message should be a natural part of a conversation with an AI language tutor.
        """.trimIndent()
    }

    private fun parseConversationMessages(response: String, llmConfig: LlmConversationConfig): List<LearnerMessage> {
        val messages = mutableListOf<LearnerMessage>()
        
        // Split the response by lines and parse each message
        val lines = response.lines()
        
        // Try different formats to extract messages
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Try format: MESSAGE X: [content] (for multiple messages)
            if (trimmedLine.startsWith("MESSAGE ") && trimmedLine.contains(":")) {
                val colonIndex = trimmedLine.indexOf(':')
                var content = trimmedLine.substring(colonIndex + 1).trim()
                
                // Remove surrounding quotes if present
                if (content.startsWith("\"") && content.endsWith("\"") && content.length > 1) {
                    content = content.substring(1, content.length - 1)
                }
                
                if (content.isNotEmpty() && content != "[message content]" && !content.startsWith("[") && content != "") {
                    messages.add(LearnerMessage(content = content))
                }
            } 
            // Try format: MESSAGE: [content] (for single messages)
            else if (trimmedLine.startsWith("MESSAGE:") || trimmedLine.startsWith("MESSAGE :")) {
                var content = trimmedLine.substringAfter(':').trim()
                
                // Remove surrounding quotes if present
                if (content.startsWith("\"") && content.endsWith("\"") && content.length > 1) {
                    content = content.substring(1, content.length - 1)
                }
                
                if (content.isNotEmpty() && content != "[message content]" && !content.startsWith("[") && content != "") {
                    messages.add(LearnerMessage(content = content))
                }
            }
            // Try format: X. [content] (alternative format)
            else if (trimmedLine.matches(Regex("""^\d+\.\s+.*"""))) {
                val content = trimmedLine.replace(Regex("""^\d+\.\s+"""), "")
                
                if (content.isNotEmpty() && content != "[message content]" && !content.startsWith("[") && content != "") {
                    messages.add(LearnerMessage(content = content))
                }
            }
        }
        
        // If we couldn't parse using the expected formats, try to extract sentences as messages
        if (messages.isEmpty()) {
            val allText = lines.joinToString(" ").trim()
            val sentenceRegex = Regex("""[^.!?]*[.!?]+""")
            val sentenceMatches = sentenceRegex.findAll(allText)
            
            sentenceMatches.forEach { match ->
                val sentence = match.value.trim()
                if (sentence.isNotEmpty() && sentence.length > 3) { // Filter out very short fragments
                    messages.add(LearnerMessage(content = sentence))
                }
            }
        }
        
        // Truncate or pad to the requested message count if needed
        return if (messages.isNotEmpty() && messages.size >= llmConfig.messageCount) {
            messages.take(llmConfig.messageCount)
        } else {
            // If we have fewer messages than requested, return what we have
            messages
        }
    }
}