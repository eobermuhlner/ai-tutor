package ch.obermuhlner.aitutor.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allow frontend origin
        configuration.allowedOrigins = listOf("http://localhost:5173", "http://localhost:5174")

        // Allow all HTTP methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

        // Allow all headers
        configuration.allowedHeaders = listOf("*")

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Cache preflight response for 1 hour
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)

        return source
    }
}
