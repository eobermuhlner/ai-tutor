package ch.obermuhlner.aitutor.image.service

interface ImageService {
    fun getImageByConcept(concept: String): ImageData?
}

data class ImageData(
    val data: ByteArray,
    val format: String,
    val contentType: String
)
