package ch.obermuhlner.aitutor.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "google.oauth")
data class GoogleOAuthProperties(
    var clientId: String = "",
    var clientSecret: String = ""
)
