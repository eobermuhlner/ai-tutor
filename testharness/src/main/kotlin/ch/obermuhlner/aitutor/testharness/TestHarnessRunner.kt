package ch.obermuhlner.aitutor.testharness

import ch.obermuhlner.aitutor.testharness.executor.TestExecutor
import ch.obermuhlner.aitutor.testharness.report.ReportGenerator
import ch.obermuhlner.aitutor.testharness.scenario.ScenarioLoader
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestHarnessRunner(
    private val scenarioLoader: ScenarioLoader,
    private val testExecutor: TestExecutor,
    private val reportGenerator: ReportGenerator
) : CommandLineRunner {
    
    override fun run(vararg args: String?) {
        println("AI Tutor Test Harness starting...")
        
        val scenariosPath = System.getProperty("scenarios.path") ?: "scenarios"
        val outputDir = System.getProperty("output.dir") ?: "."
        
        println("Loading scenarios from: $scenariosPath")
        
        val scenarios = scenarioLoader.loadScenariosFromDirectory(scenariosPath)
        println("Loaded ${scenarios.size} scenarios")
        
        val results = mutableListOf<ch.obermuhlner.aitutor.testharness.executor.ScenarioResult>()
        
        scenarios.forEach { scenario ->
            println("Executing scenario: ${scenario.name}")
            val result = testExecutor.executeScenario(scenario)
            results.add(result)
            
            if (result.success) {
                println("  ✅ PASSED - Rating: ${String.format("%.2f", result.overallRating)}")
            } else {
                println("  ❌ FAILED - ${result.message}")
            }
        }
        
        println("\nGenerating reports...")
        
        // Generate reports in the output directory
        reportGenerator.generateSummaryReport(results, "$outputDir/test_results_summary.md")
        reportGenerator.generateDetailedReport(results, "$outputDir/test_results_detailed.md")
        reportGenerator.generateCsvReport(results, "$outputDir/test_results.csv")
        
        // Print summary
        val successfulTests = results.count { it.success }
        val totalTests = results.size
        
        println("\n=== Test Execution Summary ===")
        println("Total tests: $totalTests")
        println("Passed: $successfulTests")
        println("Failed: ${totalTests - successfulTests}")
        println("Success rate: ${if (totalTests > 0) String.format("%.2f%%", successfulTests * 100.0 / totalTests) else "0.00%"}")
        
        if (totalTests > 0) {
            val averageRating = results.map { it.overallRating }.average()
            println("Average rating: ${String.format("%.2f", averageRating)}/10.0")
        }
        
        println("AI Tutor Test Harness completed.")
    }
}