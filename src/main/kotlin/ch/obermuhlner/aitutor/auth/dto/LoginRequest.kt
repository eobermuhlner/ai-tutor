package ch.obermuhlner.aitutor.auth.dto

data class LoginRequest(
    val username: String,  // Can be username or email
    val password: String
)
