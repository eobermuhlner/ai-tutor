package ch.obermuhlner.aitutor.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "",
    var expirationMs: Long = 3600000,  // 1 hour
    var refreshExpirationMs: Long = 2592000000  // 30 days
)
