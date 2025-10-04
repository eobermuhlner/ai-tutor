package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopicDecisionServiceTest {

    private lateinit var topicDecisionService: TopicDecisionService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
        topicDecisionService = TopicDecisionService(objectMapper)
    }

    @Test
    fun `should accept LLM proposal when topics are the same`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Tell me about cooking"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Cooking is...")
        )

        val result = topicDecisionService.decideTopic("cooking", "cooking", messages)

        assertEquals("cooking", result)
    }

    @Test
    fun `should reject topic change when too early (hysteresis)`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Tell me about cooking"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Cooking is...")
        )

        // Only 1 turn, need 3 to change
        val result = topicDecisionService.decideTopic("cooking", "sports", messages)

        assertEquals("cooking", result) // Should keep current topic
    }

    @Test
    fun `should accept topic change after minimum turns`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..4).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }

        // 4 turns, enough to change
        val result = topicDecisionService.decideTopic("cooking", "sports", messages)

        assertEquals("sports", result)
    }

    @Test
    fun `should reject recently discussed topic`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..4).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }
        val pastTopics = objectMapper.writeValueAsString(listOf("travel", "sports", "music"))

        val result = topicDecisionService.decideTopic("cooking", "sports", messages, pastTopics)

        assertEquals("cooking", result) // Should reject recently discussed topic
    }

    @Test
    fun `should accept change for stale topic`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..15).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }

        // 15 turns, stale (max is 12)
        val result = topicDecisionService.decideTopic("cooking", "sports", messages)

        assertEquals("sports", result) // Should encourage variety
    }

    @Test
    fun `should require minimum turns to establish new topic from null`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Hello")
        )

        // Only 1 turn, need 2 to establish
        val result = topicDecisionService.decideTopic(null, "cooking", messages)

        assertNull(result) // Should stay in free conversation
    }

    @Test
    fun `should establish new topic after minimum engagement`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..3).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }

        // 3 turns, enough to establish
        val result = topicDecisionService.decideTopic(null, "cooking", messages)

        assertEquals("cooking", result)
    }

    @Test
    fun `should allow return to free conversation after minimum turns`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..4).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }

        val result = topicDecisionService.decideTopic("cooking", null, messages)

        assertNull(result) // Natural conclusion
    }

    @Test
    fun `should keep topic alive when too early to return to null`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message 1"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply 1")
        )

        val result = topicDecisionService.decideTopic("cooking", null, messages)

        assertEquals("cooking", result) // Keep topic alive
    }

    @Test
    fun `should count turns correctly`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = (1..5).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "Message $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply $it")
            )
        }

        val turnCount = topicDecisionService.countTurnsInRecentMessages(messages)

        assertEquals(5, turnCount)
    }

    @Test
    fun `should archive topic after sufficient turns`() {
        val result = topicDecisionService.shouldArchiveTopic("cooking", 3)

        assertTrue(result)
    }

    @Test
    fun `should not archive topic with insufficient turns`() {
        val result = topicDecisionService.shouldArchiveTopic("cooking", 2)

        assertFalse(result)
    }

    @Test
    fun `should not archive null topic`() {
        val result = topicDecisionService.shouldArchiveTopic(null, 10)

        assertFalse(result)
    }

    @Test
    fun `should extract explicit topic from user message`() {
        val result = topicDecisionService.extractExplicitTopic("Let's talk about cooking")

        assertEquals("cooking", result)
    }

    @Test
    fun `should extract explicit topic case insensitive`() {
        val result = topicDecisionService.extractExplicitTopic("LET'S TALK ABOUT SPORTS")

        assertEquals("SPORTS", result)
    }

    @Test
    fun `should return null when no explicit topic in message`() {
        val result = topicDecisionService.extractExplicitTopic("Hello, how are you?")

        assertNull(result)
    }
}
