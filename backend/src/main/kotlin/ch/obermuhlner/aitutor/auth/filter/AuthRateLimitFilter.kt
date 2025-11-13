package ch.obermuhlner.aitutor.auth.filter

import ch.obermuhlner.aitutor.auth.config.AuthRateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting filter for authentication endpoints.
 * Prevents brute-force attacks by limiting requests per IP address.
 */
@Component
class AuthRateLimitFilter(
    private val rateLimitProperties: AuthRateLimitProperties
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val bucketCache = ConcurrentHashMap<String, Bucket>()

    companion object {
        private val AUTH_ENDPOINTS = setOf(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset",
            "/api/v1/auth/verify-email"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Only apply rate limiting to auth endpoints
        if (!AUTH_ENDPOINTS.contains(path)) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIp(request)
        val bucket = getBucketForIp(clientIp)

        if (bucket.tryConsume(1)) {
            // Request allowed
            filterChain.doFilter(request, response)
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for IP: $clientIp on path: $path")
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write("""{"error":"Too many requests. Please try again later."}""")
        }
    }

    private fun getBucketForIp(ip: String): Bucket {
        return bucketCache.computeIfAbsent(ip) { createBucket() }
    }

    private fun createBucket(): Bucket {
        val refill = Refill.intervally(
            rateLimitProperties.refillTokens.toLong(),
            Duration.ofMinutes(rateLimitProperties.refillPeriodMinutes.toLong())
        )
        val limit = Bandwidth.classic(rateLimitProperties.capacity.toLong(), refill)
        return Bucket.builder().addLimit(limit).build()
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // Check common proxy headers for real client IP
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (xForwardedFor != null && xForwardedFor.isNotBlank()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (xRealIp != null && xRealIp.isNotBlank()) {
            return xRealIp
        }

        // Fall back to remote address
        return request.remoteAddr ?: "unknown"
    }
}
