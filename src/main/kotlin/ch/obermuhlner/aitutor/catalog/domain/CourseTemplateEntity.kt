package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "course_templates")
class CourseTemplateEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "language_code", nullable = false, length = 32)
    var languageCode: String,

    @Column(name = "name_json", nullable = false, columnDefinition = "TEXT")
    var nameJson: String,

    @Column(name = "short_description_json", nullable = false, columnDefinition = "TEXT")
    var shortDescriptionJson: String,

    @Column(name = "description_json", nullable = false, columnDefinition = "TEXT")
    var descriptionJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    var category: CourseCategory,

    @Column(name = "target_audience_json", nullable = false, columnDefinition = "TEXT")
    var targetAudienceJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "starting_level", nullable = false, length = 8)
    var startingLevel: CEFRLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 8)
    var targetLevel: CEFRLevel,

    @Column(name = "estimated_weeks")
    var estimatedWeeks: Int? = null,

    @Column(name = "suggested_tutor_ids_json", columnDefinition = "TEXT")
    var suggestedTutorIdsJson: String? = null,  // JSON array of UUIDs

    @Enumerated(EnumType.STRING)
    @Column(name = "default_phase", nullable = false, length = 16)
    var defaultPhase: ConversationPhase = ConversationPhase.Auto,

    @Column(name = "topic_sequence_json", columnDefinition = "TEXT")
    var topicSequenceJson: String? = null,  // JSON array of topics

    @Column(name = "learning_goals_json", nullable = false, columnDefinition = "TEXT")
    var learningGoalsJson: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "tags_json", columnDefinition = "TEXT")
    var tagsJson: String? = null,  // JSON array of strings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
