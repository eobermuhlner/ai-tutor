package ch.obermuhlner.aitutor.image.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai-tutor.imagestore")
data class ImageStoreProperties(
    val baseUrl: String = "http://localhost:7080"
)
