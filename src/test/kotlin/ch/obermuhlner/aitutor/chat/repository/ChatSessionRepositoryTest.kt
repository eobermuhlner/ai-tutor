package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
class ChatSessionRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var chatSessionRepository: ChatSessionRepository

    private val testUserId = UUID.randomUUID()
    private val testCourseId = UUID.randomUUID()
    private val testTutorId = UUID.randomUUID()

    @Test
    fun `should find active sessions ordered by updated date`() {
        // Given
        val activeSession1 = createSession(testUserId, isActive = true)
        val activeSession2 = createSession(testUserId, isActive = true)
        val inactiveSession = createSession(testUserId, isActive = false)

        entityManager.persist(activeSession1)
        Thread.sleep(10)  // Ensure different timestamps
        entityManager.persist(activeSession2)
        entityManager.persist(inactiveSession)
        entityManager.flush()

        // When
        val result = chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(testUserId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isActive })
        // Most recently updated should be first
        assertTrue((result[0].updatedAt?.isAfter(result[1].updatedAt) ?: false) ||
                   (result[0].updatedAt?.equals(result[1].updatedAt) ?: false))
    }

    @Test
    fun `should find session by user and course template`() {
        // Given
        val session1 = createSession(testUserId, courseTemplateId = testCourseId, isActive = true)
        val session2 = createSession(testUserId, courseTemplateId = UUID.randomUUID(), isActive = true)
        val inactiveSession = createSession(testUserId, courseTemplateId = testCourseId, isActive = false)

        entityManager.persist(session1)
        entityManager.persist(session2)
        entityManager.persist(inactiveSession)
        entityManager.flush()

        // When
        val result = chatSessionRepository.findByUserIdAndCourseTemplateIdAndIsActiveTrue(testUserId, testCourseId)

        // Then
        assertNotNull(result)
        assertEquals(testCourseId, result?.courseTemplateId)
        assertTrue(result?.isActive ?: false)
    }

    @Test
    fun `should return null when no active session for course template`() {
        // Given
        val inactiveSession = createSession(testUserId, courseTemplateId = testCourseId, isActive = false)
        entityManager.persist(inactiveSession)
        entityManager.flush()

        // When
        val result = chatSessionRepository.findByUserIdAndCourseTemplateIdAndIsActiveTrue(testUserId, testCourseId)

        // Then
        assertNull(result)
    }

    @Test
    fun `should find sessions by user and tutor profile`() {
        // Given
        val session1 = createSession(testUserId, tutorProfileId = testTutorId, isActive = true)
        val session2 = createSession(testUserId, tutorProfileId = testTutorId, isActive = true)
        val otherTutorSession = createSession(testUserId, tutorProfileId = UUID.randomUUID(), isActive = true)
        val inactiveSession = createSession(testUserId, tutorProfileId = testTutorId, isActive = false)

        entityManager.persist(session1)
        entityManager.persist(session2)
        entityManager.persist(otherTutorSession)
        entityManager.persist(inactiveSession)
        entityManager.flush()

        // When
        val result = chatSessionRepository.findByUserIdAndTutorProfileIdAndIsActiveTrue(testUserId, testTutorId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.tutorProfileId == testTutorId })
        assertTrue(result.all { it.isActive })
    }

    @Test
    fun `should maintain backward compatibility with existing queries`() {
        // Given
        val session1 = createSession(testUserId)
        val session2 = createSession(testUserId)

        entityManager.persist(session1)
        entityManager.persist(session2)
        entityManager.flush()

        // When - existing methods should still work
        val byUserId = chatSessionRepository.findByUserId(testUserId)
        val byUserIdOrdered = chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(testUserId)

        // Then
        assertEquals(2, byUserId.size)
        assertEquals(2, byUserIdOrdered.size)
    }

    private fun createSession(
        userId: UUID,
        courseTemplateId: UUID? = null,
        tutorProfileId: UUID? = null,
        isActive: Boolean = true
    ): ChatSessionEntity {
        return ChatSessionEntity(
            id = UUID.randomUUID(),
            userId = userId,
            tutorName = "Test Tutor",
            tutorPersona = "patient coach",
            tutorDomain = "general conversation",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            currentTopic = null,
            courseTemplateId = courseTemplateId,
            tutorProfileId = tutorProfileId,
            customName = null,
            isActive = isActive
        )
    }
}
