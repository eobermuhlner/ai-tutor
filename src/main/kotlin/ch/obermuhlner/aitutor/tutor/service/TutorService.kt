package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.core.model.ConversationResponse
import ch.obermuhlner.aitutor.core.model.ConversationState
import ch.obermuhlner.aitutor.core.model.Tutor
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.stereotype.Service
import java.util.*

@Service
class TutorService(
    private val aiChatService: AiChatService,
    private val languageService: LanguageService,
    private val vocabularyContextService: VocabularyContextService
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

//        val schema = JsonSchemaGenerator.generateForType(ConversationResponse::class.java)
//        println(schema)
//        val responseSchema = Placeholder.substitute(schema, mapOf(
//            "sourceLanguage" to sourceLanguage,
//            "targetLanguage" to targetLanguage,
//        ))

        val vocabularyGuidance = buildVocabularyGuidance(vocabContext)

        val systemPrompt = """
You are a language tutor teaching the target language $targetLanguage (code: $targetLanguageCode) to a learner that speaks the source language $sourceLanguage (code: $sourceLanguageCode).
Your name: ${tutor.name}.
Your persona: ${tutor.persona}
Your domain: ${tutor.domain}
Source language: $sourceLanguage
Target language: $targetLanguage

Role:
- Communicate in $targetLanguage by default.
- Use $sourceLanguage only when the learner explicitly asks you to explain or when comprehension is estimated below 40%.
- Never mix $sourceLanguage and $targetLanguage in the same output.
- Stay within pedagogical scope. Do not provide medical, legal, or unrelated advice.

Objective:
- Maximize learner output in $targetLanguage.
- Support long-term learning through contextual practice, not lectures.

$vocabularyGuidance
        """.trimIndent()

        val phaseFreePrompt = """
- Keep a fluid and natural conversation with the learner.
- Do not correct learner's errors in the conversation.
        """.trimIndent()


        val phaseDrillPrompt = """
- Keep a fluid and natural conversation with the learner.
- Correct errors with minimal disruption.
        """.trimIndent()

        val phasePrompts = mapOf(
            ConversationPhase.Free to phaseFreePrompt,
            ConversationPhase.Drill to phaseDrillPrompt,
        )

        val developerPrompt = """
JSON Response:
- Scope the update to the MOST RECENT user message.
- Always return a single valid JSON object conforming to the schema.
- `corrections`: include an item for EVERY error you detect in that message; if none, set `corrections: []`.
- `span` values must be verbatim substrings of the last user message.
- `conversationState.estimatedCEFRLevel`: set to A1–C2 based on recent performance (consider ≥ last 3–5 user turns).
- `conversationState.phase`: set to "Drill" or "Free" for the current turn.
- Do not add fields not present in the schema. Keep explanations concise and level-appropriate (see schema descriptions).
        """.trimIndent()

        val allMessages = listOf(
            SystemMessage(systemPrompt + (phasePrompts[conversationState.phase] ?: phaseFreePrompt)),
            SystemMessage(developerPrompt),
            SystemMessage(conversationState.toString()),
        ) + messages

        val response = aiChatService.call(AiChatService.AiChatRequest(allMessages), onReplyChunk)

        return response?.let {
            TutorResponse(
                reply = it.reply,
                conversationResponse = it.conversationResponse
            )
        }
    }

    private fun buildVocabularyGuidance(vocabContext: ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext): String {
        if (vocabContext.totalWordCount == 0) {
            return """
Vocabulary Guidance:
- This learner has no tracked vocabulary yet.
- Introduce new vocabulary gradually (1-3 new words per turn maximum).
- Keep vocabulary simple and contextual.
            """.trimIndent()
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

        return """
Vocabulary Guidance:
- Total vocabulary: ${vocabContext.totalWordCount} words tracked
- Words to reinforce naturally in conversation: $reinforcementList
- Recently introduced (avoid re-introducing): $recentList
- Mastered words (don't over-explain): $masteredList
- When introducing NEW vocabulary, do so gradually (1-3 new words per turn maximum)
- Use reinforcement words in natural contexts to aid retention
        """.trimIndent()
    }
}