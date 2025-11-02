package ch.obermuhlner.aitutor.config

import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

/**
 * Configuration for ChatModel dependencies.
 *
 * Provides default beans for RetryTemplate and ObservationRegistry
 * if they're not already configured by Spring AI autoconfiguration.
 *
 * Note: ToolCallingManager is provided by Spring AI autoconfiguration.
 */
@Configuration
class ChatModelConfig {

    @Bean
    @ConditionalOnMissingBean
    fun retryTemplate(): RetryTemplate {
        return RetryTemplate()
    }

    @Bean
    @ConditionalOnMissingBean
    fun observationRegistry(): ObservationRegistry {
        return ObservationRegistry.NOOP
    }
}
