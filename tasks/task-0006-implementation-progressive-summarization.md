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
4. **Lazy upward propagation**: Create higher-level summaries when lower level has N entries
5. **Retention policy**: **Keep all raw messages** for future RAG feature (no deletion/archiving)

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

    UNIQUE(session_id, summary_level, start_sequence, end_sequence)
);

CREATE INDEX idx_summaries_session_level ON message_summaries(session_id, summary_level);
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
import java.util.UUID

interface MessageSummaryRepository : JpaRepository<MessageSummaryEntity, UUID> {
    /**
     * Get all summaries at a specific level for a session, ordered by sequence.
     */
    fun findBySessionIdAndSummaryLevelOrderByStartSequenceAsc(
        sessionId: UUID,
        summaryLevel: Int
    ): List<MessageSummaryEntity>

    /**
     * Get all summaries for a session, ordered by level then sequence.
     */
    fun findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(
        sessionId: UUID
    ): List<MessageSummaryEntity>

    /**
     * Get the most recent summary (highest end_sequence) for a session.
     */
    fun findTopBySessionIdOrderByEndSequenceDesc(
        sessionId: UUID
    ): MessageSummaryEntity?

    /**
     * Get summaries covering a specific sequence range at a given level.
     */
    fun findBySessionIdAndSummaryLevelAndEndSequenceGreaterThanEqualAndStartSequenceLessThanEqualOrderByStartSequenceAsc(
        sessionId: UUID,
        summaryLevel: Int,
        startSequence: Int,
        endSequence: Int
    ): List<MessageSummaryEntity>
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
 * Count unsummarized messages (sequence > last summary's end_sequence).
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
- Create level-1 summaries from raw messages
- Recursively create higher-level summaries
- Reconstruct conversation history from summaries + recent messages

**Key Methods:**
```kotlin
package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import ch.obermuhlner.aitutor.chat.domain.SummarySourceType
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProgressiveSummarizationService(
    private val summarizationService: ConversationSummarizationService,
    private val summaryRepository: MessageSummaryRepository,
    private val messageRepository: ChatMessageRepository,
    @Value("\${ai-tutor.summarization.chunk-size}") private val chunkSize: Int,
    @Value("\${ai-tutor.summarization.chunk-token-threshold}") private val chunkTokenThreshold: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Check if summarization is needed after new messages, and trigger if so.
     */
    @Transactional
    fun checkAndSummarize(sessionId: UUID) {
        // 1. Find last summarized sequence
        val lastSummary = summaryRepository.findTopBySessionIdOrderByEndSequenceDesc(sessionId)
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

        // 4. Summarize messages into level-1 summary
        logger.info("Triggering level-1 summarization for session $sessionId (${unsummarized.size} messages)")
        val level1Summary = summarizeMessageChunk(sessionId, unsummarized)
        summaryRepository.save(level1Summary)

        // 5. Recursively check if level-1 summaries need level-2 summarization
        propagateUpward(sessionId, level = 1)
    }

    /**
     * Recursively create higher-level summaries when lower level has enough entries.
     */
    @Transactional
    private fun propagateUpward(sessionId: UUID, level: Int) {
        val summaries = summaryRepository
            .findBySessionIdAndSummaryLevelOrderByStartSequenceAsc(sessionId, level)

        // Check if we have enough summaries at this level to create next level
        if (summaries.size >= chunkSize) {
            val chunk = summaries.take(chunkSize)
            logger.info("Propagating to level ${level + 1}: summarizing ${chunk.size} level-$level summaries")
            val higherLevelSummary = summarizeSummaryChunk(sessionId, level + 1, chunk)
            summaryRepository.save(higherLevelSummary)

            // Recursively propagate
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
                MessageRole.USER -> org.springframework.ai.chat.messages.UserMessage(entity.content)
                MessageRole.ASSISTANT -> org.springframework.ai.chat.messages.AssistantMessage(entity.content)
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
     */
    fun getCompactedHistory(sessionId: UUID, recentMessageCount: Int): List<Message> {
        // Get all messages
        val allMessages = messageRepository.findBySessionOrderByCreatedAtAsc(
            messageRepository.getReferenceById(sessionId)
        )

        if (allMessages.size <= recentMessageCount) {
            // Short conversation - return all messages
            return allMessages.map { toMessage(it) }
        }

        // Split into old and recent
        val recentMessages = allMessages.takeLast(recentMessageCount)
        val oldMessageCount = allMessages.size - recentMessageCount

        // Get highest-level summary that covers old messages
        val summary = findBestSummary(sessionId, endSequence = oldMessageCount - 1)

        return if (summary != null) {
            // Use summary + recent messages
            listOf(SystemMessage("Previous conversation summary: ${summary.summaryText}")) +
                recentMessages.map { toMessage(it) }
        } else {
            // No summary yet - return all messages
            allMessages.map { toMessage(it) }
        }
    }

    /**
     * Find the best (highest-level) summary covering a sequence range.
     */
    private fun findBestSummary(sessionId: UUID, endSequence: Int): MessageSummaryEntity? {
        val allSummaries = summaryRepository
            .findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(sessionId)

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
            MessageRole.USER -> org.springframework.ai.chat.messages.UserMessage(entity.content)
            MessageRole.ASSISTANT -> org.springframework.ai.chat.messages.AssistantMessage(entity.content)
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
    private val progressiveSummarizationService: ProgressiveSummarizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun compactMessages(
        systemMessages: List<Message>,
        conversationMessages: List<Message>,
        sessionId: UUID
    ): List<Message> {
        logger.debug("Compacting messages for session $sessionId")

        if (!summarizationEnabled) {
            // Fallback: sliding window
            return compactWithSlidingWindow(systemMessages, conversationMessages)
        }

        // Trigger progressive summarization check
        progressiveSummarizationService.checkAndSummarize(sessionId)

        // Get compacted history (summaries + recent messages)
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
    private fun compactWithSlidingWindow(...) { ... }
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

### 7. Configuration Updates (application.yml)

```yaml
ai-tutor:
  context:
    max-tokens: 100000
    recent-messages: 15
    summarization:
      enabled: true
      batch-size-tokens: 50000    # Existing - max tokens per LLM batch
      summary-token-budget: 2000  # Existing - target tokens per summary
      prompt: |                    # Existing - summarization prompt
        Summarize this conversation history between a language tutor and learner.
        Preserve: key topics discussed, vocabulary introduced, learner progress and errors, pedagogical context.
        Be concise but comprehensive. Target length: approximately 500 words.
  summarization:
    chunk-size: 10                  # NEW - messages per level-1 summary
    chunk-token-threshold: 8000     # NEW - summarize early if tokens exceed this
```

### 8. Database Migration

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
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_summary_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uq_summary_range UNIQUE(session_id, summary_level, start_sequence, end_sequence)
);

-- Create indexes for efficient summary queries
CREATE INDEX idx_summaries_session_level ON message_summaries(session_id, summary_level);
CREATE INDEX idx_summaries_end_seq ON message_summaries(session_id, end_sequence DESC);
```

### 9. Testing Strategy

**Unit Tests:**
- `ProgressiveSummarizationServiceTest.kt`
  - Test chunk detection (count threshold)
  - Test chunk detection (token threshold)
  - Test level-1 summarization from messages
  - Test level-2 summarization from summaries
  - Test upward propagation logic

**Integration Tests:**
- `ProgressiveSummarizationIntegrationTest.kt`
  - Simulate conversation with 50+ messages
  - Verify summaries created at correct points
  - Verify higher-level summaries created
  - Verify `getCompactedHistory()` returns correct mix of summaries + recent messages
  - Verify token usage reduction compared to full history

### 10. Rollout Strategy

**Phase 1: Add infrastructure (non-breaking)**
- Add database schema (migration)
- Add entities, repositories, service
- Deploy with `ai-tutor.summarization.chunk-size: 999999` (effectively disabled)

**Phase 2: Enable for new sessions**
- Set `chunk-size: 10` in production
- Monitor for 1 week
- Check summary quality, token usage, latency

**Phase 3: Backfill existing sessions (optional)**
- Background job to create summaries for old sessions
- Run during low-traffic hours

---

## Benefits

✅ **Efficiency**: Summaries computed once and reused
✅ **Scalability**: Hierarchical structure handles unlimited history
✅ **Token optimization**: Aggressive compaction with preserved context
✅ **Debugging**: Audit trail of summarization decisions
✅ **RAG-ready**: All raw messages preserved for future retrieval feature
✅ **Configurable**: Tune chunk size and token thresholds per deployment

## Trade-offs

⚠️ **Storage**: Additional database storage for summaries (~10-20% overhead)
⚠️ **Complexity**: More complex than sliding window approach
⚠️ **Latency**: First message after chunk threshold triggers LLM call (~2-5s)
⚠️ **Information loss**: Summaries lose some fidelity (acceptable for old history)

---

## Future Enhancements

1. **RAG Integration**: Use preserved messages for semantic search of important past exchanges
2. **Summary regeneration**: Admin endpoint to rebuild summaries with improved prompts
3. **Adaptive chunk size**: Dynamically adjust based on conversation density
4. **Summary caching**: Cache reconstructed histories in Redis for hot sessions