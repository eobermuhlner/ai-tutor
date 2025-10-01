package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, UUID> {
    fun findByUserId(userId: UUID): List<ChatSessionEntity>
    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<ChatSessionEntity>
}
