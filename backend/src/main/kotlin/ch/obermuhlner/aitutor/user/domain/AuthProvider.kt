package ch.obermuhlner.aitutor.user.domain

enum class AuthProvider {
    CREDENTIALS,  // Username/password
    GOOGLE,       // Google OAuth2 (future)
    GITHUB,       // GitHub OAuth2 (future)
    FACEBOOK      // Facebook OAuth2 (future)
}
