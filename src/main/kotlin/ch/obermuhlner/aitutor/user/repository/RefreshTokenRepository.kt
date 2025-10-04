package ch.obermuhlner.aitutor.user.repository

import ch.obermuhlner.aitutor.user.domain.RefreshTokenEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?
    fun findAllByUserId(userId: UUID): List<RefreshTokenEntity>
    fun deleteByUserId(userId: UUID)
}
