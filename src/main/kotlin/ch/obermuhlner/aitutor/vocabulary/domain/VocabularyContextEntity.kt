package ch.obermuhlner.aitutor.vocabulary.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

@Entity
@Table(name = "vocabulary_contexts", indexes = [Index(columnList = "vocab_item_id")])
class VocabularyContextEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_item_id")
    val vocabItem: VocabularyItemEntity,

    @Column(nullable = false, length = 512)
    val context: String,

    @Column(name = "turn_id", nullable = true)
    val turnId: UUID? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null
)
