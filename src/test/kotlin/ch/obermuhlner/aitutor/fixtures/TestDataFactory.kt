package ch.obermuhlner.aitutor.fixtures

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.SendMessageRequest
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import java.util.UUID

object TestDataFactory {

    val TEST_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val TEST_SESSION_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    fun createSessionRequest(
        userId: UUID = TEST_USER_ID,
        tutorName: String = "TestTutor",
        sourceLanguage: String = "English",
        targetLanguage: String = "Spanish",
        estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1
    ) = CreateSessionRequest(
        userId = userId,
        tutorName = tutorName,
        tutorPersona = "patient coach",
        tutorDomain = "general conversation",
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        estimatedCEFRLevel = estimatedCEFRLevel
    )

    fun createSessionEntity(
        id: UUID = TEST_SESSION_ID,
        userId: UUID = TEST_USER_ID,
        tutorName: String = "TestTutor",
        sourceLanguage: String = "English",
        targetLanguage: String = "Spanish"
    ) = ChatSessionEntity(
        id = id,
        userId = userId,
        tutorName = tutorName,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        conversationPhase = ConversationPhase.Free,
        estimatedCEFRLevel = CEFRLevel.A1
    )

    fun createMessageEntity(
        session: ChatSessionEntity,
        role: MessageRole = MessageRole.USER,
        content: String = "Test message"
    ) = ChatMessageEntity(
        session = session,
        role = role,
        content = content
    )

    fun sendMessageRequest(
        content: String = "Hola, como estas?"
    ) = SendMessageRequest(content = content)
}
