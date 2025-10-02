package ch.obermuhlner.aitutor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AiTutorApplication() {
}

fun main(args: Array<String>) {
    runApplication<AiTutorApplication>(*args)
}
