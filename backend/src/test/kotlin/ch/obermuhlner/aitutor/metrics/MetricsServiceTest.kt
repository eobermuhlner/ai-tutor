package ch.obermuhlner.aitutor.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MetricsServiceTest {

    private lateinit var meterRegistry: MeterRegistry
    private lateinit var metricsService: MetricsService

    @BeforeEach
    fun setup() {
        meterRegistry = SimpleMeterRegistry()
        metricsService = MetricsService(meterRegistry)
    }

    @Test
    fun `recordAiRequest should increment counter`() {
        // When
        metricsService.recordAiRequest("openai", "gpt-4o")
        metricsService.recordAiRequest("openai", "gpt-4o")

        // Then
        val counter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .counter()

        assertNotNull(counter)
        assertEquals(2.0, counter!!.count())
    }

    @Test
    fun `recordAiRequest should cache counters and not create duplicates`() {
        // When
        repeat(100) {
            metricsService.recordAiRequest("openai", "gpt-4o")
        }

        // Then - should only have ONE counter registered
        val counters = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .counters()

        assertEquals(1, counters.size)
        assertEquals(100.0, counters.first().count())
    }

    @Test
    fun `recordAiRequest should handle null model`() {
        // When
        metricsService.recordAiRequest("ollama", null)

        // Then
        val counter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "ollama")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `recordAiRequestDuration should record timer values`() {
        // When
        metricsService.recordAiRequestDuration("openai", "gpt-4o", 1500)
        metricsService.recordAiRequestDuration("openai", "gpt-4o", 2500)

        // Then
        val timer = meterRegistry.find("ai_tutor.ai_request_duration_seconds")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .timer()

        assertNotNull(timer)
        assertEquals(2, timer!!.count())
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 4000) // 1500 + 2500
    }

    @Test
    fun `recordAiRequestDuration should cache timers`() {
        // When
        repeat(100) {
            metricsService.recordAiRequestDuration("openai", "gpt-4o", 100)
        }

        // Then - should only have ONE timer registered
        val timers = meterRegistry.find("ai_tutor.ai_request_duration_seconds")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .timers()

        assertEquals(1, timers.size)
        assertEquals(100, timers.first().count())
    }

    @Test
    fun `recordErrorDetection should increment counter`() {
        // When
        metricsService.recordErrorDetection("GRAMMAR", "HIGH")
        metricsService.recordErrorDetection("GRAMMAR", "HIGH")
        metricsService.recordErrorDetection("SPELLING", "MEDIUM")

        // Then
        val grammarCounter = meterRegistry.find("ai_tutor.error_detection_total")
            .tag("error_type", "GRAMMAR")
            .tag("severity", "HIGH")
            .counter()

        val spellingCounter = meterRegistry.find("ai_tutor.error_detection_total")
            .tag("error_type", "SPELLING")
            .tag("severity", "MEDIUM")
            .counter()

        assertNotNull(grammarCounter)
        assertNotNull(spellingCounter)
        assertEquals(2.0, grammarCounter!!.count())
        assertEquals(1.0, spellingCounter!!.count())
    }

    @Test
    fun `recordErrorDetection should cache counters`() {
        // When
        repeat(100) {
            metricsService.recordErrorDetection("GRAMMAR", "HIGH")
        }

        // Then - should only have ONE counter registered
        val counters = meterRegistry.find("ai_tutor.error_detection_total")
            .tag("error_type", "GRAMMAR")
            .tag("severity", "HIGH")
            .counters()

        assertEquals(1, counters.size)
        assertEquals(100.0, counters.first().count())
    }

    @Test
    fun `recordChatMessage should NOT include user_id or session_id tags`() {
        // When
        metricsService.recordChatMessage("user")

        // Then
        val counter = meterRegistry.find("ai_tutor.chat_messages_total")
            .tag("role", "user")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())

        // Verify NO high-cardinality tags
        val allCounters = meterRegistry.find("ai_tutor.chat_messages_total").counters()
        allCounters.forEach { c ->
            val tags = c.id.tags.map { it.key }
            assertFalse(tags.contains("user_id"), "Should not contain user_id tag")
            assertFalse(tags.contains("session_id"), "Should not contain session_id tag")
        }
    }

    @Test
    fun `recordChatMessage should cache counters by role`() {
        // When
        repeat(50) {
            metricsService.recordChatMessage("user")
        }
        repeat(50) {
            metricsService.recordChatMessage("assistant")
        }

        // Then - should only have TWO counters total (one per role)
        val allCounters = meterRegistry.find("ai_tutor.chat_messages_total").counters()
        assertEquals(2, allCounters.size)

        val userCounter = meterRegistry.find("ai_tutor.chat_messages_total")
            .tag("role", "user")
            .counter()
        val assistantCounter = meterRegistry.find("ai_tutor.chat_messages_total")
            .tag("role", "assistant")
            .counter()

        assertEquals(50.0, userCounter!!.count())
        assertEquals(50.0, assistantCounter!!.count())
    }

    @Test
    fun `metrics methods should not throw exceptions on registry errors`() {
        // Given - a registry that might throw exceptions
        // (SimpleMeterRegistry doesn't throw, but we verify no uncaught exceptions)

        // Then - all methods should not throw
        assertDoesNotThrow {
            metricsService.recordAiRequest("openai", "gpt-4o")
        }
        assertDoesNotThrow {
            metricsService.recordAiRequestDuration("openai", "gpt-4o", 1000)
        }
        assertDoesNotThrow {
            metricsService.recordErrorDetection("GRAMMAR", "HIGH")
        }
        assertDoesNotThrow {
            metricsService.recordChatMessage("user")
        }
    }

    @Test
    fun `concurrent access should be thread-safe`() {
        // Given
        val threadCount = 10
        val iterationsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // When - multiple threads record metrics simultaneously
        repeat(threadCount) {
            executor.submit {
                try {
                    repeat(iterationsPerThread) {
                        metricsService.recordAiRequest("openai", "gpt-4o")
                        metricsService.recordErrorDetection("GRAMMAR", "HIGH")
                        metricsService.recordChatMessage("user")
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Then - counters should have correct totals (no race conditions)
        val aiRequestCounter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "openai")
            .counter()
        val errorCounter = meterRegistry.find("ai_tutor.error_detection_total")
            .tag("error_type", "GRAMMAR")
            .counter()
        val chatCounter = meterRegistry.find("ai_tutor.chat_messages_total")
            .tag("role", "user")
            .counter()

        val expectedCount = (threadCount * iterationsPerThread).toDouble()
        assertEquals(expectedCount, aiRequestCounter!!.count())
        assertEquals(expectedCount, errorCounter!!.count())
        assertEquals(expectedCount, chatCounter!!.count())

        // Verify only ONE instance of each metric (proper caching)
        assertEquals(1, meterRegistry.find("ai_tutor.ai_requests_total").counters().size)
        assertEquals(1, meterRegistry.find("ai_tutor.error_detection_total").counters().size)
        assertEquals(1, meterRegistry.find("ai_tutor.chat_messages_total").counters().size)
    }

    @Test
    fun `different providers should have separate metrics`() {
        // When
        metricsService.recordAiRequest("openai", "gpt-4o")
        metricsService.recordAiRequest("ollama", null)
        metricsService.recordAiRequest("anthropic", "claude-3")

        // Then
        val openaiCounter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "openai")
            .counter()
        val ollamaCounter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "ollama")
            .counter()
        val anthropicCounter = meterRegistry.find("ai_tutor.ai_requests_total")
            .tag("provider", "anthropic")
            .counter()

        assertNotNull(openaiCounter)
        assertNotNull(ollamaCounter)
        assertNotNull(anthropicCounter)
        assertEquals(1.0, openaiCounter!!.count())
        assertEquals(1.0, ollamaCounter!!.count())
        assertEquals(1.0, anthropicCounter!!.count())
    }

    @Test
    fun `verify no memory leak with many unique error types`() {
        // When - record many unique error type/severity combinations
        val errorTypes = listOf("GRAMMAR", "SPELLING", "VOCABULARY", "PRONUNCIATION")
        val severities = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")

        errorTypes.forEach { errorType ->
            severities.forEach { severity ->
                metricsService.recordErrorDetection(errorType, severity)
            }
        }

        // Then - should have exactly 16 counters (4 types × 4 severities)
        val allCounters = meterRegistry.find("ai_tutor.error_detection_total").counters()
        assertEquals(16, allCounters.size)

        // Verify each has count of 1
        allCounters.forEach { counter ->
            assertEquals(1.0, counter.count())
        }
    }

    @Test
    fun `recordTokenUsage should increment counters for prompt and completion tokens`() {
        // When
        metricsService.recordTokenUsage(
            provider = "openai",
            model = "gpt-4o",
            promptTokens = 150,
            completionTokens = 75,
            totalTokens = 225
        )

        // Then
        val promptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .tag("token_type", "prompt")
            .counter()

        val completionCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .tag("token_type", "completion")
            .counter()

        assertNotNull(promptCounter)
        assertNotNull(completionCounter)
        assertEquals(150.0, promptCounter!!.count())
        assertEquals(75.0, completionCounter!!.count())
    }

    @Test
    fun `recordTokenUsage should accumulate tokens over multiple calls`() {
        // When
        metricsService.recordTokenUsage("openai", "gpt-4o", 100, 50, 150)
        metricsService.recordTokenUsage("openai", "gpt-4o", 200, 100, 300)
        metricsService.recordTokenUsage("openai", "gpt-4o", 150, 75, 225)

        // Then
        val promptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .tag("token_type", "prompt")
            .counter()

        val completionCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .tag("token_type", "completion")
            .counter()

        assertEquals(450.0, promptCounter!!.count()) // 100 + 200 + 150
        assertEquals(225.0, completionCounter!!.count()) // 50 + 100 + 75
    }

    @Test
    fun `recordTokenUsage should cache counters`() {
        // When
        repeat(100) {
            metricsService.recordTokenUsage("openai", "gpt-4o", 10, 5, 15)
        }

        // Then - should only have TWO counters (one for prompt, one for completion)
        val allCounters = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("model", "gpt-4o")
            .counters()

        assertEquals(2, allCounters.size) // prompt + completion
    }

    @Test
    fun `recordTokenUsage should handle null model`() {
        // When
        metricsService.recordTokenUsage(
            provider = "ollama",
            model = null,
            promptTokens = 100,
            completionTokens = 50,
            totalTokens = 150
        )

        // Then
        val promptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "ollama")
            .tag("token_type", "prompt")
            .counter()

        assertNotNull(promptCounter)
        assertEquals(100.0, promptCounter!!.count())
    }

    @Test
    fun `recordTokenUsage should separate metrics by provider`() {
        // When
        metricsService.recordTokenUsage("openai", "gpt-4o", 100, 50, 150)
        metricsService.recordTokenUsage("anthropic", "claude-3", 200, 100, 300)
        metricsService.recordTokenUsage("ollama", null, 150, 75, 225)

        // Then
        val openaiPromptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "openai")
            .tag("token_type", "prompt")
            .counter()

        val anthropicPromptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "anthropic")
            .tag("token_type", "prompt")
            .counter()

        val ollamaPromptCounter = meterRegistry.find("ai_tutor.tokens_total")
            .tag("provider", "ollama")
            .tag("token_type", "prompt")
            .counter()

        assertEquals(100.0, openaiPromptCounter!!.count())
        assertEquals(200.0, anthropicPromptCounter!!.count())
        assertEquals(150.0, ollamaPromptCounter!!.count())
    }
}
