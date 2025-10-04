package ch.obermuhlner.aitutor.image.dto

import java.time.Instant

data class ImageUploadResponse(
    val id: Long,
    val filename: String,
    val contentType: String,
    val size: Long,
    val uploadDate: Instant,
    val tags: List<String>
)

data class ImageMetadataResponse(
    val id: Long,
    val filename: String,
    val contentType: String,
    val size: Long,
    val uploadDate: Instant,
    val storageType: String,
    val tags: List<String>
)
