package ch.obermuhlner.aitutor.auth.dto

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
