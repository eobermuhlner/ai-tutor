package ch.obermuhlner.aitutor.auth.dto

import jakarta.validation.constraints.NotBlank

data class VerifyEmailRequest(
    @field:NotBlank(message = "Token is required")
    val token: String
)
