package ch.obermuhlner.aitutor.email.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.email")
data class EmailProperties(
    var from: String = "noreply@ai-tutor.local",
    var fromName: String = "AI Tutor",
    var baseUrl: String = "http://localhost:5173",
    var verificationTokenExpirationHours: Int = 24,
    var passwordResetTokenExpirationHours: Int = 1
)
