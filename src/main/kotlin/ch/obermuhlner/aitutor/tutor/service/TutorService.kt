package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TutorService(
    private val aiChatService: AiChatService,
    private val languageService: LanguageService,
    private val vocabularyContextService: VocabularyContextService,
    private val messageCompactionService: MessageCompactionService,
    @Value("\${ai-tutor.prompts.system}") private val systemPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-free}") private val phaseFreePromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-correction}") private val phaseCorrectionPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-drill}") private val phaseDrillPromptTemplate: String,
    @Value("\${ai-tutor.prompts.developer}") private val developerPromptTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.no-tracking}") private val vocabularyNoTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.with-tracking}") private val vocabularyWithTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.reactive}") private val teachingStyleReactiveTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.guided}") private val teachingStyleGuidedTemplate: String,
    @Value("\${ai-tutor.prompts.teaching-style.directive}") private val teachingStyleDirectiveTemplate: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    data class TutorResponse(
        val reply: String,
        val conversationResponse: ConversationResponse
    )

    fun respond(
        tutor: Tutor,
        conversationState: ConversationState,
        userId: UUID,
        messages: List<Message>,
        sessionId: UUID? = null,
        onReplyChunk: (String) -> Unit = { print(it) }
    ): TutorResponse? {
        logger.debug("Tutor respond: user=$userId, session=$sessionId, phase=${conversationState.phase}, topic=${conversationState.currentTopic}")

        val sourceLanguageCode = tutor.sourceLanguageCode
        val targetLanguageCode = tutor.targetLanguageCode

        val sourceLanguage = languageService.getLanguageName(sourceLanguageCode)
        val targetLanguage = languageService.getLanguageName(targetLanguageCode)

        logger.debug("Language pair: $sourceLanguage -> $targetLanguage")

        // Get vocabulary context for the user
        val vocabContext = vocabularyContextService.getVocabularyContext(userId, targetLanguageCode)

        val vocabularyGuidance = buildVocabularyGuidance(vocabContext)
        val teachingStyleGuidance = buildTeachingStyleGuidance(tutor.teachingStyle, targetLanguage)

        // Build system prompt using template
        val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "targetLanguageCode" to targetLanguageCode,
            "sourceLanguage" to sourceLanguage,
            "sourceLanguageCode" to sourceLanguageCode,
            "tutorName" to tutor.name,
            "tutorPersona" to tutor.persona,
            "tutorDomain" to tutor.domain,
            "vocabularyGuidance" to vocabularyGuidance,
            "teachingStyleGuidance" to teachingStyleGuidance
        ))

        // Build phase prompts using templates
        val phaseFreePrompt = PromptTemplate(phaseFreePromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        ))

        val phaseCorrectionPrompt = PromptTemplate(phaseCorrectionPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        ))

        val phaseDrillPrompt = PromptTemplate(phaseDrillPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        ))

        val phasePrompts = mapOf(
            ConversationPhase.Free to phaseFreePrompt,
            ConversationPhase.Correction to phaseCorrectionPrompt,
            ConversationPhase.Drill to phaseDrillPrompt,
        )

        val developerPrompt = PromptTemplate(developerPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "sourceLanguage" to sourceLanguage
        ))

        val systemMessages = listOf(
            SystemMessage(systemPrompt + (phasePrompts[conversationState.phase] ?: phaseFreePrompt)),
            SystemMessage(developerPrompt),
            SystemMessage(conversationState.toString()),
        )

        val compactedMessages = messageCompactionService.compactMessages(systemMessages, messages, sessionId)

        val response = aiChatService.call(AiChatRequest(compactedMessages), onReplyChunk)

        return response?.let {
            TutorResponse(
                reply = it.reply,
                conversationResponse = it.conversationResponse
            )
        }
    }

    private fun buildVocabularyGuidance(vocabContext: ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext): String {
        if (vocabContext.totalWordCount == 0) {
            return PromptTemplate(vocabularyNoTrackingTemplate).render(emptyMap())
        }

        val reinforcementList = if (vocabContext.wordsForReinforcement.isNotEmpty()) {
            vocabContext.wordsForReinforcement.joinToString(", ")
        } else {
            "(none)"
        }

        val recentList = if (vocabContext.recentNewWords.isNotEmpty()) {
            vocabContext.recentNewWords.joinToString(", ")
        } else {
            "(none)"
        }

        val masteredList = if (vocabContext.masteredWords.isNotEmpty()) {
            vocabContext.masteredWords.take(20).joinToString(", ")
        } else {
            "(none)"
        }

        return PromptTemplate(vocabularyWithTrackingTemplate).render(mapOf(
            "totalWordCount" to vocabContext.totalWordCount.toString(),
            "wordsForReinforcement" to reinforcementList,
            "recentNewWords" to recentList,
            "masteredWords" to masteredList
        ))
    }

    private fun buildTeachingStyleGuidance(teachingStyle: TeachingStyle, targetLanguage: String): String {
        val template = when (teachingStyle) {
            TeachingStyle.Reactive -> teachingStyleReactiveTemplate
            TeachingStyle.Guided -> teachingStyleGuidedTemplate
            TeachingStyle.Directive -> teachingStyleDirectiveTemplate
        }
        return PromptTemplate(template).render(mapOf("targetLanguage" to targetLanguage))
    }
}