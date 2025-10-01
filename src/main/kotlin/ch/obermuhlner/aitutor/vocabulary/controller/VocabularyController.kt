package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyContextResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemWithContextsResponse
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/vocabulary")
class VocabularyController(
    private val vocabularyItemRepository: VocabularyItemRepository,
    private val vocabularyContextRepository: VocabularyContextRepository
) {

    @GetMapping
    fun getUserVocabulary(
        @RequestParam userId: UUID,
        @RequestParam(required = false) lang: String?
    ): ResponseEntity<List<VocabularyItemResponse>> {
        val items = if (lang != null) {
            vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(userId, lang)
        } else {
            vocabularyItemRepository.findByUserIdOrderByLastSeenAtDesc(userId)
        }

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
        val item = vocabularyItemRepository.findById(itemId)
            .orElse(null) ?: return ResponseEntity.notFound().build()

        val contexts = vocabularyContextRepository.findByVocabItemId(itemId).map { ctx ->
            VocabularyContextResponse(
                context = ctx.context,
                turnId = ctx.turnId
            )
        }

        val response = VocabularyItemWithContextsResponse(
            id = item.id,
            lemma = item.lemma,
            lang = item.lang,
            exposures = item.exposures,
            lastSeenAt = item.lastSeenAt,
            createdAt = item.createdAt ?: item.lastSeenAt,
            contexts = contexts
        )

        return ResponseEntity.ok(response)
    }
}
