package ch.obermuhlner.aitutor.vocabulary.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(
    name = "vocabulary_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "lang", "lemma"])]
)
class VocabularyItemEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 32)
    val lang: String,

    @Column(nullable = false, length = 256)
    val lemma: String,

    @Column(nullable = false)
    var exposures: Int = 0,

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant = Instant.now(),

    @Column(name = "concept_name", length = 256)
    var conceptName: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
