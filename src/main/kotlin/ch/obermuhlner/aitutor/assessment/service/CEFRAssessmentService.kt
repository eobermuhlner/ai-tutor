package ch.obermuhlner.aitutor.assessment.service

import ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

@Service
class CEFRAssessmentService(
    private val errorAnalyticsService: ErrorAnalyticsService,
    private val vocabularyQueryService: VocabularyQueryService,
    private val chatMessageRepository: ChatMessageRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class SkillAssessment(
        val grammar: CEFRLevel,
        val vocabulary: CEFRLevel,
        val fluency: CEFRLevel,
        val comprehension: CEFRLevel,
        val overall: CEFRLevel
    )

    /**
     * Assess skill levels using heuristics (fast, no LLM).
     */
    fun assessWithHeuristics(session: ChatSessionEntity): SkillAssessment {
        val userId = session.userId
        val lang = session.targetLanguageCode

        // Grammar: Based on error patterns (Task 0009)
        val grammarLevel = assessGrammarLevel(userId, lang)

        // Vocabulary: Based on vocabulary count (Task 0008)
        val vocabularyLevel = assessVocabularyLevel(userId, lang)

        // Fluency: Based on message length and complexity
        val fluencyLevel = assessFluencyLevel(session.id)

        // Comprehension: Inferred from tutor complexity vs performance
        val comprehensionLevel = assessComprehensionLevel(session)

        // Overall: Weighted average
        val overallLevel = computeOverallLevel(
            grammarLevel, vocabularyLevel, fluencyLevel, comprehensionLevel
        )

        logger.debug("Assessment for session ${session.id}: grammar=$grammarLevel, vocab=$vocabularyLevel, fluency=$fluencyLevel, comprehension=$comprehensionLevel, overall=$overallLevel")

        return SkillAssessment(
            grammar = grammarLevel,
            vocabulary = vocabularyLevel,
            fluency = fluencyLevel,
            comprehension = comprehensionLevel,
            overall = overallLevel
        )
    }

    private fun assessGrammarLevel(userId: UUID, lang: String): CEFRLevel {
        // Get top error patterns from Task 0009
        val patterns = try {
            errorAnalyticsService.getTopPatterns(userId, lang, limit = 10)
        } catch (e: Exception) {
            logger.warn("Failed to get error patterns for grammar assessment", e)
            emptyList()
        }

        if (patterns.isEmpty()) {
            // No error data yet, use conservative default
            return CEFRLevel.A1
        }

        // Compute weighted error score (Critical=3.0, High=2.0, Medium=1.0, Low=0.3)
        val totalScore = patterns.sumOf { it.computeWeightedScore() }

        // Map error score to CEFR level (lower score = higher level)
        return when {
            totalScore < 1.0 -> CEFRLevel.C1   // Near-native accuracy
            totalScore < 3.0 -> CEFRLevel.B2   // Advanced grammar, few errors
            totalScore < 6.0 -> CEFRLevel.B1   // Intermediate, moderate errors
            totalScore < 10.0 -> CEFRLevel.A2  // Basic grammar, frequent errors
            else -> CEFRLevel.A1               // Beginner, many errors
        }
    }

    private fun assessVocabularyLevel(userId: UUID, lang: String): CEFRLevel {
        // Get active vocabulary count from Task 0008
        val vocabCount = try {
            vocabularyQueryService.getUserVocabulary(userId, lang).size
        } catch (e: Exception) {
            logger.warn("Failed to get vocabulary count for assessment", e)
            0
        }

        // Map count to CEFR level (CEFR guidelines: A1=300, A2=600, B1=1200, B2=2500, C1=5000+)
        return when {
            vocabCount >= 5000 -> CEFRLevel.C2  // Near-native vocabulary range
            vocabCount >= 2500 -> CEFRLevel.C1  // Advanced vocabulary
            vocabCount >= 1200 -> CEFRLevel.B2  // Upper-intermediate
            vocabCount >= 600 -> CEFRLevel.B1   // Intermediate
            vocabCount >= 300 -> CEFRLevel.A2   // Elementary
            else -> CEFRLevel.A1                // Beginner
        }
    }

    private fun assessFluencyLevel(sessionId: UUID): CEFRLevel {
        // Get user messages for this session
        val messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        val userMessages = messages.filter { it.role == MessageRole.USER }

        if (userMessages.size < 10) {
            return CEFRLevel.A1  // Insufficient data
        }

        // Compute average message length (words)
        val avgLength = userMessages
            .map { it.content.split("\\s+".toRegex()).filter { word -> word.isNotBlank() }.size }
            .average()

        // Map message length to CEFR level (CEFR: A1=5-8 words, B1=10-15, C1=20+)
        return when {
            avgLength >= 25.0 -> CEFRLevel.C2  // Very sophisticated expression
            avgLength >= 20.0 -> CEFRLevel.C1  // Sophisticated expression
            avgLength >= 15.0 -> CEFRLevel.B2  // Extended discourse
            avgLength >= 10.0 -> CEFRLevel.B1  // Connected sentences
            avgLength >= 7.0 -> CEFRLevel.A2   // Simple sentences
            else -> CEFRLevel.A1               // Basic phrases
        }
    }

    private fun assessComprehensionLevel(session: ChatSessionEntity): CEFRLevel {
        // Comprehension is hard to infer from production data
        // For now, use a conservative estimate based on grammar level
        // Future: Add explicit comprehension tests

        // Conservative: Assume comprehension slightly below grammar level
        val grammarLevel = session.cefrGrammar ?: CEFRLevel.A1
        val ordinal = grammarLevel.ordinal

        // Don't go below A1 (ordinal 0)
        return CEFRLevel.values()[maxOf(0, ordinal - 1)]
    }

    private fun computeOverallLevel(
        grammar: CEFRLevel,
        vocabulary: CEFRLevel,
        fluency: CEFRLevel,
        comprehension: CEFRLevel
    ): CEFRLevel {
        // Weighted average: grammar 40%, vocabulary 30%, fluency 20%, comprehension 10%
        val levelValues = mapOf(
            CEFRLevel.A1 to 1, CEFRLevel.A2 to 2,
            CEFRLevel.B1 to 3, CEFRLevel.B2 to 4,
            CEFRLevel.C1 to 5, CEFRLevel.C2 to 6
        )

        val weightedSum = (levelValues[grammar]!! * 0.4) +
                          (levelValues[vocabulary]!! * 0.3) +
                          (levelValues[fluency]!! * 0.2) +
                          (levelValues[comprehension]!! * 0.1)

        val roundedLevel = weightedSum.roundToInt().coerceIn(1, 6)
        return levelValues.entries.first { it.value == roundedLevel }.key
    }

    /**
     * Update session skill levels if assessment has changed.
     * Returns true if any level changed.
     */
    fun updateSkillLevelsIfChanged(session: ChatSessionEntity, assessment: SkillAssessment): Boolean {
        var changed = false

        if (session.cefrGrammar != assessment.grammar) {
            logger.info("Grammar level changed: ${session.cefrGrammar} → ${assessment.grammar} for session ${session.id}")
            session.cefrGrammar = assessment.grammar
            changed = true
        }

        if (session.cefrVocabulary != assessment.vocabulary) {
            logger.info("Vocabulary level changed: ${session.cefrVocabulary} → ${assessment.vocabulary} for session ${session.id}")
            session.cefrVocabulary = assessment.vocabulary
            changed = true
        }

        if (session.cefrFluency != assessment.fluency) {
            logger.info("Fluency level changed: ${session.cefrFluency} → ${assessment.fluency} for session ${session.id}")
            session.cefrFluency = assessment.fluency
            changed = true
        }

        if (session.cefrComprehension != assessment.comprehension) {
            logger.info("Comprehension level changed: ${session.cefrComprehension} → ${assessment.comprehension} for session ${session.id}")
            session.cefrComprehension = assessment.comprehension
            changed = true
        }

        if (session.estimatedCEFRLevel != assessment.overall) {
            logger.info("Overall CEFR level changed: ${session.estimatedCEFRLevel} → ${assessment.overall} for session ${session.id}")
            session.estimatedCEFRLevel = assessment.overall
            changed = true
        }

        if (changed) {
            session.lastAssessmentAt = Instant.now()
            session.totalAssessmentCount += 1
        }

        return changed
    }
}
