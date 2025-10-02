package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.core.util.LlmJson
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.util.json.schema.JsonSchemaGenerator
import org.springframework.stereotype.Service

@Service
class StreamReplyThenJsonEntityAiChatService(
    val chatModel: ChatModel
) : AiChatService {

    override fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse? {
        val schema = JsonSchemaGenerator.generateForType(AiChatResponse::class.java)


        val json = streamUntilJsonAndParse(
            chatModel,
            request.messages + SystemMessage("""
Reply in plain text followed by JSON object fenced with triple backtick json using the following schema: 

```json
$schema
```
            """.trimIndent())
        ) { chunk ->
            onReplyText(chunk)
        }

        return LlmJson.parseAs<AiChatResponse>(json)
    }

    private fun streamUntilJsonAndParse(
        chatModel: ChatModel,
        allMessages: List<Message>,
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

        chatModel.stream(Prompt(allMessages))
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
}
