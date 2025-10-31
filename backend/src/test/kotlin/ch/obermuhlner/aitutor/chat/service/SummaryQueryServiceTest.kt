package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import ch.obermuhlner.aitutor.chat.domain.SummarySourceType
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.tutor.service.ProgressiveSummarizationService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SummaryQueryServiceTest {

    private lateinit var summaryQueryService: SummaryQueryService
    private lateinit var summaryRepository: MessageSummaryRepository
    private lateinit var messageRepository: ChatMessageRepository
    private lateinit var sessionRepository: ChatSessionRepository
    private lateinit var progressiveSummarizationService: ProgressiveSummarizationService

    @BeforeEach
    fun setup() {
        summaryRepository = mockk()
        messageRepository = mockk()
        sessionRepository = mockk()
        progressiveSummarizationService = mockk()

        summaryQueryService = SummaryQueryService(
            summaryRepository,
            messageRepository,
            sessionRepository,
            progressiveSummarizationService
        )
    }

    @Test
    fun `should get session summary info with no summaries`() {
        val sessionId = TestDataFactory.TEST_SESSION_ID
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            createMessage(session, 1, "Hello"),
            createMessage(session, 2, "World")
        )

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId) } returns emptyList()
        every { messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId) } returns messages

        val result = summaryQueryService.getSessionSummaryInfo(sessionId)

        assertNotNull(result)
        assertEquals(sessionId, result.sessionId)
        assertEquals(2, result.totalMessages)
        assertEquals(0, result.summaryLevels.size)
        assertEquals(0, result.estimatedTokenSavings)
        assertEquals(1.0, result.compressionRatio)
    }

    @Test
    fun `should get session summary info with level 1 summaries`() {
        val sessionId = TestDataFactory.TEST_SESSION_ID
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..20).map { createMessage(session, it, "Message $it test content") }
        val summaries = listOf(
            createSummary(sessionId, 1, 1, 10, 50, """["id1","id2"]"""),
            createSummary(sessionId, 1, 11, 20, 50, """["id3","id4"]""")
        )

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId) } returns summaries
        every { messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId) } returns messages

        val result = summaryQueryService.getSessionSummaryInfo(sessionId)

        assertNotNull(result)
        assertEquals(sessionId, result.sessionId)
        assertEquals(20, result.totalMessages)
        assertEquals(1, result.summaryLevels.size)
        assertEquals(1, result.summaryLevels[0].level)
        assertEquals(2, result.summaryLevels[0].count)
        assertEquals(100, result.summaryLevels[0].totalTokens)
        assertEquals(20, result.lastSummarizedSequence)
        assertTrue(result.estimatedTokenSavings > 0)
        assertTrue(result.compressionRatio < 1.0)
    }

    @Test
    fun `should get session summary info with hierarchical summaries`() {
        val sessionId = TestDataFactory.TEST_SESSION_ID
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..40).map { createMessage(session, it, "Message $it test content here") }
        val summaries = listOf(
            createSummary(sessionId, 1, 1, 10, 50, """["id1"]"""),
            createSummary(sessionId, 1, 11, 20, 50, """["id2"]"""),
            createSummary(sessionId, 2, 1, 20, 30, """["sum1","sum2"]""")
        )

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId) } returns summaries
        every { messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId) } returns messages

        val result = summaryQueryService.getSessionSummaryInfo(sessionId)

        assertNotNull(result)
        assertEquals(2, result.summaryLevels.size)
        assertEquals(1, result.summaryLevels[0].level)
        assertEquals(2, result.summaryLevels[0].count)
        assertEquals(2, result.summaryLevels[1].level)
        assertEquals(1, result.summaryLevels[1].count)
        assertEquals(30, result.summaryLevels[1].totalTokens)
    }

    @Test
    fun `should get session summary details`() {
        val sessionId = TestDataFactory.TEST_SESSION_ID
        val summaries = listOf(
            createSummary(sessionId, 1, 1, 10, 50, """["${UUID.randomUUID()}","${UUID.randomUUID()}"]"""),
            createSummary(sessionId, 1, 11, 20, 50, """["${UUID.randomUUID()}"]""")
        )

        every { summaryRepository.findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(sessionId) } returns summaries

        val result = summaryQueryService.getSessionSummaryDetails(sessionId)

        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(1, result[0].summaryLevel)
        assertEquals(1, result[0].startSequence)
        assertEquals(10, result[0].endSequence)
        assertEquals(50, result[0].tokenCount)
        assertEquals(2, result[0].sourceIds.size)
        assertTrue(result[0].isActive)
    }

    @Test
    fun `should trigger manual summarization`() {
        val sessionId = TestDataFactory.TEST_SESSION_ID
        val session = TestDataFactory.createSessionEntity()

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { progressiveSummarizationService.checkAndSummarize(sessionId) } just runs

        summaryQueryService.triggerManualSummarization(sessionId)

        verify(exactly = 1) { progressiveSummarizationService.checkAndSummarize(sessionId) }
    }

    @Test
    fun `should get global stats with no data`() {
        every { summaryRepository.findAll() } returns emptyList()
        every { messageRepository.findAll() } returns emptyList()

        val result = summaryQueryService.getGlobalStats()

        assertNotNull(result)
        assertEquals(0, result["totalSummaries"])
        assertEquals(0, result["activeSummaries"])
        assertEquals(0, result["totalMessages"])
        assertEquals(1.0, result["globalCompressionRatio"])
    }

    @Test
    fun `should get global stats with summaries`() {
        val messages = (1..50).map {
            createMessage(TestDataFactory.createSessionEntity(), it, "Test message content here")
        }
        val summaries = listOf(
            createSummary(UUID.randomUUID(), 1, 1, 10, 50, """["id1"]"""),
            createSummary(UUID.randomUUID(), 1, 11, 20, 50, """["id2"]"""),
            createSummary(UUID.randomUUID(), 2, 1, 20, 30, """["sum1"]""", isActive = false)
        )

        every { summaryRepository.findAll() } returns summaries
        every { messageRepository.findAll() } returns messages

        val result = summaryQueryService.getGlobalStats()

        assertNotNull(result)
        assertEquals(3, result["totalSummaries"])
        assertEquals(2, result["activeSummaries"])
        assertEquals(50, result["totalMessages"])

        @Suppress("UNCHECKED_CAST")
        val summariesByLevel = result["summariesByLevel"] as Map<Int, Int>
        assertEquals(2, summariesByLevel[1])
        assertEquals(1, summariesByLevel[2])
    }

    @Test
    fun `should throw exception when session not found for summary info`() {
        val sessionId = UUID.randomUUID()
        every { sessionRepository.findById(sessionId) } returns Optional.empty()

        try {
            summaryQueryService.getSessionSummaryInfo(sessionId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Session not found: $sessionId", e.message)
        }
    }

    @Test
    fun `should throw exception when session not found for manual trigger`() {
        val sessionId = UUID.randomUUID()
        every { sessionRepository.findById(sessionId) } returns Optional.empty()

        try {
            summaryQueryService.triggerManualSummarization(sessionId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Session not found: $sessionId", e.message)
        }
    }

    private fun createMessage(session: ChatSessionEntity, sequence: Int, content: String): ChatMessageEntity {
        return ChatMessageEntity(
            id = UUID.randomUUID(),
            session = session,
            sequenceNumber = sequence,
            role = MessageRole.USER,
            content = content,
            correctionsJson = null,
            vocabularyJson = null,
            wordCardsJson = null
        )
    }

    private fun createSummary(
        sessionId: UUID,
        level: Int,
        startSeq: Int,
        endSeq: Int,
        tokens: Int,
        sourceIdsJson: String,
        isActive: Boolean = true
    ): MessageSummaryEntity {
        return MessageSummaryEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            summaryLevel = level,
            startSequence = startSeq,
            endSequence = endSeq,
            summaryText = "Summary of sequences $startSeq to $endSeq",
            tokenCount = tokens,
            sourceType = SummarySourceType.MESSAGE,
            sourceIdsJson = sourceIdsJson,
            supersededById = null,
            isActive = isActive,
            createdAt = Instant.now()
        )
    }
}
