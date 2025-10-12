package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyContextResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemWithContextsResponse
import ch.obermuhlner.aitutor.vocabulary.dto.toResponse
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/vocabulary")
class VocabularyController(
    private val vocabularyQueryService: VocabularyQueryService,
    private val vocabularyReviewService: VocabularyReviewService,
    private val authorizationService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getUserVocabulary(
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) lang: String?
    ): ResponseEntity<List<VocabularyItemResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val items = vocabularyQueryService.getUserVocabulary(resolvedUserId, lang)

        val response = items.map { it.toResponse() }
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
            contexts = contexts,
            imageUrl = result.item.conceptName?.let { "/api/v1/images/concept/$it/data" }
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get due vocabulary items for review in a specific language.
     * GET /api/v1/vocabulary/due?lang=es&limit=20
     */
    @GetMapping("/due")
    fun getDueVocabulary(
        @RequestParam lang: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<VocabularyItemResponse>> {
        val userId = authorizationService.getCurrentUserId()
        val items = vocabularyReviewService.getDueVocabulary(userId, lang, limit)
        logger.info("GET /due: user=$userId, lang=$lang, limit=$limit, returned=${items.size} items")
        return ResponseEntity.ok(items.map { it.toResponse() })
    }

    /**
     * Get count of due vocabulary items in a specific language.
     * GET /api/v1/vocabulary/due/count?lang=es
     */
    @GetMapping("/due/count")
    fun getDueCount(
        @RequestParam lang: String
    ): ResponseEntity<DueCountResponse> {
        val userId = authorizationService.getCurrentUserId()
        val count = vocabularyReviewService.getDueCount(userId, lang)
        logger.info("GET /due/count: user=$userId, lang=$lang, count=$count")
        return ResponseEntity.ok(DueCountResponse(count))
    }

    /**
     * Record vocabulary review result.
     * POST /api/v1/vocabulary/{itemId}/review
     * Body: { "success": true }
     */
    @PostMapping("/{itemId}/review")
    fun recordReview(
        @PathVariable itemId: UUID,
        @RequestBody request: RecordReviewRequest
    ): ResponseEntity<VocabularyItemResponse> {
        val userId = authorizationService.getCurrentUserId()

        // Verify ownership
        val item = vocabularyQueryService.getVocabularyItemById(itemId)
        if (item.userId != userId) {
            logger.warn("POST /{itemId}/review: Access denied for user=$userId, itemId=$itemId (not owner)")
            return ResponseEntity.status(403).build()
        }

        val updated = vocabularyReviewService.recordReview(itemId, request.success)
        logger.info("POST /{itemId}/review: user=$userId, itemId=$itemId, success=${request.success}")
        return ResponseEntity.ok(updated.toResponse())
    }
}

data class RecordReviewRequest(val success: Boolean)
data class DueCountResponse(val count: Long)
