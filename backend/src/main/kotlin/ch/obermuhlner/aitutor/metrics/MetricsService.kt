package ch.obermuhlner.aitutor.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Service for managing AI Tutor application metrics.
 * Provides methods to record AI requests, chat interactions, and other business metrics.
 * All metrics are cached to prevent memory leaks and wrapped in error handling.
 */
@Service
class MetricsService(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Cache all metric instances to prevent memory leaks
    private val aiRequestCounters = ConcurrentHashMap<String, Counter>()
    private val aiRequestTimers = ConcurrentHashMap<String, Timer>()
    private val errorCounters = ConcurrentHashMap<String, Counter>()
    private val messageCounters = ConcurrentHashMap<String, Counter>()
    private val tokenCounters = ConcurrentHashMap<String, Counter>()

    /**
     * Records an AI request for a specific provider.
     * Safe to call - will not throw exceptions.
     */
    fun recordAiRequest(provider: String, model: String? = null) {
        try {
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
        } catch (e: Exception) {
            logger.error("Failed to record AI request metric for provider=$provider, model=$model", e)
        }
    }

    /**
     * Records an AI request duration for a specific provider.
     * Safe to call - will not throw exceptions.
     */
    fun recordAiRequestDuration(provider: String, model: String? = null, durationMs: Long) {
        try {
            val tags = mutableListOf(
                Tag.of("provider", provider)
            )
            if (!model.isNullOrBlank()) {
                tags.add(Tag.of("model", model))
            }

            val key = "$provider${model?.let { "_$it" } ?: ""}"
            val timer = aiRequestTimers.computeIfAbsent(key) {
                Timer.builder("ai_tutor.ai_request_duration_seconds")
                    .description("Duration of AI requests in seconds")
                    .tags(tags)
                    .register(meterRegistry)
            }
            timer.record(durationMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            logger.error("Failed to record AI request duration metric for provider=$provider, model=$model, durationMs=$durationMs", e)
        }
    }

    /**
     * Records an error detection event.
     * Safe to call - will not throw exceptions.
     */
    fun recordErrorDetection(errorType: String, severity: String) {
        try {
            val key = "${errorType}_${severity}"
            val counter = errorCounters.computeIfAbsent(key) {
                Counter.builder("ai_tutor.error_detection_total")
                    .description("Total number of errors detected by the tutor")
                    .tag("error_type", errorType)
                    .tag("severity", severity)
                    .register(meterRegistry)
            }
            counter.increment()
        } catch (e: Exception) {
            logger.error("Failed to record error detection metric for errorType=$errorType, severity=$severity", e)
        }
    }

    /**
     * Records a chat message by role (user/assistant).
     * Does NOT track user_id or session_id to prevent high cardinality and privacy issues.
     * Safe to call - will not throw exceptions.
     */
    fun recordChatMessage(role: String) {
        try {
            val counter = messageCounters.computeIfAbsent(role) {
                Counter.builder("ai_tutor.chat_messages_total")
                    .description("Total number of chat messages processed")
                    .tag("role", role)
                    .register(meterRegistry)
            }
            counter.increment()
        } catch (e: Exception) {
            logger.error("Failed to record chat message metric for role=$role", e)
        }
    }

    /**
     * Records token usage from AI requests.
     * Tracks prompt tokens, completion tokens, and total tokens by provider and model.
     * Safe to call - will not throw exceptions.
     *
     * @param provider AI provider (openai, ollama, anthropic, etc.)
     * @param model Optional model name
     * @param promptTokens Number of tokens in the prompt
     * @param completionTokens Number of tokens in the completion
     * @param totalTokens Total tokens used (prompt + completion)
     */
    fun recordTokenUsage(
        provider: String,
        model: String?,
        promptTokens: Long,
        completionTokens: Long,
        totalTokens: Long
    ) {
        try {
            val tags = mutableListOf(
                Tag.of("provider", provider),
                Tag.of("token_type", "prompt")
            )
            if (!model.isNullOrBlank()) {
                tags.add(Tag.of("model", model))
            }

            // Record prompt tokens
            val promptKey = "$provider${model?.let { "_$it" } ?: ""}_prompt"
            val promptCounter = tokenCounters.computeIfAbsent(promptKey) {
                Counter.builder("ai_tutor.tokens_total")
                    .description("Total number of tokens used by AI requests")
                    .tags(tags)
                    .register(meterRegistry)
            }
            promptCounter.increment(promptTokens.toDouble())

            // Record completion tokens
            val completionTags = mutableListOf(
                Tag.of("provider", provider),
                Tag.of("token_type", "completion")
            )
            if (!model.isNullOrBlank()) {
                completionTags.add(Tag.of("model", model))
            }

            val completionKey = "$provider${model?.let { "_$it" } ?: ""}_completion"
            val completionCounter = tokenCounters.computeIfAbsent(completionKey) {
                Counter.builder("ai_tutor.tokens_total")
                    .description("Total number of tokens used by AI requests")
                    .tags(completionTags)
                    .register(meterRegistry)
            }
            completionCounter.increment(completionTokens.toDouble())

        } catch (e: Exception) {
            logger.error("Failed to record token usage metric for provider=$provider, model=$model", e)
        }
    }
}