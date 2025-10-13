package ch.obermuhlner.aitutor.testharness.report

import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.ScenarioResult
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates test reports in markdown and HTML formats.
 */
class ReportGenerator(private val config: TestHarnessConfig) {
    private val logger = LoggerFactory.getLogger(ReportGenerator::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Generate comprehensive report from scenario results.
     *
     * @return Path to generated report file
     */
    fun generateReport(results: List<ScenarioResult>): String {
        logger.info("📊 Generating report for ${results.size} scenario(s)...")

        val reportDir = File(config.reportsOutputDir)
        reportDir.mkdirs()

        val timestamp = Instant.now().toString().replace(":", "-")
        val reportFile = File(reportDir, "test-report-$timestamp.md")

        val markdown = buildMarkdownReport(results)
        reportFile.writeText(markdown)

        logger.info("✅ Report saved to: ${reportFile.absolutePath}")
        return reportFile.absolutePath
    }

    private fun buildMarkdownReport(results: List<ScenarioResult>): String {
        val overallScore = results.map { it.overallScore }.average()
        val timestamp = dateFormatter.format(Instant.now())

        return buildString {
            appendLine("# AI Tutor Pedagogical Test Report")
            appendLine()
            appendLine("**Generated:** $timestamp")
            appendLine()
            appendLine("**Overall Score:** ${"%.1f".format(overallScore)}/100 ${getScoreEmoji(overallScore)}")
            appendLine()

            // Summary table
            appendLine("## Summary")
            appendLine()
            appendLine("| Scenario | Overall Score | Error Detection | Phase | Correction | Encouragement | Topic | Vocabulary |")
            appendLine("|----------|--------------|----------------|-------|------------|---------------|-------|------------|")

            results.forEach { result ->
                val e = result.judgeEvaluation
                appendLine("| ${result.scenarioName} | " +
                        "${"%.1f".format(result.overallScore)} | " +
                        "${"%.1f".format(e.errorDetectionScore)} | " +
                        "${"%.1f".format(e.phaseAppropriatenessScore)} | " +
                        "${"%.1f".format(e.correctionQualityScore)} | " +
                        "${"%.1f".format(e.encouragementBalanceScore)} | " +
                        "${"%.1f".format(e.topicManagementScore)} | " +
                        "${"%.1f".format(e.vocabularyTeachingScore)} |")
            }

            appendLine()

            // Technical metrics summary
            appendLine("## Technical Metrics")
            appendLine()
            appendLine("| Scenario | Messages | Corrections | Detected | Missed | False+ | Phases | Topics | Vocab |")
            appendLine("|----------|----------|-------------|----------|--------|--------|--------|--------|-------|")

            results.forEach { result ->
                val m = result.technicalMetrics
                appendLine("| ${result.scenarioName} | " +
                        "${m.totalMessages} | " +
                        "${m.totalCorrections} | " +
                        "${m.correctionsDetected} | " +
                        "${m.correctionsMissed} | " +
                        "${m.falsePositives} | " +
                        "${m.phaseTransitions.size} | " +
                        "${m.topicChanges} | " +
                        "${m.vocabularyItemsIntroduced} |")
            }

            appendLine()

            // Detailed results for each scenario
            appendLine("## Detailed Results")
            appendLine()

            results.forEach { result ->
                appendLine("### ${result.scenarioName}")
                appendLine()
                appendLine("**Scenario ID:** `${result.scenarioId}`")
                appendLine("**Session ID:** `${result.sessionId}`")
                appendLine("**Execution Time:** ${dateFormatter.format(result.executionTime)}")
                appendLine("**Overall Score:** ${"%.1f".format(result.overallScore)}/100")
                appendLine()

                // Judge evaluation
                appendLine("#### Judge Evaluation")
                appendLine()
                appendLine("**Overall Feedback:**")
                appendLine(result.judgeEvaluation.overallFeedback)
                appendLine()

                appendLine("**Strengths:**")
                result.judgeEvaluation.strengths.forEach { strength ->
                    appendLine("- $strength")
                }
                appendLine()

                appendLine("**Improvements:**")
                result.judgeEvaluation.improvements.forEach { improvement ->
                    appendLine("- $improvement")
                }
                appendLine()

                // Dimension details
                appendLine("**Error Detection (${"%.1f".format(result.judgeEvaluation.errorDetectionScore)}/100):**")
                appendLine(result.judgeEvaluation.errorDetectionFeedback)
                appendLine()

                appendLine("**Phase Appropriateness (${"%.1f".format(result.judgeEvaluation.phaseAppropriatenessScore)}/100):**")
                appendLine(result.judgeEvaluation.phaseAppropriatenessFeedback)
                appendLine()

                appendLine("**Correction Quality (${"%.1f".format(result.judgeEvaluation.correctionQualityScore)}/100):**")
                appendLine(result.judgeEvaluation.correctionQualityFeedback)
                appendLine()

                appendLine("**Encouragement Balance (${"%.1f".format(result.judgeEvaluation.encouragementBalanceScore)}/100):**")
                appendLine(result.judgeEvaluation.encouragementBalanceFeedback)
                appendLine()

                appendLine("**Topic Management (${"%.1f".format(result.judgeEvaluation.topicManagementScore)}/100):**")
                appendLine(result.judgeEvaluation.topicManagementFeedback)
                appendLine()

                appendLine("**Vocabulary Teaching (${"%.1f".format(result.judgeEvaluation.vocabularyTeachingScore)}/100):**")
                appendLine(result.judgeEvaluation.vocabularyTeachingFeedback)
                appendLine()

                // Technical metrics
                appendLine("#### Technical Metrics")
                appendLine()
                val m = result.technicalMetrics
                appendLine("- **Total messages:** ${m.totalMessages}")
                appendLine("- **Corrections detected:** ${m.correctionsDetected}/${m.totalCorrections}")
                appendLine("- **Corrections missed:** ${m.correctionsMissed}")
                appendLine("- **False positives:** ${m.falsePositives}")
                appendLine("- **Phase transitions:** ${m.phaseTransitions.size}")
                m.phaseTransitions.forEach { transition ->
                    appendLine("  - Turn ${transition.atTurnIndex}: ${transition.fromPhase} → ${transition.toPhase} ${if (transition.wasExpected) "✓" else "✗"}")
                }
                appendLine("- **Topic changes:** ${m.topicChanges}")
                appendLine("- **Vocabulary introduced:** ${m.vocabularyItemsIntroduced}")
                appendLine()

                // Conversation transcript
                appendLine("#### Conversation Transcript")
                appendLine()
                result.conversationTranscript.forEach { turn ->
                    appendLine("**Turn ${turn.turnIndex}:**")
                    appendLine()
                    appendLine("**Learner:** ${turn.learnerMessage}")
                    if (turn.intentionalErrors.isNotEmpty()) {
                        appendLine()
                        appendLine("*[Intentional errors: ${turn.intentionalErrors.joinToString(", ") { it.span }}]*")
                    }
                    appendLine()
                    appendLine("**Tutor:** ${turn.tutorResponse.content}")
                    appendLine()
                    appendLine("- **Phase:** ${turn.tutorResponse.currentPhase}")
                    if (turn.tutorResponse.currentTopic != null) {
                        appendLine("- **Topic:** ${turn.tutorResponse.currentTopic}")
                    }
                    if (turn.tutorResponse.corrections.isNotEmpty()) {
                        appendLine("- **Corrections:**")
                        turn.tutorResponse.corrections.forEach { correction ->
                            appendLine("  - `${correction.span}` → `${correction.correctedForm}` (${correction.errorType}, ${correction.severity})")
                        }
                    }
                    if (turn.tutorResponse.newVocabulary.isNotEmpty()) {
                        appendLine("- **New vocabulary:** ${turn.tutorResponse.newVocabulary.joinToString(", ")}")
                    }
                    appendLine()
                }

                appendLine("---")
                appendLine()
            }

            // Overall recommendations
            appendLine("## Overall Recommendations")
            appendLine()

            val allImprovements = results.flatMap { it.judgeEvaluation.improvements }.distinct()
            if (allImprovements.isNotEmpty()) {
                appendLine("**Priority Improvements:**")
                allImprovements.take(5).forEach { improvement ->
                    appendLine("- $improvement")
                }
            } else {
                appendLine("No specific improvements identified. Great work!")
            }

            appendLine()
            appendLine("---")
            appendLine()
            appendLine("*Report generated by AI Tutor Pedagogical Test Harness*")
        }
    }

    private fun getScoreEmoji(score: Double): String {
        return when {
            score >= 90 -> "🌟"
            score >= 80 -> "✅"
            score >= 70 -> "👍"
            score >= 60 -> "⚠️"
            else -> "❌"
        }
    }
}
