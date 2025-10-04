package ch.obermuhlner.aitutor.user.repository

import ch.obermuhlner.aitutor.user.domain.UserEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByUsername(username: String): UserEntity?
    fun findByEmail(email: String): UserEntity?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}
