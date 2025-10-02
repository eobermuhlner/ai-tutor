package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyContextResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemWithContextsResponse
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/vocabulary")
class VocabularyController(
    private val vocabularyQueryService: VocabularyQueryService,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    fun getUserVocabulary(
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) lang: String?
    ): ResponseEntity<List<VocabularyItemResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val items = vocabularyQueryService.getUserVocabulary(resolvedUserId, lang)

        val response = items.map { item ->
            VocabularyItemResponse(
                id = item.id,
                lemma = item.lemma,
                lang = item.lang,
                exposures = item.exposures,
                lastSeenAt = item.lastSeenAt,
                createdAt = item.createdAt ?: item.lastSeenAt
            )
        }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{itemId}")
    fun getVocabularyItemWithContexts(
        @PathVariable itemId: UUID
    ): ResponseEntity<VocabularyItemWithContextsResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val result = vocabularyQueryService.getVocabularyItemWithContexts(itemId, currentUserId)
            ?: return ResponseEntity.notFound().build()

        val contexts = result.contexts.map { ctx ->
            VocabularyContextResponse(
                context = ctx.context,
                turnId = ctx.turnId
            )
        }

        val response = VocabularyItemWithContextsResponse(
            id = result.item.id,
            lemma = result.item.lemma,
            lang = result.item.lang,
            exposures = result.item.exposures,
            lastSeenAt = result.item.lastSeenAt,
            createdAt = result.item.createdAt ?: result.item.lastSeenAt,
            contexts = contexts
        )

        return ResponseEntity.ok(response)
    }
}
