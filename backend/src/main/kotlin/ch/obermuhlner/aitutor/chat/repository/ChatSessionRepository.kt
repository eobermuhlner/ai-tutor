package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, UUID> {
    fun findByUserId(userId: UUID): List<ChatSessionEntity>
    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<ChatSessionEntity>

    // NEW: Course-related queries
    fun findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId: UUID): List<ChatSessionEntity>
    fun findByUserIdAndCourseTemplateIdAndIsActiveTrue(userId: UUID, courseTemplateId: UUID): ChatSessionEntity?
    fun findByUserIdAndTutorProfileIdAndIsActiveTrue(userId: UUID, tutorProfileId: UUID): List<ChatSessionEntity>
}
