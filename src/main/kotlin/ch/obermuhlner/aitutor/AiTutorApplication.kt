package ch.obermuhlner.aitutor

import ch.obermuhlner.aitutor.core.model.ConversationState
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.core.model.Tutor
import ch.obermuhlner.aitutor.tutor.service.TutorService
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class AiTutorApplication(
    private val tutorService: TutorService
) {
    @Bean
    fun runner() = CommandLineRunner { args ->

        val tutor = Tutor(
            name = "Hans",
            sourceLanguageCode = "pt",
            targetLanguageCode = "de"
        )

        val conversationState = ConversationState(
            phase = ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1,
        )

        val messages = listOf(
            //UserMessage("ich gehen nach Hause"), // with grammar error "gehen" -> "gehe"
            UserMessage("Solipsismus ist eine interessante Philosphie."), // with typo "Philosphie" -> "Philosophie
        )

        val response = tutorService.respond(tutor, conversationState, messages)
        println()
        println("Response:")
        println(response)
    }
}

fun main(args: Array<String>) {
    runApplication<AiTutorApplication>(*args)
}
