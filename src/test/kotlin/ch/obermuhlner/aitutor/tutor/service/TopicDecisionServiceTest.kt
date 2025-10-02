package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopicDecisionServiceTest {

    private lateinit var topicDecisionService: TopicDecisionService

    @BeforeEach
    fun setup() {
        topicDecisionService = TopicDecisionService()
    }

    @Test
    fun `should return null when not enough messages`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Hello"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Hi")
        )

        val result = topicDecisionService.decideTopic(null, messages)

        assertNull(result)
    }

    @Test
    fun `should maintain current topic when enough messages`() {
        val session = TestDataFactory.createSessionEntity()
        val currentTopic = "cooking"
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "What is a recipe?"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "A recipe is..."),
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Tell me about baking"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Baking is...")
        )

        val result = topicDecisionService.decideTopic(currentTopic, messages)

        assertEquals(currentTopic, result)
    }

    @Test
    fun `should return null when no current topic and not enough messages`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Hello")
        )

        val result = topicDecisionService.decideTopic(null, messages)

        assertNull(result)
    }

    @Test
    fun `should archive topic when enough messages exchanged`() {
        val topic = "sports"
        val messageCount = 10 // 5 exchanges

        val result = topicDecisionService.shouldArchiveTopic(topic, messageCount)

        assertTrue(result)
    }

    @Test
    fun `should not archive topic when too few messages`() {
        val topic = "sports"
        val messageCount = 4 // 2 exchanges

        val result = topicDecisionService.shouldArchiveTopic(topic, messageCount)

        assertFalse(result)
    }

    @Test
    fun `should not archive null topic`() {
        val result = topicDecisionService.shouldArchiveTopic(null, 10)

        assertFalse(result)
    }

    @Test
    fun `should extract explicit topic from user message`() {
        val userMessage = "Let's talk about cooking"

        val result = topicDecisionService.extractExplicitTopic(userMessage)

        assertEquals("cooking", result)
    }

    @Test
    fun `should extract explicit topic case insensitive`() {
        val userMessage = "LET'S TALK ABOUT SPORTS"

        val result = topicDecisionService.extractExplicitTopic(userMessage)

        assertEquals("SPORTS", result)
    }

    @Test
    fun `should return null when no explicit topic in message`() {
        val userMessage = "Hello, how are you?"

        val result = topicDecisionService.extractExplicitTopic(userMessage)

        assertNull(result)
    }

    @Test
    fun `should extract topic with apostrophe variation`() {
        val userMessage = "Lets talk about music"

        val result = topicDecisionService.extractExplicitTopic(userMessage)

        assertEquals("music", result)
    }
}
