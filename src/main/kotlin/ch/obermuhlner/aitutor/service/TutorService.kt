package ch.obermuhlner.aitutor.service

import ch.obermuhlner.aitutor.model.ConversationPhase
import ch.obermuhlner.aitutor.model.ConversationResponse
import ch.obermuhlner.aitutor.model.ConversationState
import ch.obermuhlner.aitutor.model.Tutor
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.util.json.schema.JsonSchemaGenerator
import org.springframework.stereotype.Service

@Service
class TutorService(
    private val aiChatService: AiChatService
) {
    fun respond(tutor: Tutor, conversationState: ConversationState, messages: List<Message>): ConversationResponse? {

        val sourceLanguage = tutor.sourceLanguage
        val targetLanguage = tutor.targetLanguage

        val schema = JsonSchemaGenerator.generateForType(ConversationResponse::class.java)
        println(schema)
//        val responseSchema = Placeholder.substitute(schema, mapOf(
//            "sourceLanguage" to sourceLanguage,
//            "targetLanguage" to targetLanguage,
//        ))

        val systemPrompt = """
You are a language tutor teaching the target language $targetLanguage to a learner that speaks the source language $sourceLanguage.
Your name: ${tutor.name}.
Your persona: ${tutor.persona} 
Your domain: ${tutor.domain} 
Source language: $sourceLanguage
Target language: $sourceLanguage

Role:
- Communicate in $targetLanguage by default.
- Use $sourceLanguage only when the learner explicitly asks you to explain or when comprehension is estimated below 40%.
- Never mix $sourceLanguage and $targetLanguage in the same output.
- Stay within pedagogical scope. Do not provide medical, legal, or unrelated advice.

Objective:
- Maximize learner output in $targetLanguage.
- Support long-term learning through contextual practice, not lectures.
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
- `conversationState.phase`: set to "Drill" or "Free" for the current turn (echo orchestrator state if provided).
- Do not add fields not present in the schema. Keep explanations concise and level-appropriate (see schema descriptions).
        """.trimIndent()

        val allMessages = listOf(
            org.springframework.ai.chat.messages.SystemMessage(systemPrompt + (phasePrompts[conversationState.phase] ?: phaseFreePrompt)),
            org.springframework.ai.chat.messages.SystemMessage(developerPrompt),
            org.springframework.ai.chat.messages.SystemMessage(conversationState.toString()),
        ) + messages

        val response = aiChatService.call(AiChatService.AiChatRequest(allMessages)) { replyChunk ->
            print(replyChunk)
        }
        println()

        return response?.conversationResponse
    }
}

