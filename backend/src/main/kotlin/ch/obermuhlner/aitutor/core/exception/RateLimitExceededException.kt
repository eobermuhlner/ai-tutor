package ch.obermuhlner.aitutor.core.exception

import java.time.Duration
import java.time.Instant

/**
 * Exception thrown when a user exceeds their rate limit for AI LLM calls.
 *
 * @property userId The ID of the user who exceeded the rate limit
 * @property retryAfter Duration until the user can retry (based on when tokens will be available)
 * @property limitType The type of limit that was exceeded (e.g., "hourly", "daily")
 */
class RateLimitExceededException(
    val userId: String,
    val retryAfter: Duration,
    val limitType: String,
    message: String = "Rate limit exceeded. Please try again in ${retryAfter.toMinutes()} minutes."
) : RuntimeException(message) {
    val retryAfterSeconds: Long = retryAfter.seconds
    val retryAfterTimestamp: Instant = Instant.now().plus(retryAfter)
}
