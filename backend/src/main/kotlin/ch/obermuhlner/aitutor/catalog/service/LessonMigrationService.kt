package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

@Service
class LessonMigrationService(
    private val lessonContentRepository: LessonContentRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val resourceLoader: ResourceLoader
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun migrateLessonsOnStartup() {
        logger.info("Starting lesson migration process...")
        
        try {
            val allCourses = courseTemplateRepository.findAll()
            
            for (course in allCourses) {
                logger.info("Processing course ${course.id} for lesson migration")
                
                // Find lessons in the filesystem for this course
                val coursePath = "course-content/${course.languageCode}/${course.id}"
                val courseResource = resourceLoader.getResource("classpath:$coursePath")
                
                if (courseResource.exists() && courseResource.isReadable()) {
                    // Get all lesson files in the course directory
                    val lessonFiles = getLessonFilesForCourse(courseResource, course.id.toString())
                    
                    for ((lessonFileName, lessonContent) in lessonFiles) {
                        val lessonId = extractLessonId(lessonFileName)
                        if (lessonId != null) {
                            // Check if lesson already exists in database
                            if (!lessonContentRepository.existsByCourseIdAndLessonId(course.id, lessonId)) {
                                // Create lesson entity and save to database
                                val lessonEntity = LessonContentEntity(
                                    courseId = course.id,
                                    lessonId = lessonId,
                                    title = lessonId.replace('-', ' ').replaceFirstChar { it.uppercase() },
                                    content = lessonContent,
                                    displayOrder = getNextDisplayOrder(course.id), // Assign order based on file sequence
                                    minimumDays = null,
                                    requiredTurns = null,
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now()
                                )
                                
                                lessonContentRepository.save(lessonEntity)
                                logger.info("Migrated lesson $lessonId for course ${course.id}")
                            } else {
                                logger.debug("Lesson $lessonId already exists in database for course ${course.id}, skipping migration")
                            }
                        }
                    }
                }
            }
            
            logger.info("Lesson migration process completed successfully")
        } catch (e: Exception) {
            logger.error("Error during lesson migration process", e)
        }
    }

    private fun getLessonFilesForCourse(courseResource: Resource, courseDirectoryName: String): Map<String, String> {
        val lessonFiles = mutableMapOf<String, String>()
        
        try {
            val courseDir = courseResource.file
            if (courseDir.exists() && courseDir.isDirectory) {
                val lessonFilesArray = courseDir.listFiles { file -> 
                    file.isFile && (file.name.endsWith(".md") || file.name.endsWith(".txt"))
                }
                
                if (lessonFilesArray != null) {
                    for (lessonFile in lessonFilesArray) {
                        val content = lessonFile.readText(StandardCharsets.UTF_8)
                        lessonFiles[lessonFile.name] = content
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not read lesson files from course directory: ${courseDirectoryName}", e)
        }
        
        return lessonFiles
    }

    private fun extractLessonId(fileName: String): String? {
        return fileName
            .substringBeforeLast('.', fileName) // Remove file extension
            .lowercase()
            .replace(" ", "-")
            .replace("[^a-z0-9-]".toRegex(), "")
    }

    private fun getNextDisplayOrder(courseId: UUID): Int {
        val existingLessons = lessonContentRepository.findByCourseId(courseId)
        return if (existingLessons.isEmpty()) 0 else existingLessons.size
    }
}