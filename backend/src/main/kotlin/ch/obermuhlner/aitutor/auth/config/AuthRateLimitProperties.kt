package ch.obermuhlner.aitutor.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.rate-limit.auth")
data class AuthRateLimitProperties(
    var capacity: Int = 10,
    var refillTokens: Int = 10,
    var refillPeriodMinutes: Int = 1
)
