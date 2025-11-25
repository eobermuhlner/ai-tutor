package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory
import ch.obermuhlner.aitutor.language.service.LanguageService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

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
    @Value("\${ai-tutor.prompts.lesson-goals-evaluation}") private val lessonGoalsEvaluationPrompt: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluates lesson goals completion using LLM analysis.
     * Updates the goalsCompleted field in lessonProgressJson.
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

            // Extract goals section from lesson markdown
            val goalsSection = extractLessonGoals(lessonContent.fullMarkdown)
            if (goalsSection.isBlank()) {
                logger.debug("No goals section found in lesson ${session.currentLessonId}")
                return
            }

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
                "lessonGoals" to goalsSection,
                "conversationContext" to conversationContext
            ))

            // Get chat model for this user
            val chatModel = userChatModelFactory.getChatModelForUser(session.userId)

            // Call LLM
            val response = chatModel.call(renderedPrompt)

            // Parse response
            val jsonResponse = extractJsonFromResponse(response)
            val resultMap = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            val goalsCompleted = resultMap["goalsCompleted"] as? Boolean ?: false
            val reasoning = resultMap["reasoning"] as? String ?: "No reasoning provided"

            // Update session lessonProgressJson
            val progressJson = session.lessonProgressJson ?: """{"turnCount": 0, "goalsCompleted": false}"""
            val progressMap = objectMapper.readValue<MutableMap<String, Any>>(progressJson)
            progressMap["goalsCompleted"] = goalsCompleted
            session.lessonProgressJson = objectMapper.writeValueAsString(progressMap)
            chatSessionRepository.save(session)

            logger.info("Session ${session.id} lesson goals evaluated: $goalsCompleted - $reasoning")

        } catch (e: Exception) {
            logger.error("Failed to evaluate lesson goals for session ${session.id}", e)
        }
    }

    /**
     * Extracts the "This Week's Goals" or "Lesson Goals" section from lesson markdown.
     *
     * @param markdown The full lesson markdown content
     * @return The goals section text, or empty string if not found
     */
    private fun extractLessonGoals(markdown: String): String {
        val lines = markdown.lines()
        val goalsSectionStart = lines.indexOfFirst {
            it.trim().startsWith("##") &&
            (it.contains("Goal", ignoreCase = true) || it.contains("Objective", ignoreCase = true))
        }

        if (goalsSectionStart == -1) {
            return ""
        }

        // Find next section (starting with ##)
        val nextSectionStart = lines.subList(goalsSectionStart + 1, lines.size)
            .indexOfFirst { it.trim().startsWith("##") }

        val goalsSectionEnd = if (nextSectionStart == -1) {
            lines.size
        } else {
            goalsSectionStart + 1 + nextSectionStart
        }

        return lines.subList(goalsSectionStart, goalsSectionEnd).joinToString("\n").trim()
    }

    /**
     * Extracts JSON from LLM response, handling markdown code blocks.
     *
     * @param response The raw LLM response
     * @return The extracted JSON string
     */
    private fun extractJsonFromResponse(response: String): String {
        val trimmed = response.trim()

        // Check if response is wrapped in markdown code block
        if (trimmed.startsWith("```")) {
            val lines = trimmed.lines()
            val contentLines = lines.drop(1).dropLast(1) // Remove first and last lines
            return contentLines.joinToString("\n").trim()
        }

        return trimmed
    }
}
