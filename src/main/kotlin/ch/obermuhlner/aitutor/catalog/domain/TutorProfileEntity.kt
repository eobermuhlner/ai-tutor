package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
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
@Table(name = "tutor_profiles")
class TutorProfileEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "emoji", nullable = false, length = 16)
    var emoji: String,

    // For AI system prompt (English only, always required)
    @Column(name = "persona_english", nullable = false, length = 256)
    var personaEnglish: String,

    @Column(name = "domain_english", nullable = false, length = 256)
    var domainEnglish: String,

    @Column(name = "description_english", nullable = false, columnDefinition = "TEXT")
    var descriptionEnglish: String,

    // For UI catalog (multilingual JSON with AI translation fallback)
    @Column(name = "persona_json", nullable = false, columnDefinition = "TEXT")
    var personaJson: String,

    @Column(name = "domain_json", nullable = false, columnDefinition = "TEXT")
    var domainJson: String,

    @Column(name = "description_json", nullable = false, columnDefinition = "TEXT")
    var descriptionJson: String,

    @Column(name = "cultural_background_json", columnDefinition = "TEXT")
    var culturalBackgroundJson: String? = null,

    @Column(name = "location", length = 128)
    var location: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "personality", nullable = false, length = 32)
    var personality: TutorPersonality,

    @Enumerated(EnumType.STRING)
    @Column(name = "teaching_style", nullable = false, length = 16)
    var teachingStyle: TeachingStyle = TeachingStyle.Reactive,

    @Enumerated(EnumType.STRING)
    @Column(name = "voice_id", length = 32)
    var voiceId: TutorVoice? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    var gender: TutorGender? = null,

    @Column(name = "age", nullable = false)
    var age: Int = 30,

    @Column(name = "target_language_code", nullable = false, length = 32)
    var targetLanguageCode: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
