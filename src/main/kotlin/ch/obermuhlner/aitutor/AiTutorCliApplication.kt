package ch.obermuhlner.aitutor

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(AiTutorApplication::class.java)
        .web(WebApplicationType.NONE)
        .run(*args)
}
