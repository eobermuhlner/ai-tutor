package ch.obermuhlner.aitutor.cli

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class AiTutorCli(private val config: CliConfig) {
    private val apiClient = HttpApiClient(config.apiUrl)
    private var currentSessionId: UUID? = config.getLastSessionIdAsUUID()
    private var currentConfig = config
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val config = CliConfig.load()
            val cli = AiTutorCli(config)
            cli.run()
        }
    }

    fun run() {
        printWelcome()

        // Try to resume last session or create new one
        if (currentSessionId == null) {
            println("No active session found. Creating new session...")
            createNewSession()
        } else {
            println("Resuming session: $currentSessionId")
            // Verify session still exists
            try {
                apiClient.getUserSessions(currentConfig.getUserIdAsUUID())
                    .find { it.id == currentSessionId.toString() }
                    ?: run {
                        println("Session no longer exists. Creating new session...")
                        createNewSession()
                    }
            } catch (e: Exception) {
                println("Could not verify session. Creating new session...")
                createNewSession()
            }
        }

        println()
        printHelp()
        println()

        // Main interaction loop
        while (true) {
            print("You: ")
            val input = readMultiLineInput() ?: break // Ctrl-D exits

            if (input.isBlank()) {
                continue
            }

            when {
                input.startsWith("/") -> {
                    if (!handleCommand(input.trim())) {
                        break
                    }
                }
                currentSessionId == null -> {
                    println("No active session. Use /new to create one.")
                }
                else -> {
                    sendMessage(input)
                }
            }
        }

        println("\nGoodbye!")
    }

    private fun printWelcome() {
        println("=" .repeat(60))
        println("AI Tutor CLI - Language Learning Assistant")
        println("=" .repeat(60))
        println("Learning: ${config.sourceLanguage} → ${config.targetLanguage}")
        println("Tutor: ${config.defaultTutor}")
        println("=" .repeat(60))
    }

    private fun printHelp() {
        println("""
            Commands:
              /new                  Create new session
              /sessions             List all sessions
              /switch <id>          Switch to session
              /phase <phase>        Change phase (Free/Correction/Drill/Auto)
              /delete               Delete current session
              /help                 Show this help
              /quit or /exit        Exit CLI

            Multi-line input:
              - Press Enter on empty line to send message
              - Or type //send on a new line to send immediately
              - Ctrl-D exits the application
        """.trimIndent())
    }

    private fun readMultiLineInput(): String? {
        val lines = mutableListOf<String>()

        while (true) {
            val line = reader.readLine() ?: return null // EOF (Ctrl-D)

            // Check for //send command
            if (line.trim() == "//send") {
                break
            }

            // Check for single empty line (Enter on empty line sends)
            if (line.isEmpty()) {
                break
            }

            lines.add(line)

            // Show continuation prompt for multi-line
            print("... ")
        }

        return lines.joinToString("\n").trim()
    }

    private fun handleCommand(input: String): Boolean {
        val parts = input.split(" ", limit = 2)
        val command = parts[0].lowercase()
        val arg = parts.getOrNull(1)

        return when (command) {
            "/quit", "/exit" -> false
            "/help" -> {
                printHelp()
                true
            }
            "/new" -> {
                createNewSession()
                true
            }
            "/sessions" -> {
                listSessions()
                true
            }
            "/switch" -> {
                if (arg != null) {
                    switchSession(arg)
                } else {
                    println("Usage: /switch <session-id>")
                }
                true
            }
            "/phase" -> {
                if (arg != null) {
                    updatePhase(arg)
                } else {
                    println("Usage: /phase <Free|Correction|Drill|Auto>")
                }
                true
            }
            "/delete" -> {
                deleteCurrentSession()
                true
            }
            else -> {
                println("Unknown command: $command. Type /help for available commands.")
                true
            }
        }
    }

    private fun createNewSession() {
        try {
            val session = apiClient.createSession(
                userId = currentConfig.getUserIdAsUUID(),
                tutorName = currentConfig.defaultTutor,
                tutorPersona = currentConfig.defaultTutorPersona,
                tutorDomain = currentConfig.defaultTutorDomain,
                sourceLanguage = currentConfig.sourceLanguage,
                targetLanguage = currentConfig.targetLanguage,
                phase = currentConfig.defaultPhase,
                cefrLevel = currentConfig.defaultCEFRLevel
            )

            currentSessionId = UUID.fromString(session.id)
            currentConfig = currentConfig.copy(lastSessionId = session.id)
            CliConfig.save(currentConfig)

            println("✓ Created new session: ${session.id}")
            println("  Tutor: ${session.tutorName}")
            println("  Phase: ${session.conversationPhase}")
            println("  Languages: ${session.sourceLanguageCode} → ${session.targetLanguageCode}")
        } catch (e: Exception) {
            println("✗ Failed to create session: ${e.message}")
        }
    }

    private fun listSessions() {
        try {
            val sessions = apiClient.getUserSessions(currentConfig.getUserIdAsUUID())

            if (sessions.isEmpty()) {
                println("No sessions found.")
                return
            }

            println("\nYour sessions:")
            sessions.forEach { session ->
                val current = if (session.id == currentSessionId.toString()) " (current)" else ""
                println("  ${session.id}$current")
                println("    Tutor: ${session.tutorName}, Phase: ${session.conversationPhase}")
            }
        } catch (e: Exception) {
            println("✗ Failed to list sessions: ${e.message}")
        }
    }

    private fun switchSession(sessionId: String) {
        try {
            val uuid = UUID.fromString(sessionId)
            currentSessionId = uuid
            currentConfig = currentConfig.copy(lastSessionId = sessionId)
            CliConfig.save(currentConfig)

            println("✓ Switched to session: $sessionId")
        } catch (e: IllegalArgumentException) {
            println("✗ Invalid session ID format")
        } catch (e: Exception) {
            println("✗ Failed to switch session: ${e.message}")
        }
    }

    private fun updatePhase(phase: String) {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        val validPhases = setOf("Free", "Correction", "Drill", "Auto")
        val normalizedPhase = phase.replaceFirstChar { it.uppercase() }

        if (normalizedPhase !in validPhases) {
            println("✗ Invalid phase. Must be one of: ${validPhases.joinToString(", ")}")
            return
        }

        try {
            val session = apiClient.updatePhase(currentSessionId!!, normalizedPhase)
            println("✓ Phase updated to: ${session.conversationPhase}")
        } catch (e: Exception) {
            println("✗ Failed to update phase: ${e.message}")
        }
    }

    private fun deleteCurrentSession() {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        try {
            apiClient.deleteSession(currentSessionId!!)
            println("✓ Deleted session: $currentSessionId")

            currentSessionId = null
            currentConfig = currentConfig.copy(lastSessionId = null)
            CliConfig.save(currentConfig)
        } catch (e: Exception) {
            println("✗ Failed to delete session: ${e.message}")
        }
    }

    private fun sendMessage(content: String) {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        try {
            println("\n${currentConfig.defaultTutor}: ")

            val message = apiClient.sendMessage(currentSessionId!!, content)

            println(message.content)

            // Display corrections if any
            if (!message.corrections.isNullOrEmpty()) {
                println("\n📝 Corrections:")
                message.corrections.forEach { correction ->
                    val severityIcon = when (correction.severity) {
                        "Critical" -> "🔴"
                        "High" -> "🟠"
                        "Medium" -> "🟡"
                        "Low" -> "🟢"
                        else -> "⚪"
                    }
                    println("  $severityIcon ${correction.span} → ${correction.correctedTargetLanguage}")
                    println("     ${correction.errorType}: ${correction.whySourceLanguage}")
                }
            }

            // Display new vocabulary if any
            if (!message.newVocabulary.isNullOrEmpty()) {
                println("\n📚 New Vocabulary:")
                message.newVocabulary.forEach { vocab ->
                    println("  • ${vocab.lemma}")
                    println("    Context: ${vocab.context}")
                }
            }

            // Display word cards if any
            if (!message.wordCards.isNullOrEmpty()) {
                println("\n🃏 Word Cards:")
                message.wordCards.forEach { card ->
                    println("  ┌─────────────────────────────────────────")
                    println("  │ ${card.titleTargetLanguage} / ${card.titleSourceLanguage}")
                    println("  ├─────────────────────────────────────────")
                    println("  │ ${card.descriptionTargetLanguage}")
                    println("  │ ${card.descriptionSourceLanguage}")
                    println("  └─────────────────────────────────────────")
                }
            }

            println()
        } catch (e: Exception) {
            println("\n✗ Failed to send message: ${e.message}")
        }
    }
}
