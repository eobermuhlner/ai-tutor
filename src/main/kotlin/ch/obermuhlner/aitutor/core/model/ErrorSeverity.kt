package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class ErrorSeverity {
    @JsonPropertyDescription("Completely blocks comprehension - meaning is entirely lost")
    Critical,

    @JsonPropertyDescription("Significantly impairs comprehension - native speaker would be confused (global error)")
    High,

    @JsonPropertyDescription("Noticeable grammar issue but meaning is clear from context")
    Medium,

    @JsonPropertyDescription("Minor issue or acceptable in casual chat/texting - doesn't affect comprehension (local error)")
    Low
}
