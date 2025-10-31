package ch.obermuhlner.aitutor

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import ch.obermuhlner.aitutor.config.OpenApiConfig

@OpenAPIDefinition(
    info = Info(
        title = "AI Tutor API",
        description = "REST API for the AI Tutor application - a language learning assistant with conversational AI tutoring and vocabulary tracking",
        version = "1.0.0",
        contact = Contact(
            name = "AI Tutor Support",
            email = "support@aitutor.example.com"
        )
    ),
    security = [SecurityRequirement(name = "bearerAuth")]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@SpringBootApplication
@ConfigurationPropertiesScan
@Import(OpenApiConfig::class)
class AiTutorApplication() {
}

fun main(args: Array<String>) {
    runApplication<AiTutorApplication>(*args)
}
