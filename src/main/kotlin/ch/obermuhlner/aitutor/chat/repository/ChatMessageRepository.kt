package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<ChatMessageEntity>
    fun countBySessionId(sessionId: UUID): Long
}
