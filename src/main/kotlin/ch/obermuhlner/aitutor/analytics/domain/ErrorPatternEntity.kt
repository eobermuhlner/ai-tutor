package ch.obermuhlner.aitutor.analytics.domain

import ch.obermuhlner.aitutor.core.model.ErrorType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(
    name = "error_patterns",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "lang", "error_type"])]
)
class ErrorPatternEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 32)
    val lang: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 32)
    val errorType: ErrorType,

    @Column(name = "total_count", nullable = false)
    var totalCount: Int = 0,

    @Column(name = "critical_count", nullable = false)
    var criticalCount: Int = 0,

    @Column(name = "high_count", nullable = false)
    var highCount: Int = 0,

    @Column(name = "medium_count", nullable = false)
    var mediumCount: Int = 0,

    @Column(name = "low_count", nullable = false)
    var lowCount: Int = 0,

    @Column(name = "first_seen_at", nullable = false)
    val firstSeenAt: Instant,

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
) {
    /**
     * Weighted severity score using PhaseDecisionService weights.
     */
    fun computeWeightedScore(): Double {
        return (criticalCount * 3.0) +
               (highCount * 2.0) +
               (mediumCount * 1.0) +
               (lowCount * 0.3)
    }
}
