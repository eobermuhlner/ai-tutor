package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.GrammarPoint
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.Scenario
import ch.obermuhlner.aitutor.lesson.domain.VocabEntry
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class LessonContentService(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lessonCache = ConcurrentHashMap<String, LessonContent>()
    private val curriculumCache = ConcurrentHashMap<String, CourseCurriculum>()

    // YAML mapper for curriculum.yml files
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun getLesson(courseId: String, lessonId: String): LessonContent? {
        val cacheKey = "$courseId/$lessonId"
        // computeIfAbsent requires non-null value, so we need special handling
        val cached = lessonCache[cacheKey]
        if (cached != null) return cached

        val loaded = loadLessonFromFile(courseId, lessonId)
        if (loaded != null) {
            lessonCache[cacheKey] = loaded
        }
        return loaded
    }

    private fun loadLessonFromFile(courseId: String, lessonId: String): LessonContent? {
        return try {
            val resource = ClassPathResource("course-content/$courseId/$lessonId.md")
            if (!resource.exists()) {
                logger.warn("Lesson file not found: $courseId/$lessonId.md")
                return null
            }
            val markdown = resource.inputStream.bufferedReader().readText()
            parseLesson(lessonId, markdown)
        } catch (e: Exception) {
            logger.error("Failed to load lesson $lessonId for course $courseId", e)
            null
        }
    }

    private fun parseLesson(lessonId: String, markdown: String): LessonContent {
        // Extract YAML frontmatter (between --- delimiters)
        val frontmatterRegex = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)
        val frontmatterMatch = frontmatterRegex.find(markdown)

        val frontmatter = if (frontmatterMatch != null) {
            yamlMapper.readValue<Map<String, Any>>(frontmatterMatch.groupValues[1])
        } else {
            emptyMap()
        }

        val contentWithoutFrontmatter = if (frontmatterMatch != null) {
            markdown.substring(frontmatterMatch.range.last + 1)
        } else {
            markdown
        }

        // Extract sections using regex
        val goals = extractListSection(contentWithoutFrontmatter, "This Week's Goals")
        val grammarPoints = extractGrammarPoints(contentWithoutFrontmatter)
        val vocabulary = extractVocabulary(contentWithoutFrontmatter)
        val scenarios = extractScenarios(contentWithoutFrontmatter)
        val practicePatterns = extractListSection(contentWithoutFrontmatter, "Practice Patterns")
        val commonMistakes = extractListSection(contentWithoutFrontmatter, "Common Mistakes to Watch")

        return LessonContent(
            id = frontmatter["lessonId"] as? String ?: lessonId,
            title = frontmatter["title"] as? String ?: "Untitled",
            weekNumber = (frontmatter["weekNumber"] as? Number)?.toInt(),
            estimatedDuration = frontmatter["estimatedDuration"] as? String,
            focusAreas = (frontmatter["focusAreas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            targetCEFR = CEFRLevel.valueOf(frontmatter["targetCEFR"] as? String ?: "A1"),
            goals = goals,
            grammarPoints = grammarPoints,
            essentialVocabulary = vocabulary,
            conversationScenarios = scenarios,
            practicePatterns = practicePatterns,
            commonMistakes = commonMistakes,
            fullMarkdown = markdown
        )
    }

    private fun extractListSection(markdown: String, sectionTitle: String): List<String> {
        val sectionRegex = Regex("""## $sectionTitle\s*\n(.*?)(?=\n##|\z)""", RegexOption.DOT_MATCHES_ALL)
        val match = sectionRegex.find(markdown) ?: return emptyList()

        return match.groupValues[1]
            .lines()
            .filter { it.trim().startsWith("-") }
            .map { it.trim().removePrefix("-").trim() }
    }

    private fun extractGrammarPoints(markdown: String): List<GrammarPoint> {
        // Simplified: Extract subsections under ## Grammar Focus
        val grammarSection = Regex("""## Grammar Focus\s*\n(.*?)(?=\n##|\z)""", RegexOption.DOT_MATCHES_ALL)
            .find(markdown)?.groupValues?.get(1) ?: return emptyList()

        // Extract ### subsections
        val subsections = Regex("""### (.*?)\n(.*?)(?=\n###|\z)""", RegexOption.DOT_MATCHES_ALL)
            .findAll(grammarSection)
            .map { match ->
                val title = match.groupValues[1].trim()
                val content = match.groupValues[2]
                val ruleMatch = Regex("""\*\*Rule:\*\* (.+)""").find(content)
                val rule = ruleMatch?.groupValues?.get(1) ?: ""

                GrammarPoint(
                    title = title,
                    rule = rule,
                    examples = extractBulletPoints(content, "Examples:"),
                    patterns = extractBulletPoints(content, "Patterns to Practice:")
                )
            }
            .toList()

        return subsections
    }

    private fun extractVocabulary(markdown: String): List<VocabEntry> {
        val vocabSection = Regex("""## Essential Vocabulary\s*\n(.*?)(?=\n##|\z)""", RegexOption.DOT_MATCHES_ALL)
            .find(markdown)?.groupValues?.get(1) ?: return emptyList()

        return Regex("""- \*\*(.*?)\*\* - (.+)""")
            .findAll(vocabSection)
            .map { match ->
                VocabEntry(
                    word = match.groupValues[1].trim(),
                    translation = match.groupValues[2].trim()
                )
            }
            .toList()
    }

    private fun extractScenarios(markdown: String): List<Scenario> {
        val scenariosSection = Regex("""## Conversation Scenarios\s*\n(.*?)(?=\n##|\z)""", RegexOption.DOT_MATCHES_ALL)
            .find(markdown)?.groupValues?.get(1) ?: return emptyList()

        // Flexible regex: tolerate any amount of whitespace between title and code block
        return Regex("""### (.*?)\s*\n\s*```\s*\n(.*?)\n```""", RegexOption.DOT_MATCHES_ALL)
            .findAll(scenariosSection)
            .map { match ->
                Scenario(
                    title = match.groupValues[1].trim(),
                    dialogue = match.groupValues[2].trim()
                )
            }
            .toList()
    }

    private fun extractBulletPoints(text: String, header: String): List<String> {
        val headerPos = text.indexOf(header)
        if (headerPos == -1) return emptyList()

        val afterHeader = text.substring(headerPos + header.length)
        return afterHeader
            .lines()
            .takeWhile { it.trim().startsWith("-") || it.isBlank() }
            .filter { it.trim().startsWith("-") }
            .map { it.trim().removePrefix("-").trim() }
    }

    fun getCurriculum(courseId: String): CourseCurriculum? {
        // Check cache first
        val cached = curriculumCache[courseId]
        if (cached != null) return cached

        // Load from file
        val loaded = try {
            val resource = ClassPathResource("course-content/$courseId/curriculum.yml")
            if (!resource.exists()) {
                logger.warn("Curriculum file not found: $courseId/curriculum.yml")
                return null
            }
            yamlMapper.readValue(resource.inputStream, CourseCurriculum::class.java)
        } catch (e: Exception) {
            logger.error("Failed to load curriculum for course $courseId", e)
            return null
        }

        // Cache and return
        if (loaded != null) {
            curriculumCache[courseId] = loaded
        }
        return loaded
    }
}
