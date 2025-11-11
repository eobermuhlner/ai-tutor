package ch.obermuhlner.aitutor.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "curriculum_rules")
class CurriculumRuleEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "course_id", nullable = false)
    var courseId: UUID,

    @Column(name = "progression_mode", nullable = false, length = 32)
    var progressionMode: String,  // TIME_BASED/LINEAR/ADAPTIVE

    @Column(name = "allow_skipping", nullable = false)
    var allowSkipping: Boolean = false,

    @Column(name = "require_completion", nullable = false)
    var requireCompletion: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)