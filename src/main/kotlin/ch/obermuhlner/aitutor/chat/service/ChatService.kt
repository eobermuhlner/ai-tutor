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
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.tutor.service.PhaseDecision
import ch.obermuhlner.aitutor.tutor.service.TopicDecision
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
    private val vocabularyService: VocabularyService,
    private val vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService,
    private val phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService,
    private val topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val errorAnalyticsService: ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService,
    private val userLanguageService: ch.obermuhlner.aitutor.user.service.UserLanguageService,
    private val objectMapper: ObjectMapper,
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
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        // Validate ownership
        if (session.userId != currentUserId) {
            return null
        }

        // Calculate next sequence number
        val maxSequence = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .maxOfOrNull { it.sequenceNumber } ?: -1
        val nextSequence = maxSequence + 1

        // Save user message
        val userMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = userContent,
            sequenceNumber = nextSequence
        )
        chatMessageRepository.save(userMessage)

        // Build message history
        val messageHistory = buildMessageHistory(sessionId)

        // Call tutor service
        val tutor = Tutor(
            name = session.tutorName,
            persona = session.tutorPersona,
            domain = session.tutorDomain,
            teachingStyle = session.tutorTeachingStyle,
            sourceLanguageCode = session.sourceLanguageCode,
            targetLanguageCode = session.targetLanguageCode,
            gender = session.tutorGender,
            age = session.tutorAge
        )

        // Initialize effectivePhase if null (migration case)
        if (session.effectivePhase == null) {
            session.effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
                ConversationPhase.Correction  // Default for Auto mode
            } else {
                session.conversationPhase
            }
        }

        // Resolve effective phase and compute decision metadata
        val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)

        // Compute phase decision with metadata
        val phaseDecision = if (session.conversationPhase == ConversationPhase.Auto) {
            phaseDecisionService.decidePhase(session.conversationPhase, allMessages)
        } else {
            // For manually set phases, create PhaseDecision wrapper
            PhaseDecision(
                phase = session.conversationPhase,
                reason = "User-selected phase",
                severityScore = 0.0
            )
        }

        // Compute topic metadata for prompt context (LLM hasn't responded yet, use current topic)
        val topicMetadata = topicDecisionService.decideTopic(
            currentTopic = session.currentTopic,
            llmProposedTopic = session.currentTopic,
            recentMessages = allMessages,
            pastTopicsJson = session.pastTopicsJson
        )

        // Get due vocabulary count if review mode enabled
        val dueCount = if (session.vocabularyReviewMode) {
            val count = vocabularyReviewService.getDueCount(session.userId, session.targetLanguageCode)
            if (count > 0) {
                logger.info("Vocabulary review mode active: session=$sessionId, dueCount=$count")
            }
            count
        } else {
            null
        }

        // Pass effective phase and metadata to LLM via enriched ConversationState
        val conversationState = ConversationState(
            phase = phaseDecision.phase,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            currentTopic = session.currentTopic,
            phaseReason = phaseDecision.reason,
            topicEligibilityStatus = topicMetadata.eligibilityStatus,
            pastTopics = topicMetadata.pastTopics,
            vocabularyReviewMode = session.vocabularyReviewMode,
            dueVocabularyCount = dueCount
        )

        val tutorResponse = try {
            tutorService.respond(tutor, conversationState, session.userId, messageHistory, session.id, session, onReplyChunk)
        } catch (e: Exception) {
            logger.error("ChatModel call failed for session ${session.id}, user ${session.userId}", e)
            null
        }

        if (tutorResponse == null) {
            // Create an error message response
            val errorMessage = technicalErrorMessage
            val errorAssistantMessage = ChatMessageEntity(
                session = session,
                role = MessageRole.ASSISTANT,
                content = errorMessage,
                sequenceNumber = nextSequence + 1
            )
            val savedAssistantMessage = chatMessageRepository.save(errorAssistantMessage)
            return toMessageResponse(savedAssistantMessage, errorMessage)
        }

        // Update effective phase from PhaseDecisionService, not from LLM response
        // PhaseDecisionService has the fossilization detection logic
        if (session.conversationPhase == ConversationPhase.Auto) {
            session.effectivePhase = phaseDecision.phase
            logger.debug("Auto mode: effectivePhase updated to ${phaseDecision.phase} (${phaseDecision.reason})")
        } else {
            // When user has explicit phase preference, effective phase matches it
            session.effectivePhase = session.conversationPhase
        }
        session.estimatedCEFRLevel = tutorResponse.conversationResponse.conversationState.estimatedCEFRLevel

        // Validate and apply topic change from LLM with hysteresis
        val llmProposedTopic = tutorResponse.conversationResponse.conversationState.currentTopic
        val topicDecision = topicDecisionService.decideTopic(
            currentTopic = session.currentTopic,
            llmProposedTopic = llmProposedTopic,
            recentMessages = allMessages,
            pastTopicsJson = session.pastTopicsJson
        )

        // Handle topic changes
        if (topicDecision.topic != session.currentTopic) {
            // Topic changed - archive old topic if it was sustained long enough
            if (session.currentTopic != null) {
                if (topicDecisionService.shouldArchiveTopic(session.currentTopic, topicDecision.turnCount)) {
                    archiveTopic(session, session.currentTopic!!)
                }
            }
            session.currentTopic = topicDecision.topic
        }

        chatSessionRepository.save(session)

        // Save assistant message with next sequence number
        val assistantMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = tutorResponse.reply,
            correctionsJson = if (tutorResponse.conversationResponse.corrections.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.corrections) else null,
            vocabularyJson = if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.newVocabulary) else null,
            wordCardsJson = if (tutorResponse.conversationResponse.wordCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.wordCards) else null,
            sequenceNumber = nextSequence + 1
        )
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        // Track vocabulary
        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            vocabularyService.addNewVocabulary(
                userId = session.userId,
                lang = session.targetLanguageCode,
                items = tutorResponse.conversationResponse.newVocabulary.map {
                    NewVocabularyDTO(it.lemma, it.context, it.conceptName)
                },
                turnId = savedAssistantMessage.id
            )
        }

        // Record error patterns for analytics
        if (tutorResponse.conversationResponse.corrections.isNotEmpty()) {
            try {
                errorAnalyticsService.recordErrors(
                    userId = session.userId,
                    lang = session.targetLanguageCode,
                    messageId = savedAssistantMessage.id,
                    corrections = tutorResponse.conversationResponse.corrections
                )
            } catch (e: Exception) {
                logger.error("Failed to record error patterns", e)
                // Don't fail message sending if analytics fails
            }
        }

        return toMessageResponse(savedAssistantMessage)
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
        val tutor = catalogService.getTutorById(tutorProfileId) ?: return null

        // For course-based sessions, determine the initial CEFR level: use user's proficiency if available, 
        // otherwise fall back to the course starting level
        val initialCEFRLevel = determineInitialCEFRLevel(userId, tutor.targetLanguageCode, course.startingLevel)

        // Determine source language from user's native language profile as per original design
        val resolvedSourceLanguageCode = userLanguageService.suggestSourceLanguage(userId, tutor.targetLanguageCode)

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = tutor.name,
            tutorPersona = tutor.personaEnglish,
            tutorDomain = tutor.domainEnglish,
            tutorTeachingStyle = tutor.teachingStyle,
            tutorAge = tutor.age,
            tutorImage = "/api/v1/images/tutor/${tutor.id}/data",
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

    private fun buildMessageHistory(sessionId: UUID): List<Message> {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .map { entity ->
                when (entity.role) {
                    MessageRole.USER -> UserMessage(entity.content)
                    MessageRole.ASSISTANT -> AssistantMessage(entity.content)
                }
            }
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
                imageUrl = vocab.conceptName?.let { "/api/v1/images/concept/$it/data" }
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
                imageUrl = card.conceptName?.let { "/api/v1/images/concept/$it/data" }
            )
        }

        return MessageResponse(
            id = entity.id,
            role = entity.role.name,
            content = entity.content,
            corrections = corrections,
            newVocabulary = vocabularyWithImages,
            wordCards = wordCardsWithImages,
            errorMessage = errorMessage,
            createdAt = entity.createdAt ?: java.time.Instant.now()
        )
    }
}
