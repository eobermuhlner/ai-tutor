package ch.obermuhlner.aitutor.auth.dto

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null
)
