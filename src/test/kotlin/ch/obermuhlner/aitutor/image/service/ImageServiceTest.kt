package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.domain.ImageEntity
import ch.obermuhlner.aitutor.image.repository.ImageRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.springframework.boot.ApplicationArguments
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class ImageServiceTest {

    private lateinit var imageRepository: ImageRepository
    private lateinit var imageServiceImpl: ImageServiceImpl

    @BeforeEach
    fun setUp() {
        imageRepository = mock(ImageRepository::class.java)
        imageServiceImpl = ImageServiceImpl(imageRepository)
    }

    @Test
    fun `createImage should save image entity`() {
        val concept = "apple"
        val data = ByteArray(100)
        val format = "png"
        val width = 100
        val height = 100

        val entity = ImageEntity(
            concept = concept,
            data = data,
            format = format,
            widthPx = width,
            heightPx = height
        )

        `when`(imageRepository.save(any(ImageEntity::class.java))).thenReturn(entity)

        val result = imageServiceImpl.createImage(concept, data, format, width, height)

        assertNotNull(result)
        assertEquals(concept, result.concept)
        assertEquals(format, result.format)
        assertEquals(width, result.widthPx)
        assertEquals(height, result.heightPx)

        verify(imageRepository, times(1)).save(any(ImageEntity::class.java))
    }

    @Test
    fun `getImageByConcept should return image if exists`() {
        val concept = "book"
        val entity = ImageEntity(
            concept = concept,
            data = ByteArray(100),
            format = "png",
            widthPx = 100,
            heightPx = 100
        )

        `when`(imageRepository.findByConcept(concept)).thenReturn(entity)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNotNull(result)
        assertEquals(concept, result?.concept)

        verify(imageRepository, times(1)).findByConcept(concept)
    }

    @Test
    fun `getImageByConcept should return null if not exists`() {
        val concept = "nonexistent"

        `when`(imageRepository.findByConcept(concept)).thenReturn(null)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageRepository, times(1)).findByConcept(concept)
    }

    @Test
    fun `deleteImage should delete image if exists`() {
        val concept = "water"
        val entity = ImageEntity(
            concept = concept,
            data = ByteArray(100),
            format = "png",
            widthPx = 100,
            heightPx = 100
        )

        `when`(imageRepository.findByConcept(concept)).thenReturn(entity)

        imageServiceImpl.deleteImage(concept)

        verify(imageRepository, times(1)).findByConcept(concept)
        verify(imageRepository, times(1)).delete(entity)
    }

    @Test
    fun `deleteImage should not throw if concept does not exist`() {
        val concept = "nonexistent"

        `when`(imageRepository.findByConcept(concept)).thenReturn(null)

        assertDoesNotThrow {
            imageServiceImpl.deleteImage(concept)
        }

        verify(imageRepository, times(1)).findByConcept(concept)
        verify(imageRepository, never()).delete(any(ImageEntity::class.java))
    }

    @Test
    fun `loadImagesFromDirectory should load images from directory`(@TempDir tempDir: Path) {
        // Create test image file
        val imageData = createTestImageData()
        val imagePath = tempDir.resolve("apple.png")
        Files.write(imagePath, imageData)

        `when`(imageRepository.findByConcept("apple")).thenReturn(null)
        `when`(imageRepository.save(any(ImageEntity::class.java))).thenAnswer { it.arguments[0] }

        imageServiceImpl.loadImagesFromDirectory(tempDir)

        verify(imageRepository, times(1)).findByConcept("apple")
        verify(imageRepository, times(1)).save(any(ImageEntity::class.java))
    }

    @Test
    fun `loadImagesFromDirectory should skip existing images`(@TempDir tempDir: Path) {
        // Create test image file
        val imageData = createTestImageData()
        val imagePath = tempDir.resolve("book.png")
        Files.write(imagePath, imageData)

        val existingEntity = ImageEntity(
            concept = "book",
            data = imageData,
            format = "png",
            widthPx = 100,
            heightPx = 100
        )

        `when`(imageRepository.findByConcept("book")).thenReturn(existingEntity)

        imageServiceImpl.loadImagesFromDirectory(tempDir)

        verify(imageRepository, times(1)).findByConcept("book")
        verify(imageRepository, never()).save(any(ImageEntity::class.java))
    }

    private fun createTestImageData(): ByteArray {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}
