package ch.obermuhlner.aitutor.image.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "images")
class ImageEntity(
    @field:Id
    val id: UUID = UUID.randomUUID(),

    @field:Column(unique = true, nullable = false, length = 256)
    val concept: String,

    @field:Column(nullable = false, length = 16)
    val format: String,

    @field:Column(nullable = false)
    val widthPx: Int,

    @field:Column(nullable = false)
    val heightPx: Int,

    @field:Lob
    @field:Column(nullable = false)
    val data: ByteArray,

    @field:CreationTimestamp
    @field:Column(nullable = false, updatable = false)
    val createdAt: Instant? = null
)
