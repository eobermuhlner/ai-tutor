package ch.obermuhlner.aitutor.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import java.util.*

@Serializable
data class CliConfig(
    val apiUrl: String = "http://localhost:8080",
    // Authentication
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiresAt: Long? = null, // Timestamp in milliseconds
    // Optional credentials for auto-login (WARNING: storing passwords is insecure, for dev convenience only)
    val username: String? = null,
    val password: String? = null,
    // Session preferences
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

    fun getLastSessionIdAsUUID(): UUID? = lastSessionId?.let { UUID.fromString(it) }

    fun isTokenValid(): Boolean {
        val expiresAt = tokenExpiresAt ?: return false
        // Consider token invalid if it expires within 5 minutes
        return System.currentTimeMillis() + (5 * 60 * 1000) < expiresAt
    }

    fun isTokenExpired(): Boolean {
        val expiresAt = tokenExpiresAt ?: return true
        return System.currentTimeMillis() >= expiresAt
    }
}
