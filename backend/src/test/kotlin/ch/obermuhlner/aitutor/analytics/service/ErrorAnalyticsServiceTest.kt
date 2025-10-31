package ch.obermuhlner.aitutor.analytics.service

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.analytics.domain.RecentErrorSampleEntity
import ch.obermuhlner.aitutor.analytics.repository.ErrorPatternRepository
import ch.obermuhlner.aitutor.analytics.repository.RecentErrorSampleRepository
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import io.mockk.*
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

class ErrorAnalyticsServiceTest {

    private lateinit var errorPatternRepository: ErrorPatternRepository
    private lateinit var recentErrorSampleRepository: RecentErrorSampleRepository
    private lateinit var errorAnalyticsService: ErrorAnalyticsService

    @BeforeEach
    fun setup() {
        errorPatternRepository = mockk()
        recentErrorSampleRepository = mockk()
        errorAnalyticsService = ErrorAnalyticsService(
            errorPatternRepository,
            recentErrorSampleRepository
        )
    }

    @Test
    fun `recordErrors should do nothing when corrections list is empty`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()

        errorAnalyticsService.recordErrors(userId, "es", messageId, emptyList())

        verify(exactly = 0) { errorPatternRepository.save(any()) }
        verify(exactly = 0) { recentErrorSampleRepository.save(any()) }
    }

    @Test
    fun `recordErrors should create new pattern when none exists`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val correction = Correction(
            span = "gehen",
            errorType = ErrorType.Agreement,
            severity = ErrorSeverity.High,
            correctedTargetLanguage = "gehe",
            whySourceLanguage = "Verb conjugation",
            whyTargetLanguage = "Konjugation"
        )

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(userId, "de", ErrorType.Agreement) } returns null
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 1

        errorAnalyticsService.recordErrors(userId, "de", messageId, listOf(correction))

        verify {
            errorPatternRepository.save(match<ErrorPatternEntity> {
                it.userId == userId &&
                it.lang == "de" &&
                it.errorType == ErrorType.Agreement &&
                it.totalCount == 1 &&
                it.highCount == 1 &&
                it.criticalCount == 0 &&
                it.mediumCount == 0 &&
                it.lowCount == 0
            })
        }

        verify {
            recentErrorSampleRepository.save(match<RecentErrorSampleEntity> {
                it.userId == userId &&
                it.lang == "de" &&
                it.errorType == ErrorType.Agreement &&
                it.severity == ErrorSeverity.High &&
                it.messageId == messageId &&
                it.errorSpan == "gehen"
            })
        }
    }

    @Test
    fun `recordErrors should update existing pattern`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val existingPattern = ErrorPatternEntity(
            userId = userId,
            lang = "es",
            errorType = ErrorType.TenseAspect,
            totalCount = 5,
            criticalCount = 1,
            highCount = 2,
            mediumCount = 2,
            lowCount = 0,
            firstSeenAt = Instant.now().minusSeconds(86400),
            lastSeenAt = Instant.now().minusSeconds(3600)
        )

        val correction = Correction(
            span = "fui",
            errorType = ErrorType.TenseAspect,
            severity = ErrorSeverity.Medium,
            correctedTargetLanguage = "iba",
            whySourceLanguage = "Past tense",
            whyTargetLanguage = "Tiempo pasado"
        )

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(userId, "es", ErrorType.TenseAspect) } returns existingPattern
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 6

        errorAnalyticsService.recordErrors(userId, "es", messageId, listOf(correction))

        verify {
            errorPatternRepository.save(match<ErrorPatternEntity> {
                it.totalCount == 6 &&
                it.criticalCount == 1 &&
                it.highCount == 2 &&
                it.mediumCount == 3 &&
                it.lowCount == 0
            })
        }
    }

    @Test
    fun `recordErrors should handle multiple corrections with different severities`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val corrections = listOf(
            Correction("word1", ErrorType.Agreement, ErrorSeverity.Critical, "fix1", "why1", "porque1"),
            Correction("word2", ErrorType.Agreement, ErrorSeverity.High, "fix2", "why2", "porque2"),
            Correction("word3", ErrorType.TenseAspect, ErrorSeverity.Low, "fix3", "why3", "porque3")
        )

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(any(), any(), any()) } returns null
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 3

        errorAnalyticsService.recordErrors(userId, "es", messageId, corrections)

        verify(exactly = 2) { errorPatternRepository.save(match<ErrorPatternEntity> { it.errorType == ErrorType.Agreement }) }
        verify(exactly = 1) { errorPatternRepository.save(match<ErrorPatternEntity> { it.errorType == ErrorType.TenseAspect }) }
        verify(exactly = 3) { recentErrorSampleRepository.save(any()) }
    }

    @Test
    fun `recordErrors should prune old samples when exceeding limit`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val correction = Correction("test", ErrorType.Agreement, ErrorSeverity.Low, "fix", "why", "porque")

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(any(), any(), any()) } returns null
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 105

        val oldSamples = (1..105).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Low,
                messageId = UUID.randomUUID(),
                errorSpan = "test$it",
                occurredAt = Instant.now().minusSeconds(it.toLong() * 3600)
            )
        }

        every { recentErrorSampleRepository.findByUserIdOrderByOccurredAtDesc(userId, PageRequest.of(0, 105)) } returns oldSamples
        every { recentErrorSampleRepository.deleteAll(any<List<RecentErrorSampleEntity>>()) } just Runs

        errorAnalyticsService.recordErrors(userId, "es", messageId, listOf(correction))

        verify { recentErrorSampleRepository.deleteAll(match<List<RecentErrorSampleEntity>> { it.size == 5 }) }
    }

    @Test
    fun `recordErrors should not prune when under limit`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val correction = Correction("test", ErrorType.Agreement, ErrorSeverity.Low, "fix", "why", "porque")

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(any(), any(), any()) } returns null
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 50

        errorAnalyticsService.recordErrors(userId, "es", messageId, listOf(correction))

        verify(exactly = 0) { recentErrorSampleRepository.deleteAll(any<List<RecentErrorSampleEntity>>()) }
    }

    @Test
    fun `getTopPatterns should return patterns sorted by weighted score`() {
        val userId = UUID.randomUUID()
        val patterns = listOf(
            ErrorPatternEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                totalCount = 20,
                criticalCount = 5,
                highCount = 10,
                mediumCount = 5,
                lowCount = 0,
                firstSeenAt = Instant.now(),
                lastSeenAt = Instant.now()
            ),
            ErrorPatternEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.TenseAspect,
                totalCount = 10,
                criticalCount = 0,
                highCount = 5,
                mediumCount = 5,
                lowCount = 0,
                firstSeenAt = Instant.now(),
                lastSeenAt = Instant.now()
            )
        )

        every { errorPatternRepository.findTopPatternsByWeightedScore(userId, "es") } returns patterns

        val result = errorAnalyticsService.getTopPatterns(userId, "es", 5)

        assertEquals(2, result.size)
        assertEquals(ErrorType.Agreement, result[0].errorType)
        verify { errorPatternRepository.findTopPatternsByWeightedScore(userId, "es") }
    }

    @Test
    fun `getTopPatterns should respect limit parameter`() {
        val userId = UUID.randomUUID()
        val patterns = (1..10).map {
            ErrorPatternEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                totalCount = it,
                firstSeenAt = Instant.now(),
                lastSeenAt = Instant.now()
            )
        }

        every { errorPatternRepository.findTopPatternsByWeightedScore(userId, "es") } returns patterns

        val result = errorAnalyticsService.getTopPatterns(userId, "es", 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `computeTrend should return INSUFFICIENT_DATA when less than 20 samples`() {
        val userId = UUID.randomUUID()
        val samples = (1..15).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Medium,
                messageId = UUID.randomUUID(),
                errorSpan = "test$it"
            )
        }

        every { recentErrorSampleRepository.findByUserIdAndErrorTypeOrderByOccurredAtDesc(
            userId, ErrorType.Agreement, PageRequest.of(0, 100)
        ) } returns samples

        val result = errorAnalyticsService.computeTrend(userId, ErrorType.Agreement)

        assertEquals("INSUFFICIENT_DATA", result)
    }

    @Test
    fun `computeTrend should return IMPROVING when recent errors are 30 percent lower`() {
        val userId = UUID.randomUUID()

        // Create 40 samples: first 20 with high severity, last 20 with low severity
        val olderSamples = (1..20).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.High,  // Weight 2.0
                messageId = UUID.randomUUID(),
                errorSpan = "older$it"
            )
        }
        val recentSamples = (1..20).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Low,  // Weight 0.3
                messageId = UUID.randomUUID(),
                errorSpan = "recent$it"
            )
        }

        every { recentErrorSampleRepository.findByUserIdAndErrorTypeOrderByOccurredAtDesc(
            userId, ErrorType.Agreement, PageRequest.of(0, 100)
        ) } returns (recentSamples + olderSamples)

        val result = errorAnalyticsService.computeTrend(userId, ErrorType.Agreement)

        assertEquals("IMPROVING", result)
    }

    @Test
    fun `computeTrend should return WORSENING when recent errors are 30 percent higher`() {
        val userId = UUID.randomUUID()

        // Create 40 samples: first 20 with low severity, last 20 with high severity
        val olderSamples = (1..20).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Low,  // Weight 0.3
                messageId = UUID.randomUUID(),
                errorSpan = "older$it"
            )
        }
        val recentSamples = (1..20).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.High,  // Weight 2.0
                messageId = UUID.randomUUID(),
                errorSpan = "recent$it"
            )
        }

        every { recentErrorSampleRepository.findByUserIdAndErrorTypeOrderByOccurredAtDesc(
            userId, ErrorType.Agreement, PageRequest.of(0, 100)
        ) } returns (recentSamples + olderSamples)

        val result = errorAnalyticsService.computeTrend(userId, ErrorType.Agreement)

        assertEquals("WORSENING", result)
    }

    @Test
    fun `computeTrend should return STABLE when recent errors are similar to older`() {
        val userId = UUID.randomUUID()

        // Create 40 samples with consistent severity
        val samples = (1..40).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Medium,  // Weight 1.0
                messageId = UUID.randomUUID(),
                errorSpan = "test$it"
            )
        }

        every { recentErrorSampleRepository.findByUserIdAndErrorTypeOrderByOccurredAtDesc(
            userId, ErrorType.Agreement, PageRequest.of(0, 100)
        ) } returns samples

        val result = errorAnalyticsService.computeTrend(userId, ErrorType.Agreement)

        assertEquals("STABLE", result)
    }

    @Test
    fun `getRecentSamples should return limited samples`() {
        val userId = UUID.randomUUID()
        val samples = (1..30).map {
            RecentErrorSampleEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Medium,
                messageId = UUID.randomUUID(),
                errorSpan = "test$it"
            )
        }

        every { recentErrorSampleRepository.findByUserIdOrderByOccurredAtDesc(
            userId, PageRequest.of(0, 20)
        ) } returns samples.take(20)

        val result = errorAnalyticsService.getRecentSamples(userId, 20)

        assertEquals(20, result.size)
        verify { recentErrorSampleRepository.findByUserIdOrderByOccurredAtDesc(userId, PageRequest.of(0, 20)) }
    }

    @Test
    fun `recordErrors should truncate error span to 256 characters`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val longSpan = "a".repeat(300)
        val correction = Correction(
            span = longSpan,
            errorType = ErrorType.Agreement,
            severity = ErrorSeverity.Low,
            correctedTargetLanguage = "fix",
            whySourceLanguage = "why",
            whyTargetLanguage = "porque"
        )

        every { errorPatternRepository.findByUserIdAndLangAndErrorType(any(), any(), any()) } returns null
        every { errorPatternRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.save(any()) } returnsArgument 0
        every { recentErrorSampleRepository.countByUserId(userId) } returns 1

        errorAnalyticsService.recordErrors(userId, "es", messageId, listOf(correction))

        verify {
            recentErrorSampleRepository.save(match<RecentErrorSampleEntity> {
                it.errorSpan?.length == 256
            })
        }
    }
}
