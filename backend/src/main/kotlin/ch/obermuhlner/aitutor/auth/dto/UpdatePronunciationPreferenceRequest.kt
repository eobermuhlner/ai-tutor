package ch.obermuhlner.aitutor.auth.dto

import ch.obermuhlner.aitutor.user.domain.PronunciationPreference

data class UpdatePronunciationPreferenceRequest(
    val pronunciationPreference: PronunciationPreference
)