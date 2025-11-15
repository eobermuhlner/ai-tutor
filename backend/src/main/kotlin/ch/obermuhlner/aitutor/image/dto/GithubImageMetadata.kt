package ch.obermuhlner.aitutor.image.dto

import kotlinx.serialization.Serializable

@Serializable
data class GithubImageMetadata(
    val filename: String,
    val tags: List<String>
)