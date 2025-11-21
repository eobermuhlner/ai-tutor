package ch.obermuhlner.aitutor.conversation.dto

import ch.obermuhlner.aitutor.core.model.Correction
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response from LLM containing only error corrections for a user's message.
 * Separate from the main conversational response to reduce LLM context and cognitive burden.
 */
data class CorrectionResponse(
    @field:JsonProperty(required = true)
    val corrections: List<Correction>
)
