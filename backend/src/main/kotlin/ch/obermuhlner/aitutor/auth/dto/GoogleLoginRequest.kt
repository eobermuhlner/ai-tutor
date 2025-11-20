package ch.obermuhlner.aitutor.auth.dto

data class GoogleLoginRequest(
    val googleToken: String  // Google ID token from frontend
)
