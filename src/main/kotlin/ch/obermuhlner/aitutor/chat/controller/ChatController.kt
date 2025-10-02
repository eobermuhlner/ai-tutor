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
    private val authorizationService: AuthorizationService
) {

    @PostMapping("/sessions")
    fun createSession(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        // Validate that user is creating session for themselves (or admin can create for anyone)
        authorizationService.requireAccessToUser(request.userId)
        val session = chatService.createSession(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @GetMapping("/sessions")
    fun getUserSessions(@RequestParam(required = false) userId: UUID?): ResponseEntity<List<SessionResponse>> {
        // Resolve userId: use authenticated user's ID or validate admin access to requested user
        val resolvedUserId = authorizationService.resolveUserId(userId)
        val sessions = chatService.getUserSessions(resolvedUserId)
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
