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
- Maintain enthusiastic, encouraging conversation focused entirely on communication success.
- Do NOT detect or track errors in your response (the system will handle minimal tracking for statistics only).
- Prioritize building confidence, fluency, and reducing anxiety.
- Never reference accuracy, correctness, or errors in any way.
- Celebrate successful communication and meaning-making.
        """.trimIndent()

        val phaseCorrectionPrompt = """
- Maintain natural, flowing conversation while tracking errors for learner self-discovery.
- Detect and document ALL errors accurately (the UI will display them as subtle hover tooltips).
- NEVER mention errors explicitly in your conversational responses.
- Continue the conversation as if no errors occurred - focus on meaning and engagement.
- Let the learner discover and engage with corrections at their own pace through the UI.
- This creates awareness without disruption, supporting autonomous learning.
        """.trimIndent()

        val phaseDrillPrompt = """
- Maintain supportive conversation while actively addressing errors for accuracy improvement.
- Detect all errors and reference them explicitly when pedagogically valuable.
- Timing: Address errors after the learner completes their thought to avoid interrupting mid-sentence.
- Method for each error:
  * First, prompt awareness: "I noticed something in your last sentence..."
  * Then, encourage self-correction: "Can you spot what needs adjusting?"
  * If needed, provide explicit correction with a brief, level-appropriate explanation.
- Prioritize by severity: Focus on Critical/High-severity errors (comprehension-blocking), then Medium, defer Low-severity unless fossilizing.
- Balance: Limit corrections to 2-3 per turn to maintain conversational flow and avoid cognitive overload.
- Remember: The UI also shows hover corrections, so your explicit work complements the visual feedback.
        """.trimIndent()

        val phasePrompts = mapOf(
            ConversationPhase.Free to phaseFreePrompt,
            ConversationPhase.Correction to phaseCorrectionPrompt,
            ConversationPhase.Drill to phaseDrillPrompt,
        )

        val developerPrompt = """
JSON Response:
- Scope the update to the MOST RECENT user message.
- Always return a single valid JSON object conforming to the schema.
- `corrections`: include an item for EVERY error you detect in that message; if none, set `corrections: []`.
- `span` values must be verbatim substrings of the last user message.
- `conversationState.estimatedCEFRLevel`: set to A1–C2 based on recent performance (consider ≥ last 3–5 user turns).
- `conversationState.phase`: set to "Free", "Correction", or "Drill" for the current turn.
- Do not add fields not present in the schema. Keep explanations concise and level-appropriate (see schema descriptions).

Error Severity Assignment (CRITICAL):
- Critical: Meaning completely lost, no context helps comprehension
- High: Significant barrier, native speaker would be confused (global error)
- Medium: Grammar error but meaning clear from context
- Low: Minor issue OR acceptable in casual chat/texting (local error)

Chat Context Rules for Typography errors:
- Missing accents/diacritics (café→cafe) in casual chat: Low or ignore
- No capitalization at start: Low or ignore (chat norm)
- Missing end punctuation (period, question mark): Low or ignore (chat norm)
- Quick typos that don't change word meaning: Low
- Ask: "Would a native speaker make this 'error' in casual texting?" If YES → Low/ignore

Severity Guidelines by ErrorType:
- WordOrder, Lexis (wrong word): Usually High/Critical
- TenseAspect: Medium-High (depends if meaning changes)
- Agreement, Morphology, Pronouns: Usually Medium
- Prepositions: Low-Medium (context-dependent)
- Articles: Usually Low (rarely blocks comprehension)
- Typography: Context-dependent (see chat rules above)
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