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
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val tutorService: TutorService,
    private val vocabularyService: VocabularyService,
    private val phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService,
    private val topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse {
        logger.info("Creating chat session for user ${request.userId}: ${request.tutorName} (${request.targetLanguageCode})")

        val session = ChatSessionEntity(
            userId = request.userId,
            tutorName = request.tutorName,
            tutorPersona = request.tutorPersona,
            tutorDomain = request.tutorDomain,
            sourceLanguageCode = request.sourceLanguageCode,
            targetLanguageCode = request.targetLanguageCode,
            conversationPhase = request.conversationPhase,
            effectivePhase = if (request.conversationPhase == ConversationPhase.Auto) ConversationPhase.Correction else request.conversationPhase,
            estimatedCEFRLevel = request.estimatedCEFRLevel,
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

        // Save user message
        val userMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = userContent
        )
        chatMessageRepository.save(userMessage)

        // Build message history
        val messageHistory = buildMessageHistory(sessionId)

        // Call tutor service
        val tutor = Tutor(
            name = session.tutorName,
            persona = session.tutorPersona,
            domain = session.tutorDomain,
            sourceLanguageCode = session.sourceLanguageCode,
            targetLanguageCode = session.targetLanguageCode
        )

        // Initialize effectivePhase if null (migration case)
        if (session.effectivePhase == null) {
            session.effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
                ConversationPhase.Correction  // Default for Auto mode
            } else {
                session.conversationPhase
            }
        }

        // Resolve effective phase: if Auto, decide based on history; otherwise use user preference
        val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        val effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
            phaseDecisionService.decidePhase(session.conversationPhase, allMessages)
        } else {
            session.conversationPhase
        }

        // Pass effective phase to LLM (never pass Auto - always resolved Free/Correction/Drill)
        val conversationState = ConversationState(
            phase = effectivePhase,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            currentTopic = session.currentTopic
        )

        val tutorResponse = tutorService.respond(tutor, conversationState, session.userId, messageHistory, session.id, onReplyChunk)
            ?: return null

        // Update effective phase from LLM response only if in Auto mode
        // User-controlled phase (conversationPhase) never changes automatically
        if (session.conversationPhase == ConversationPhase.Auto) {
            session.effectivePhase = tutorResponse.conversationResponse.conversationState.phase
        } else {
            // When user has explicit phase preference, effective phase matches it
            session.effectivePhase = session.conversationPhase
        }
        session.estimatedCEFRLevel = tutorResponse.conversationResponse.conversationState.estimatedCEFRLevel

        // Validate and apply topic change from LLM with hysteresis
        val llmProposedTopic = tutorResponse.conversationResponse.conversationState.currentTopic
        val validatedTopic = topicDecisionService.decideTopic(
            currentTopic = session.currentTopic,
            llmProposedTopic = llmProposedTopic,
            recentMessages = allMessages,
            pastTopicsJson = session.pastTopicsJson
        )

        // Handle topic changes
        if (validatedTopic != session.currentTopic) {
            // Topic changed - archive old topic if it was sustained long enough
            if (session.currentTopic != null) {
                val turnCount = topicDecisionService.countTurnsInRecentMessages(allMessages)
                if (topicDecisionService.shouldArchiveTopic(session.currentTopic, turnCount)) {
                    archiveTopic(session, session.currentTopic!!)
                }
            }
            session.currentTopic = validatedTopic
        }

        chatSessionRepository.save(session)

        // Save assistant message
        val assistantMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = tutorResponse.reply,
            correctionsJson = if (tutorResponse.conversationResponse.corrections.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.corrections) else null,
            vocabularyJson = if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.newVocabulary) else null,
            wordCardsJson = if (tutorResponse.conversationResponse.wordCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.wordCards) else null
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

        return toMessageResponse(savedAssistantMessage)
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

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = tutor.name,
            tutorPersona = tutor.personaEnglish,
            tutorDomain = tutor.domainEnglish,
            sourceLanguageCode = sourceLanguageCode,
            targetLanguageCode = tutor.targetLanguageCode,
            conversationPhase = course.defaultPhase,
            effectivePhase = if (course.defaultPhase == ConversationPhase.Auto) ConversationPhase.Correction else course.defaultPhase,
            estimatedCEFRLevel = course.startingLevel,
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
            sourceLanguageCode = entity.sourceLanguageCode,
            targetLanguageCode = entity.targetLanguageCode,
            conversationPhase = entity.conversationPhase,
            effectivePhase = effectivePhase,
            estimatedCEFRLevel = entity.estimatedCEFRLevel,
            currentTopic = entity.currentTopic,
            courseTemplateId = entity.courseTemplateId,
            tutorProfileId = entity.tutorProfileId,
            customName = entity.customName,
            isActive = entity.isActive,
            createdAt = entity.createdAt ?: java.time.Instant.now(),
            updatedAt = entity.updatedAt ?: java.time.Instant.now()
        )
    }

    private fun toMessageResponse(entity: ChatMessageEntity): MessageResponse {
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
            createdAt = entity.createdAt ?: java.time.Instant.now()
        )
    }
}
