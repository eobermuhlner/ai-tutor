package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.Correction

data class UpdateCorrectionsRequest(
    val corrections: List<Correction>
)
