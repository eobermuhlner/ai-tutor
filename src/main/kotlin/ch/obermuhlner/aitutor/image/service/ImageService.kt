package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.domain.ImageEntity
import java.nio.file.Path

interface ImageService {
    fun getImageByConcept(concept: String): ImageEntity?
    fun createImage(concept: String, data: ByteArray, format: String, width: Int, height: Int): ImageEntity
    fun deleteImage(concept: String)
    fun loadImagesFromDirectory(directory: Path)
}
