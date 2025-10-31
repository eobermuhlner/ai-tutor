package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return createUserDetails(user)
    }

    fun loadUserById(userId: String): UserDetails {
        val user = userRepository.findById(java.util.UUID.fromString(userId))
            .orElseThrow { UsernameNotFoundException("User not found with id: $userId") }

        return createUserDetails(user)
    }

    private fun createUserDetails(user: UserEntity): UserDetails {
        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

        return User.builder()
            .username(user.username)
            .password(user.passwordHash ?: "")
            .authorities(authorities)
            .accountExpired(user.accountExpired)
            .accountLocked(user.locked)
            .credentialsExpired(user.credentialsExpired)
            .disabled(!user.enabled)
            .build()
    }
}
