package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Component
@Profile("dev", "default")  // Only run in dev mode
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun seedData() {
        // Only seed if database is empty
        if (tutorProfileRepository.count() > 0) {
            println("Seed data already exists, skipping...")
            return
        }

        println("Seeding catalog data...")

        val tutors = seedTutors()
        val courses = seedCourses(tutors)

        println("Seeded ${tutors.size} tutors and ${courses.size} courses")
    }

    private fun seedTutors(): Map<String, List<TutorProfileEntity>> {
        val spanishTutors = listOf(
            createTutor(
                name = "María",
                emoji = "👩‍🏫",
                personaEnglish = "patient coach",
                domainEnglish = "general conversation, grammar, typography",
                descriptionEnglish = "Patient coach from Madrid who loves helping beginners feel confident",
                personality = TutorPersonality.Encouraging,
                targetLanguageCode = "es",
                displayOrder = 0
            ),
            createTutor(
                name = "Professor Rodríguez",
                emoji = "🎓",
                personaEnglish = "strict academic",
                domainEnglish = "Spanish grammar, literature, formal writing",
                descriptionEnglish = "Academic expert in Spanish grammar and literature with high standards",
                personality = TutorPersonality.Academic,
                targetLanguageCode = "es",
                displayOrder = 1
            ),
            createTutor(
                name = "Carlos",
                emoji = "😊",
                personaEnglish = "casual friend",
                domainEnglish = "everyday conversation, slang, culture",
                descriptionEnglish = "Friendly guy from Barcelona who makes learning Spanish fun and relaxed",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "es",
                displayOrder = 2
            )
        )

        val frenchTutors = listOf(
            createTutor(
                name = "François",
                emoji = "👨‍🏫",
                personaEnglish = "academic professor",
                domainEnglish = "French linguistics, literature, formal grammar",
                descriptionEnglish = "Parisian professor with expertise in French linguistics and literature",
                personality = TutorPersonality.Academic,
                targetLanguageCode = "fr",
                displayOrder = 0
            ),
            createTutor(
                name = "Céline",
                emoji = "😊",
                personaEnglish = "casual tutor",
                domainEnglish = "everyday French, conversation, culture",
                descriptionEnglish = "Young tutor from Lyon who loves casual conversation and cultural exchange",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "fr",
                displayOrder = 1
            )
        )

        val germanTutors = listOf(
            createTutor(
                name = "Herr Schmidt",
                emoji = "🎓",
                personaEnglish = "strict teacher",
                domainEnglish = "German grammar, formal writing, business German",
                descriptionEnglish = "Traditional grammar expert from Berlin who focuses on proper form",
                personality = TutorPersonality.Strict,
                targetLanguageCode = "de",
                displayOrder = 0
            ),
            createTutor(
                name = "Anna",
                emoji = "😊",
                personaEnglish = "friendly tutor",
                domainEnglish = "everyday German, conversation, culture",
                descriptionEnglish = "Friendly tutor from Munich who keeps German learning relaxed and fun",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "de",
                displayOrder = 1
            )
        )

        val japaneseTutors = listOf(
            createTutor(
                name = "Yuki",
                emoji = "👩‍🏫",
                personaEnglish = "patient teacher",
                domainEnglish = "Japanese basics, hiragana, katakana, simple conversation",
                descriptionEnglish = "Patient teacher from Tokyo who specializes in helping beginners",
                personality = TutorPersonality.Encouraging,
                targetLanguageCode = "ja",
                displayOrder = 0
            ),
            createTutor(
                name = "Tanaka-sensei",
                emoji = "🎓",
                personaEnglish = "traditional teacher",
                domainEnglish = "Japanese grammar, kanji, formal language, etiquette",
                descriptionEnglish = "Traditional teacher focused on proper form and cultural etiquette",
                personality = TutorPersonality.Strict,
                targetLanguageCode = "ja",
                displayOrder = 1
            )
        )

        val allTutors = spanishTutors + frenchTutors + germanTutors + japaneseTutors
        tutorProfileRepository.saveAll(allTutors)

        return mapOf(
            "es" to spanishTutors,
            "fr" to frenchTutors,
            "de" to germanTutors,
            "ja" to japaneseTutors
        )
    }

    private fun createTutor(
        name: String,
        emoji: String,
        personaEnglish: String,
        domainEnglish: String,
        descriptionEnglish: String,
        personality: TutorPersonality,
        targetLanguageCode: String,
        displayOrder: Int
    ): TutorProfileEntity {
        return TutorProfileEntity(
            name = name,
            emoji = emoji,
            personaEnglish = personaEnglish,
            domainEnglish = domainEnglish,
            descriptionEnglish = descriptionEnglish,
            personaJson = """{"en": "$personaEnglish"}""",
            domainJson = """{"en": "$domainEnglish"}""",
            descriptionJson = """{"en": "$descriptionEnglish"}""",
            culturalBackgroundJson = null,
            personality = personality,
            targetLanguageCode = targetLanguageCode,
            isActive = true,
            displayOrder = displayOrder
        )
    }

    private fun seedCourses(tutorsByLanguage: Map<String, List<TutorProfileEntity>>): List<CourseTemplateEntity> {
        val courses = mutableListOf<CourseTemplateEntity>()

        // Spanish courses
        tutorsByLanguage["es"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "es",
                    nameEnglish = "Conversational Spanish",
                    shortDescriptionEnglish = "Master everyday conversations at your own pace",
                    descriptionEnglish = "Learn to communicate naturally in Spanish through practical conversations covering greetings, daily routines, opinions, and more",
                    category = CourseCategory.Conversational,
                    targetAudienceEnglish = "Anyone wanting to speak Spanish naturally",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.C1,
                    estimatedWeeks = null,  // Self-paced
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Greet people and introduce yourself",
                        "Talk about daily routines and activities",
                        "Describe people, places, and things",
                        "Order food and shop for items",
                        "Express opinions and feelings"
                    ),
                    displayOrder = 0
                )
            )
            courses.add(
                createCourse(
                    languageCode = "es",
                    nameEnglish = "Spanish for Travelers",
                    shortDescriptionEnglish = "Essential phrases for tourists and travelers",
                    descriptionEnglish = "Quick-start Spanish course focusing on practical phrases you'll need for travel",
                    category = CourseCategory.Travel,
                    targetAudienceEnglish = "Travelers planning a trip to Spanish-speaking countries",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.A2,
                    estimatedWeeks = 6,
                    suggestedTutorIds = tutors.take(2).map { it.id },
                    learningGoalsEnglish = listOf(
                        "Navigate airports and transportation",
                        "Check into hotels and accommodations",
                        "Order food at restaurants",
                        "Ask for directions and help",
                        "Handle emergencies"
                    ),
                    displayOrder = 1
                )
            )
        }

        // French courses
        tutorsByLanguage["fr"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "fr",
                    nameEnglish = "French Conversation",
                    shortDescriptionEnglish = "Develop fluency in everyday French",
                    descriptionEnglish = "Practice natural French conversation skills from basic to intermediate level",
                    category = CourseCategory.Conversational,
                    targetAudienceEnglish = "Learners wanting to speak French confidently",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.B2,
                    estimatedWeeks = null,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Engage in everyday conversations",
                        "Understand French pronunciation",
                        "Use common idiomatic expressions",
                        "Discuss various topics naturally"
                    ),
                    displayOrder = 0
                )
            )
        }

        // German courses
        tutorsByLanguage["de"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "de",
                    nameEnglish = "German Fundamentals",
                    shortDescriptionEnglish = "Build a strong foundation in German",
                    descriptionEnglish = "Comprehensive beginner to intermediate German course covering grammar and conversation",
                    category = CourseCategory.General,
                    targetAudienceEnglish = "New German learners wanting a structured approach",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.B1,
                    estimatedWeeks = 12,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Master German cases and grammar",
                        "Build essential vocabulary",
                        "Form correct sentences",
                        "Understand German culture"
                    ),
                    displayOrder = 0
                )
            )
        }

        // Japanese courses
        tutorsByLanguage["ja"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "ja",
                    nameEnglish = "Japanese for Beginners",
                    shortDescriptionEnglish = "Start your Japanese journey from zero",
                    descriptionEnglish = "Learn hiragana, katakana, and basic Japanese conversation step by step",
                    category = CourseCategory.General,
                    targetAudienceEnglish = "Complete beginners to Japanese",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.A2,
                    estimatedWeeks = 16,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Master hiragana and katakana",
                        "Learn basic kanji",
                        "Understand Japanese sentence structure",
                        "Practice polite conversation"
                    ),
                    displayOrder = 0
                )
            )
        }

        courseTemplateRepository.saveAll(courses)
        return courses
    }

    private fun createCourse(
        languageCode: String,
        nameEnglish: String,
        shortDescriptionEnglish: String,
        descriptionEnglish: String,
        category: CourseCategory,
        targetAudienceEnglish: String,
        startingLevel: CEFRLevel,
        targetLevel: CEFRLevel,
        estimatedWeeks: Int?,
        suggestedTutorIds: List<UUID>,
        learningGoalsEnglish: List<String>,
        displayOrder: Int
    ): CourseTemplateEntity {
        return CourseTemplateEntity(
            languageCode = languageCode,
            nameJson = """{"en": "$nameEnglish"}""",
            shortDescriptionJson = """{"en": "$shortDescriptionEnglish"}""",
            descriptionJson = """{"en": "$descriptionEnglish"}""",
            category = category,
            targetAudienceJson = """{"en": "$targetAudienceEnglish"}""",
            startingLevel = startingLevel,
            targetLevel = targetLevel,
            estimatedWeeks = estimatedWeeks,
            suggestedTutorIdsJson = objectMapper.writeValueAsString(suggestedTutorIds),
            defaultPhase = ConversationPhase.Auto,
            topicSequenceJson = null,
            learningGoalsJson = """{"en": ${objectMapper.writeValueAsString(learningGoalsEnglish)}}""",
            isActive = true,
            displayOrder = displayOrder,
            tagsJson = null
        )
    }
}
