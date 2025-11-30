package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.MessageResponse
import ch.obermuhlner.aitutor.chat.dto.SessionProgressResponse
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse
import ch.obermuhlner.aitutor.chat.dto.SessionWithProgressResponse
import ch.obermuhlner.aitutor.chat.dto.TopicHistoryResponse
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
import ch.obermuhlner.aitutor.image.service.ImageService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.dto.NewVocabularyDTO
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val tutorService: TutorService,
    private val correctionService: ch.obermuhlner.aitutor.tutor.service.CorrectionService,
    private val vocabularyService: VocabularyService,
    private val vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService,
    private val phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService,
    private val topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService,
    private val metadataEvaluationService: ch.obermuhlner.aitutor.tutor.service.MetadataEvaluationService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val errorAnalyticsService: ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService,
    private val userLanguageService: ch.obermuhlner.aitutor.user.service.UserLanguageService,
    private val lessonProgressionService: ch.obermuhlner.aitutor.lesson.service.LessonProgressionService,
    private val userChatModelFactory: ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory,
    private val rateLimitingService: ch.obermuhlner.aitutor.user.service.RateLimitingService,
    private val userRepository: ch.obermuhlner.aitutor.user.repository.UserRepository,
    private val imageService: ImageService,
    private val objectMapper: ObjectMapper,
    private val metricsService: ch.obermuhlner.aitutor.metrics.MetricsService,
    @Value("\${ai-tutor.messages.technical-error}") private val technicalErrorMessage: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse {
        logger.info("Creating chat session for user ${request.userId}: ${request.tutorName} (${request.targetLanguageCode})")

        // Determine the initial CEFR level: use user's proficiency if available, otherwise use the request value
        val initialCEFRLevel = determineInitialCEFRLevel(request.userId, request.targetLanguageCode, request.estimatedCEFRLevel)

        // Determine source language from user's native language profile
        val sourceLanguageCode = userLanguageService.suggestSourceLanguage(request.userId, request.targetLanguageCode)

        val session = ChatSessionEntity(
            userId = request.userId,
            tutorName = request.tutorName,
            tutorPersona = request.tutorPersona,
            tutorDomain = request.tutorDomain,
            sourceLanguageCode = sourceLanguageCode,
            targetLanguageCode = request.targetLanguageCode,
            conversationPhase = request.conversationPhase,
            effectivePhase = if (request.conversationPhase == ConversationPhase.Auto) ConversationPhase.Correction else request.conversationPhase,
            estimatedCEFRLevel = initialCEFRLevel,
            currentTopic = request.currentTopic
        )
        val saved = chatSessionRepository.save(session)
        logger.debug("Chat session created: ${saved.id}")
        return toSessionResponse(saved)
    }

    fun getSession(sessionId: UUID): SessionResponse? {
        return chatSessionRepository.findById(sessionId)
            .map { toSessionResponse(it) }
            .orElse(null)
    }

    fun getMessage(sessionId: UUID, messageId: UUID): ChatMessageEntity? {
        val message = chatMessageRepository.findById(messageId).orElse(null) ?: return null
        // Validate message belongs to session
        if (message.session.id != sessionId) {
            return null
        }
        return message
    }

    @Transactional
    fun updateMessageAudioCache(
        sessionId: UUID,
        messageId: UUID,
        audioData: ByteArray,
        voiceId: String?,
        speed: Double?
    ): ChatMessageEntity? {
        val message = getMessage(sessionId, messageId) ?: return null
        message.audioData = audioData
        message.audioVoiceId = voiceId
        message.audioSpeed = speed
        return chatMessageRepository.save(message)
    }

    @Transactional
    fun updateMessageCorrections(
        sessionId: UUID,
        messageId: UUID,
        userId: UUID,
        corrections: List<Correction>
    ): MessageResponse? {
        val message = getMessage(sessionId, messageId) ?: return null

        // Validate message belongs to the user's session
        if (message.session.userId != userId) {
            logger.warn("User $userId attempted to update corrections for message $messageId in session $sessionId owned by ${message.session.userId}")
            return null
        }

        // Only allow updating USER role messages (corrections are for user's input)
        if (message.role != MessageRole.USER) {
            logger.warn("Attempted to update corrections for non-USER message $messageId (role: ${message.role})")
            return null
        }

        message.correctionsJson = objectMapper.writeValueAsString(corrections)
        logger.debug("Updated corrections for message $messageId in session $sessionId: ${corrections.size} corrections")
        val savedMessage = chatMessageRepository.save(message)
        return toMessageResponse(savedMessage)
    }

    fun getUserSessions(userId: UUID): List<SessionResponse> {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .map { toSessionResponse(it) }
    }

    fun getSessionWithMessages(sessionId: UUID, currentUserId: UUID): SessionWithMessagesResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        val messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .map { toMessageResponse(it) }

        return SessionWithMessagesResponse(
            session = toSessionResponse(session),
            messages = messages
        )
    }

    @Transactional
    fun deleteSession(sessionId: UUID, currentUserId: UUID): Boolean {
        logger.info("Delete session request: $sessionId by user $currentUserId")

        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: run {
            logger.warn("Delete session failed: session $sessionId not found")
            return false
        }

        // Validate ownership
        if (session.userId != currentUserId) {
            logger.warn("Delete session failed: user $currentUserId does not own session $sessionId")
            return false
        }

        // Delete all messages first to avoid foreign key constraint violation
        val messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        chatMessageRepository.deleteAll(messages)

        chatSessionRepository.deleteById(sessionId)
        logger.info("Session deleted successfully: $sessionId")
        return true
    }

    @Transactional
    fun updateSessionPhase(sessionId: UUID, phase: ConversationPhase, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        session.conversationPhase = phase
        val saved = chatSessionRepository.save(session)
        return toSessionResponse(saved)
    }

    @Transactional
    fun updateSessionTopic(sessionId: UUID, topic: String?, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        // Archive old topic if changing
        if (session.currentTopic != null && session.currentTopic != topic) {
            val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            val turnCount = topicDecisionService.countTurnsInRecentMessages(allMessages)
            if (topicDecisionService.shouldArchiveTopic(session.currentTopic, turnCount)) {
                archiveTopic(session, session.currentTopic!!)
            }
        }

        session.currentTopic = topic
        val saved = chatSessionRepository.save(session)
        return toSessionResponse(saved)
    }

    @Transactional
    fun updateSessionTeachingStyle(sessionId: UUID, teachingStyle: ch.obermuhlner.aitutor.tutor.domain.TeachingStyle, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        session.tutorTeachingStyle = teachingStyle
        val saved = chatSessionRepository.save(session)
        return toSessionResponse(saved)
    }

    @Transactional
    fun updateVocabularyReviewMode(sessionId: UUID, enabled: Boolean, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        session.vocabularyReviewMode = enabled
        val saved = chatSessionRepository.save(session)
        logger.info("Vocabulary review mode ${if (enabled) "enabled" else "disabled"} for session $sessionId")
        return toSessionResponse(saved)
    }

    @Transactional
    fun updateSessionLesson(sessionId: UUID, direction: ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        // Only allow lesson navigation for course-based sessions
        if (session.courseTemplateId == null) {
            logger.warn("Attempt to navigate lessons in non-course session: $sessionId")
            return null
        }

        val lessonContent = when (direction) {
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT -> 
                lessonProgressionService.navigateToNextLesson(sessionId)
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.PREVIOUS -> 
                lessonProgressionService.navigateToPreviousLesson(sessionId)
        }

        return if (lessonContent != null) toSessionResponse(session) else null
    }

    @Transactional
    fun updateSessionToSpecificLesson(sessionId: UUID, lessonId: String, currentUserId: UUID): SessionResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        // Only allow lesson navigation for course-based sessions
        if (session.courseTemplateId == null) {
            logger.warn("Attempt to navigate to specific lesson in non-course session: $sessionId")
            return null
        }

        val lessonContent = lessonProgressionService.navigateToSpecificLesson(sessionId, lessonId)

        return if (lessonContent != null) toSessionResponse(session) else null
    }

    fun getTopicHistory(sessionId: UUID, currentUserId: UUID): TopicHistoryResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        val pastTopics = session.pastTopicsJson?.let {
            try {
                objectMapper.readValue<List<String>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return TopicHistoryResponse(
            currentTopic = session.currentTopic,
            pastTopics = pastTopics
        )
    }

    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        currentUserId: UUID,
        corrections: List<Correction>? = null,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        // Calculate next sequence number
        val nextSequence = calculateNextSequence(sessionId)

        // Save user message with corrections if provided
        val userMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = userContent,
            correctionsJson = corrections?.let { objectMapper.writeValueAsString(it) },
            sequenceNumber = nextSequence
        )
        chatMessageRepository.save(userMessage)

        // Record user message metrics
        metricsService.recordChatMessage("user")

        // Increment lesson turn count for user messages
        incrementLessonTurnCount(session)

        // Delegate to shared generation logic (assistant message follows user message)
        return generateTutorResponse(
            session = session,
            nextSequence = nextSequence + 1,
            initiationContext = null,
            includeUserMessageInEvaluation = true,
            userMessageForEvaluation = userMessage,
            onReplyChunk = onReplyChunk
        )?.also {
            // Record assistant message metrics if we received a response
            metricsService.recordChatMessage("assistant")
        }
    }

    /**
     * Analyze user text for corrections without storing to database.
     * Intended for real-time correction analysis called from the frontend.
     *
     * @param sessionId The chat session ID (for context and authorization)
     * @param userText The user's message text to analyze
     * @param userId The current user ID (for authorization)
     * @return List of corrections found (empty if no errors)
     */
    fun analyzeCorrections(sessionId: UUID, userText: String, userId: UUID): List<Correction> {
        // Get session for authorization and context
        val session = chatSessionRepository.findById(sessionId).orElse(null)
            ?: throw IllegalArgumentException("Session not found")

        // Authorization check
        if (session.userId != userId) {
            throw SecurityException("Unauthorized access to session")
        }

        // Get user's custom ChatModel if they have BYOK configured
        val userChatModel = userChatModelFactory.getChatModelForUser(userId)

        // Call CorrectionService directly (synchronous)
        return correctionService.generateCorrections(
            userText = userText,
            sourceLanguageCode = session.sourceLanguageCode,
            targetLanguageCode = session.targetLanguageCode,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            phase = session.effectivePhase ?: ConversationPhase.Correction,
            chatModel = userChatModel
        )
    }

    /**
     * Initiates a tutor message without requiring a user message first.
     * Used for welcome messages when starting a course or re-engagement after inactivity.
     *
     * @param sessionId The chat session ID
     * @param currentUserId The current user ID (for ownership validation)
     * @param initiationContext Context for the initiation: "welcome" or "reengage"
     * @param onReplyChunk Callback for streaming response chunks
     * @return The generated assistant message response, or null if session not found or unauthorized
     */
    @Transactional
    fun initiateTutorMessage(
        sessionId: UUID,
        currentUserId: UUID,
        initiationContext: String = "welcome",
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        logger.info("Tutor initiating message for session $sessionId (context: $initiationContext)")

        // Calculate next sequence number (no user message to save)
        val nextSequence = calculateNextSequence(sessionId)

        // Delegate to shared generation logic (assistant message is first message)
        return generateTutorResponse(
            session = session,
            nextSequence = nextSequence,
            initiationContext = initiationContext,
            includeUserMessageInEvaluation = false,
            userMessageForEvaluation = null,
            onReplyChunk = onReplyChunk
        )?.also {
            logger.info("Tutor message initiated successfully for session $sessionId (context: $initiationContext)")
            // Record initiated message metrics
            metricsService.recordChatMessage("assistant")
        }
    }

    /**
     * Shared logic for generating tutor responses.
     * Handles both user-initiated messages (sendMessage) and tutor-initiated messages (initiateTutorMessage).
     *
     * This method encapsulates the common pipeline:
     * 1. Build message history and conversation state
     * 2. Resolve user-specific ChatModel and check rate limits
     * 3. Call TutorService to generate response
     * 4. Handle errors with technical error message
     * 5. Save assistant message with vocabulary/cards
     * 6. Track new vocabulary
     * 7. Trigger metadata evaluation
     *
     * @param session The chat session (already validated)
     * @param nextSequence The sequence number for the assistant message
     * @param initiationContext Optional context for tutor-initiated messages ("welcome", "reengage-light", "reengage")
     * @param includeUserMessageInEvaluation Whether to include user message in metadata evaluation
     * @param userMessageForEvaluation The user message to include in evaluation (if applicable)
     * @param onReplyChunk Callback for streaming response chunks
     * @return The saved assistant message response
     */
    private fun generateTutorResponse(
        session: ChatSessionEntity,
        nextSequence: Int,
        initiationContext: String?,
        includeUserMessageInEvaluation: Boolean,
        userMessageForEvaluation: ChatMessageEntity?,
        onReplyChunk: (String) -> Unit
    ): MessageResponse? {
        // Get message history for both AI call and metadata evaluation (single query)
        val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id)
        val messageHistory = allMessages.map { entity ->
            when (entity.role) {
                MessageRole.USER -> UserMessage(entity.content)
                MessageRole.ASSISTANT -> AssistantMessage(entity.content)
            }
        }

        // Build Tutor object
        val tutor = buildTutorFromSession(session)

        // Initialize effectivePhase if null (migration case)
        if (session.effectivePhase == null) {
            session.effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
                ConversationPhase.Correction
            } else {
                session.conversationPhase
            }
        }

        // Parse past topics from session
        val pastTopics = parsePastTopics(session)

        // Get due vocabulary count if review mode enabled
        val dueCount = if (session.vocabularyReviewMode) {
            vocabularyReviewService.getDueCount(session.userId, session.targetLanguageCode)
                .also { count ->
                    if (count > 0) {
                        logger.info("Vocabulary review mode active: session=${session.id}, dueCount=$count")
                    }
                }
        } else null

        // Build ConversationState
        val conversationState = buildConversationState(
            session = session,
            pastTopics = pastTopics,
            dueCount = dueCount,
            initiationContext = initiationContext
        )

        // Resolve user and check rate limit
        val user = userRepository.findById(session.userId).orElse(null)
        if (user != null) {
            rateLimitingService.checkRateLimit(session.userId, user.subscriptionPlan)
        }

        // Get user-specific ChatModel (or null to use system default)
        val userChatModel = resolveUserChatModel(session.userId)

        // Extract user's display name
        val userName = extractUserName(user)

        // Call TutorService
        val tutorResponse = invokeTutorService(
            tutor = tutor,
            conversationState = conversationState,
            session = session,
            user = user,
            userName = userName,
            messageHistory = messageHistory,
            userChatModel = userChatModel,
            onReplyChunk = onReplyChunk
        )

        // Handle error case
        if (tutorResponse == null) {
            return handleTutorServiceError(session, nextSequence)
        }

        // Save session updates
        chatSessionRepository.save(session)

        // Save assistant message with vocabulary and cards
        val assistantMessage = buildAssistantMessage(
            session = session,
            tutorResponse = tutorResponse,
            sequenceNumber = nextSequence
        )
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        // Track new vocabulary
        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            trackVocabulary(session, tutorResponse, savedAssistantMessage)
        }

        // Evaluate session metadata (CEFR level, topic, phase, lesson progression)
        val messagesForEvaluation = if (includeUserMessageInEvaluation && userMessageForEvaluation != null) {
            allMessages + listOf(userMessageForEvaluation, savedAssistantMessage)
        } else {
            allMessages + listOf(savedAssistantMessage)
        }

        metadataEvaluationService.evaluateIfNeeded(
            sessionId = session.id,
            messageHistory = messagesForEvaluation
        )

        return toMessageResponse(savedAssistantMessage)
    }

    private fun getAndValidateSession(sessionId: UUID, userId: UUID): ChatSessionEntity? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null
        if (session.userId != userId) return null
        return session
    }

    private fun calculateNextSequence(sessionId: UUID): Int {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .maxOfOrNull { it.sequenceNumber }?.plus(1) ?: 0
    }

    private fun buildTutorFromSession(session: ChatSessionEntity): Tutor {
        return Tutor(
            name = session.tutorName,
            persona = session.tutorPersona,
            domain = session.tutorDomain,
            teachingStyle = session.tutorTeachingStyle,
            sourceLanguageCode = session.sourceLanguageCode,
            targetLanguageCode = session.targetLanguageCode,
            gender = session.tutorGender,
            age = session.tutorAge,
            location = session.tutorLocation
        )
    }

    private fun parsePastTopics(session: ChatSessionEntity): List<String> {
        return session.pastTopicsJson?.let {
            try {
                objectMapper.readValue<List<String>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    private fun buildConversationState(
        session: ChatSessionEntity,
        pastTopics: List<String>,
        dueCount: Long?,
        initiationContext: String?
    ): ConversationState {
        return ConversationState(
            phase = session.effectivePhase ?: ConversationPhase.Correction,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            currentTopic = session.currentTopic,
            phaseReason = session.phaseReason ?: "Balanced default phase",
            topicEligibilityStatus = session.topicEligibilityStatus ?: "Active conversation",
            pastTopics = pastTopics,
            vocabularyReviewMode = session.vocabularyReviewMode,
            dueVocabularyCount = dueCount,
            initiationContext = initiationContext
        )
    }

    private fun resolveUserChatModel(userId: UUID): org.springframework.ai.chat.model.ChatModel? {
        return try {
            userChatModelFactory.getChatModelForUser(userId)
        } catch (e: Exception) {
            logger.warn("Failed to get user ChatModel for user $userId, using system default", e)
            null
        }
    }

    private fun extractUserName(user: ch.obermuhlner.aitutor.user.domain.UserEntity?): String? {
        if (user == null) return null
        val nameParts = listOfNotNull(user.firstName, user.lastName).filter { it.isNotBlank() }
        return if (nameParts.isNotEmpty()) nameParts.joinToString(" ") else user.username
    }

    private fun invokeTutorService(
        tutor: Tutor,
        conversationState: ConversationState,
        session: ChatSessionEntity,
        user: ch.obermuhlner.aitutor.user.domain.UserEntity?,
        userName: String?,
        messageHistory: List<Message>,
        userChatModel: org.springframework.ai.chat.model.ChatModel?,
        onReplyChunk: (String) -> Unit
    ): TutorService.TutorResponse? {
        return try {
            tutorService.respond(
                tutor = tutor,
                conversationState = conversationState,
                userId = session.userId,
                messages = messageHistory,
                sessionId = session.id,
                session = session,
                userName = userName,
                pronunciationPreference = user?.pronunciationPreference,
                onReplyChunk = onReplyChunk,
                chatModel = userChatModel
            )
        } catch (e: Exception) {
            logger.error("ChatModel call failed for session ${session.id}, user ${session.userId}", e)
            null
        }
    }

    private fun handleTutorServiceError(session: ChatSessionEntity, sequenceNumber: Int): MessageResponse {
        val errorMessage = technicalErrorMessage
        val errorAssistantMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = errorMessage,
            sequenceNumber = sequenceNumber
        )
        val savedAssistantMessage = chatMessageRepository.save(errorAssistantMessage)
        return toMessageResponse(savedAssistantMessage, errorMessage)
    }

    private fun buildAssistantMessage(
        session: ChatSessionEntity,
        tutorResponse: TutorService.TutorResponse,
        sequenceNumber: Int
    ): ChatMessageEntity {
        return ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = tutorResponse.reply,
            vocabularyJson = if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.newVocabulary) else null,
            wordCardsJson = if (tutorResponse.conversationResponse.wordCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.wordCards) else null,
            characterCardsJson = if (tutorResponse.conversationResponse.characterCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.characterCards) else null,
            sequenceNumber = sequenceNumber
        )
    }

    private fun trackVocabulary(
        session: ChatSessionEntity,
        tutorResponse: TutorService.TutorResponse,
        savedMessage: ChatMessageEntity
    ) {
        vocabularyService.addNewVocabulary(
            userId = session.userId,
            lang = session.targetLanguageCode,
            items = tutorResponse.conversationResponse.newVocabulary.map { vocab ->
                NewVocabularyDTO(vocab.lemma, vocab.context, vocab.conceptName)
            },
            turnId = savedMessage.id
        )
    }

    /**
     * Determines the initial CEFR level for a new session.
     * First checks the user's language proficiencies in their profile for the target language.
     * If found and has a CEFR level, returns that level; otherwise returns the provided default.
     */
    private fun determineInitialCEFRLevel(userId: UUID, targetLanguageCode: String, defaultCEFRLevel: CEFRLevel): CEFRLevel {
        try {
            // Get user's language proficiencies for the target language
            val userLanguages = userLanguageService.getLearningLanguages(userId)
            val targetLanguageProficiency = userLanguages.find { it.languageCode == targetLanguageCode }
            
            // If the target language exists in user's proficiencies and has a CEFR level, use it
            return if (targetLanguageProficiency?.cefrLevel != null) {
                logger.debug("Using CEFR level ${targetLanguageProficiency.cefrLevel} from user profile for language $targetLanguageCode")
                targetLanguageProficiency.cefrLevel!!
            } else {
                logger.debug("No CEFR level found in user profile for language $targetLanguageCode, using default: $defaultCEFRLevel")
                defaultCEFRLevel
            }
        } catch (e: Exception) {
            logger.warn("Failed to retrieve user language proficiencies for user $userId, using default CEFR level: $defaultCEFRLevel", e)
            return defaultCEFRLevel
        }
    }

    @Transactional
    fun createSessionFromCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String,
        customName: String? = null
    ): SessionResponse? {
        val course = catalogService.getCourseById(courseTemplateId) ?: return null
        val tutor = catalogService.getTutorById(tutorProfileId, userId) ?: return null

        // For course-based sessions, determine the initial CEFR level: use user's proficiency if available, 
        // otherwise fall back to the course starting level
        val initialCEFRLevel = determineInitialCEFRLevel(userId, tutor.targetLanguageCode, course.startingLevel)

        // Determine source language from user's native language profile as per original design
        val resolvedSourceLanguageCode = userLanguageService.suggestSourceLanguage(userId, tutor.targetLanguageCode)

        val gender = tutor.gender ?: ch.obermuhlner.aitutor.core.model.catalog.TutorGender.Neutral
        val countryCode = tutor.targetLanguageCode.substringAfterLast("-").uppercase()
        val combinedText = "${tutor.location} ${tutor.personaEnglish}"

        val tutorImageUrl = imageService.getImageUrlByPerson(
            countryCode = countryCode,
            gender = gender,
            age = tutor.age,
            text = combinedText
        )

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = tutor.name,
            tutorPersona = tutor.personaEnglish,
            tutorDomain = tutor.domainEnglish,
            tutorTeachingStyle = tutor.teachingStyle,
            tutorAge = tutor.age,
            tutorLocation = tutor.location,
            tutorImage = tutorImageUrl,
            tutorEmoji = tutor.emoji,
            tutorVoiceId = tutor.voiceId,
            tutorGender = tutor.gender,
            sourceLanguageCode = resolvedSourceLanguageCode,
            targetLanguageCode = tutor.targetLanguageCode,
            conversationPhase = course.defaultPhase,
            effectivePhase = if (course.defaultPhase == ConversationPhase.Auto) ConversationPhase.Correction else course.defaultPhase,
            estimatedCEFRLevel = initialCEFRLevel,
            courseTemplateId = courseTemplateId,
            tutorProfileId = tutorProfileId,
            customName = customName,
            isActive = true
        )

        return toSessionResponse(chatSessionRepository.save(session))
    }

    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse> {
        val sessions = chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
        return sessions.map { session ->
            SessionWithProgressResponse(
                session = toSessionResponse(session),
                progress = getSessionProgress(session.id)
            )
        }
    }

    fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
        val session = chatSessionRepository.findById(sessionId).orElse(null)
            ?: return SessionProgressResponse(0, 0, 0)

        val messageCount = chatMessageRepository.countBySessionId(sessionId)
        val vocabularyCount = vocabularyService.getVocabularyCountForLanguage(
            session.userId,
            session.targetLanguageCode
        )

        val daysActive = java.time.Duration.between(
            session.createdAt ?: java.time.Instant.now(),
            session.updatedAt ?: java.time.Instant.now()
        ).toDays()

        return SessionProgressResponse(
            messageCount = messageCount.toInt(),
            vocabularyCount = vocabularyCount,
            daysActive = daysActive
        )
    }

    /**
     * Archives a topic to the past topics history.
     * Maintains a list of the last 20 topics.
     */
    private fun archiveTopic(session: ChatSessionEntity, topic: String) {
        val pastTopics = session.pastTopicsJson?.let {
            try {
                objectMapper.readValue<List<String>>(it).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        } ?: mutableListOf()

        // Add new topic if not already at the end
        if (pastTopics.lastOrNull() != topic) {
            pastTopics.add(topic)
        }

        // Keep only last 20 topics
        if (pastTopics.size > 20) {
            pastTopics.removeAt(0)
        }

        session.pastTopicsJson = objectMapper.writeValueAsString(pastTopics)
    }

    private fun toSessionResponse(entity: ChatSessionEntity): SessionResponse {
        // Handle migration case: if effectivePhase is null, derive it from conversationPhase
        val effectivePhase = entity.effectivePhase ?: run {
            if (entity.conversationPhase == ConversationPhase.Auto) {
                ConversationPhase.Correction
            } else {
                entity.conversationPhase
            }
        }

        return SessionResponse(
            id = entity.id,
            userId = entity.userId,
            tutorName = entity.tutorName,
            tutorPersona = entity.tutorPersona,
            tutorDomain = entity.tutorDomain,
            tutorTeachingStyle = entity.tutorTeachingStyle,
            tutorAge = entity.tutorAge,
            tutorImage = entity.tutorImage,
            tutorEmoji = entity.tutorEmoji,
            sourceLanguageCode = entity.sourceLanguageCode,
            targetLanguageCode = entity.targetLanguageCode,
            conversationPhase = entity.conversationPhase,
            effectivePhase = effectivePhase,
            estimatedCEFRLevel = entity.estimatedCEFRLevel,
            currentTopic = entity.currentTopic,
            cefrGrammar = entity.cefrGrammar,
            cefrVocabulary = entity.cefrVocabulary,
            cefrFluency = entity.cefrFluency,
            cefrComprehension = entity.cefrComprehension,
            lastAssessmentAt = entity.lastAssessmentAt,
            courseTemplateId = entity.courseTemplateId,
            tutorProfileId = entity.tutorProfileId,
            customName = entity.customName,
            isActive = entity.isActive,
            vocabularyReviewMode = entity.vocabularyReviewMode,
            createdAt = entity.createdAt ?: java.time.Instant.now(),
            updatedAt = entity.updatedAt ?: java.time.Instant.now()
        )
    }

    private fun toMessageResponse(entity: ChatMessageEntity, errorMessage: String? = null): MessageResponse {
        val corrections: List<Correction>? = entity.correctionsJson?.let {
            objectMapper.readValue(it)
        }

        val vocabulary: List<NewVocabulary>? = entity.vocabularyJson?.let {
            objectMapper.readValue(it)
        }
        val vocabularyWithImages = vocabulary?.map { vocab ->
            ch.obermuhlner.aitutor.chat.dto.VocabularyWithImageResponse(
                lemma = vocab.lemma,
                context = vocab.context,
                conceptName = vocab.conceptName,
                imageUrl = vocab.conceptName?.let { imageService.getImageUrlByConcept(it) }
            )
        }

        val wordCards: List<WordCard>? = entity.wordCardsJson?.let {
            objectMapper.readValue(it)
        }
        val wordCardsWithImages = wordCards?.map { card ->
            ch.obermuhlner.aitutor.chat.dto.WordCardResponse(
                titleSourceLanguage = card.titleSourceLanguage,
                titleTargetLanguage = card.titleTargetLanguage,
                descriptionSourceLanguage = card.descriptionSourceLanguage,
                descriptionTargetLanguage = card.descriptionTargetLanguage,
                conceptName = card.conceptName,
                imageUrl = card.conceptName?.let { imageService.getImageUrlByConcept(it) }
            )
        }

        val characterCards: List<ch.obermuhlner.aitutor.core.model.CharacterCard>? = entity.characterCardsJson?.let {
            objectMapper.readValue(it)
        }
        val characterCardResponses = characterCards?.map { card ->
            ch.obermuhlner.aitutor.chat.dto.CharacterCardResponse(
                character = card.character,
                pronunciation = card.pronunciation,
                description = card.description
            )
        }

        return MessageResponse(
            id = entity.id,
            role = entity.role.name,
            content = entity.content,
            corrections = corrections,
            newVocabulary = vocabularyWithImages,
            wordCards = wordCardsWithImages,
            characterCards = characterCardResponses,
            errorMessage = errorMessage,
            createdAt = entity.createdAt ?: java.time.Instant.now()
        )
    }

    /**
     * Increments the turn count in the lesson progress JSON.
     * This tracks how many user turns have occurred in the current lesson.
     * Uses optimistic locking with retry logic to handle concurrent updates.
     */
    private fun incrementLessonTurnCount(session: ChatSessionEntity) {
        if (session.currentLessonId == null) {
            // No active lesson, nothing to increment
            return
        }

        val maxRetries = 3
        var attempt = 0

        while (attempt < maxRetries) {
            try {
                // Fetch fresh session to get latest version
                val freshSession = if (attempt > 0) {
                    chatSessionRepository.findById(session.id).orElse(null) ?: run {
                        logger.warn("Session ${session.id} not found during retry $attempt")
                        return
                    }
                } else {
                    session
                }

                freshSession.lessonProgressTurnCount = freshSession.lessonProgressTurnCount + 1
                chatSessionRepository.save(freshSession)

                logger.debug("Incremented lesson turn count for session ${freshSession.id}, lesson ${freshSession.currentLessonId}: ${freshSession.lessonProgressTurnCount}")
                return // Success!

            } catch (e: jakarta.persistence.OptimisticLockException) {
                attempt++
                if (attempt < maxRetries) {
                    val backoffMs = (50L * (1 shl attempt)) // Exponential backoff: 100ms, 200ms
                    logger.debug("Optimistic lock conflict on session ${session.id}, retrying in ${backoffMs}ms (attempt $attempt/$maxRetries)")
                    Thread.sleep(backoffMs)
                } else {
                    logger.error("Failed to increment lesson turn count after $maxRetries attempts due to optimistic locking", e)
                }
            } catch (e: Exception) {
                logger.error("Failed to increment lesson turn count for session ${session.id}", e)
                // For parsing errors, try to reset with valid JSON (only on first attempt)
                if (attempt == 0 && e !is jakarta.persistence.OptimisticLockException) {
                    try {
                        val freshSession = chatSessionRepository.findById(session.id).orElse(null)
                        if (freshSession != null) {
                            freshSession.lessonProgressTurnCount = 1
                            freshSession.lessonProgressGoalsCompleted = false
                            chatSessionRepository.save(freshSession)
                        }
                    } catch (resetException: Exception) {
                        logger.error("Failed to reset progress JSON for session ${session.id}", resetException)
                    }
                }
                return // Don't retry for non-locking exceptions
            }
        }
    }
}
