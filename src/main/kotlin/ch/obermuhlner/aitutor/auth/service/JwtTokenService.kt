package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.JwtProperties
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtTokenService(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun generateAccessToken(user: UserEntity): String {
        val now = Instant.now()
        val expiresAt = Date.from(now.plusMillis(jwtProperties.expirationMs))

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("username", user.username)
            .claim("email", user.email)
            .claim("roles", user.roles.map { it.name })
            .issuedAt(Date.from(now))
            .expiration(expiresAt)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(user: UserEntity): String {
        val now = Instant.now()
        val expiresAt = Date.from(now.plusMillis(jwtProperties.refreshExpirationMs))

        return Jwts.builder()
            .subject(user.id.toString())
            .issuedAt(Date.from(now))
            .expiration(expiresAt)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun getUsernameFromToken(token: String): String {
        val claims = getClaims(token)
        return claims["username"] as String
    }

    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<UserRole> {
        val claims = getClaims(token)
        val roleNames = claims["roles"] as? List<String> ?: emptyList()
        return roleNames.map { UserRole.valueOf(it) }
    }

    fun getExpirationFromToken(token: String): Instant {
        val claims = getClaims(token)
        return claims.expiration.toInstant()
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
