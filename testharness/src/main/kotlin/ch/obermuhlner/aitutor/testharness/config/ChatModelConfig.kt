package ch.obermuhlner.aitutor.testharness.config

import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value

@Configuration
class ChatModelSelectionConfig {

    @Value("\${testharness.learners.default-model:openai}")
    private lateinit var learnerModelType: String

    @Value("\${testharness.judges.default-model:openai}")
    private lateinit var judgeModelType: String

    // For now, let's create a simple config that doesn't interfere with the autoconfigured beans
    // The ChatModel beans will be autoconfigured, and we'll select the right one in the services
}