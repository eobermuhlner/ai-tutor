package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.domain.ImageEntity
import ch.obermuhlner.aitutor.image.repository.ImageRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

@Service
class ImageServiceImpl(
    private val imageRepository: ImageRepository
) : ImageService, ApplicationRunner {

    private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

    @Value("\${ai-tutor.images.seed-directory:src/main/resources/images}")
    private lateinit var seedDirectory: String

    override fun run(args: ApplicationArguments) {
        val dir = Paths.get(seedDirectory)
        if (Files.exists(dir)) {
            logger.info("Loading images from directory: $dir")
            loadImagesFromDirectory(dir)
        } else {
            logger.warn("Image seed directory not found: $dir")
        }
    }

    @Transactional
    override fun loadImagesFromDirectory(directory: Path) {
        Files.list(directory)
            .filter { Files.isRegularFile(it) }
            .forEach { path ->
                try {
                    val filename = path.fileName.toString()
                    val concept = filename.substringBeforeLast('.')

                    // Skip if already exists
                    if (imageRepository.findByConcept(concept) != null) {
                        logger.debug("Image already exists for concept: $concept")
                        return@forEach
                    }

                    val imageData = Files.readAllBytes(path)
                    val image = ImageIO.read(ByteArrayInputStream(imageData))

                    if (image == null) {
                        logger.warn("Failed to read image: $filename")
                        return@forEach
                    }

                    val format = filename.substringAfterLast('.', "png")

                    createImage(concept, imageData, format, image.width, image.height)
                    logger.info("Loaded image for concept: $concept ($format, ${image.width}x${image.height})")
                } catch (e: Exception) {
                    logger.error("Error loading image from ${path.fileName}: ${e.message}", e)
                }
            }
    }

    @Transactional
    override fun createImage(concept: String, data: ByteArray, format: String, width: Int, height: Int): ImageEntity {
        val entity = ImageEntity(
            concept = concept,
            data = data,
            format = format,
            widthPx = width,
            heightPx = height
        )
        return imageRepository.save(entity)
    }

    @Transactional(readOnly = true)
    override fun getImageByConcept(concept: String): ImageEntity? {
        return imageRepository.findByConcept(concept)
    }

    @Transactional
    override fun deleteImage(concept: String) {
        imageRepository.findByConcept(concept)?.let {
            imageRepository.delete(it)
        }
    }
}
