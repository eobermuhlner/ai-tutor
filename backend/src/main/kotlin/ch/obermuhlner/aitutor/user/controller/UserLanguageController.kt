package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.dto.AddLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UpdateLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UserLanguageProficiencyResponse
import ch.obermuhlner.aitutor.user.service.UserLanguageService
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserLanguageController(
    private val userLanguageService: UserLanguageService,
    private val authorizationService: AuthorizationService
) {

    @GetMapping("/{userId}/languages")
    fun getUserLanguages(
        @PathVariable userId: UUID,
        @RequestParam(required = false) type: LanguageProficiencyType?,
        authentication: Authentication
    ): List<UserLanguageProficiencyResponse> {
        authorizationService.requireAccessToUser(userId)
        val languages = if (type == LanguageProficiencyType.Native) {
            userLanguageService.getNativeLanguages(userId)
        } else if (type == LanguageProficiencyType.Learning) {
            userLanguageService.getLearningLanguages(userId)
        } else {
            userLanguageService.getUserLanguages(userId)
        }
        return languages.map { it.toResponse() }
    }

    @PostMapping("/{userId}/languages")
    @ResponseStatus(HttpStatus.CREATED)
    fun addLanguage(
        @PathVariable userId: UUID,
        @RequestBody request: AddLanguageRequest,
        authentication: Authentication
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        return userLanguageService.addLanguage(
            userId = userId,
            languageCode = request.languageCode,
            type = request.type,
            cefrLevel = request.cefrLevel,
            isNative = request.isNative
        ).toResponse()
    }

    @PatchMapping("/{userId}/languages/{languageCode}")
    fun updateLanguageLevel(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        @RequestBody request: UpdateLanguageRequest,
        authentication: Authentication
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        return userLanguageService.updateLanguage(userId, languageCode, request.cefrLevel).toResponse()
    }

    @PostMapping("/{userId}/languages/{languageCode}/primary")
    fun setPrimaryTargetLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        authentication: Authentication
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        userLanguageService.setPrimaryLanguage(userId, languageCode)
        // Return the updated language
        return userLanguageService.getUserLanguages(userId)
            .find { it.languageCode == languageCode && it.isPrimary }!!
            .toResponse()
    }

    @DeleteMapping("/{userId}/languages/{languageCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        authentication: Authentication
    ) {
        authorizationService.requireAccessToUser(userId)
        userLanguageService.removeLanguage(userId, languageCode)
    }

    private fun ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity.toResponse() =
        UserLanguageProficiencyResponse(
            id = id,
            userId = userId,
            languageCode = languageCode,
            proficiencyType = proficiencyType,
            cefrLevel = cefrLevel,
            isNative = isNative,
            isPrimary = isPrimary,
            selfAssessed = selfAssessed,
            lastAssessedAt = lastAssessedAt,
            createdAt = createdAt ?: java.time.Instant.now(),
            updatedAt = updatedAt ?: java.time.Instant.now()
        )
}
