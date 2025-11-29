package ch.obermuhlner.aitutor.user.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.stereotype.Component

/**
 * Configuration properties for rate limiting based on subscription plans.
 */
@ConfigurationProperties(prefix = "ai-tutor.rate-limits")
@Component
class RateLimitProperties {
    var free: PlanLimits = PlanLimits()
    var freeByok: PlanLimits = PlanLimits(messagesPerHour = 60, messagesPerDay = 300)
    var premium: PlanLimits = PlanLimits(messagesPerHour = 100, messagesPerDay = 500)

    /**
     * Class representing limits for a single subscription plan.
     */
    class PlanLimits {
        var messagesPerHour: Long = 10L
        var messagesPerDay: Long = 50L

        constructor()
        constructor(messagesPerHour: Long, messagesPerDay: Long) {
            this.messagesPerHour = messagesPerHour
            this.messagesPerDay = messagesPerDay
        }
    }
}