package ch.obermuhlner.aitutor

import ch.obermuhlner.aitutor.model.ConversationState
import ch.obermuhlner.aitutor.model.CEFRLevel
import ch.obermuhlner.aitutor.model.ConversationPhase
import ch.obermuhlner.aitutor.model.Tutor
import ch.obermuhlner.aitutor.service.TutorService
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
            sourceLanguage = "Portuguese",
            targetLanguage = "German"
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
        println("Reponse:")
        println(response)
    }
}

fun main(args: Array<String>) {
	runApplication<AiTutorApplication>(*args)
}
