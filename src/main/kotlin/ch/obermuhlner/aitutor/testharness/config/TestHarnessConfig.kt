package ch.obermuhlner.aitutor.testharness.config

import ch.obermuhlner.aitutor.testharness.ai.AiProviderConfig
import ch.obermuhlner.aitutor.testharness.ai.AiProviderType
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
    val judgeProvider: String = "openai",
    val judgeApiKey: String? = null,
    val judgeApiEndpoint: String? = null,
    val judgeDeploymentName: String? = null,
    val scenariosPath: String = "scenarios",
    val reportsOutputDir: String = "test-reports",
    val scenarioFilter: String = "all",
    val passThreshold: Double = 70.0,
    val maxConcurrentSessions: Int = 3,
    val requestTimeoutMs: Long = 30000,
    val delayBetweenRequestsMs: Long = 1000,
    val maxRetries: Int = 3,
    val retryBackoffMultiplier: Double = 2.0
) {
    /**
     * Get AI provider configuration from test harness config.
     */
    fun getAiProviderConfig(): AiProviderConfig {
        val type = when (judgeProvider.lowercase()) {
            "openai" -> AiProviderType.OPENAI
            "azure-openai", "azure_openai", "azure" -> AiProviderType.AZURE_OPENAI
            "ollama" -> AiProviderType.OLLAMA
            else -> throw IllegalArgumentException("Unknown AI provider: $judgeProvider (supported: openai, azure-openai, ollama)")
        }

        return AiProviderConfig(
            type = type,
            apiKey = judgeApiKey,
            apiEndpoint = judgeApiEndpoint,
            deploymentName = judgeDeploymentName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TestHarnessConfig::class.java)
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

        /**
         * Load configuration from a YAML file.
         * Returns built-in defaults if the specified file doesn't exist.
         */
        fun loadFromFile(path: String): TestHarnessConfig {
            val file = File(path)
            if (!file.exists()) {
                logger.warn("Config file not found: $path, using built-in defaults")
                return TestHarnessConfig()
            }

            return try {
                mapper.readValue(file, TestHarnessConfig::class.java)
            } catch (e: Exception) {
                logger.error("Failed to load config from $path: ${e.message}", e)
                TestHarnessConfig()
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
                judgeProvider = System.getenv("TESTHARNESS_JUDGE_PROVIDER") ?: "openai",
                judgeApiKey = System.getenv("TESTHARNESS_JUDGE_API_KEY"),
                judgeApiEndpoint = System.getenv("TESTHARNESS_JUDGE_API_ENDPOINT"),
                judgeDeploymentName = System.getenv("TESTHARNESS_JUDGE_DEPLOYMENT_NAME"),
                scenariosPath = System.getenv("TESTHARNESS_SCENARIOS_PATH") ?: "scenarios",
                reportsOutputDir = System.getenv("TESTHARNESS_REPORTS_DIR") ?: "test-reports",
                passThreshold = System.getenv("TESTHARNESS_PASS_THRESHOLD")?.toDoubleOrNull() ?: 70.0,
                delayBetweenRequestsMs = System.getenv("TESTHARNESS_DELAY_BETWEEN_REQUESTS_MS")?.toLongOrNull() ?: 1000,
                maxRetries = System.getenv("TESTHARNESS_MAX_RETRIES")?.toIntOrNull() ?: 3,
                retryBackoffMultiplier = System.getenv("TESTHARNESS_RETRY_BACKOFF_MULTIPLIER")?.toDoubleOrNull() ?: 2.0
            )
        }
    }
}
