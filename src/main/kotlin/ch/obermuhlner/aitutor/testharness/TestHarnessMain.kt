package ch.obermuhlner.aitutor.testharness

import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.executor.TestExecutor
import ch.obermuhlner.aitutor.testharness.report.ReportGenerator
import ch.obermuhlner.aitutor.testharness.scenario.ScenarioLoader
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Main entry point for the AI Tutor pedagogical test harness.
 *
 * Usage:
 *   ./gradlew runTestHarness --args="--scenario all"
 *   ./gradlew runTestHarness --args="--scenario beginner-errors"
 *   ./gradlew runTestHarness --args="--config custom-config.yml"
 */
object TestHarnessMain {
    private val logger = LoggerFactory.getLogger(TestHarnessMain::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("🚀 AI Tutor Pedagogical Test Harness Starting...")

        try {
            val config = parseArguments(args)
            // parseArguments returns null when handling --list or --help (exits internally)
            if (config != null) {
                runTestHarness(config)
            }
        } catch (e: Exception) {
            logger.error("❌ Test harness failed: ${e.message}", e)
            exitProcess(1)
        }
    }

    private fun parseArguments(args: Array<String>): TestHarnessConfig? {
        var scenarioFilter: String? = "all"
        var configPath: String? = null
        var listScenarios = false

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--scenario" -> {
                    scenarioFilter = args.getOrNull(i + 1)
                    i += 2
                }
                "--config" -> {
                    configPath = args.getOrNull(i + 1)
                    i += 2
                }
                "--list" -> {
                    listScenarios = true
                    i++
                }
                "--help" -> {
                    printHelp()
                    exitProcess(0)
                }
                else -> {
                    logger.warn("Unknown argument: ${args[i]}")
                    i++
                }
            }
        }

        val config = if (configPath != null) {
            TestHarnessConfig.loadFromFile(configPath)
        } else {
            TestHarnessConfig.loadDefault()
        }

        // Handle --list command
        if (listScenarios) {
            listAvailableScenarios(config)
            exitProcess(0)
        }

        return config.copy(scenarioFilter = scenarioFilter ?: "all")
    }

    private fun listAvailableScenarios(config: TestHarnessConfig) {
        println("\n📚 Available Test Scenarios\n")
        println("Scenarios directory: ${config.scenariosPath}")
        println()

        val scenarioLoader = ScenarioLoader(config.scenariosPath)
        val scenarios = scenarioLoader.loadScenarios("all")

        if (scenarios.isEmpty()) {
            println("⚠️  No scenarios found in ${config.scenariosPath}")
            println()
            println("To create scenarios, add YAML files to the scenarios/ directory.")
            println("See scenarios/README.md for documentation.")
            return
        }

        // Print table header
        println("┌─────────────────────────────────────┬────────┬──────────┬─────────────────────────┐")
        println("│ Scenario ID                         │ Level  │ Language │ Focus                   │")
        println("├─────────────────────────────────────┼────────┼──────────┼─────────────────────────┤")

        scenarios.forEach { scenario ->
            val id = scenario.id.take(35).padEnd(35)
            val level = scenario.learnerPersona.cefrLevel.name.padEnd(6)
            val lang = scenario.learnerPersona.targetLanguage.padEnd(8)
            val focus = scenario.evaluationFocus.firstOrNull()?.name?.take(23)?.padEnd(23) ?: "N/A".padEnd(23)

            println("│ $id │ $level │ $lang │ $focus │")
        }

        println("└─────────────────────────────────────┴────────┴──────────┴─────────────────────────┘")
        println()
        println("Total: ${scenarios.size} scenario(s)")
        println()
        println("Usage:")
        println("  ./gradlew runTestHarness                                    # Run all scenarios")
        println("  ./gradlew runTestHarness --args=\"--scenario SCENARIO_ID\"   # Run specific scenario")
        println("  ./gradlew runTestHarness --args=\"--scenario FILTER\"        # Run matching scenarios")
        println()
    }

    private fun runTestHarness(config: TestHarnessConfig) {
        logger.info("📋 Configuration: ${config.apiBaseUrl}, Scenario Filter: ${config.scenarioFilter}")

        // Load scenarios
        val scenarioLoader = ScenarioLoader(config.scenariosPath)
        val scenarios = scenarioLoader.loadScenarios(config.scenarioFilter)
        logger.info("📚 Loaded ${scenarios.size} test scenario(s)")

        if (scenarios.isEmpty()) {
            logger.warn("⚠️ No scenarios found matching filter: ${config.scenarioFilter}")
            return
        }

        // Execute tests
        val executor = TestExecutor(config)
        val results = executor.executeScenarios(scenarios)
        logger.info("✅ Executed ${results.size} scenario(s)")

        // Generate reports
        val reportGenerator = ReportGenerator(config)
        val reportPath = reportGenerator.generateReport(results)

        logger.info("📊 Report generated: $reportPath")
        logger.info("✨ Test harness completed successfully")

        // Exit with appropriate code
        val overallScore = results.map { it.overallScore }.average()
        val threshold = config.passThreshold

        if (overallScore >= threshold) {
            logger.info("✅ PASS: Overall score ${"%.2f".format(overallScore)} >= $threshold")
            exitProcess(0)
        } else {
            logger.error("❌ FAIL: Overall score ${"%.2f".format(overallScore)} < $threshold")
            exitProcess(1)
        }
    }

    private fun printHelp() {
        println("""
            AI Tutor Pedagogical Test Harness

            Usage:
              ./gradlew runTestHarness [OPTIONS]

            Options:
              --list               List all available test scenarios
              --scenario FILTER    Run specific scenario(s) (default: all)
                                   Examples: all, beginner-errors, phase
              --config PATH        Use custom configuration file
              --help               Show this help message

            Examples:
              ./gradlew runTestHarness --args="--list"
              ./gradlew runTestHarness
              ./gradlew runTestHarness --args="--scenario beginner-errors"
              ./gradlew runTestHarness --args="--config test-config.yml"
        """.trimIndent())
    }
}
