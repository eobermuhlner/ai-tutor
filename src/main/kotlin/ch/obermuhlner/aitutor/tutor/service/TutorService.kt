package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class TutorService(
    private val aiChatService: AiChatService,
    private val languageService: LanguageService,
    private val vocabularyContextService: VocabularyContextService,
    @Value("\${ai-tutor.prompts.system}") private val systemPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-free}") private val phaseFreePromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-correction}") private val phaseCorrectionPromptTemplate: String,
    @Value("\${ai-tutor.prompts.phase-drill}") private val phaseDrillPromptTemplate: String,
    @Value("\${ai-tutor.prompts.developer}") private val developerPromptTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.no-tracking}") private val vocabularyNoTrackingTemplate: String,
    @Value("\${ai-tutor.prompts.vocabulary.with-tracking}") private val vocabularyWithTrackingTemplate: String
) {
    data class TutorResponse(
        val reply: String,
        val conversationResponse: ConversationResponse
    )

    fun respond(
        tutor: Tutor,
        conversationState: ConversationState,
        userId: UUID,
        messages: List<Message>,
        onReplyChunk: (String) -> Unit = { print(it) }
    ): TutorResponse? {

        val sourceLanguageCode = tutor.sourceLanguageCode
        val targetLanguageCode = tutor.targetLanguageCode

        val sourceLanguage = languageService.getLanguageName(sourceLanguageCode)
        val targetLanguage = languageService.getLanguageName(targetLanguageCode)

        // Get vocabulary context for the user
        val vocabContext = vocabularyContextService.getVocabularyContext(userId, targetLanguageCode)

        val vocabularyGuidance = buildVocabularyGuidance(vocabContext)

        // Build system prompt using template
        val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
            "targetLanguage" to targetLanguage,
            "targetLanguageCode" to targetLanguageCode,
            "sourceLanguage" to sourceLanguage,
            "sourceLanguageCode" to sourceLanguageCode,
            "tutorName" to tutor.name,
            "tutorPersona" to tutor.persona,
            "tutorDomain" to tutor.domain,
            "vocabularyGuidance" to vocabularyGuidance
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

        val allMessages = listOf(
            SystemMessage(systemPrompt + (phasePrompts[conversationState.phase] ?: phaseFreePrompt)),
            SystemMessage(developerPrompt),
            SystemMessage(conversationState.toString()),
        ) + messages

        val response = aiChatService.call(AiChatRequest(allMessages), onReplyChunk)

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
}