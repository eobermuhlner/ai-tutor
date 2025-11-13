package ch.obermuhlner.aitutor.auth.repository

import ch.obermuhlner.aitutor.auth.domain.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, UUID> {
    fun findByToken(token: String): PasswordResetTokenEntity?
    fun findByUserId(userId: UUID): List<PasswordResetTokenEntity>
    fun deleteByExpiresAtBefore(instant: Instant): Int
}
