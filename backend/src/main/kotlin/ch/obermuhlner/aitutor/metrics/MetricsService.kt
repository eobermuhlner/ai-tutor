package ch.obermuhlner.aitutor.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing AI Tutor application metrics.
 * Provides methods to record AI requests, chat interactions, and other business metrics.
 */
@Service
class MetricsService(
    private val meterRegistry: MeterRegistry
) {
    private val aiRequestCounters = ConcurrentHashMap<String, Counter>()
    private val aiRequestTimers = ConcurrentHashMap<String, Timer>()

    /**
     * Records an AI request for a specific provider
     */
    fun recordAiRequest(provider: String, model: String? = null) {
        val tags = mutableListOf(
            Tag.of("provider", provider)
        )
        if (!model.isNullOrBlank()) {
            tags.add(Tag.of("model", model))
        }

        val key = "$provider${model?.let { "_$it" } ?: ""}"
        val counter = aiRequestCounters.computeIfAbsent(key) {
            Counter.builder("ai_tutor.ai_requests_total")
                .description("Total number of AI requests made to different providers")
                .tags(tags)
                .register(meterRegistry)
        }
        counter.increment()
    }

    /**
     * Records an AI request duration for a specific provider
     */
    fun recordAiRequestDuration(provider: String, model: String? = null, durationMs: Long) {
        val tags = mutableListOf(
            Tag.of("provider", provider)
        )
        if (!model.isNullOrBlank()) {
            tags.add(Tag.of("model", model))
        }

        Timer.builder("ai_tutor.ai_request_duration_seconds")
            .description("Duration of AI requests in seconds")
            .tags(tags)
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    /**
     * Records an error detection event
     */
    fun recordErrorDetection(errorType: String, severity: String) {
        Counter.builder("ai_tutor.error_detection_total")
            .description("Total number of errors detected by the tutor")
            .tag("error_type", errorType)
            .tag("severity", severity)
            .register(meterRegistry)
            .increment()
    }

    /**
     * Records a chat message
     */
    fun recordChatMessage(userId: String, sessionId: String, role: String) {
        Counter.builder("ai_tutor.chat_messages_total")
            .description("Total number of chat messages processed")
            .tag("user_id", userId)
            .tag("session_id", sessionId)
            .tag("role", role)
            .register(meterRegistry)
            .increment()
    }
}