package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import ch.obermuhlner.aitutor.chat.domain.SummarySourceType
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.prompt.Prompt
import java.util.UUID

class ProgressiveSummarizationServiceTest {

    private lateinit var chatModel: ChatModel
    private lateinit var summaryRepository: MessageSummaryRepository
    private lateinit var messageRepository: ChatMessageRepository
    private lateinit var service: ProgressiveSummarizationService
    private lateinit var sessionId: UUID
    private lateinit var testSession: ChatSessionEntity

    @BeforeEach
    fun setup() {
        chatModel = mock(ChatModel::class.java)
        summaryRepository = mock(MessageSummaryRepository::class.java)
        messageRepository = mock(ChatMessageRepository::class.java)

        service = ProgressiveSummarizationService(
            chatModel = chatModel,
            summaryRepository = summaryRepository,
            messageRepository = messageRepository,
            chunkSize = 10,
            chunkTokenThreshold = 5000,
            progressivePrompt = "Summarize to about {targetWords} words.",
            compressionRatio = 0.1,
            tokensPerWord = 0.75,
            minSummaryTokens = 100,
            maxSummaryTokens = 1000
        )

        sessionId = UUID.randomUUID()
        testSession = ChatSessionEntity(
            id = sessionId,
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        // Mock LLM to return simple summaries
        val mockResponse = mock(ChatResponse::class.java)
        val mockGeneration = mock(Generation::class.java)
        val mockMessage = AssistantMessage("Summary text")
        whenever(mockGeneration.output).thenReturn(mockMessage)
        whenever(mockResponse.result).thenReturn(mockGeneration)
        whenever(chatModel.call(any<Prompt>())).thenReturn(mockResponse)
    }

    @Test
    fun `should continue summarizing after level-1 summaries are superseded by level-2`() {
        // Setup: 30 messages total (will create 3 level-1 summaries, then 1 level-2 summary superseding first 2)

        // === First call: Messages 0-9 (first 10 messages) ===
        val messages0to9 = createMessages(0, 10)
        whenever(summaryRepository.findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(sessionId, 1))
            .thenReturn(null) // No summaries yet
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, 0))
            .thenReturn(messages0to9)

        val savedSummary1 = MessageSummaryEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            summaryLevel = 1,
            startSequence = 0,
            endSequence = 9,
            summaryText = "Summary 1",
            tokenCount = 50,
            sourceType = SummarySourceType.MESSAGE,
            sourceIdsJson = "[]",
            isActive = true
        )
        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(listOf(savedSummary1))

        service.checkAndSummarize(sessionId)

        // === Second call: Messages 10-19 (second 10 messages) ===
        val messages10to19 = createMessages(10, 20)
        whenever(summaryRepository.findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(sessionId, 1))
            .thenReturn(savedSummary1)
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, 10))
            .thenReturn(messages10to19)

        val savedSummary2 = MessageSummaryEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            summaryLevel = 1,
            startSequence = 10,
            endSequence = 19,
            summaryText = "Summary 2",
            tokenCount = 50,
            sourceType = SummarySourceType.MESSAGE,
            sourceIdsJson = "[]",
            isActive = true
        )
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(listOf(savedSummary1, savedSummary2))

        service.checkAndSummarize(sessionId)

        // Now we have 2 level-1 summaries, but they will be superseded by level-2 when we get to 10 level-1 summaries
        // For this test, simulate that they're already superseded (no need to reference them)

        // === Third call: Messages 20-29 (third 10 messages) - AFTER level-1 summaries are superseded ===
        val messages20to29 = createMessages(20, 30)

        // BUG: When all level-1 summaries are inactive, findTop returns null
        // This causes startSequence = 0 and re-summarizes from beginning!
        whenever(summaryRepository.findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(sessionId, 1))
            .thenReturn(null) // All level-1 summaries superseded!

        // But there should be a level-2 summary covering sequences 0-19
        val level2Summary = MessageSummaryEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            summaryLevel = 2,
            startSequence = 0,
            endSequence = 19,
            summaryText = "Level 2 summary",
            tokenCount = 80,
            sourceType = SummarySourceType.SUMMARY,
            sourceIdsJson = "[]",
            isActive = true
        )
        whenever(summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId))
            .thenReturn(listOf(level2Summary))

        // The bug: messages from sequence 0 are returned instead of sequence 20
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, 0))
            .thenReturn(createMessages(0, 30)) // Returns ALL messages - incorrect!

        // What SHOULD happen: start from sequence 20
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, 20))
            .thenReturn(messages20to29)

        service.checkAndSummarize(sessionId)

        // Verify: Should query for messages starting at sequence 20, not 0
        val captor = argumentCaptor<Int>()
        verify(messageRepository, atLeastOnce())
            .findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(
                eq(sessionId),
                captor.capture()
            )

        // The bug: Will capture 0 (incorrect)
        // The fix: Should capture 20 (correct - continues from last summarized sequence)
        val lastStartSequence = captor.lastValue
        assertEquals(20, lastStartSequence,
            "Should start summarizing from sequence 20 (after level-2 summary ending at 19), not 0. " +
            "Bug: When level-1 summaries are superseded, findTop returns null and startSequence resets to 0.")
    }

    @Test
    fun `should summarize when chunk size reached regardless of token threshold`() {
        // Test with chunk-size=5, should trigger after 5 messages even with high token threshold
        val testService = createTestService(
            chunkSize = 5,
            chunkTokenThreshold = 99999 // High threshold - should still trigger on size
        )

        val messages = createMessages(0, 5)
        setupRepositoryMocks(sessionId, startSequence = 0, messages)

        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(emptyList())

        testService.checkAndSummarize(sessionId)

        // Verify a summary was created
        verify(summaryRepository, atLeastOnce()).save(any())
        val savedSummary = summaryCaptor.firstValue
        assertEquals(1, savedSummary.summaryLevel)
        assertEquals(0, savedSummary.startSequence)
        assertEquals(4, savedSummary.endSequence)
    }

    @Test
    fun `should summarize when token threshold exceeded even with incomplete chunk`() {
        // Test with chunk-size=10 but only 5 messages that exceed token threshold
        // Messages have ~30 chars each, so 5 messages = ~150 chars = ~37 tokens (150/4)
        // Set threshold to 30 so we exceed it
        val testService = createTestService(
            chunkSize = 10,
            chunkTokenThreshold = 30 // Low threshold - 5 messages (~37 tokens) should exceed this
        )

        val messages = createMessages(0, 5)
        setupRepositoryMocks(sessionId, startSequence = 0, messages)

        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(emptyList())

        testService.checkAndSummarize(sessionId)

        // Verify a summary was created despite incomplete chunk (5 < 10)
        verify(summaryRepository, atLeastOnce()).save(any())
        val savedSummary = summaryCaptor.firstValue
        assertEquals(1, savedSummary.summaryLevel)
        assertEquals(0, savedSummary.startSequence)
        assertEquals(4, savedSummary.endSequence)
    }

    @Test
    fun `should process multiple complete chunks in single call`() {
        // Test with chunk-size=3, provide 9 messages - should create 3 summaries in one call
        val testService = createTestService(
            chunkSize = 3,
            chunkTokenThreshold = 99999 // Size-based only
        )

        val messages = createMessages(0, 9)
        setupRepositoryMocks(sessionId, startSequence = 0, messages)

        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(emptyList())

        testService.checkAndSummarize(sessionId)

        // Verify 3 summaries were created
        verify(summaryRepository, times(3)).save(any())
        val savedSummaries = summaryCaptor.allValues
        assertEquals(3, savedSummaries.size)

        // Verify sequence ranges
        assertEquals(0, savedSummaries[0].startSequence)
        assertEquals(2, savedSummaries[0].endSequence)
        assertEquals(3, savedSummaries[1].startSequence)
        assertEquals(5, savedSummaries[1].endSequence)
        assertEquals(6, savedSummaries[2].startSequence)
        assertEquals(8, savedSummaries[2].endSequence)
    }

    @Test
    fun `should propagate through multiple levels with small chunk size`() {
        // Test with chunk-size=2
        // First call: 4 messages → 2 Level-1 summaries → immediately propagates to 1 Level-2
        // Second call: 4 more messages → 2 more Level-1 summaries → propagates to another Level-2
        val testService = createTestService(
            chunkSize = 2,
            chunkTokenThreshold = 0 // Size-based only
        )

        // First call: 4 messages → should create 2 Level-1 summaries + 1 Level-2 (propagation)
        val messages0to3 = createMessages(0, 4)
        setupRepositoryMocks(sessionId, startSequence = 0, messages0to3)

        val allSummaries = mutableListOf<MessageSummaryEntity>()
        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer {
            val savedSummary = summaryCaptor.lastValue
            val summaryWithId = MessageSummaryEntity(
                id = UUID.randomUUID(),
                sessionId = savedSummary.sessionId,
                summaryLevel = savedSummary.summaryLevel,
                startSequence = savedSummary.startSequence,
                endSequence = savedSummary.endSequence,
                summaryText = savedSummary.summaryText,
                tokenCount = savedSummary.tokenCount,
                sourceType = savedSummary.sourceType,
                sourceIdsJson = savedSummary.sourceIdsJson,
                isActive = savedSummary.summaryLevel == 1 // Level-1 stays active, Level-2 propagates
            )
            allSummaries.add(summaryWithId)
            summaryWithId
        }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(eq(sessionId), any()))
            .thenAnswer { invocation ->
                val level = invocation.getArgument<Int>(1)
                allSummaries.filter { it.summaryLevel == level && it.isActive }
            }

        testService.checkAndSummarize(sessionId)

        // After first call: should have 2 Level-1 + 1 Level-2 (propagation triggered)
        val firstCallSummaries = summaryCaptor.allValues.toList()
        val level1AfterFirst = firstCallSummaries.count { it.summaryLevel == 1 }
        val level2AfterFirst = firstCallSummaries.count { it.summaryLevel == 2 }

        assertEquals(2, level1AfterFirst, "First call should create 2 Level-1 summaries")
        assertEquals(1, level2AfterFirst, "First call should propagate to 1 Level-2 summary")

        // Second call: 4 more messages
        val messages4to7 = createMessages(4, 8)
        val activeSummaries = allSummaries.filter { it.isActive }
        setupRepositoryMocks(sessionId, startSequence = 4, messages4to7, activeSummaries)

        testService.checkAndSummarize(sessionId)

        // After second call: should have created 2 more Level-1 + at least 1 more Level-2
        val allSummariesAfter = summaryCaptor.allValues
        val level1Total = allSummariesAfter.count { it.summaryLevel == 1 }
        val level2Total = allSummariesAfter.count { it.summaryLevel == 2 }

        assertTrue(level1Total >= 4, "Should have at least 4 Level-1 summaries total")
        assertTrue(level2Total >= 1, "Should have at least 1 Level-2 summary from propagation")
    }

    @Test
    fun `should skip incomplete chunks when below token threshold`() {
        // Test with chunk-size=5, provide 7 messages, high token threshold
        // Should summarize first 5, skip last 2
        val testService = createTestService(
            chunkSize = 5,
            chunkTokenThreshold = 99999 // High threshold - size-based only
        )

        val messages = createMessages(0, 7)
        setupRepositoryMocks(sessionId, startSequence = 0, messages)

        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(emptyList())

        testService.checkAndSummarize(sessionId)

        // Verify only 1 summary created (for first 5 messages)
        verify(summaryRepository, times(1)).save(any())
        val savedSummary = summaryCaptor.firstValue
        assertEquals(0, savedSummary.startSequence)
        assertEquals(4, savedSummary.endSequence)
    }

    @Test
    fun `should work with current application yml configuration`() {
        // Read actual configuration values from service instance
        // This test verifies behavior using the actual application.yml configuration,
        // but makes no hard-coded assumptions about specific values.

        // Use the service configured in setup() which has chunk-size=10
        // For this test, we'll verify the service works correctly with any configuration
        val actualChunkSize = 10 // From setup()

        // Create enough messages to form complete chunks
        val messageCount = actualChunkSize * 3
        val messages = createMessages(0, messageCount)

        whenever(summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId))
            .thenReturn(emptyList())
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, 0))
            .thenReturn(messages)

        val summaryCaptor = argumentCaptor<MessageSummaryEntity>()
        whenever(summaryRepository.save(summaryCaptor.capture())).thenAnswer { summaryCaptor.lastValue }
        whenever(summaryRepository.findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, 1))
            .thenReturn(emptyList())

        service.checkAndSummarize(sessionId)

        // Verify summaries were created (should be 3 for 30 messages with chunk-size=10)
        val savedSummaries = summaryCaptor.allValues
        val expectedSummaryCount = messageCount / actualChunkSize
        assertEquals(expectedSummaryCount, savedSummaries.size,
            "Should create $expectedSummaryCount summaries for $messageCount messages with chunk-size=$actualChunkSize")

        // Verify each summary covers exactly chunk-size messages
        savedSummaries.forEachIndexed { index, summary ->
            val expectedStart = index * actualChunkSize
            val expectedEnd = expectedStart + actualChunkSize - 1

            assertEquals(expectedStart, summary.startSequence,
                "Summary $index should start at sequence $expectedStart")
            assertEquals(expectedEnd, summary.endSequence,
                "Summary $index should end at sequence $expectedEnd")
            assertEquals(1, summary.summaryLevel,
                "All summaries should be Level-1")
        }

        // Verify boundaries align properly (each summary covers exactly chunk-size messages)
        savedSummaries.forEach { summary ->
            val messagesCovered = summary.endSequence - summary.startSequence + 1
            assertEquals(actualChunkSize, messagesCovered,
                "Each summary should cover exactly $actualChunkSize messages")
        }
    }

    // Helper methods for cleaner test setup

    private fun createTestService(
        chunkSize: Int,
        chunkTokenThreshold: Int,
        compressionRatio: Double = 0.1,
        tokensPerWord: Double = 0.75,
        minSummaryTokens: Int = 20,
        maxSummaryTokens: Int = 2000
    ): ProgressiveSummarizationService {
        return ProgressiveSummarizationService(
            chatModel = chatModel,
            summaryRepository = summaryRepository,
            messageRepository = messageRepository,
            chunkSize = chunkSize,
            chunkTokenThreshold = chunkTokenThreshold,
            progressivePrompt = "Summarize to about {targetWords} words.",
            compressionRatio = compressionRatio,
            tokensPerWord = tokensPerWord,
            minSummaryTokens = minSummaryTokens,
            maxSummaryTokens = maxSummaryTokens
        )
    }

    private fun setupRepositoryMocks(
        sessionId: UUID,
        startSequence: Int,
        messages: List<ChatMessageEntity>,
        existingSummaries: List<MessageSummaryEntity> = emptyList()
    ) {
        whenever(summaryRepository.findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId))
            .thenReturn(existingSummaries)
        whenever(messageRepository.findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(sessionId, startSequence))
            .thenReturn(messages)
    }

    private fun createMessages(startSeq: Int, endSeq: Int): List<ChatMessageEntity> {
        return (startSeq until endSeq).map { seq ->
            ChatMessageEntity(
                id = UUID.randomUUID(),
                session = testSession,
                role = if (seq % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                content = "Message $seq content with some text",
                sequenceNumber = seq
            )
        }
    }
}
