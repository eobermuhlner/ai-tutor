package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import jakarta.persistence.*

@Entity
@Table(name = "languages")
data class LanguageEntity(
    @Id
    @Column(length = 32, nullable = false)
    var code: String = "",

    @Column(name = "name_json", nullable = false, columnDefinition = "TEXT")
    var nameJson: String = "",

    @Column(name = "flag_emoji", length = 10, nullable = false)
    var flagEmoji: String = "",

    @Column(name = "native_name", length = 100, nullable = false)
    var nativeName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    var difficulty: Difficulty = Difficulty.Easy,

    @Column(name = "description_json", columnDefinition = "TEXT")
    var descriptionJson: String = "",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at")
    var createdAt: java.time.Instant? = null,

    @Column(name = "updated_at")
    var updatedAt: java.time.Instant? = null
)