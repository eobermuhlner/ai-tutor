package ch.obermuhlner.aitutor.testharness.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class TestHarnessConfigTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should load configuration from YAML file`() {
        // Given
        val configContent = """
            apiBaseUrl: http://test.example.com:8080
            apiUsername: testuser
            apiPassword: testpass
            judgeModel: gpt-4o-mini
            judgeTemperature: 0.3
            scenariosPath: test-scenarios
            reportsOutputDir: test-reports
            passThreshold: 75.0
            maxConcurrentSessions: 5
            requestTimeoutMs: 60000
        """.trimIndent()

        val configFile = File(tempDir, "test-config.yml")
        configFile.writeText(configContent)

        // When
        val config = TestHarnessConfig.loadFromFile(configFile.absolutePath)

        // Then
        assertEquals("http://test.example.com:8080", config.apiBaseUrl)
        assertEquals("testuser", config.apiUsername)
        assertEquals("testpass", config.apiPassword)
        assertEquals("gpt-4o-mini", config.judgeModel)
        assertEquals(0.3, config.judgeTemperature)
        assertEquals("test-scenarios", config.scenariosPath)
        assertEquals("test-reports", config.reportsOutputDir)
        assertEquals(75.0, config.passThreshold)
        assertEquals(5, config.maxConcurrentSessions)
        assertEquals(60000L, config.requestTimeoutMs)
    }

    @Test
    fun `should use defaults for non-existent config file`() {
        // When
        val config = TestHarnessConfig.loadFromFile("non-existent-file.yml")

        // Then
        assertEquals("http://localhost:8080", config.apiBaseUrl)
        assertEquals("demo", config.apiUsername)
        assertEquals("demo", config.apiPassword)
        assertEquals("gpt-4o", config.judgeModel)
        assertEquals(0.2, config.judgeTemperature)
        assertEquals("scenarios", config.scenariosPath)
        assertEquals("test-reports", config.reportsOutputDir)
        assertEquals("all", config.scenarioFilter)
        assertEquals(70.0, config.passThreshold)
    }

    @Test
    fun `should load default configuration with all defaults`() {
        // When
        val config = TestHarnessConfig()

        // Then
        assertEquals("http://localhost:8080", config.apiBaseUrl)
        assertEquals("demo", config.apiUsername)
        assertEquals("gpt-4o", config.judgeModel)
        assertEquals(0.2, config.judgeTemperature)
        assertEquals(70.0, config.passThreshold)
        assertEquals(3, config.maxConcurrentSessions)
    }

    @Test
    fun `should support copy with modifications`() {
        // Given
        val config = TestHarnessConfig()

        // When
        val modified = config.copy(
            scenarioFilter = "beginner-errors",
            passThreshold = 80.0
        )

        // Then
        assertEquals("beginner-errors", modified.scenarioFilter)
        assertEquals(80.0, modified.passThreshold)
        assertEquals("http://localhost:8080", modified.apiBaseUrl) // Unchanged
    }
}
