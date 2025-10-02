package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<ChatMessageEntity>
    fun countBySessionId(sessionId: UUID): Long
}
