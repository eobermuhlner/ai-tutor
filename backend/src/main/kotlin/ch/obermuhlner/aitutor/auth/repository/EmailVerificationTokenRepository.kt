package ch.obermuhlner.aitutor.auth.repository

import ch.obermuhlner.aitutor.auth.domain.EmailVerificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationTokenEntity, UUID> {
    fun findByToken(token: String): EmailVerificationTokenEntity?
    fun findByUserId(userId: UUID): List<EmailVerificationTokenEntity>
    fun deleteByExpiresAtBefore(instant: Instant): Int
}
