package ch.obermuhlner.aitutor.testharness.report

import ch.obermuhlner.aitutor.testharness.executor.ScenarioResult
import org.springframework.stereotype.Service
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates reports for test execution results
 */
@Service
class ReportGenerator {
    
    /**
     * Generate a summary report of scenario results
     */
    fun generateSummaryReport(results: List<ScenarioResult>, outputFilePath: String = "test_results_summary.md") {
        val reportContent = buildString {
            appendLine("# AI Tutor Test Harness - Results Summary")
            appendLine()
            appendLine("**Generated:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
            appendLine()
            
            // Summary statistics
            val successfulTests = results.count { it.success }
            val totalTests = results.size
            val averageRating = if (results.isNotEmpty()) results.map { it.overallRating }.average() else 0.0
            val averagePedagogicalScore = if (results.isNotEmpty()) results.map { it.pedagogicalScore }.average() else 0.0
            val averageAccuracyScore = if (results.isNotEmpty()) results.map { it.accuracyScore }.average() else 0.0
            val averageEngagementScore = if (results.isNotEmpty()) results.map { it.engagementScore }.average() else 0.0
            
            appendLine("## Test Summary")
            appendLine("- Total Tests: $totalTests")
            appendLine("- Successful: $successfulTests")
            appendLine("- Failed: ${totalTests - successfulTests}")
            appendLine("- Success Rate: ${if (totalTests > 0) String.format("%.2f", successfulTests * 100.0 / totalTests) else "0.00"}%")
            appendLine()
            
            appendLine("## Average Scores")
            appendLine("- Overall Rating: ${String.format("%.2f", averageRating)}/10.0")
            appendLine("- Pedagogical: ${String.format("%.2f", averagePedagogicalScore)}/10.0")
            appendLine("- Accuracy: ${String.format("%.2f", averageAccuracyScore)}/10.0")
            appendLine("- Engagement: ${String.format("%.2f", averageEngagementScore)}/10.0")
            appendLine()
            
            // Detailed results
            appendLine("## Detailed Results")
            results.forEachIndexed { index, result ->
                appendLine("### Test ${index + 1}: ${result.scenarioId}")
                appendLine("**Status:** ${if (result.success) "✅ PASSED" else "❌ FAILED"}")
                appendLine("**Message:** ${result.message}")

                if (result.success) {
                    appendLine("**Overall Rating:** ${String.format("%.2f", result.overallRating)}/10.0")
                    appendLine("**Pedagogical Score:** ${String.format("%.2f", result.pedagogicalScore)}/10.0")
                    appendLine("**Accuracy Score:** ${String.format("%.2f", result.accuracyScore)}/10.0")
                    appendLine("**Engagement Score:** ${String.format("%.2f", result.engagementScore)}/10.0")
                }

                // Display validation results
                if (result.validationResults.isNotEmpty()) {
                    val passedValidations = result.validationResults.count { it.passed }
                    val totalValidations = result.validationResults.size
                    appendLine("**Validations:** $passedValidations/$totalValidations passed")
                }

                appendLine()
            }
        }
        
        FileWriter(outputFilePath).use { it.write(reportContent) }
        println("Summary report generated: $outputFilePath")
    }
    
    /**
     * Generate a detailed report including conversation history
     */
    fun generateDetailedReport(results: List<ScenarioResult>, outputFilePath: String = "test_results_detailed.md") {
        val reportContent = buildString {
            appendLine("# AI Tutor Test Harness - Detailed Results")
            appendLine()
            appendLine("**Generated:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
            appendLine()
            
            results.forEachIndexed { index, result ->
                appendLine("## Test ${index + 1}: ${result.scenarioId}")
                appendLine("**Status:** ${if (result.success) "✅ PASSED" else "❌ FAILED"}")
                appendLine("**Message:** ${result.message}")
                
                if (result.success) {
                    appendLine("**Overall Rating:** ${String.format("%.2f", result.overallRating)}/10.0")
                    appendLine("**Pedagogical Score:** ${String.format("%.2f", result.pedagogicalScore)}/10.0")
                    appendLine("**Accuracy Score:** ${String.format("%.2f", result.accuracyScore)}/10.0")
                    appendLine("**Engagement Score:** ${String.format("%.2f", result.engagementScore)}/10.0")
                    
                    appendLine()
                    appendLine("### Conversation History:")
                    result.conversationHistory.forEach { message ->
                        appendLine("- $message")
                    }
                    
                    appendLine()
                    appendLine("### Validation Results:")
                    if (result.validationResults.isNotEmpty()) {
                        result.validationResults.groupBy { it.category }.forEach { (category, validations) ->
                            appendLine("#### $category:")
                            validations.forEach { validation ->
                                val status = if (validation.passed) "✅" else "❌"
                                appendLine("- $status ${validation.message}")
                            }
                        }
                    } else {
                        appendLine("No validations configured for this scenario.")
                    }

                    appendLine()
                    appendLine("### Evaluation Results:")
                    result.evaluationResults.forEachIndexed { evalIndex, eval ->
                        appendLine("#### Evaluation ${evalIndex + 1}:")
                        appendLine("- Rating: ${eval.rating}/10")
                        appendLine("- Feedback: ${eval.feedback}")
                        if (eval.strengths.isNotEmpty()) {
                            appendLine("- Strengths: ${eval.strengths.joinToString(", ")}")
                        }
                        if (eval.improvements.isNotEmpty()) {
                            appendLine("- Improvements: ${eval.improvements.joinToString(", ")}")
                        }
                        appendLine("- Pedagogical Score: ${eval.pedagogicalScore}/10")
                        appendLine("- Accuracy Score: ${eval.accuracyScore}/10")
                        appendLine("- Engagement Score: ${eval.engagementScore}/10")
                        appendLine()
                    }
                }
                
                appendLine("---")
                appendLine()
            }
        }
        
        FileWriter(outputFilePath).use { it.write(reportContent) }
        println("Detailed report generated: $outputFilePath")
    }
    
    /**
     * Generate a CSV report for data analysis
     */
    fun generateCsvReport(results: List<ScenarioResult>, outputFilePath: String = "test_results.csv") {
        val csvContent = buildString {
            // Header
            appendLine("scenario_id,success,message,overall_rating,pedagogical_score,accuracy_score,engagement_score")
            
            // Data rows
            results.forEach { result ->
                appendLine("\"${result.scenarioId}\",${result.success},\"${result.message.replace("\"", "\"\"")}\"," +
                        "${result.overallRating},${result.pedagogicalScore},${result.accuracyScore},${result.engagementScore}")
            }
        }
        
        FileWriter(outputFilePath).use { it.write(csvContent) }
        println("CSV report generated: $outputFilePath")
    }
}