package ch.obermuhlner.aitutor.chat.controller

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
    private val chatService: ChatService
) {

    @PostMapping("/sessions")
    fun createSession(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        val session = chatService.createSession(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @GetMapping("/sessions")
    fun getUserSessions(@RequestParam userId: UUID): ResponseEntity<List<SessionResponse>> {
        val sessions = chatService.getUserSessions(userId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSession(@PathVariable sessionId: UUID): ResponseEntity<SessionWithMessagesResponse> {
        val session = chatService.getSessionWithMessages(sessionId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/sessions/{sessionId}")
    fun deleteSession(@PathVariable sessionId: UUID): ResponseEntity<Void> {
        chatService.deleteSession(sessionId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/sessions/{sessionId}/phase")
    fun updateSessionPhase(
        @PathVariable sessionId: UUID,
        @RequestBody request: UpdatePhaseRequest
    ): ResponseEntity<SessionResponse> {
        val session = chatService.updateSessionPhase(sessionId, request.phase)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }

    @PostMapping("/sessions/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: UUID,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<MessageResponse> {
        val message = chatService.sendMessage(sessionId, request.content)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(message)
    }

    @PostMapping("/sessions/{sessionId}/messages/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sendMessageStream(
        @PathVariable sessionId: UUID,
        @RequestBody request: SendMessageRequest
    ): SseEmitter {
        val emitter = SseEmitter(30_000L)

        Thread {
            try {
                val message = chatService.sendMessage(sessionId, request.content) { chunk ->
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
