package ch.obermuhlner.aitutor.user.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(
    name = "user_language_proficiency",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "language_code"])
    ]
)
class UserLanguageProficiencyEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "language_code", nullable = false, length = 32)
    var languageCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_type", nullable = false, length = 32)
    var proficiencyType: LanguageProficiencyType,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 8)
    var cefrLevel: CEFRLevel? = null,

    @Column(name = "is_native", nullable = false)
    var isNative: Boolean = false,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "self_assessed", nullable = false)
    var selfAssessed: Boolean = true,

    @Column(name = "last_assessed_at")
    var lastAssessedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
