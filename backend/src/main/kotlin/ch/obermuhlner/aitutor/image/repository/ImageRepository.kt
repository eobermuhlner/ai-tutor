package ch.obermuhlner.aitutor.image.repository

import ch.obermuhlner.aitutor.image.domain.ImageEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageRepository : JpaRepository<ImageEntity, UUID> {
    fun findByConcept(concept: String): ImageEntity?
}
