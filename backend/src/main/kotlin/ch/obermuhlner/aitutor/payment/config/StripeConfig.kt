package ch.obermuhlner.aitutor.payment.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "stripe")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeConfig {
    var enabled: Boolean = false
    lateinit var apiKey: String
    lateinit var webhookSecret: String
    lateinit var priceIdSubscription10: String
    lateinit var successUrl: String
    lateinit var cancelUrl: String

    @PostConstruct
    fun init() {
        Stripe.apiKey = apiKey
    }
}
