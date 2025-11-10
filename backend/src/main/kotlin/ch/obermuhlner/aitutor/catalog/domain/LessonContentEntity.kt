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
@Table(name = "lesson_content")
class LessonContentEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "course_id", nullable = false)
    var courseId: UUID,

    @Column(name = "lesson_id", nullable = false, length = 128)
    var lessonId: String,

    @Column(nullable = false, length = 256)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,  // markdown content

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "minimum_days")
    var minimumDays: Int? = null,

    @Column(name = "required_turns")
    var requiredTurns: Int? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)