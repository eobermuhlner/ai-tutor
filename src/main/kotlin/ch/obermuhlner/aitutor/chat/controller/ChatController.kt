package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.chat.service.ChatService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val authorizationService: AuthorizationService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val chatSessionRepository: ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
) {

    @PostMapping("/sessions")
    fun createSession(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        // Validate that user is creating session for themselves (or admin can create for anyone)
        authorizationService.requireAccessToUser(request.userId)
        val session = chatService.createSession(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PostMapping("/sessions/from-course")
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
    fun getUserSessions(@RequestParam(required = false) userId: UUID?): ResponseEntity<List<SessionResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val sessions = chatService.getUserSessions(resolvedUserId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/sessions/active")
    fun getActiveLearningSessions(@RequestParam(required = false) userId: UUID?): ResponseEntity<List<SessionWithProgressResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val sessions = chatService.getActiveLearningSessions(resolvedUserId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSession(@PathVariable sessionId: UUID): ResponseEntity<SessionWithMessagesResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val session = chatService.getSessionWithMessages(sessionId, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/sessions/{sessionId}")
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

    @GetMapping("/sessions/{sessionId}/topics/history")
    fun getTopicHistory(@PathVariable sessionId: UUID): ResponseEntity<TopicHistoryResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        val history = chatService.getTopicHistory(sessionId, currentUserId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(history)
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

        Thread {
            try {
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
            }
        }.start()

        return emitter
    }
}
