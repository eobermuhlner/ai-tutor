package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseImport
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.CurriculumImport
import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.domain.LanguageImport
import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.domain.TutorArchetypeImport
import ch.obermuhlner.aitutor.catalog.domain.TutorImport
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.domain.UnifiedCatalogImport
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * Service for importing catalog data from unified YAML format.
 * Supports importing languages, tutors, and courses in a single file.
 */
@Service
class UnifiedCatalogImportService(
    private val objectMapper: ObjectMapper,
    private val fileImportService: FileImportService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    /**
     * Result of importing catalog data.
     */
    data class CatalogImportResult(
        val languages: List<LanguageEntity>,
        val tutors: List<TutorProfileEntity>,
        val courses: List<CourseTemplateEntity>,
        val lessons: List<LessonContentEntity>,
        val errors: List<String> = emptyList()
    ) {
        val success: Boolean
            get() = errors.isEmpty()
    }

    /**
     * Parse unified catalog YAML file.
     */
    fun parseCatalog(file: MultipartFile): UnifiedCatalogImport {
        try {
            val catalog = yamlMapper.readValue(file.inputStream, UnifiedCatalogImport::class.java)
            logger.info("Parsed unified catalog: ${catalog.languages.size} languages, " +
                    "${catalog.tutors.size} tutors, ${catalog.courses.size} courses")
            return catalog
        } catch (e: Exception) {
            logger.error("Failed to parse unified catalog file: ${file.originalFilename}", e)
            throw IllegalArgumentException("Invalid unified catalog format: ${e.message}", e)
        }
    }

    /**
     * Parse unified catalog YAML from string.
     */
    fun parseCatalogFromString(content: String): UnifiedCatalogImport {
        try {
            val catalog = yamlMapper.readValue(content, UnifiedCatalogImport::class.java)
            logger.info("Parsed unified catalog from string: ${catalog.languages.size} languages, " +
                    "${catalog.tutors.size} tutors, ${catalog.courses.size} courses")
            return catalog
        } catch (e: Exception) {
            logger.error("Failed to parse unified catalog string", e)
            throw IllegalArgumentException("Invalid unified catalog format: ${e.message}", e)
        }
    }

    /**
     * Import complete catalog from unified YAML file and optional lesson files.
     */
    fun importCatalog(
        catalogFile: MultipartFile,
        lessonFiles: List<MultipartFile> = emptyList(),
        sourceType: SourceType = SourceType.UPLOADED
    ): CatalogImportResult {
        val errors = mutableListOf<String>()

        try {
            // 1. Parse unified catalog
            val catalog = parseCatalog(catalogFile)

            // 2. Import languages
            val languageEntities = importLanguages(catalog.languages)
            logger.info("Imported ${languageEntities.size} languages")

            // 3. Import tutors (with archetype resolution)
            val tutorEntities = importTutors(
                catalog.tutors,
                catalog.tutorArchetypes,
                errors
            )
            logger.info("Imported ${tutorEntities.size} tutors with ${errors.size} errors")

            // 4. Import courses and lessons
            val (courseEntities, lessonEntities) = importCoursesAndLessons(
                catalog.courses,
                tutorEntities,
                lessonFiles,
                sourceType,
                errors
            )
            logger.info("Imported ${courseEntities.size} courses with ${lessonEntities.size} lessons")

            return CatalogImportResult(
                languages = languageEntities,
                tutors = tutorEntities,
                courses = courseEntities,
                lessons = lessonEntities,
                errors = errors
            )
        } catch (e: Exception) {
            logger.error("Failed to import catalog", e)
            throw IllegalArgumentException("Catalog import failed: ${e.message}", e)
        }
    }

    /**
     * Import languages from unified format.
     */
    private fun importLanguages(languages: List<LanguageImport>): List<LanguageEntity> {
        return languages.map { lang ->
            LanguageEntity(
                code = lang.code,
                nameJson = objectMapper.writeValueAsString(lang.name),
                flagEmoji = lang.flagEmoji,
                nativeName = lang.nativeName,
                difficulty = lang.difficulty,
                descriptionJson = objectMapper.writeValueAsString(lang.description),
                isActive = lang.isActive,
                displayOrder = lang.displayOrder,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
    }

    /**
     * Import tutors from unified format with archetype resolution.
     */
    private fun importTutors(
        tutors: List<TutorImport>,
        archetypes: List<TutorArchetypeImport>,
        errors: MutableList<String>
    ): List<TutorProfileEntity> {
        // Create archetype lookup map
        val archetypeMap = archetypes.associateBy { it.id }

        return tutors.mapNotNull { tutor ->
            try {
                createTutorEntity(tutor, archetypeMap)
            } catch (e: Exception) {
                errors.add("Failed to import tutor '${tutor.name}': ${e.message}")
                logger.error("Failed to import tutor '${tutor.name}'", e)
                null
            }
        }
    }

    /**
     * Create TutorProfileEntity from import data with archetype resolution.
     */
    private fun createTutorEntity(
        tutor: TutorImport,
        archetypeMap: Map<String, TutorArchetypeImport>
    ): TutorProfileEntity {
        // Resolve properties from archetype or direct definition
        val (emoji, personaEnglish, domainEnglish, descriptionEnglish, personality, teachingStyle, voiceId) =
            if (tutor.archetypeId != null) {
                // Option 1: Use archetype
                val archetype = archetypeMap[tutor.archetypeId]
                    ?: throw IllegalArgumentException("Archetype '${tutor.archetypeId}' not found")

                // Template interpolation: replace {culturalNotes}
                val description = archetype.descriptionTemplateEnglish
                    .replace("{culturalNotes}", tutor.culturalNotes ?: "")

                TutorProperties(
                    emoji = tutor.emoji ?: archetype.emoji,
                    personaEnglish = archetype.personaEnglish,
                    domainEnglish = archetype.domainEnglish,
                    descriptionEnglish = description,
                    personality = tutor.personality ?: archetype.personality,
                    teachingStyle = tutor.teachingStyle ?: archetype.teachingStyle,
                    voiceId = tutor.voiceId ?: archetype.voiceId
                )
            } else {
                // Option 2: Use direct definition
                require(tutor.personality != null) { "Tutor '${tutor.name}' must have either archetypeId or personality" }
                require(tutor.teachingStyle != null) { "Tutor '${tutor.name}' must have either archetypeId or teachingStyle" }
                require(tutor.persona != null) { "Tutor '${tutor.name}' must have either archetypeId or persona" }
                require(tutor.domain != null) { "Tutor '${tutor.name}' must have either archetypeId or domain" }
                require(tutor.description != null) { "Tutor '${tutor.name}' must have either archetypeId or description" }

                TutorProperties(
                    emoji = tutor.emoji ?: "👤",
                    personaEnglish = tutor.persona["en"] ?: "",
                    domainEnglish = tutor.domain["en"] ?: "",
                    descriptionEnglish = tutor.description["en"] ?: "",
                    personality = tutor.personality,
                    teachingStyle = tutor.teachingStyle,
                    voiceId = tutor.voiceId
                )
            }

        // Create persona/domain/description JSON
        val personaJson = tutor.persona?.let { objectMapper.writeValueAsString(it) }
            ?: """{"en": "$personaEnglish"}"""
        val domainJson = tutor.domain?.let { objectMapper.writeValueAsString(it) }
            ?: """{"en": "$domainEnglish"}"""
        val descriptionJson = tutor.description?.let { objectMapper.writeValueAsString(it) }
            ?: """{"en": "$descriptionEnglish"}"""
        val culturalBackgroundJson = tutor.culturalBackground?.let { objectMapper.writeValueAsString(it) }
            ?: tutor.culturalNotes?.let { """{"en": "$it"}""" }

        return TutorProfileEntity(
            name = tutor.name,
            emoji = emoji,
            personaEnglish = personaEnglish,
            domainEnglish = domainEnglish,
            descriptionEnglish = descriptionEnglish,
            personaJson = personaJson,
            domainJson = domainJson,
            descriptionJson = descriptionJson,
            culturalBackgroundJson = culturalBackgroundJson,
            location = tutor.location,
            personality = personality,
            teachingStyle = teachingStyle,
            voiceId = voiceId,
            gender = tutor.gender,
            age = tutor.age,
            targetLanguageCode = tutor.targetLanguage,
            isActive = true,
            displayOrder = tutor.displayOrder,
            isGlobal = tutor.isGlobal,
            createdByUserId = null,  // Set by caller if needed
            sourceType = SourceType.UPLOADED
        )
    }

    /**
     * Import courses and lessons from unified format.
     */
    private fun importCoursesAndLessons(
        courses: List<CourseImport>,
        tutors: List<TutorProfileEntity>,
        lessonFiles: List<MultipartFile>,
        sourceType: SourceType,
        errors: MutableList<String>
    ): Pair<List<CourseTemplateEntity>, List<LessonContentEntity>> {
        val allCourses = mutableListOf<CourseTemplateEntity>()
        val allLessons = mutableListOf<LessonContentEntity>()

        // Create tutor name-to-ID lookup
        val tutorByName = tutors.associateBy { it.name.lowercase() }

        // Parse all lesson files into a lookup map
        val lessonFileMap = lessonFiles.mapNotNull { file ->
            try {
                val parsed = fileImportService.parseLesson(file)
                parsed.lessonId to parsed
            } catch (e: Exception) {
                errors.add("Failed to parse lesson file ${file.originalFilename}: ${e.message}")
                null
            }
        }.toMap()

        for (course in courses) {
            try {
                // Resolve suggested tutor IDs
                val suggestedTutorIds = course.suggestedTutors?.mapNotNull { name ->
                    tutorByName[name.lowercase()]?.id
                        ?: run {
                            errors.add("Tutor '$name' not found for course ${course.name["en"]}")
                            null
                        }
                } ?: emptyList()

                // Create course entity
                // Seed data should be published immediately, uploads start as drafts
                val isDraft = sourceType != SourceType.SEEDED
                val publishedAt = if (isDraft) null else Instant.now()

                val courseEntity = CourseTemplateEntity(
                    languageCode = course.languageCode,
                    nameJson = objectMapper.writeValueAsString(course.name),
                    shortDescriptionJson = objectMapper.writeValueAsString(course.shortDescription),
                    descriptionJson = objectMapper.writeValueAsString(course.description),
                    category = course.category,
                    targetAudienceJson = objectMapper.writeValueAsString(course.targetAudience),
                    startingLevel = course.startingLevel,
                    targetLevel = course.targetLevel,
                    estimatedWeeks = course.estimatedWeeks,
                    suggestedTutorIdsJson = if (suggestedTutorIds.isNotEmpty())
                        objectMapper.writeValueAsString(suggestedTutorIds) else null,
                    defaultPhase = ConversationPhase.Auto,
                    topicSequenceJson = null,
                    learningGoalsJson = objectMapper.writeValueAsString(course.learningGoals),
                    tagsJson = course.tags?.let { objectMapper.writeValueAsString(it) },
                    isActive = true,
                    displayOrder = course.displayOrder,
                    isDraft = isDraft,
                    publishedAt = publishedAt,
                    sourceType = sourceType
                )

                allCourses.add(courseEntity)

                // Import lessons if curriculum is provided
                if (course.curriculum != null) {
                    val lessons = importLessons(
                        courseEntity.id,
                        course.curriculum,
                        lessonFileMap,
                        errors
                    )
                    allLessons.addAll(lessons)
                }
            } catch (e: Exception) {
                errors.add("Failed to import course ${course.name["en"]}: ${e.message}")
                logger.error("Failed to import course ${course.name["en"]}", e)
            }
        }

        return Pair(allCourses, allLessons)
    }

    /**
     * Import lessons for a course from curriculum.
     */
    private fun importLessons(
        courseId: UUID,
        curriculum: CurriculumImport,
        lessonFileMap: Map<String, FileImportService.LessonParseResult>,
        errors: MutableList<String>
    ): List<LessonContentEntity> {
        return curriculum.lessons.mapIndexedNotNull { index, lesson ->
            try {
                val markdown = when {
                    // Option 1: Embedded content
                    lesson.content != null -> lesson.content

                    // Option 2: File reference
                    lesson.file != null -> {
                        val parsed = lessonFileMap[lesson.id]
                            ?: throw IllegalArgumentException("Lesson file not found for ${lesson.id}")
                        parsed.markdown
                    }

                    else -> throw IllegalArgumentException("Lesson ${lesson.id} has neither content nor file")
                }

                LessonContentEntity(
                    courseId = courseId,
                    lessonId = lesson.id,
                    title = lesson.title ?: "Untitled Lesson",
                    content = markdown,
                    displayOrder = index,
                    requiredTurns = lesson.requiredTurns.takeIf { it > 0 },
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            } catch (e: Exception) {
                errors.add("Failed to import lesson ${lesson.id}: ${e.message}")
                logger.error("Failed to import lesson ${lesson.id}", e)
                null
            }
        }
    }

    /**
     * Validate unified catalog file before importing.
     */
    fun validateCatalog(
        catalogFile: MultipartFile?,
        lessonFiles: List<MultipartFile> = emptyList()
    ): List<String> {
        val errors = mutableListOf<String>()

        // Validate catalog file
        if (catalogFile == null || catalogFile.isEmpty) {
            errors.add("Catalog file is required")
            return errors
        }

        try {
            val catalog = parseCatalog(catalogFile)

            // Validate tutors
            val archetypeIds = catalog.tutorArchetypes.map { it.id }.toSet()
            catalog.tutors.forEach { tutor ->
                if (tutor.archetypeId != null && tutor.archetypeId !in archetypeIds) {
                    errors.add("Tutor '${tutor.name}' references unknown archetype '${tutor.archetypeId}'")
                }
                if (tutor.archetypeId == null) {
                    // Direct definition validation
                    if (tutor.personality == null) errors.add("Tutor '${tutor.name}' missing personality")
                    if (tutor.teachingStyle == null) errors.add("Tutor '${tutor.name}' missing teachingStyle")
                    if (tutor.persona == null) errors.add("Tutor '${tutor.name}' missing persona")
                    if (tutor.domain == null) errors.add("Tutor '${tutor.name}' missing domain")
                    if (tutor.description == null) errors.add("Tutor '${tutor.name}' missing description")
                }
            }

            // Validate courses
            val tutorNames = catalog.tutors.map { it.name.lowercase() }.toSet()
            catalog.courses.forEach { course ->
                course.suggestedTutors?.forEach { tutorName ->
                    if (tutorName.lowercase() !in tutorNames) {
                        errors.add("Course '${course.name["en"]}' references unknown tutor '$tutorName'")
                    }
                }

                // Validate lessons
                course.curriculum?.lessons?.forEach { lesson ->
                    if (lesson.content == null && lesson.file == null) {
                        errors.add("Lesson '${lesson.id}' must have either content or file")
                    }
                    if (lesson.file != null) {
                        val fileExists = lessonFiles.any {
                            val parsed = try {
                                fileImportService.parseLesson(it)
                            } catch (e: Exception) {
                                null
                            }
                            parsed?.lessonId == lesson.id
                        }
                        if (!fileExists) {
                            errors.add("Lesson '${lesson.id}' references file '${lesson.file}' but file not found")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Failed to parse catalog file: ${e.message}")
        }

        return errors
    }

    /**
     * Helper data class for tutor properties resolution.
     */
    private data class TutorProperties(
        val emoji: String,
        val personaEnglish: String,
        val domainEnglish: String,
        val descriptionEnglish: String,
        val personality: ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality,
        val teachingStyle: ch.obermuhlner.aitutor.tutor.domain.TeachingStyle,
        val voiceId: ch.obermuhlner.aitutor.core.model.catalog.TutorVoice?
    )
}
