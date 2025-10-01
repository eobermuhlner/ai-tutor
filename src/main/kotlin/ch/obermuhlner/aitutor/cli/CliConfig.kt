package ch.obermuhlner.aitutor.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import java.util.*

@Serializable
data class CliConfig(
    val apiUrl: String = "http://localhost:8080",
    val userId: String = "00000000-0000-0000-0000-000000000001",
    val defaultTutor: String = "Maria",
    val defaultTutorPersona: String = "patient coach",
    val defaultTutorDomain: String = "general conversation, grammar, typography",
    val sourceLanguage: String = "en",
    val targetLanguage: String = "es",
    val defaultPhase: String = "Auto",
    val defaultCEFRLevel: String = "A1",
    val lastSessionId: String? = null
) {
    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        private val configDir = Paths.get(System.getProperty("user.home"), ".aitutor-cli").toFile()
        private val configFile = File(configDir, "config.json")

        fun load(): CliConfig {
            return if (configFile.exists()) {
                try {
                    json.decodeFromString<CliConfig>(configFile.readText())
                } catch (e: Exception) {
                    println("Warning: Could not load config file, using defaults: ${e.message}")
                    CliConfig()
                }
            } else {
                CliConfig()
            }
        }

        fun save(config: CliConfig) {
            try {
                if (!configDir.exists()) {
                    configDir.mkdirs()
                }
                configFile.writeText(json.encodeToString(serializer(), config))
            } catch (e: Exception) {
                println("Warning: Could not save config file: ${e.message}")
            }
        }
    }

    fun getUserIdAsUUID(): UUID = UUID.fromString(userId)

    fun getLastSessionIdAsUUID(): UUID? = lastSessionId?.let { UUID.fromString(it) }
}
