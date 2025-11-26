package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.conversation.service.ChatOptionsFactory
import ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory
import ch.obermuhlner.aitutor.language.service.LanguageService
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Data class for structured output of lesson goals evaluation
 */
data class LessonGoalsEvaluationResponse(
    @JsonProperty("goalsCompleted")
    val goalsCompleted: Boolean,

    @JsonProperty("reasoning")
    val reasoning: String
)

/**
 * Service for evaluating lesson goals completion using LLM analysis.
 *
 * This service analyzes conversation history against lesson goals to determine
 * if the learner has sufficiently practiced and demonstrated the target skills.
 */
@Service
class LessonGoalsEvaluationService(
    private val lessonContentService: LessonContentService,
    private val catalogService: CatalogService,
    private val userChatModelFactory: UserChatModelFactory,
    private val languageService: LanguageService,
    private val chatSessionRepository: ChatSessionRepository,
    private val objectMapper: ObjectMapper,
    private val chatOptionsFactory: ChatOptionsFactory,
    @Value("\${ai-tutor.prompts.lesson-goals-evaluation}") private val lessonGoalsEvaluationPrompt: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluates lesson goals completion using LLM analysis.
     * Updates the goalsCompleted field in lessonProgressGoalsCompleted.
     *
     * This method runs asynchronously to avoid blocking user message processing.
     * Evaluation failures are logged but do not affect the user experience.
     *
     * @param session The chat session to evaluate
     * @param messages The conversation history for context
     */
    @Async
    fun evaluateLessonGoals(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>
    ) {
        // Only evaluate if there's an active lesson
        if (session.currentLessonId == null || session.courseTemplateId == null) {
            return
        }

        try {
            // Get course slug for lesson lookup
            val course = catalogService.getCourseById(session.courseTemplateId!!) ?: return
            val nameMap = objectMapper.readValue<Map<String, String>>(course.nameJson)
            val courseNameEn = nameMap["en"] ?: "unknown"
            val languageOnly = course.languageCode.lowercase().substringBefore("-")
            val courseSlug = "$languageOnly-${courseNameEn.lowercase().replace(" ", "-")}"

            // Get lesson content to extract goals
            val lessonContent = lessonContentService.getLesson(courseSlug, session.currentLessonId!!) ?: return

            // Get recent conversation context (last 10 messages)
            val recentMessages = messages.takeLast(10)
            val conversationContext = recentMessages.joinToString("\n") { msg ->
                "${msg.role.name}: ${msg.content}"
            }

            // Get language names
            val targetLanguage = languageService.getLanguageName(session.targetLanguageCode)
            val sourceLanguage = languageService.getLanguageName(session.sourceLanguageCode)

            // Render prompt template with placeholders
            val renderedPrompt = org.springframework.ai.chat.prompt.PromptTemplate(lessonGoalsEvaluationPrompt).render(mapOf(
                "targetLanguage" to targetLanguage,
                "targetLanguageCode" to session.targetLanguageCode,
                "sourceLanguage" to sourceLanguage,
                "sourceLanguageCode" to session.sourceLanguageCode,
                "cefrLevel" to session.estimatedCEFRLevel.name,
                "lessonContent" to lessonContent.fullMarkdown,
                "conversationContext" to conversationContext
            ))

            // Get chat model for this user
            val chatModel = userChatModelFactory.getChatModelForUser(session.userId)

            // Use structured output with the standardized factory
            val outputConverter = BeanOutputConverter(LessonGoalsEvaluationResponse::class.java)
            val jsonSchema = outputConverter.jsonSchema

            // Use the factory to get provider-specific options
            val chatOptions = chatOptionsFactory.createOptions(chatModel, jsonSchema)

            // Create prompt with appropriate options
            val prompt = if (chatOptions != null) {
                Prompt(listOf(UserMessage(renderedPrompt)), chatOptions)
            } else {
                logger.warn("No supported provider found for lesson goals evaluation, using soft enforcement")
                Prompt(listOf(UserMessage(renderedPrompt)))
            }

            // Call LLM with structured output
            val response = chatModel.call(prompt)
            val content = response.result.output.text ?: ""

            // Parse response using BeanOutputConverter for type safety
            val evaluationResponse = outputConverter.convert(content)
            val goalsCompleted = evaluationResponse.goalsCompleted
            val reasoning = evaluationResponse.reasoning

            // Update session lesson progress fields
            session.lessonProgressGoalsCompleted = goalsCompleted
            chatSessionRepository.save(session)

            logger.info("Session ${session.id} lesson goals evaluated: $goalsCompleted - $reasoning")

        } catch (e: Exception) {
            logger.error("Failed to evaluate lesson goals for session ${session.id}", e)
        }
    }
}
