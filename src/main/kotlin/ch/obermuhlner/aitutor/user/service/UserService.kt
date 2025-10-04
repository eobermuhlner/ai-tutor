package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import java.util.UUID
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun findById(id: UUID): UserEntity? {
        return userRepository.findById(id).orElse(null)
    }

    fun findByUsername(username: String): UserEntity? {
        return userRepository.findByUsername(username)
    }

    fun findByEmail(email: String): UserEntity? {
        return userRepository.findByEmail(email)
    }

    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun createUser(user: UserEntity): UserEntity {
        // Hash password if provided
        if (user.passwordHash != null && !user.passwordHash!!.startsWith("$2a$")) {
            user.passwordHash = passwordEncoder.encode(user.passwordHash)
        }
        return userRepository.save(user)
    }

    fun updateUser(user: UserEntity): UserEntity {
        return userRepository.save(user)
    }

    fun updatePassword(userId: UUID, newPassword: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found: $userId") }

        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
    }

    fun deleteUser(userId: UUID) {
        userRepository.deleteById(userId)
    }

    fun findAll(): List<UserEntity> {
        return userRepository.findAll()
    }
}
