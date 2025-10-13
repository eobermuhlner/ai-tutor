package ch.obermuhlner.aitutor.testharness.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

/**
 * Configuration for the test harness application.
 */
data class TestHarnessConfig(
    val apiBaseUrl: String = "http://localhost:8080",
    val apiUsername: String = "demo",
    val apiPassword: String = "demo",
    val judgeModel: String = "gpt-4o",
    val judgeTemperature: Double = 0.2,
    val scenariosPath: String = "scenarios",
    val reportsOutputDir: String = "test-reports",
    val scenarioFilter: String = "all",
    val passThreshold: Double = 70.0,
    val maxConcurrentSessions: Int = 3,
    val requestTimeoutMs: Long = 30000
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TestHarnessConfig::class.java)
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

        /**
         * Load configuration from a YAML file.
         */
        fun loadFromFile(path: String): TestHarnessConfig {
            val file = File(path)
            if (!file.exists()) {
                logger.warn("Config file not found: $path, using defaults")
                return loadDefault()
            }

            return try {
                mapper.readValue(file, TestHarnessConfig::class.java)
            } catch (e: Exception) {
                logger.error("Failed to load config from $path: ${e.message}", e)
                loadDefault()
            }
        }

        /**
         * Load default configuration (looks for testharness-config.yml in current directory).
         */
        fun loadDefault(): TestHarnessConfig {
            val defaultFile = File("testharness-config.yml")
            return if (defaultFile.exists()) {
                logger.info("Loading default config from testharness-config.yml")
                loadFromFile(defaultFile.absolutePath)
            } else {
                logger.info("Using built-in default configuration")
                TestHarnessConfig()
            }
        }

        /**
         * Load configuration from environment variables (for CI/CD).
         */
        fun loadFromEnvironment(): TestHarnessConfig {
            return TestHarnessConfig(
                apiBaseUrl = System.getenv("TESTHARNESS_API_URL") ?: "http://localhost:8080",
                apiUsername = System.getenv("TESTHARNESS_API_USERNAME") ?: "demo",
                apiPassword = System.getenv("TESTHARNESS_API_PASSWORD") ?: "demo",
                judgeModel = System.getenv("TESTHARNESS_JUDGE_MODEL") ?: "gpt-4o",
                judgeTemperature = System.getenv("TESTHARNESS_JUDGE_TEMPERATURE")?.toDoubleOrNull() ?: 0.2,
                scenariosPath = System.getenv("TESTHARNESS_SCENARIOS_PATH") ?: "scenarios",
                reportsOutputDir = System.getenv("TESTHARNESS_REPORTS_DIR") ?: "test-reports",
                passThreshold = System.getenv("TESTHARNESS_PASS_THRESHOLD")?.toDoubleOrNull() ?: 70.0
            )
        }
    }
}
