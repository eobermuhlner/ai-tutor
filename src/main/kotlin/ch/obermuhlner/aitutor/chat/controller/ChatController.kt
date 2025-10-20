package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.CreateSessionFromCourseRequest
import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.MessageResponse
import ch.obermuhlner.aitutor.chat.dto.SendMessageRequest
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse
import ch.obermuhlner.aitutor.chat.dto.SessionWithProgressResponse
import ch.obermuhlner.aitutor.chat.dto.TopicHistoryResponse
import ch.obermuhlner.aitutor.chat.dto.UpdatePhaseRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateTopicRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateVocabularyReviewModeRequest
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.conversation.dto.SynthesizeRequest
import ch.obermuhlner.aitutor.conversation.dto.VoiceListResponse
import ch.obermuhlner.aitutor.conversation.service.AiAudioService
import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Endpoints for chat sessions and messages")
class ChatController(
    private val chatService: ChatService,
    private val authorizationService: AuthorizationService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val chatSessionRepository: ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository,
    private val audioService: AiAudioService,
    private val audioProperties: AudioProperties
) {

    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session", description = "Creates a new chat session for the specified user")
    fun createSession(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        // Validate that user is creating session for themselves (or admin can create for anyone)
        authorizationService.requireAccessToUser(request.userId)
        val session = chatService.createSession(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PostMapping("/sessions/from-course")
    @Operation(summary = "Create a session from a course", description = "Creates a new chat session based on a specific course template")
    fun createSessionFromCourse(@RequestBody request: CreateSessionFromCourseRequest): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()

        // Get tutor ID - either from request or first suggested tutor from course
        val tutorId = request.tutorProfileId ?: run {
            catalogService.getTutorsForCourse(request.courseTemplateId).firstOrNull()?.id
                ?: return ResponseEntity.badRequest().build()
        }

        // Get source language - for now use English, should be from user profile
        val sourceLanguageCode = "en"

        val session = chatService.createSessionFromCourse(
            userId = currentUserId,
            courseTemplateId = request.courseTemplateId,
            tutorProfileId = tutorId,
            sourceLanguageCode = sourceLanguageCode,
            customName = request.customName
        ) ?: return ResponseEntity.badRequest().build()

        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @GetMapping("/sessions/{sessionId}/progress")
    @Operation(summary = "Get session progress", description = "Retrieves the progress information for a specific chat session")
    fun getSessionProgress(@PathVariable sessionId: UUID): ResponseEntity<SessionWithProgressResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.getSession(sessionId) ?: return ResponseEntity.notFound().build()

        // Validate ownership
        if (session.userId != currentUserId) {
            return ResponseEntity.notFound().build()
        }

        val progress = chatService.getSessionProgress(sessionId)
        return ResponseEntity.ok(SessionWithProgressResponse(session, progress))
    }

    @PostMapping("/sessions/{sessionId}/deactivate")
    @Operation(summary = "Deactivate a session", description = "Marks a session as inactive without deleting it")
    fun deactivateSession(@PathVariable sessionId: UUID): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()

        // Get session entity and validate ownership
        val sessionEntity = chatSessionRepository.findById(sessionId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (sessionEntity.userId != currentUserId) {
            return ResponseEntity.notFound().build()
        }

        // Deactivate
        sessionEntity.isActive = false
        val saved = chatSessionRepository.save(sessionEntity)

        // Convert to response
        return ResponseEntity.ok(chatService.getSession(saved.id))
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get user sessions", description = "Retrieves all chat sessions for the specified user")
    fun getUserSessions(@RequestParam(required = false) userId: UUID?): ResponseEntity<List<SessionResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val sessions = chatService.getUserSessions(resolvedUserId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/sessions/active")
    @Operation(summary = "Get active learning sessions", description = "Retrieves all active learning sessions for the specified user")
    fun getActiveLearningSessions(@RequestParam(required = false) userId: UUID?): ResponseEntity<List<SessionWithProgressResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val sessions = chatService.getActiveLearningSessions(resolvedUserId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get a specific session", description = "Retrieves a specific chat session along with its messages")
    fun getSession(@PathVariable sessionId: UUID): ResponseEntity<SessionWithMessagesResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.getSessionWithMessages(sessionId, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete a session", description = "Permanently deletes a chat session and all its messages")
    fun deleteSession(@PathVariable sessionId: UUID): ResponseEntity<Void> {
        val currentUserId = authorizationService.getCurrentUserId()
        val deleted = chatService.deleteSession(sessionId, currentUserId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/sessions/{sessionId}/phase")
    fun updateSessionPhase(
        @PathVariable sessionId: UUID,
        @RequestBody request: UpdatePhaseRequest
    ): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.updateSessionPhase(sessionId, request.phase, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @PatchMapping("/sessions/{sessionId}/topic")
    fun updateSessionTopic(
        @PathVariable sessionId: UUID,
        @RequestBody request: UpdateTopicRequest
    ): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.updateSessionTopic(sessionId, request.currentTopic, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @PatchMapping("/sessions/{sessionId}/teaching-style")
    fun updateSessionTeachingStyle(
        @PathVariable sessionId: UUID,
        @RequestBody request: ch.obermuhlner.aitutor.chat.dto.UpdateTeachingStyleRequest
    ): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.updateSessionTeachingStyle(sessionId, request.teachingStyle, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @GetMapping("/sessions/{sessionId}/topics/history")
    fun getTopicHistory(@PathVariable sessionId: UUID): ResponseEntity<TopicHistoryResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val history = chatService.getTopicHistory(sessionId, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(history)
    }

    @PatchMapping("/sessions/{sessionId}/vocabulary-review-mode")
    fun updateVocabularyReviewMode(
        @PathVariable sessionId: UUID,
        @RequestBody request: UpdateVocabularyReviewModeRequest
    ): ResponseEntity<SessionResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.updateVocabularyReviewMode(sessionId, request.enabled, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @PostMapping("/sessions/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: UUID,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<MessageResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val message = chatService.sendMessage(sessionId, request.content, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(message)
    }

    @PostMapping("/sessions/{sessionId}/messages/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sendMessageStream(
        @PathVariable sessionId: UUID,
        @RequestBody request: SendMessageRequest
    ): SseEmitter {
        val currentUserId = authorizationService.getCurrentUserId()
        val emitter = SseEmitter(30_000L)

        // Capture SecurityContext for async processing
        val context = org.springframework.security.core.context.SecurityContextHolder.getContext()

        Thread {
            try {
                // Propagate SecurityContext to async thread
                org.springframework.security.core.context.SecurityContextHolder.setContext(context)

                val message = chatService.sendMessage(sessionId, request.content, currentUserId) { chunk ->
                    try {
                        emitter.send(
                            SseEmitter.event()
                                .name("chunk")
                                .data(chunk)
                        )
                    } catch (e: Exception) {
                        emitter.completeWithError(e)
                    }
                }

                if (message != null) {
                    emitter.send(
                        SseEmitter.event()
                            .name("complete")
                            .data(message)
                    )
                    emitter.complete()
                } else {
                    emitter.completeWithError(RuntimeException("Failed to send message"))
                }
            } catch (e: Exception) {
                emitter.completeWithError(e)
            } finally {
                // Clear SecurityContext from thread
                org.springframework.security.core.context.SecurityContextHolder.clearContext()
            }
        }.start()

        return emitter
    }

    // ========== Text-to-Speech Endpoints ==========

    @PostMapping("/synthesize", produces = ["audio/mpeg"])
    @Operation(summary = "Synthesize speech from text", description = "Converts arbitrary text to speech audio using specified voice")
    fun synthesizeSpeech(@RequestBody request: SynthesizeRequest): ResponseEntity<ByteArray> {
        if (!audioService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val voiceEnum = request.voiceId?.let {
            try {
                TutorVoice.valueOf(it)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }
        }

        // For arbitrary text synthesis, gender defaults to null (uses gender-neutral mapping)
        val audioBytes = try {
            audioService.synthesizeSpeech(
                text = request.text,
                voiceId = voiceEnum,
                gender = null,
                speed = request.speed
            )
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
            .body(audioBytes)
    }

    @PostMapping("/sessions/{sessionId}/messages/{messageId}/audio", produces = ["audio/mpeg"])
    @Operation(summary = "Synthesize audio for a message", description = "Generates speech audio for a specific chat message using the tutor's voice")
    fun synthesizeMessageAudio(
        @PathVariable sessionId: UUID,
        @PathVariable messageId: UUID,
        @RequestParam(required = false) speed: Double?
    ): ResponseEntity<ByteArray> {
        if (!audioService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val currentUserId = authorizationService.getCurrentUserId()

        // Get session and validate ownership
        val sessionEntity = chatSessionRepository.findById(sessionId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (sessionEntity.userId != currentUserId) {
            return ResponseEntity.notFound().build()
        }

        // Get message
        val messageEntity = chatService.getMessage(sessionId, messageId)
            ?: return ResponseEntity.notFound().build()

        // Get tutor voice and gender from session
        val tutorVoice = sessionEntity.tutorVoiceId
        val tutorGender = sessionEntity.tutorGender

        // Build composite cache key: voice-gender (e.g., "Warm-Female")
        val voiceIdString = if (tutorVoice != null && tutorGender != null) {
            "${tutorVoice.name}-${tutorGender.name}"
        } else {
            tutorVoice?.name
        }

        // Check cache: audio is valid if voiceId and speed match
        val cachedAudio = messageEntity.audioData
        val cachedVoiceId = messageEntity.audioVoiceId
        val cachedSpeed = messageEntity.audioSpeed

        if (cachedAudio != null && cachedVoiceId == voiceIdString && cachedSpeed == speed) {
            // Cache hit - return cached audio
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"message-${messageId}.mp3\"")
                .body(cachedAudio)
        }

        // Cache miss - generate audio with gender-aware voice selection
        val audioBytes = try {
            audioService.synthesizeSpeech(
                text = messageEntity.content,
                voiceId = tutorVoice,
                gender = tutorGender,
                languageCode = sessionEntity.targetLanguageCode,
                speed = speed
            )
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        // Save to cache with composite key
        chatService.updateMessageAudioCache(sessionId, messageId, audioBytes, voiceIdString, speed)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"message-${messageId}.mp3\"")
            .body(audioBytes)
    }

    @GetMapping("/audio/voices")
    @Operation(summary = "Get available voices", description = "Lists all available abstract voices and their provider-specific mappings")
    fun getAvailableVoices(): ResponseEntity<VoiceListResponse> {
        if (!audioService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val response = VoiceListResponse(
            abstractVoices = TutorVoice.values().map { it.name },
            voiceMappings = audioService.getVoiceMappings(),
            defaultVoice = audioProperties.defaultVoice
        )

        return ResponseEntity.ok(response)
    }
}
