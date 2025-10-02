package ch.obermuhlner.aitutor.cli

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class AiTutorCli(private val config: CliConfig) {
    private val apiClient = HttpApiClient(config.apiUrl)
    private var currentSessionId: UUID? = config.getLastSessionIdAsUUID()
    private var currentConfig = config
    private val reader = BufferedReader(InputStreamReader(System.`in`))
    private var currentUserId: UUID? = null
    private var currentTutorName: String? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val config = CliConfig.load()
            val cli = AiTutorCli(config)
            cli.run()
        }
    }

    private fun ensureAuthenticated(): Boolean {
        // Check if we have a valid access token
        if (currentConfig.accessToken != null && currentConfig.isTokenValid()) {
            apiClient.setAccessToken(currentConfig.accessToken)
            currentUserId = extractUserIdFromToken()
            return true
        }

        // Try to refresh the token
        if (currentConfig.refreshToken != null) {
            try {
                val loginResponse = apiClient.refreshAccessToken(currentConfig.refreshToken!!)
                saveTokens(loginResponse)
                println("✓ Session refreshed")
                return true
            } catch (e: Exception) {
                println("Session expired. Please login.")
            }
        }

        // Try auto-login with stored credentials
        if (currentConfig.username != null && currentConfig.password != null) {
            try {
                val loginResponse = apiClient.login(currentConfig.username!!, currentConfig.password!!)
                saveTokens(loginResponse)
                println("✓ Logged in as ${loginResponse.user.username}")
                return true
            } catch (e: Exception) {
                println("Auto-login failed: ${e.message}")
            }
        }

        // Prompt for login
        return promptLogin()
    }

    private fun promptLogin(): Boolean {
        println("\n=== Login Required ===")
        println("Don't have an account? Type 'register' to create one.")
        print("Username or email (or 'register'): ")
        val username = reader.readLine()?.trim()

        if (username.isNullOrBlank()) {
            println("Login cancelled.")
            return false
        }

        if (username.equals("register", ignoreCase = true)) {
            return promptRegister()
        }

        print("Password: ")
        val password = reader.readLine()?.trim()
        if (password.isNullOrBlank()) {
            println("Login cancelled.")
            return false
        }

        return try {
            val loginResponse = apiClient.login(username, password)
            saveTokens(loginResponse)
            println("✓ Logged in as ${loginResponse.user.username}")
            true
        } catch (e: Exception) {
            println("✗ Login failed: ${e.message}")

            // Offer to register
            print("\nWould you like to register a new account? (y/n): ")
            val response = reader.readLine()?.trim()?.lowercase()
            if (response == "y" || response == "yes") {
                return promptRegister()
            }
            false
        }
    }

    private fun promptRegister(): Boolean {
        println("\n=== Register New Account ===")

        print("Username: ")
        val username = reader.readLine()?.trim()
        if (username.isNullOrBlank()) {
            println("Registration cancelled.")
            return false
        }

        print("Email: ")
        val email = reader.readLine()?.trim()
        if (email.isNullOrBlank()) {
            println("Registration cancelled.")
            return false
        }

        print("Password: ")
        val password = reader.readLine()?.trim()
        if (password.isNullOrBlank()) {
            println("Registration cancelled.")
            return false
        }

        print("First name (optional): ")
        val firstName = reader.readLine()?.trim()?.ifBlank { null }

        print("Last name (optional): ")
        val lastName = reader.readLine()?.trim()?.ifBlank { null }

        return try {
            val userResponse = apiClient.register(username, email, password, firstName, lastName)
            println("✓ Registration successful! Account created: ${userResponse.username}")

            // Automatically log in after registration
            println("Logging in...")
            val loginResponse = apiClient.login(username, password)
            saveTokens(loginResponse)
            println("✓ Logged in as ${loginResponse.user.username}")

            // Prompt for source language
            promptForSourceLanguage()

            true
        } catch (e: Exception) {
            println("✗ Registration failed: ${e.message}")
            false
        }
    }

    private fun promptForSourceLanguage() {
        println("\n=== Choose Your Native Language ===")
        println("This is the language you're most comfortable with (for explanations and translations).")
        println()

        val supportedSourceLanguages = listOf(
            "en" to "🇬🇧 English",
            "es" to "🇪🇸 Español (Spanish)",
            "fr" to "🇫🇷 Français (French)",
            "de" to "🇩🇪 Deutsch (German)",
            "ja" to "🇯🇵 日本語 (Japanese)"
        )

        supportedSourceLanguages.forEachIndexed { index, (code, name) ->
            println("  ${index + 1}. $name ($code)")
        }

        print("\nChoose your native language (1-${supportedSourceLanguages.size}, language code, or press Enter for English): ")
        val input = reader.readLine()?.trim()

        val sourceLanguage = if (input.isNullOrBlank()) {
            "en"
        } else {
            // Try to parse as number first
            val choice = input.toIntOrNull()
            if (choice != null && choice in 1..supportedSourceLanguages.size) {
                supportedSourceLanguages[choice - 1].first
            } else {
                // Extract base language code from locale format (e.g., de-CH -> de)
                val baseLangCode = input.split("-", "_").first()

                // Check if base code matches a supported language
                val matchingLang = supportedSourceLanguages.find { it.first.equals(baseLangCode, ignoreCase = true) }
                if (matchingLang != null) {
                    matchingLang.first
                } else {
                    println("Invalid choice, defaulting to English")
                    "en"
                }
            }
        }

        // Save the source language to config
        currentConfig = currentConfig.copy(defaultSourceLanguage = sourceLanguage)
        CliConfig.save(currentConfig)

        val languageName = supportedSourceLanguages.find { it.first == sourceLanguage }?.second ?: sourceLanguage
        println("✓ Native language set to: $languageName")
    }

    private fun saveTokens(loginResponse: HttpApiClient.LoginResponse) {
        val expiresAt = System.currentTimeMillis() + loginResponse.expiresIn
        currentConfig = currentConfig.copy(
            accessToken = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            tokenExpiresAt = expiresAt
        )
        CliConfig.save(currentConfig)
        apiClient.setAccessToken(loginResponse.accessToken)
        currentUserId = UUID.fromString(loginResponse.user.id)
    }

    private fun extractUserIdFromToken(): UUID? {
        // Extract user ID from stored login response or token
        // For now, we'll need to call an endpoint or decode the JWT
        // Simple approach: try to get sessions and extract userId from response
        return try {
            // We'll set this from login response, so just return null here
            // and rely on saveTokens to set it
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun logout() {
        currentConfig = currentConfig.copy(
            accessToken = null,
            refreshToken = null,
            tokenExpiresAt = null
        )
        CliConfig.save(currentConfig)
        apiClient.setAccessToken(null)
        currentUserId = null
        println("✓ Logged out")
    }

    private fun ensureTokenValid(): Boolean {
        // If token is still valid, no action needed
        if (currentConfig.isTokenValid()) {
            return true
        }

        // Token expired or near expiration, try to refresh
        if (currentConfig.refreshToken != null) {
            try {
                val loginResponse = apiClient.refreshAccessToken(currentConfig.refreshToken!!)
                saveTokens(loginResponse)
                return true
            } catch (e: Exception) {
                println("Session expired. Please login again with /login")
                return false
            }
        }

        println("Session expired. Please login again with /login")
        return false
    }

    fun run() {
        printWelcome()

        // Ensure we're authenticated before proceeding
        if (!ensureAuthenticated()) {
            println("Authentication required. Exiting.")
            return
        }

        // Show active learning sessions with progress
        val userId = currentUserId
        if (userId != null) {
            try {
                val sessions = apiClient.getActiveLearningSessions(userId)
                if (sessions.isNotEmpty()) {
                    println("\n📚 Your Learning Sessions:")
                    sessions.forEachIndexed { index, sessionWithProgress ->
                        val session = sessionWithProgress.session
                        val progress = sessionWithProgress.progress
                        val current = if (session.id == currentSessionId.toString()) " (current)" else ""
                        println("  ${index + 1}. ${session.tutorName} - ${session.targetLanguageCode}$current")
                        println("     ${progress.messageCount} messages | ${progress.vocabularyCount} words | ${progress.daysActive} days active")
                    }

                    // If there's a last session, resume it
                    if (currentSessionId != null) {
                        val currentSession = sessions.find { it.session.id == currentSessionId.toString() }
                        currentTutorName = currentSession?.session?.tutorName
                        println("\nResuming session: $currentSessionId")
                    } else if (sessions.isNotEmpty()) {
                        // Resume the most recent session
                        val mostRecent = sessions.first()
                        currentSessionId = UUID.fromString(mostRecent.session.id)
                        currentTutorName = mostRecent.session.tutorName
                        currentConfig = currentConfig.copy(lastSessionId = mostRecent.session.id)
                        CliConfig.save(currentConfig)
                        println("\nResuming most recent session: ${mostRecent.session.id}")
                    }
                } else {
                    println("\nNo active learning sessions found.")
                    println("Use /start-course to begin a new learning journey, or /new for a custom session.")
                }
            } catch (e: Exception) {
                println("Could not load sessions: ${e.message}")
                println("Use /start-course to begin a new learning journey, or /new for a custom session.")
            }
        } else {
            println("\nNo user ID found. Please login again.")
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
                    println("No active session. Use /start-course to create one.")
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
    }

    private fun printHelp() {
        println("""
            Commands:
              /languages            List available languages
              /courses [lang]       List courses (optionally filtered by language)
              /tutors [lang]        List tutors (optionally filtered by language)
              /start-course         Interactive course selection wizard
              /progress             Show progress for current session
              /new                  Create new session (legacy)
              /sessions             List all sessions
              /switch <id>          Switch to session
              /phase <phase>        Change phase (Free/Correction/Drill/Auto)
              /topic <topic>        Set conversation topic (or "none" for free conversation)
              /topics               Show topic history
              /delete               Delete current session
              /register             Register new account
              /login                Login (will prompt for credentials)
              /logout               Logout and clear tokens
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
            "/topic" -> {
                if (arg != null) {
                    updateTopic(arg)
                } else {
                    println("Usage: /topic <topic-name> or /topic none")
                }
                true
            }
            "/topics" -> {
                showTopicHistory()
                true
            }
            "/delete" -> {
                deleteCurrentSession()
                true
            }
            "/login" -> {
                if (promptLogin()) {
                    println("You may need to create a new session with /new")
                }
                true
            }
            "/register" -> {
                if (promptRegister()) {
                    println("You may need to create a new session with /new")
                }
                true
            }
            "/logout" -> {
                logout()
                false // Exit after logout
            }
            "/languages" -> {
                cmdLanguages()
                true
            }
            "/courses" -> {
                if (arg != null) {
                    cmdCourses(arg)
                } else {
                    println("Usage: /courses <language-code>")
                    println("Example: /courses es")
                }
                true
            }
            "/tutors" -> {
                if (arg != null) {
                    cmdTutors(arg)
                } else {
                    println("Usage: /tutors <language-code>")
                    println("Example: /tutors es")
                }
                true
            }
            "/start-course" -> {
                cmdStartCourse()
                true
            }
            "/progress" -> {
                cmdProgress()
                true
            }
            else -> {
                println("Unknown command: $command. Type /help for available commands.")
                true
            }
        }
    }

    private fun createNewSession() {
        val userId = currentUserId
        if (userId == null) {
            println("✗ Cannot create session: not logged in")
            return
        }

        try {
            val session = apiClient.createSession(
                userId = userId,
                tutorName = currentConfig.defaultTutor,
                tutorPersona = currentConfig.defaultTutorPersona,
                tutorDomain = currentConfig.defaultTutorDomain,
                sourceLanguage = currentConfig.sourceLanguage,
                targetLanguage = currentConfig.targetLanguage,
                phase = currentConfig.defaultPhase,
                cefrLevel = currentConfig.defaultCEFRLevel
            )

            currentSessionId = UUID.fromString(session.id)
            currentTutorName = session.tutorName
            currentConfig = currentConfig.copy(lastSessionId = session.id)
            CliConfig.save(currentConfig)

            println("✓ Created new session: ${session.id}")
            println("  Tutor: ${session.tutorName}")
            println("  Phase: ${session.conversationPhase}")
            println("  Topic: ${session.currentTopic ?: "(none)"}")
            println("  Languages: ${session.sourceLanguageCode} → ${session.targetLanguageCode}")
        } catch (e: Exception) {
            println("✗ Failed to create session: ${e.message}")
        }
    }

    private fun listSessions() {
        val userId = currentUserId
        if (userId == null) {
            println("✗ Cannot list sessions: not logged in")
            return
        }

        try {
            val sessions = apiClient.getUserSessions(userId)

            if (sessions.isEmpty()) {
                println("No sessions found.")
                return
            }

            println("\nYour sessions:")
            sessions.forEach { session ->
                val current = if (session.id == currentSessionId.toString()) " (current)" else ""
                println("  ${session.id}$current")
                println("    Tutor: ${session.tutorName}, Phase: ${session.conversationPhase}")
                println("    Topic: ${session.currentTopic ?: "(none)"}")
            }
        } catch (e: Exception) {
            println("✗ Failed to list sessions: ${e.message}")
        }
    }

    private fun switchSession(sessionId: String) {
        try {
            val uuid = UUID.fromString(sessionId)
            val userId = currentUserId
            if (userId != null) {
                try {
                    val sessions = apiClient.getActiveLearningSessions(userId)
                    val session = sessions.find { it.session.id == sessionId }
                    currentTutorName = session?.session?.tutorName
                } catch (e: Exception) {
                    // Couldn't get session info, continue anyway
                }
            }

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

    private fun updateTopic(topicArg: String) {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        // "none" means clear the topic (null)
        val topic = if (topicArg.lowercase() == "none") null else topicArg

        try {
            val session = apiClient.updateTopic(currentSessionId!!, topic)
            if (topic == null) {
                println("✓ Topic cleared - now in free conversation mode")
            } else {
                println("✓ Topic updated to: ${session.currentTopic}")
            }
        } catch (e: Exception) {
            println("✗ Failed to update topic: ${e.message}")
        }
    }

    private fun showTopicHistory() {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        try {
            val history = apiClient.getTopicHistory(currentSessionId!!)

            println("\n📚 Topic History:")
            println("  Current: ${history.currentTopic ?: "(free conversation)"}")

            if (history.pastTopics.isEmpty()) {
                println("  Past: (none)")
            } else {
                println("  Past topics:")
                history.pastTopics.reversed().forEachIndexed { index, topic ->
                    println("    ${history.pastTopics.size - index}. $topic")
                }
            }
            println()
        } catch (e: Exception) {
            println("✗ Failed to get topic history: ${e.message}")
        }
    }

    private fun cmdLanguages() {
        try {
            println("\n📚 Available Languages:")
            val languages = apiClient.getAvailableLanguages(currentConfig.defaultSourceLanguage)
            languages.forEachIndexed { index, lang ->
                println("  ${index + 1}. ${lang.name} ${lang.flagEmoji} (${lang.courseCount} courses) - ${lang.difficulty}")
                println("     ${lang.description}")
            }
            println()
        } catch (e: Exception) {
            println("✗ Failed to get languages: ${e.message}")
        }
    }

    private fun cmdCourses(langCode: String) {
        try {
            val courses = apiClient.getCoursesForLanguage(langCode, currentConfig.defaultSourceLanguage)
            if (courses.isEmpty()) {
                println("No courses found for language: $langCode")
                return
            }

            println("\nCourses for $langCode:")
            courses.forEachIndexed { index, course ->
                println("  ${index + 1}. ${course.name} (${course.startingLevel}→${course.targetLevel})")
                println("     ${course.shortDescription}")
                val duration = course.estimatedWeeks?.let { "$it weeks" } ?: "Self-paced"
                println("     Category: ${course.category}, Duration: $duration")
            }
            println()
        } catch (e: Exception) {
            println("✗ Failed to get courses: ${e.message}")
        }
    }

    private fun cmdTutors(langCode: String) {
        try {
            val tutors = apiClient.getTutorsForLanguage(langCode, currentConfig.defaultSourceLanguage)
            if (tutors.isEmpty()) {
                println("No tutors found for language: $langCode")
                return
            }

            println("\nTutors for $langCode:")
            tutors.forEachIndexed { index, tutor ->
                println("  ${index + 1}. ${tutor.emoji} ${tutor.name} (${tutor.personality})")
                println("     ${tutor.description}")
            }
            println()
        } catch (e: Exception) {
            println("✗ Failed to get tutors: ${e.message}")
        }
    }

    private fun cmdStartCourse() {
        println("\n=== Course Selection Wizard ===\n")

        val userId = currentUserId
        if (userId == null) {
            println("✗ Cannot start course: not logged in")
            return
        }

        try {
            // Step 1: Select language
            println("📚 Available Languages:")
            val languages = apiClient.getAvailableLanguages(currentConfig.defaultSourceLanguage)
            languages.forEachIndexed { index, lang ->
                println("  ${index + 1}. ${lang.name} ${lang.flagEmoji} (${lang.courseCount} courses)")
            }
            print("\nChoose a language (1-${languages.size}): ")
            val langChoice = reader.readLine()?.toIntOrNull()
            if (langChoice == null || langChoice < 1 || langChoice > languages.size) {
                println("Invalid choice")
                return
            }
            val selectedLang = languages[langChoice - 1]

            // Step 2: Select course
            println("\n${selectedLang.name} ${selectedLang.flagEmoji} - Available Courses:")
            val courses = apiClient.getCoursesForLanguage(selectedLang.code, currentConfig.defaultSourceLanguage)
            if (courses.isEmpty()) {
                println("No courses available for this language")
                return
            }
            courses.forEachIndexed { index, course ->
                println("  ${index + 1}. ${course.name} (${course.startingLevel}→${course.targetLevel})")
                println("     ${course.shortDescription}")
            }
            print("\nChoose a course (1-${courses.size}): ")
            val courseChoice = reader.readLine()?.toIntOrNull()
            if (courseChoice == null || courseChoice < 1 || courseChoice > courses.size) {
                println("Invalid choice")
                return
            }
            val selectedCourse = courses[courseChoice - 1]

            // Get course details
            val courseDetails = apiClient.getCourseDetails(UUID.fromString(selectedCourse.id), currentConfig.defaultSourceLanguage)

            // Step 3: Select tutor
            println("\nChoose your tutor:")
            val tutors = courseDetails.suggestedTutors
            if (tutors.isEmpty()) {
                println("No tutors available for this course")
                return
            }
            tutors.forEachIndexed { index, tutor ->
                println("  ${index + 1}. ${tutor.emoji} ${tutor.name} (${tutor.personality})")
                println("     ${tutor.description}")
            }
            print("\nChoose tutor (1-${tutors.size}): ")
            val tutorChoice = reader.readLine()?.toIntOrNull()
            if (tutorChoice == null || tutorChoice < 1 || tutorChoice > tutors.size) {
                println("Invalid choice")
                return
            }
            val selectedTutor = tutors[tutorChoice - 1]

            // Step 4: Create session
            print("\nCustom session name (optional, press Enter to skip): ")
            val customName = reader.readLine()?.trim()?.ifBlank { null }

            val session = apiClient.createSessionFromCourse(
                userId = userId,
                courseTemplateId = UUID.fromString(selectedCourse.id),
                tutorProfileId = UUID.fromString(selectedTutor.id),
                sourceLanguageCode = currentConfig.defaultSourceLanguage,
                customName = customName
            )

            currentSessionId = UUID.fromString(session.id)
            currentTutorName = selectedTutor.name
            currentConfig = currentConfig.copy(lastSessionId = session.id)
            CliConfig.save(currentConfig)

            println("\n✓ Created learning session!")
            println("  Course: ${selectedCourse.name}")
            println("  Tutor: ${selectedTutor.name}")
            println("  Session ID: ${session.id}")
            println("\nYou can now start chatting!")
        } catch (e: Exception) {
            println("✗ Failed to start course: ${e.message}")
        }
    }

    private fun cmdProgress() {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        try {
            val progress = apiClient.getSessionProgress(currentSessionId!!)
            println("\n📊 Session Progress:")
            println("  Messages: ${progress.messageCount}")
            println("  Vocabulary: ${progress.vocabularyCount} words")
            println("  Days active: ${progress.daysActive}")
            println("  Last accessed: ${progress.lastAccessedAt}")
            println()
        } catch (e: Exception) {
            println("✗ Failed to get progress: ${e.message}")
        }
    }

    private fun sendMessage(content: String) {
        if (currentSessionId == null) {
            println("✗ No active session")
            return
        }

        if (!ensureTokenValid()) {
            return
        }

        try {
            val tutorName = currentTutorName ?: "Tutor"
            println("\n$tutorName: ")

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
