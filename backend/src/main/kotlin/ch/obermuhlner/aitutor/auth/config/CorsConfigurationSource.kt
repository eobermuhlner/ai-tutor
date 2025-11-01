package ch.obermuhlner.aitutor.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@ConfigurationProperties(prefix = "cors")
data class CorsProps(
    val allowedOrigins: List<String> = emptyList(),
    val allowedOriginPatterns: List<String> = emptyList(),
    val allowedMethods: List<String> = listOf("GET", "POST"),
    val allowedHeaders: List<String> = listOf("Authorization", "Content-Type"),
    val exposedHeaders: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
    val maxAge: Long = 3600
)

@Configuration
@EnableConfigurationProperties(CorsProps::class)
class CorsConfig(private val props: CorsProps) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // IMPORTANT: If allowCredentials=true you CANNOT use "*" in allowedOrigins.
        // Use exact origins or allowedOriginPatterns.
        if (props.allowedOrigins.isNotEmpty()) {
            configuration.allowedOrigins = props.allowedOrigins
        }
        if (props.allowedOriginPatterns.isNotEmpty()) {
            configuration.addAllowedOriginPattern(props.allowedOriginPatterns.joinToString(","))
            // Alternatively: props.allowedOriginPatterns.forEach(configuration::addAllowedOriginPattern)
        }

        configuration.allowedMethods = props.allowedMethods
        configuration.allowedHeaders = props.allowedHeaders
        configuration.exposedHeaders = props.exposedHeaders
        configuration.allowCredentials = props.allowCredentials
        configuration.maxAge = props.maxAge

        val source = UrlBasedCorsConfigurationSource()
        // Scope CORS only to your API surface, not static resources or actuator by default
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }
}
