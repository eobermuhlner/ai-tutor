package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.ConversationState
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.Tutor
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.NewVocabularyDTO
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ChatService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val tutorService: TutorService,
    private val vocabularyService: VocabularyService,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse {
        val session = ChatSessionEntity(
            userId = request.userId,
            tutorName = request.tutorName,
            tutorPersona = request.tutorPersona,
            tutorDomain = request.tutorDomain,
            sourceLanguageCode = request.sourceLanguageCode,
            targetLanguageCode = request.targetLanguageCode,
            estimatedCEFRLevel = request.estimatedCEFRLevel
        )
        val saved = chatSessionRepository.save(session)
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

    fun getSessionWithMessages(sessionId: UUID): SessionWithMessagesResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null
        val messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .map { toMessageResponse(it) }

        return SessionWithMessagesResponse(
            session = toSessionResponse(session),
            messages = messages
        )
    }

    @Transactional
    fun deleteSession(sessionId: UUID) {
        chatSessionRepository.deleteById(sessionId)
    }

    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

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

        val conversationState = ConversationState(
            phase = session.conversationPhase,
            estimatedCEFRLevel = session.estimatedCEFRLevel
        )

        val tutorResponse = tutorService.respond(tutor, conversationState, session.userId, messageHistory, onReplyChunk)
            ?: return null

        // Update session state
        session.conversationPhase = tutorResponse.conversationResponse.conversationState.phase
        session.estimatedCEFRLevel = tutorResponse.conversationResponse.conversationState.estimatedCEFRLevel
        chatSessionRepository.save(session)

        // Save assistant message
        val assistantMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = tutorResponse.reply,
            correctionsJson = if (tutorResponse.conversationResponse.corrections.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.corrections) else null,
            vocabularyJson = if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.newVocabulary) else null
        )
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        // Track vocabulary
        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            vocabularyService.addNewVocabulary(
                userId = session.userId,
                lang = session.targetLanguageCode,
                items = tutorResponse.conversationResponse.newVocabulary.map { NewVocabularyDTO(it.lemma, it.context) },
                turnId = savedAssistantMessage.id
            )
        }

        return toMessageResponse(savedAssistantMessage)
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

    private fun toSessionResponse(entity: ChatSessionEntity): SessionResponse {
        return SessionResponse(
            id = entity.id,
            userId = entity.userId,
            tutorName = entity.tutorName,
            tutorPersona = entity.tutorPersona,
            tutorDomain = entity.tutorDomain,
            sourceLanguageCode = entity.sourceLanguageCode,
            targetLanguageCode = entity.targetLanguageCode,
            conversationPhase = entity.conversationPhase,
            estimatedCEFRLevel = entity.estimatedCEFRLevel,
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

        return MessageResponse(
            id = entity.id,
            role = entity.role.name,
            content = entity.content,
            corrections = corrections,
            newVocabulary = vocabulary,
            createdAt = entity.createdAt ?: java.time.Instant.now()
        )
    }
}
