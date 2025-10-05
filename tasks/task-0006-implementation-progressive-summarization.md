# Task 0006: Progressive Summarization Implementation

## Problem Statement

**Current inefficiency:**
- `ConversationSummarizationService` and `MessageCompactionService` re-summarize old messages on every request
- Summaries are ephemeral (not persisted)
- Highly inefficient for long conversations with hundreds of messages

**Goal:**
Design and implement a progressive summarization system that:
- Summarizes messages in chunks (every N turns, configurable)
- Persists summaries at multiple hierarchical levels
- Replaces old messages in context with appropriate summaries
- Preserves all raw messages for future RAG feature
- **Runs asynchronously to avoid blocking user requests**

---

## Critical Review Findings

### Issues Identified in Initial Design

1. **❌ Synchronous blocking**: Summarization triggers in request path, causing 2-5s latency
2. **❌ `getCompactedHistory()` bug**: Uses non-existent repository method
3. **❌ Upward propagation bug**: Only processes first chunk, leaves dangling summaries
4. **❌ Configuration structure**: Duplicate `summarization` keys at different YAML levels
5. **❌ Missing imports**: `MessageRole` import missing in code examples

### Design Decision: Asynchronous Background Summarization

**Selected approach:** Hierarchical progressive summarization with **async execution**

**Rationale:**
- User never waits for summarization (happens in background)
- Summaries eventually consistent (lag by 1-2 requests, acceptable)
- Hierarchical structure handles unlimited conversation length
- Better production UX than synchronous approach

---

## Alternative Solutions Considered

### Alternative 1: Simple Rolling Summary
**Concept:** Single summary per session, updated periodically
**Verdict:** ❌ Too simple, degrades quality over 100+ messages

### Alternative 2: Token-Based Chunking
**Concept:** Chunk by token budget instead of message count
**Verdict:** ⚠️ Interesting but adds complexity without clear benefit

### Alternative 3: Hybrid Extraction
**Concept:** LLM extracts key facts/vocabulary separately from summary
**Verdict:** ⚠️ Over-engineered for current needs; consider for v2

### Alternative 4: Hierarchical + Async (SELECTED)
**Concept:** Proposed design with async execution and bug fixes
**Verdict:** ✅ Best balance of scalability, UX, and maintainability

---

## Progressive Summarization Design

### Core Concept

**Hierarchical summary tree** with persistent summaries at multiple levels:

```
Level 0: Raw messages [M1, M2, ... M100]
Level 1: Summaries of chunks [S1(M1-10), S2(M11-20), ... S10(M91-100)]
Level 2: Summaries of summaries [SS1(S1-S5), SS2(S6-S10)]
Level 3: Summary of level 2 [SSS1(SS1-SS2)]
```

### Key Design Principles

1. **Trigger by count**: Summarize every N messages (configurable, default 10)
2. **Token-aware**: Summarize early if token count exceeds threshold
3. **Persistent**: Store summaries in database with level metadata
4. **Asynchronous**: Summarization runs in background, doesn't block requests
5. **Complete chunk processing**: Process all complete chunks, not just first
6. **Retention policy**: **Keep all raw messages** for future RAG feature (no deletion/archiving)

---

## Implementation Plan

### 1. Database Schema

**New Table: `message_summaries`**
```sql
CREATE TABLE message_summaries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    summary_level INT NOT NULL,       -- 1, 2, 3, etc.
    start_sequence INT NOT NULL,      -- Starting message sequence
    end_sequence INT NOT NULL,        -- Ending message sequence
    summary_text TEXT NOT NULL,       -- The actual summary
    token_count INT NOT NULL,         -- Estimated tokens
    created_at TIMESTAMP NOT NULL,

    -- Reference to child entities (messages or lower-level summaries)
    source_type VARCHAR(16) NOT NULL, -- 'MESSAGE' or 'SUMMARY'
    source_ids_json TEXT NOT NULL,    -- JSON array of source UUIDs

    -- Track if summary has been superseded by higher level
    superseded_by_id UUID,            -- Reference to higher-level summary that includes this
    is_active BOOLEAN DEFAULT TRUE,   -- False if superseded

    UNIQUE(session_id, summary_level, start_sequence, end_sequence)
);

CREATE INDEX idx_summaries_session_level ON message_summaries(session_id, summary_level);
CREATE INDEX idx_summaries_session_active ON message_summaries(session_id, is_active);
```

**Update: `chat_messages` table**
```sql
ALTER TABLE chat_messages ADD COLUMN sequence_number INT NOT NULL DEFAULT 0;
CREATE INDEX idx_messages_session_sequence ON chat_messages(session_id, sequence_number);
```

**Note:** Do NOT add `is_archived` column. All messages remain accessible.

### 2. New Domain Entities

**MessageSummaryEntity.kt**
```kotlin
package ch.obermuhlner.aitutor.chat.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

@Entity
@Table(name = "message_summaries")
class MessageSummaryEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "session_id", nullable = false)
    val sessionId: UUID,

    @Column(name = "summary_level", nullable = false)
    val summaryLevel: Int,

    @Column(name = "start_sequence", nullable = false)
    val startSequence: Int,

    @Column(name = "end_sequence", nullable = false)
    val endSequence: Int,

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    val summaryText: String,

    @Column(name = "token_count", nullable = false)
    val tokenCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    val sourceType: SummarySourceType,

    @Column(name = "source_ids_json", nullable = false, columnDefinition = "TEXT")
    val sourceIdsJson: String,

    @Column(name = "superseded_by_id")
    var supersededById: UUID? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null
)

enum class SummarySourceType {
    MESSAGE,  // Level-1 summaries (from raw messages)
    SUMMARY   // Level-2+ summaries (from lower-level summaries)
}
```

**Update ChatMessageEntity.kt:**
```kotlin
@Column(name = "sequence_number", nullable = false)
var sequenceNumber: Int = 0
```

### 3. New Repository

**MessageSummaryRepository.kt**
```kotlin
package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageSummaryRepository : JpaRepository<MessageSummaryEntity, UUID> {
    /**
     * Get all active summaries at a specific level for a session, ordered by sequence.
     */
    fun findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(
        sessionId: UUID,
        summaryLevel: Int
    ): List<MessageSummaryEntity>

    /**
     * Get all active summaries for a session, ordered by level then sequence.
     */
    fun findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(
        sessionId: UUID
    ): List<MessageSummaryEntity>

    /**
     * Get the most recent summary (highest end_sequence) for a session at level 1.
     */
    fun findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(
        sessionId: UUID,
        summaryLevel: Int
    ): MessageSummaryEntity?

    /**
     * Mark summaries as superseded.
     */
    @Modifying
    @Query("UPDATE MessageSummaryEntity m SET m.isActive = false, m.supersededById = :supersededById WHERE m.id IN :ids")
    fun markAsSuperseded(ids: List<UUID>, supersededById: UUID)
}
```

**Update ChatMessageRepository.kt:**
```kotlin
/**
 * Find messages by session starting from a specific sequence number.
 */
fun findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(
    sessionId: UUID,
    startSequence: Int
): List<ChatMessageEntity>

/**
 * Find all messages by session, ordered by sequence.
 */
fun findBySessionIdOrderBySequenceNumberAsc(
    sessionId: UUID
): List<ChatMessageEntity>

/**
 * Count messages in a session starting from a sequence number.
 */
fun countBySessionIdAndSequenceNumberGreaterThanEqual(
    sessionId: UUID,
    startSequence: Int
): Long
```

### 4. New Service: ProgressiveSummarizationService

**Location:** `src/main/kotlin/ch/obermuhlner/aitutor/tutor/service/ProgressiveSummarizationService.kt`

**Responsibilities:**
- Detect when to trigger summarization (message count or token threshold)
- Create level-1 summaries from raw messages (async)
- Recursively create higher-level summaries (async)
- Process ALL complete chunks, not just first
- Reconstruct conversation history from summaries + recent messages

**Key Methods:**
```kotlin
package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import ch.obermuhlner.aitutor.chat.domain.SummarySourceType
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProgressiveSummarizationService(
    private val summarizationService: ConversationSummarizationService,
    private val summaryRepository: MessageSummaryRepository,
    private val messageRepository: ChatMessageRepository,
    @Value("\${ai-tutor.context.summarization.progressive.chunk-size}") private val chunkSize: Int,
    @Value("\${ai-tutor.context.summarization.progressive.chunk-token-threshold}") private val chunkTokenThreshold: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Check if summarization is needed after new messages, and trigger if so.
     * Runs asynchronously to avoid blocking user requests.
     */
    @Async
    @Transactional
    fun checkAndSummarize(sessionId: UUID) {
        try {
            logger.debug("Async summarization check for session $sessionId")

            // 1. Find last summarized sequence
            val lastSummary = summaryRepository
                .findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(sessionId, 1)
            val startSequence = (lastSummary?.endSequence ?: -1) + 1

            // 2. Get unsummarized messages
            val unsummarized = messageRepository
                .findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(
                    sessionId, startSequence
                )

            if (unsummarized.isEmpty()) {
                logger.debug("No unsummarized messages for session $sessionId")
                return
            }

            // 3. Check trigger conditions
            val tokenCount = estimateTokens(unsummarized.map { it.content })
            logger.debug(
                "Session $sessionId: ${unsummarized.size} unsummarized messages, ~$tokenCount tokens"
            )

            if (unsummarized.size < chunkSize && tokenCount < chunkTokenThreshold) {
                logger.debug("Not yet ready to summarize (size: ${unsummarized.size}/$chunkSize, tokens: $tokenCount/$chunkTokenThreshold)")
                return
            }

            // 4. Summarize all complete chunks (not just first!)
            val chunks = unsummarized.chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                if (chunk.size == chunkSize || tokenCount >= chunkTokenThreshold) {
                    logger.info("Summarizing chunk ${index + 1}/${chunks.size} for session $sessionId (${chunk.size} messages)")
                    val level1Summary = summarizeMessageChunk(sessionId, chunk)
                    summaryRepository.save(level1Summary)
                } else {
                    logger.debug("Skipping incomplete chunk ${index + 1} (${chunk.size}/$chunkSize messages)")
                }
            }

            // 5. Recursively check if level-1 summaries need level-2 summarization
            propagateUpward(sessionId, level = 1)

        } catch (e: Exception) {
            logger.error("Error during async summarization for session $sessionId", e)
        }
    }

    /**
     * Recursively create higher-level summaries when lower level has enough entries.
     * Process ALL complete chunks, not just first.
     */
    @Transactional
    private fun propagateUpward(sessionId: UUID, level: Int) {
        val summaries = summaryRepository
            .findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, level)

        if (summaries.size < chunkSize) {
            logger.debug("Level $level: Only ${summaries.size} summaries, need $chunkSize to propagate")
            return
        }

        // Process ALL complete chunks
        val chunks = summaries.chunked(chunkSize)
        for ((index, chunk) in chunks.withIndex()) {
            if (chunk.size == chunkSize) {
                logger.info("Propagating to level ${level + 1}: chunk ${index + 1}, summarizing ${chunk.size} level-$level summaries")
                val higherLevelSummary = summarizeSummaryChunk(sessionId, level + 1, chunk)
                summaryRepository.save(higherLevelSummary)

                // Mark child summaries as superseded
                summaryRepository.markAsSuperseded(
                    chunk.map { it.id },
                    higherLevelSummary.id
                )
            }
        }

        // Recursively propagate to next level
        if (chunks.any { it.size == chunkSize }) {
            propagateUpward(sessionId, level + 1)
        }
    }

    /**
     * Create level-1 summary from raw messages.
     */
    private fun summarizeMessageChunk(
        sessionId: UUID,
        messages: List<ChatMessageEntity>
    ): MessageSummaryEntity {
        // Convert to Spring AI Message objects
        val messageObjects = messages.map { entity ->
            when (entity.role) {
                MessageRole.USER -> UserMessage(entity.content)
                MessageRole.ASSISTANT -> AssistantMessage(entity.content)
                MessageRole.SYSTEM -> SystemMessage(entity.content)
            }
        }

        // Call LLM to summarize
        val summaryText = summarizationService.summarizeMessages(messageObjects)
        val tokenCount = estimateTokens(listOf(summaryText))

        // Store source message IDs
        val sourceIds = messages.map { it.id.toString() }
        val sourceIdsJson = objectMapper.writeValueAsString(sourceIds)

        return MessageSummaryEntity(
            sessionId = sessionId,
            summaryLevel = 1,
            startSequence = messages.first().sequenceNumber,
            endSequence = messages.last().sequenceNumber,
            summaryText = summaryText,
            tokenCount = tokenCount,
            sourceType = SummarySourceType.MESSAGE,
            sourceIdsJson = sourceIdsJson
        )
    }

    /**
     * Create higher-level summary from lower-level summaries.
     */
    private fun summarizeSummaryChunk(
        sessionId: UUID,
        level: Int,
        summaries: List<MessageSummaryEntity>
    ): MessageSummaryEntity {
        // Convert summaries to messages for LLM
        val summaryMessages = summaries.mapIndexed { index, summary ->
            SystemMessage("Summary ${index + 1}: ${summary.summaryText}")
        }

        // Call LLM to summarize summaries
        val summaryText = summarizationService.summarizeMessages(summaryMessages)
        val tokenCount = estimateTokens(listOf(summaryText))

        // Store source summary IDs
        val sourceIds = summaries.map { it.id.toString() }
        val sourceIdsJson = objectMapper.writeValueAsString(sourceIds)

        return MessageSummaryEntity(
            sessionId = sessionId,
            summaryLevel = level,
            startSequence = summaries.first().startSequence,
            endSequence = summaries.last().endSequence,
            summaryText = summaryText,
            tokenCount = tokenCount,
            sourceType = SummarySourceType.SUMMARY,
            sourceIdsJson = sourceIdsJson
        )
    }

    /**
     * Get compacted conversation history: summaries + recent messages.
     * Uses existing summaries (may be slightly stale if async summarization in progress).
     */
    fun getCompactedHistory(sessionId: UUID, recentMessageCount: Int): List<Message> {
        // Get all messages
        val allMessages = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId)

        if (allMessages.size <= recentMessageCount) {
            // Short conversation - return all messages
            return allMessages.map { toMessage(it) }
        }

        // Split into old and recent
        val recentMessages = allMessages.takeLast(recentMessageCount)
        val lastOldMessageSequence = allMessages[allMessages.size - recentMessageCount - 1].sequenceNumber

        // Get highest-level active summary that covers old messages
        val summary = findBestSummary(sessionId, endSequence = lastOldMessageSequence)

        return if (summary != null) {
            // Use summary + recent messages
            logger.debug("Using level-${summary.summaryLevel} summary (sequences ${summary.startSequence}-${summary.endSequence}) + ${recentMessages.size} recent messages")
            listOf(SystemMessage("Previous conversation summary: ${summary.summaryText}")) +
                recentMessages.map { toMessage(it) }
        } else {
            // No summary yet - return all messages
            logger.debug("No summary available, returning all ${allMessages.size} messages")
            allMessages.map { toMessage(it) }
        }
    }

    /**
     * Find the best (highest-level, active) summary covering a sequence range.
     */
    private fun findBestSummary(sessionId: UUID, endSequence: Int): MessageSummaryEntity? {
        val allSummaries = summaryRepository
            .findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        // Find highest-level summary that covers the range [0, endSequence]
        return allSummaries
            .filter { it.startSequence == 0 && it.endSequence >= endSequence }
            .maxByOrNull { it.summaryLevel }
    }

    /**
     * Convert ChatMessageEntity to Spring AI Message.
     */
    private fun toMessage(entity: ChatMessageEntity): Message {
        return when (entity.role) {
            MessageRole.USER -> UserMessage(entity.content)
            MessageRole.ASSISTANT -> AssistantMessage(entity.content)
            MessageRole.SYSTEM -> SystemMessage(entity.content)
        }
    }

    /**
     * Estimate token count (4 chars ≈ 1 token).
     */
    private fun estimateTokens(texts: List<String>): Int {
        return texts.sumOf { it.length } / 4
    }
}
```

### 5. Update MessageCompactionService

**Replace ephemeral summarization with progressive summaries:**

```kotlin
@Service
class MessageCompactionService(
    @Value("\${ai-tutor.context.max-tokens}") private val maxTokens: Int,
    @Value("\${ai-tutor.context.recent-messages}") private val recentMessageCount: Int,
    @Value("\${ai-tutor.context.summarization.enabled}") private val summarizationEnabled: Boolean,
    @Value("\${ai-tutor.context.summarization.progressive.enabled}") private val progressiveEnabled: Boolean,
    private val progressiveSummarizationService: ProgressiveSummarizationService,
    private val summarizationService: ConversationSummarizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun compactMessages(
        systemMessages: List<Message>,
        conversationMessages: List<Message>,
        sessionId: UUID
    ): List<Message> {
        logger.debug("Compacting messages for session $sessionId")

        if (!summarizationEnabled || !progressiveEnabled) {
            // Fallback: sliding window or old summarization
            return compactWithSlidingWindow(systemMessages, conversationMessages)
        }

        // Trigger async progressive summarization check (non-blocking)
        progressiveSummarizationService.checkAndSummarize(sessionId)

        // Get compacted history using existing summaries (may be slightly stale)
        val compactedHistory = progressiveSummarizationService.getCompactedHistory(
            sessionId,
            recentMessageCount
        )

        // Fit within token budget
        val systemTokens = estimateTokens(systemMessages)
        val budget = maxTokens - systemTokens
        val trimmed = trimToTokenBudget(compactedHistory, budget)

        logger.info("Compaction complete: ${trimmed.size} messages within budget")
        return systemMessages + trimmed
    }

    // Keep existing sliding window logic as fallback
    private fun compactWithSlidingWindow(
        systemMessages: List<Message>,
        conversationMessages: List<Message>
    ): List<Message> {
        val recentMessages = conversationMessages.takeLast(recentMessageCount)
        val systemTokens = estimateTokens(systemMessages)
        val budget = maxTokens - systemTokens
        val trimmed = trimToTokenBudget(recentMessages, budget)
        return systemMessages + trimmed
    }

    private fun estimateTokens(messages: List<Message>): Int {
        val totalChars = messages.sumOf { getMessageText(it).length }
        return totalChars / 4
    }

    private fun trimToTokenBudget(messages: List<Message>, budget: Int): List<Message> {
        val result = mutableListOf<Message>()
        var currentTokens = 0

        for (message in messages.reversed()) {
            val messageTokens = getMessageText(message).length / 4
            if (currentTokens + messageTokens <= budget) {
                result.add(0, message)
                currentTokens += messageTokens
            } else {
                break
            }
        }

        return result
    }

    private fun getMessageText(message: Message): String {
        return when (message) {
            is SystemMessage -> message.text ?: ""
            is UserMessage -> message.text ?: ""
            is AssistantMessage -> message.text ?: ""
            else -> ""
        }
    }
}
```

### 6. Update ChatService

**Pass sessionId to compaction:**

```kotlin
// In ChatService.sendMessage()
val compactedMessages = messageCompactionService.compactMessages(
    systemMessages = systemMessages,
    conversationMessages = conversationHistory,
    sessionId = session.id  // ADD THIS
)
```

### 7. Add REST Endpoints for Summary Information

**Add SummaryController.kt:**
```kotlin
package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse
import ch.obermuhlner.aitutor.chat.service.SummaryQueryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST endpoints for querying message summarization metadata.
 * Useful for debugging, monitoring, and admin dashboards.
 */
@RestController
@RequestMapping("/api/v1/summaries")
class SummaryController(
    private val summaryQueryService: SummaryQueryService,
    private val authorizationService: AuthorizationService
) {
    /**
     * Get summary statistics for a session.
     * Shows how many summaries exist at each level, token savings, etc.
     *
     * Accessible by: session owner OR admin
     */
    @GetMapping("/sessions/{sessionId}/info")
    fun getSessionSummaryInfo(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<SessionSummaryInfoResponse> {
        authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
        val info = summaryQueryService.getSessionSummaryInfo(sessionId)
        return ResponseEntity.ok(info)
    }

    /**
     * Get detailed view of all summaries for a session (hierarchical tree).
     * Includes summary text, sequence ranges, tokens, etc.
     *
     * Accessible by: admin only (contains potentially sensitive summary content)
     */
    @GetMapping("/sessions/{sessionId}/details")
    fun getSessionSummaryDetails(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<List<SummaryDetailResponse>> {
        authorizationService.requireAdmin(authentication)
        val details = summaryQueryService.getSessionSummaryDetails(sessionId)
        return ResponseEntity.ok(details)
    }

    /**
     * Trigger manual summarization for a session (admin only).
     * Useful for backfilling or re-summarizing with updated prompts.
     *
     * Accessible by: admin only
     */
    @PostMapping("/sessions/{sessionId}/trigger")
    fun triggerSummarization(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        authorizationService.requireAdmin(authentication)
        summaryQueryService.triggerManualSummarization(sessionId)
        return ResponseEntity.accepted().body(mapOf(
            "status" to "accepted",
            "message" to "Summarization triggered asynchronously for session $sessionId"
        ))
    }

    /**
     * Get global summarization statistics (all sessions).
     * Shows total summaries, average compression ratio, etc.
     *
     * Accessible by: admin only
     */
    @GetMapping("/stats")
    fun getGlobalStats(
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireAdmin(authentication)
        val stats = summaryQueryService.getGlobalStats()
        return ResponseEntity.ok(stats)
    }
}
```

**Add DTOs:**
```kotlin
package ch.obermuhlner.aitutor.chat.dto

import java.time.Instant
import java.util.UUID

/**
 * High-level summary statistics for a session.
 */
data class SessionSummaryInfoResponse(
    val sessionId: UUID,
    val totalMessages: Int,
    val summaryLevels: List<SummaryLevelInfo>,
    val lastSummarizedSequence: Int?,
    val estimatedTokenSavings: Int,  // Tokens saved by using summaries vs full history
    val compressionRatio: Double     // (original tokens) / (compacted tokens)
)

data class SummaryLevelInfo(
    val level: Int,
    val count: Int,          // Number of summaries at this level
    val totalTokens: Int,    // Sum of tokens in all summaries at this level
    val coveredSequences: IntRange?  // Range of message sequences covered
)

/**
 * Detailed view of a single summary (for admin debugging).
 */
data class SummaryDetailResponse(
    val id: UUID,
    val summaryLevel: Int,
    val startSequence: Int,
    val endSequence: Int,
    val summaryText: String,
    val tokenCount: Int,
    val sourceType: String,
    val sourceIds: List<UUID>,
    val supersededById: UUID?,
    val isActive: Boolean,
    val createdAt: Instant
)
```

**Add SummaryQueryService.kt:**
```kotlin
package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryLevelInfo
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import ch.obermuhlner.aitutor.tutor.service.ProgressiveSummarizationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SummaryQueryService(
    private val summaryRepository: MessageSummaryRepository,
    private val messageRepository: ChatMessageRepository,
    private val sessionRepository: ChatSessionRepository,
    private val progressiveSummarizationService: ProgressiveSummarizationService
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Get summary statistics for a session.
     */
    fun getSessionSummaryInfo(sessionId: UUID): SessionSummaryInfoResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }

        val allSummaries = summaryRepository
            .findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        val allMessages = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId)

        // Group summaries by level
        val levelGroups = allSummaries.groupBy { it.summaryLevel }

        val summaryLevels = levelGroups.map { (level, summaries) ->
            SummaryLevelInfo(
                level = level,
                count = summaries.size,
                totalTokens = summaries.sumOf { it.tokenCount },
                coveredSequences = if (summaries.isNotEmpty()) {
                    summaries.minOf { it.startSequence }..summaries.maxOf { it.endSequence }
                } else null
            )
        }.sortedBy { it.level }

        // Calculate token savings
        val lastSummarizedSequence = allSummaries.maxOfOrNull { it.endSequence }
        val originalTokens = estimateTokens(allMessages.map { it.content })
        val compactedTokens = if (allSummaries.isNotEmpty()) {
            val highestLevelSummary = allSummaries.maxByOrNull { it.summaryLevel }
            highestLevelSummary?.tokenCount ?: originalTokens
        } else {
            originalTokens
        }

        return SessionSummaryInfoResponse(
            sessionId = sessionId,
            totalMessages = allMessages.size,
            summaryLevels = summaryLevels,
            lastSummarizedSequence = lastSummarizedSequence,
            estimatedTokenSavings = originalTokens - compactedTokens,
            compressionRatio = if (compactedTokens > 0) originalTokens.toDouble() / compactedTokens else 1.0
        )
    }

    /**
     * Get detailed view of all summaries for a session.
     */
    fun getSessionSummaryDetails(sessionId: UUID): List<SummaryDetailResponse> {
        val allSummaries = summaryRepository
            .findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        return allSummaries.map { summary ->
            val sourceIds: List<UUID> = objectMapper.readValue(summary.sourceIdsJson)
            SummaryDetailResponse(
                id = summary.id,
                summaryLevel = summary.summaryLevel,
                startSequence = summary.startSequence,
                endSequence = summary.endSequence,
                summaryText = summary.summaryText,
                tokenCount = summary.tokenCount,
                sourceType = summary.sourceType.name,
                sourceIds = sourceIds,
                supersededById = summary.supersededById,
                isActive = summary.isActive,
                createdAt = summary.createdAt ?: Instant.now()
            )
        }
    }

    /**
     * Trigger manual summarization for a session.
     */
    fun triggerManualSummarization(sessionId: UUID) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }

        progressiveSummarizationService.checkAndSummarize(sessionId)
    }

    /**
     * Get global summarization statistics.
     */
    fun getGlobalStats(): Map<String, Any> {
        val allSummaries = summaryRepository.findAll()
        val allMessages = messageRepository.findAll()

        val summariesByLevel = allSummaries.groupBy { it.summaryLevel }

        val totalTokensOriginal = estimateTokens(allMessages.map { it.content })
        val totalTokensCompacted = allSummaries
            .filter { it.isActive }
            .sumOf { it.tokenCount }

        return mapOf(
            "totalSummaries" to allSummaries.size,
            "activeSummaries" to allSummaries.count { it.isActive },
            "summariesByLevel" to summariesByLevel.mapValues { it.value.size },
            "totalMessages" to allMessages.size,
            "totalTokensOriginal" to totalTokensOriginal,
            "totalTokensCompacted" to totalTokensCompacted,
            "globalCompressionRatio" to if (totalTokensCompacted > 0) {
                totalTokensOriginal.toDouble() / totalTokensCompacted
            } else 1.0
        )
    }

    private fun estimateTokens(texts: List<String>): Int {
        return texts.sumOf { it.length } / 4
    }
}
```

**Update AuthorizationService.kt:**
```kotlin
/**
 * Check if user can access session (is owner OR is admin).
 */
fun requireSessionAccessOrAdmin(sessionId: UUID, authentication: Authentication) {
    val session = sessionRepository.findById(sessionId)
        .orElseThrow { IllegalArgumentException("Session not found") }

    val userId = (authentication.principal as? UserEntity)?.id
        ?: throw IllegalStateException("Invalid authentication principal")

    val isOwner = session.userId == userId
    val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }

    if (!isOwner && !isAdmin) {
        throw AccessDeniedException("Not authorized to access this session")
    }
}

/**
 * Require admin role.
 */
fun requireAdmin(authentication: Authentication) {
    val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
    if (!isAdmin) {
        throw AccessDeniedException("Admin role required")
    }
}
```

**REST Endpoint Summary:**

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/api/v1/summaries/sessions/{id}/info` | GET | Owner or Admin | Get summary statistics for session |
| `/api/v1/summaries/sessions/{id}/details` | GET | Admin only | Get detailed summaries (including text) |
| `/api/v1/summaries/sessions/{id}/trigger` | POST | Admin only | Manually trigger summarization |
| `/api/v1/summaries/stats` | GET | Admin only | Get global summarization statistics |

**Example Response: GET /api/v1/summaries/sessions/{id}/info**
```json
{
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "totalMessages": 45,
  "summaryLevels": [
    {
      "level": 1,
      "count": 4,
      "totalTokens": 8000,
      "coveredSequences": {
        "start": 0,
        "end": 39
      }
    },
    {
      "level": 2,
      "count": 1,
      "totalTokens": 2000,
      "coveredSequences": {
        "start": 0,
        "end": 39
      }
    }
  ],
  "lastSummarizedSequence": 39,
  "estimatedTokenSavings": 6000,
  "compressionRatio": 4.0
}
```

### 8. Enable Async Configuration

**Add AsyncConfig.kt:**
```kotlin
package ch.obermuhlner.aitutor.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("summarization-")
        executor.initialize()
        return executor
    }
}
```

### 8. Configuration Updates (application.yml)

```yaml
ai-tutor:
  context:
    max-tokens: 100000
    recent-messages: 15
    summarization:
      enabled: true
      batch-size-tokens: 50000    # Max tokens per LLM batch when summarizing
      summary-token-budget: 2000  # Target tokens for each summary output
      prompt: |
        Summarize this conversation history between a language tutor and learner.
        Preserve: key topics discussed, vocabulary introduced, learner progress and errors, pedagogical context.
        Be concise but comprehensive. Target length: approximately 500 words.
      progressive:
        enabled: true               # Enable progressive summarization (feature flag)
        chunk-size: 10              # Messages per level-1 summary
        chunk-token-threshold: 8000 # Summarize early if tokens exceed this
```

### 9. Database Migration

**Create migration file:** `src/main/resources/db/migration/V6__progressive_summarization.sql`

```sql
-- Add sequence_number to chat_messages
ALTER TABLE chat_messages ADD COLUMN sequence_number INT NOT NULL DEFAULT 0;

-- Backfill sequence numbers based on creation time
UPDATE chat_messages m
SET sequence_number = (
    SELECT COUNT(*)
    FROM chat_messages m2
    WHERE m2.session_id = m.session_id
      AND m2.created_at < m.created_at
);

-- Create index for efficient sequence queries
CREATE INDEX idx_messages_session_sequence ON chat_messages(session_id, sequence_number);

-- Create message_summaries table
CREATE TABLE message_summaries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    summary_level INT NOT NULL,
    start_sequence INT NOT NULL,
    end_sequence INT NOT NULL,
    summary_text TEXT NOT NULL,
    token_count INT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_ids_json TEXT NOT NULL,
    superseded_by_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_summary_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uq_summary_range UNIQUE(session_id, summary_level, start_sequence, end_sequence)
);

-- Create indexes for efficient summary queries
CREATE INDEX idx_summaries_session_level ON message_summaries(session_id, summary_level);
CREATE INDEX idx_summaries_session_active ON message_summaries(session_id, is_active);
CREATE INDEX idx_summaries_end_seq ON message_summaries(session_id, end_sequence DESC);
```

### 10. Testing Strategy

**Unit Tests:**
- `ProgressiveSummarizationServiceTest.kt`
  - Test chunk detection (count threshold)
  - Test chunk detection (token threshold)
  - Test level-1 summarization from messages
  - Test level-2 summarization from summaries
  - Test complete chunk processing (all chunks, not just first)
  - Test superseding logic (marking old summaries inactive)
  - Test async behavior (mock async execution)

**Integration Tests:**
- `ProgressiveSummarizationIntegrationTest.kt`
  - Simulate conversation with 50+ messages
  - Verify summaries created at correct points
  - Verify ALL complete chunks processed (not just first)
  - Verify higher-level summaries created
  - Verify superseding works correctly
  - Verify `getCompactedHistory()` returns correct mix of summaries + recent messages
  - Verify token usage reduction compared to full history
  - Test async execution (wait for completion, verify non-blocking)

**Load Tests:**
- Test 100+ message conversation
- Verify performance under concurrent summarization requests
- Measure latency impact (should be near-zero with async)

### 11. Rollout Strategy

**Phase 1: Add infrastructure (non-breaking) - Week 1**
- Add database schema (migration)
- Add entities, repositories, service, async config
- Deploy with `progressive.enabled: false` (use old summarization)
- Run for 3 days, monitor for regressions

**Phase 2: Enable for new sessions - Week 2**
- Set `progressive.enabled: true` with `chunk-size: 20` (conservative)
- Monitor for 3 days
- Check summary quality (manual review of 10 sessions)
- Check token usage (should drop ~30-50% for long sessions)
- Check latency (should be unchanged due to async)

**Phase 3: Tune and optimize - Week 3-4**
- Gradually lower `chunk-size` to 10 if quality acceptable
- Monitor for 1 week
- Adjust `chunk-token-threshold` if needed
- Remove old summarization code if stable

**Phase 4: Backfill existing sessions (optional) - Week 5+**
- Background job to create summaries for old sessions
- Run during low-traffic hours
- Process 100 sessions/hour max

---

## Benefits

✅ **Efficiency**: Summaries computed once and reused
✅ **Zero latency**: Async execution doesn't block user requests
✅ **Scalability**: Hierarchical structure handles unlimited history
✅ **Token optimization**: Aggressive compaction with preserved context
✅ **Debugging**: Audit trail of summarization decisions
✅ **RAG-ready**: All raw messages preserved for future retrieval feature
✅ **Configurable**: Tune chunk size and token thresholds per deployment
✅ **Graceful degradation**: Falls back to old behavior if progressive disabled

## Trade-offs

⚠️ **Storage**: Additional database storage for summaries (~10-20% overhead)
⚠️ **Complexity**: More complex than sliding window approach (~500 LOC)
⚠️ **Eventual consistency**: Summaries lag by 1-2 requests after threshold (acceptable)
⚠️ **Information loss**: Summaries lose some fidelity (acceptable for old history)
⚠️ **Async infrastructure**: Requires thread pool management and monitoring

---

## Monitoring and Observability

### Key Metrics to Track

1. **Summarization Rate**
   - Summaries created per hour
   - Average time per summarization
   - Failures and retries

2. **Token Efficiency**
   - Average token reduction (before/after compaction)
   - Percentage of sessions using summaries
   - Token budget utilization

3. **Latency**
   - p50/p95/p99 request latency (should be unchanged)
   - Async executor queue depth
   - Thread pool utilization

4. **Quality**
   - Manual review sample (10 summaries/week)
   - User feedback on conversation coherence

### Logging

```kotlin
// Key log statements to include:
logger.info("Summarizing chunk ${index + 1}/${chunks.size} for session $sessionId (${chunk.size} messages)")
logger.info("Propagating to level ${level + 1}: chunk ${index + 1}, summarizing ${chunk.size} level-$level summaries")
logger.error("Error during async summarization for session $sessionId", e)
logger.debug("Using level-${summary.summaryLevel} summary (sequences ${summary.startSequence}-${summary.endSequence})")
```

---

## Future Enhancements

1. **RAG Integration**: Use preserved messages for semantic search of important past exchanges
2. **Summary regeneration**: Admin endpoint to rebuild summaries with improved prompts
3. **Adaptive chunk size**: Dynamically adjust based on conversation density
4. **Summary caching**: Cache reconstructed histories in Redis for hot sessions
5. **Compression**: GZIP summary_text to reduce storage by ~50%
6. **Smart selection**: Use embeddings to find most relevant summary level for context

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Async summarization fails silently | Users get full history (inefficient) | Comprehensive error logging + monitoring alerts |
| Thread pool exhaustion | Summarization pauses | Bounded queue (100), reject policy logs errors |
| Summary quality degrades | Tutor context poor | Manual review + rollback to old system if needed |
| Database migration fails | Deployment blocked | Test migration on staging, have rollback script |
| Concurrent summarization conflict | Duplicate summaries | Use unique constraint, handle gracefully |

**Rollback plan:** Set `progressive.enabled: false`, redeploy. Old system remains functional.