package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase

data class UpdatePhaseRequest(
    val phase: ConversationPhase
)
