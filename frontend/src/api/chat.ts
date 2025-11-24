import apiClient from './client';
import { CEFRLevel, TeachingStyle, ConversationPhase } from '../types';
import type { Session, Message, SessionProgress, InitiateMessageContext } from '../types';
import {
  transformBackendMessage,
  transformCorrection,
  type BackendMessageResponse,
  type BackendCorrection,
} from '../utils/messageTransformer';
import * as storage from '../utils/storage';
import { API_BASE_URL } from '../utils/constants';

export async function createSessionFromCourse(
  courseId: string,
  tutorProfileId?: string,
  customName?: string
): Promise<Session> {
  const response = await apiClient.post<Session>('/chat/sessions/from-course', {
    courseTemplateId: courseId,
    tutorProfileId,
    customName,
  });
  return response.data;
}

export async function getSessions(userId: string): Promise<Session[]> {
  const response = await apiClient.get<Session[]>(`/chat/sessions`, {
    params: { userId },
  });
  return response.data;
}

interface SessionWithProgressResponse {
  session: {
    id: string;
    userId: string;
    tutorName: string;
    targetLanguageCode: string;
    conversationPhase: string;
    effectivePhase: string;
    estimatedCEFRLevel: string;
    currentTopic: string | null;
    courseTemplateId: string | null;
    customName: string | null; // NEW: Include custom name field
    createdAt: string;
    updatedAt: string;
  };
  progress: {
    messageCount: number;
    vocabularyCount: number;
    daysActive: number;
  };
}

export async function getActiveSessions(userId: string): Promise<Session[]> {
  const response = await apiClient.get<SessionWithProgressResponse[]>('/chat/sessions/active', {
    params: { userId },
  });

  // Transform backend response to frontend Session format
  return response.data.map((item) => ({
    id: item.session.id,
    userId: item.session.userId,
    courseId: item.session.courseTemplateId || '',
    courseName: item.session.customName || item.session.tutorName || 'Conversation',
    targetLanguageCode: item.session.targetLanguageCode,
    userLevel: item.session.estimatedCEFRLevel as CEFRLevel | '',
    phase: normalizePhase(item.session.conversationPhase),
    effectivePhase: normalizePhase(item.session.effectivePhase),
    currentTopic: item.session.currentTopic,
    vocabularyReviewMode: false, // Default to false if not present in response
    createdAt: item.session.createdAt,
    updatedAt: item.session.updatedAt,
  }));
}

// Backend session response format
interface BackendSessionResponse {
  session: {
    id: string;
    userId: string;
    courseTemplateId: string;
    customName: string | null;
    targetLanguageCode: string;
    sourceLanguageCode: string;
    estimatedCEFRLevel: string;
    conversationPhase: string;
    effectivePhase: string;
    currentTopic: string | null;
    tutorProfileId: string;
    tutorName: string;
    tutorPersona: string;
    tutorDomain: string;
    tutorTeachingStyle: string;
    isActive: boolean;
    vocabularyReviewMode: boolean;
    // Skill-specific CEFR levels
    cefrGrammar: string | null;
    cefrVocabulary: string | null;
    cefrFluency: string | null;
    cefrComprehension: string | null;
    lastAssessmentAt: string | null;
    createdAt: string;
    updatedAt: string;
    // Additional fields that may be present
    tutorAge?: number;
    tutorImage?: string;
    tutorEmoji?: string;
  };
  messages: BackendMessageResponse[];
}

// Normalize backend phase format (capitalized) to frontend format (uppercase)
function normalizePhase(backendPhase: string): ConversationPhase {
  const phaseMap: Record<string, ConversationPhase> = {
    'Auto': ConversationPhase.AUTO,
    'Free': ConversationPhase.FREE,
    'Correction': ConversationPhase.CORRECTION,
    'Drill': ConversationPhase.DRILL,
  };
  return phaseMap[backendPhase] || ConversationPhase.FREE;
}

export async function getSession(sessionId: string): Promise<Session & { messages: Message[] }> {
  const response = await apiClient.get<BackendSessionResponse>(
    `/chat/sessions/${sessionId}`
  );

  const backend = response.data;

  // Transform messages from backend format to frontend format
  const messages: Message[] = [];
  for (let i = 0; i < response.data.messages.length; i++) {
    const msg = response.data.messages[i];

    // For USER messages with corrections, pass the user's text for transformation
    // For ASSISTANT messages, no userText needed
    const userText = msg.role === 'USER' && msg.corrections ? msg.content : undefined;
    const frontendMsg = transformBackendMessage(msg, sessionId, userText);

    // Legacy support: If this is an assistant message with corrections, move them to the previous user message
    // (This handles old messages that were saved before the correction persistence change)
    if (msg.role === 'ASSISTANT' && msg.corrections && msg.corrections.length > 0 && messages.length > 0) {
      const lastMsg = messages[messages.length - 1];
      if (lastMsg.role === 'USER') {
        // Transform corrections using the previous user message content
        const transformedCorrections = msg.corrections
          .map(c => transformCorrection(c, lastMsg.content))
          .filter(c => c !== null);

        if (transformedCorrections.length > 0) {
          lastMsg.metadata = {
            ...lastMsg.metadata,
            corrections: transformedCorrections,
            phase: 'CORRECTION' as ConversationPhase,
          };
        }
      }
    }

    messages.push(frontendMsg);
  }

  const session = backend.session;

  // Determine course name with proper hierarchy: custom name > course name (if course-based) > tutor name > default
  let resolvedCourseName = session.customName || '';

  if (!resolvedCourseName && session.courseTemplateId) {
    // If no custom name but there's a course template, fetch the course name
    try {
      // Need to import the catalog API dynamically to avoid circular dependencies
      const catalogModule = await import('./catalog');
      const courseDetail = await catalogModule.getCourse(session.courseTemplateId);
      resolvedCourseName = courseDetail.name || session.tutorName || 'Conversation';
    } catch (error) {
      console.warn('Failed to fetch course name for session:', session.courseTemplateId, error);
      // Fallback to tutor name if course fetch fails
      resolvedCourseName = session.tutorName || 'Conversation';
    }
  } else if (!resolvedCourseName) {
    // If no course template ID, use tutor name or default
    resolvedCourseName = session.tutorName || 'Conversation';
  }

  const result: Session & { messages: Message[] } = {
    id: session.id,
    userId: session.userId,
    courseId: session.courseTemplateId,
    courseName: resolvedCourseName, // Use the properly determined course name
    targetLanguageCode: session.targetLanguageCode,
    userLevel: session.estimatedCEFRLevel as CEFRLevel | '',
    phase: normalizePhase(session.conversationPhase),
    effectivePhase: normalizePhase(session.effectivePhase),
    tutorTeachingStyle: session.tutorTeachingStyle as TeachingStyle,
    tutorAge: session.tutorAge,
    tutorImage: session.tutorImage,
    tutorEmoji: session.tutorEmoji,
    tutorProfileId: session.tutorProfileId,
    tutorName: session.tutorName,
    tutorPersona: session.tutorPersona,
    currentTopic: session.currentTopic,
    vocabularyReviewMode: session.vocabularyReviewMode,
    // Skill-specific CEFR levels
    cefrGrammar: session.cefrGrammar as CEFRLevel | null,
    cefrVocabulary: session.cefrVocabulary as CEFRLevel | null,
    cefrFluency: session.cefrFluency as CEFRLevel | null,
    cefrComprehension: session.cefrComprehension as CEFRLevel | null,
    lastAssessmentAt: session.lastAssessmentAt,
    createdAt: session.createdAt,
    updatedAt: session.updatedAt,
    messages,
  };

  console.log('📡 getSession API: Backend response', {
    tutorAge: result.tutorAge,
    tutorImage: result.tutorImage,
    tutorEmoji: result.tutorEmoji,
  });

  return result;
}

export async function getSessionProgress(sessionId: string): Promise<SessionProgress> {
  const response = await apiClient.get<SessionProgress>(
    `/chat/sessions/${sessionId}/progress`
  );
  return response.data;
}

export async function updatePhase(
  sessionId: string,
  phase: ConversationPhase
): Promise<Session> {
  // Convert frontend enum (uppercase) to backend format (capitalized)
  const phaseMap: Record<ConversationPhase, string> = {
    FREE: 'Free',
    CORRECTION: 'Correction',
    DRILL: 'Drill',
    AUTO: 'Auto',
  };

  // Backend returns SessionResponse with conversationPhase and effectivePhase
  interface UpdatePhaseResponse {
    conversationPhase: string;
    effectivePhase: string;
    id: string;
    userId: string;
    courseTemplateId: string;
    targetLanguageCode: string;
    estimatedCEFRLevel: string;
    currentTopic: string | null;
    createdAt: string;
    updatedAt: string;
  }

  const response = await apiClient.patch<UpdatePhaseResponse>(`/chat/sessions/${sessionId}/phase`, {
    phase: phaseMap[phase],
  });

  // Transform to frontend Session format
  return {
    id: response.data.id,
    userId: response.data.userId,
    courseId: response.data.courseTemplateId,
    courseName: '', // Not returned by this endpoint
    targetLanguageCode: response.data.targetLanguageCode,
    userLevel: response.data.estimatedCEFRLevel as CEFRLevel | '',
    phase: normalizePhase(response.data.conversationPhase),
    effectivePhase: normalizePhase(response.data.effectivePhase),
    currentTopic: response.data.currentTopic,
    vocabularyReviewMode: false, // Default to false
    createdAt: response.data.createdAt,
    updatedAt: response.data.updatedAt,
  };
}

// Backend SessionResponse format for teaching style update
interface UpdateTeachingStyleResponse {
  id: string;
  userId: string;
  tutorName: string;
  tutorPersona: string;
  tutorDomain: string;
  tutorTeachingStyle: string;
  sourceLanguageCode: string;
  targetLanguageCode: string;
  conversationPhase: string;
  effectivePhase: string;
  estimatedCEFRLevel: string;
  currentTopic: string | null;
  courseTemplateId: string | null;
  tutorProfileId: string | null;
  customName: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export async function updateTeachingStyle(
  sessionId: string,
  teachingStyle: TeachingStyle
): Promise<UpdateTeachingStyleResponse> {
  const response = await apiClient.patch<UpdateTeachingStyleResponse>(
    `/chat/sessions/${sessionId}/teaching-style`,
    { teachingStyle }
  );
  return response.data;
}

export async function updateTopic(
  sessionId: string,
  topic: string | null
): Promise<Session> {
  const response = await apiClient.patch<Session>(`/chat/sessions/${sessionId}/topic`, {
    currentTopic: topic,
  });
  return response.data;
}

export async function updateVocabularyReviewMode(
  sessionId: string,
  enabled: boolean
): Promise<Session> {
  const response = await apiClient.patch<Session>(
    `/chat/sessions/${sessionId}/vocabulary-review-mode`,
    { enabled }
  );
  return response.data;
}

export async function deleteSession(sessionId: string): Promise<void> {
  await apiClient.delete(`/chat/sessions/${sessionId}`);
}

// Non-streaming REST endpoint implementation
export async function navigateLesson(
  sessionId: string,
  direction: 'NEXT' | 'PREVIOUS'
): Promise<Session> {
  const response = await apiClient.patch<Session>(`/chat/sessions/${sessionId}/lesson`, {
    direction
  });
  return response.data;
}

export async function sendChatMessage(
  sessionId: string,
  message: string,
  userText: string, // The user's message text for correction mapping
  corrections?: BackendCorrection[] // Optional corrections to save with the message
): Promise<Message> {
  const response = await apiClient.post<BackendMessageResponse>(
    `/chat/sessions/${sessionId}/messages`,
    {
      content: message,
      corrections: corrections || null
    }
  );

  // Transform backend response to frontend Message format using utility
  return transformBackendMessage(response.data, sessionId, userText);
}

/**
 * Analyze user text for corrections without storing to database.
 * This is a synchronous call that returns corrections immediately.
 *
 * @param sessionId The session ID (for context and authorization)
 * @param userText The user's message text to analyze
 * @returns Array of corrections found (empty array if no errors)
 * @throws Error if analysis failed or unauthorized
 */
export async function analyzeCorrections(
  sessionId: string,
  userText: string
): Promise<BackendCorrection[]> {
  const response = await apiClient.post(
    `/chat/sessions/${sessionId}/analyze-corrections`,
    { userText }
  );
  return response.data;
}

/**
 * Update corrections for a user message.
 * This persists corrections to the database so they appear when the chat is reloaded.
 *
 * @param sessionId The session ID
 * @param messageId The user message ID to update
 * @param corrections Array of corrections to save
 * @returns Updated message with saved corrections
 * @throws Error if update failed or unauthorized
 */
export async function updateMessageCorrections(
  sessionId: string,
  messageId: string,
  corrections: BackendCorrection[]
): Promise<Message> {
  const response = await apiClient.patch<BackendMessageResponse>(
    `/chat/sessions/${sessionId}/messages/${messageId}/corrections`,
    { corrections }
  );
  return transformBackendMessage(response.data, sessionId);
}

// Tutor-initiated message APIs

/**
 * Initiate a tutor message without user input (regular, non-streaming)
 */
export async function initiateTutorMessage(
  sessionId: string,
  context: InitiateMessageContext
): Promise<Message> {
  const response = await apiClient.post<BackendMessageResponse>(
    `/chat/sessions/${sessionId}/messages/initiate`,
    { context }
  );

  // Transform backend response to frontend Message format using utility
  return transformBackendMessage(response.data, sessionId);
}

/**
 * Initiate a tutor message with streaming (SSE)
 * Returns an AbortController that can be used to cancel the stream
 */
export function initiateTutorMessageStream(
  sessionId: string,
  context: InitiateMessageContext,
  onChunk: (chunk: string) => void,
  onComplete: (message: Message) => void,
  onError: (error: Error) => void
): AbortController {
  console.log('🚀 initiateTutorMessageStream called:', { sessionId, context });
  const controller = new AbortController();

  const startStream = async () => {
    try {
      const token = storage.getAccessToken();
      const url = `${API_BASE_URL}/chat/sessions/${sessionId}/messages/initiate/stream`;
      console.log('📡 Fetching SSE from:', url, 'with token:', token?.substring(0, 20) + '...');

      // Create a timeout promise
      const timeoutPromise = new Promise<Response>((_, reject) => {
        setTimeout(() => reject(new Error('SSE fetch timeout after 30 seconds')), 30000);
      });

      // Race between fetch and timeout
      const response = await (Promise.race([
        fetch(url, {
          method: 'POST',
          headers: {
            'Authorization': token ? `Bearer ${token}` : '',
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
          },
          body: JSON.stringify({ context }),
          signal: controller.signal,
        }),
        timeoutPromise
      ])).catch(err => {
        console.error('❌ Fetch error:', err);
        throw err;
      }) as Response;

      console.log('📡 SSE Response received:', {
        status: response.status,
        statusText: response.statusText,
        headers: Object.fromEntries((response as Response).headers.entries()),
        bodyUsed: (response as Response).bodyUsed
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ SSE Error response:', errorText);
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body is not readable');
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let currentEvent = '';

      console.log('📖 Starting to read SSE stream...');

      while (true) {
        const { done, value } = await reader.read();
        console.log('📖 Read chunk:', { done, valueLength: value?.length });

        if (done) {
          console.log('📖 Stream ended');
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        console.log('📖 Processing lines:', lines.length);

        for (const line of lines) {
          if (!line.trim()) continue;

          console.log('📖 Raw line:', line);

          if (line.startsWith('event:')) {
            currentEvent = line.substring(6).trim();
            console.log('📨 SSE Event:', currentEvent);
            continue;
          }

          if (line.startsWith('data:')) {
            const data = line.substring(5).trim();
            console.log('📨 SSE Data (first 100 chars):', data.substring(0, 100), 'Event type:', currentEvent);

            if (currentEvent === 'complete') {
              // Parse the complete message object
              try {
                const backendMsg = JSON.parse(data) as BackendMessageResponse;
                // Transform backend response to frontend Message format using utility
                const frontendMsg = transformBackendMessage(backendMsg, sessionId);

                console.log('✅ SSE Complete:', frontendMsg);
                onComplete(frontendMsg);
              } catch (error) {
                console.error('❌ Failed to parse complete event:', error, 'Data:', data);
                onError(new Error('Failed to parse completion message'));
              }
            } else if (currentEvent === 'chunk') {
              // This is a text chunk
              console.log('📝 Passing chunk to callback');
              onChunk(data);
            } else {
              console.warn('⚠️ Unknown event type:', currentEvent, 'for data:', data.substring(0, 50));
            }

            // Reset event after processing data
            currentEvent = '';
          }
        }
      }
    } catch (error: unknown) {
      if (error && typeof error === 'object' && (error as Error).name === 'AbortError') {
        // Stream was cancelled - let the caller decide if this is expected
        console.log('🛑 Stream aborted');
        onError(error as Error);
        return;
      }
      onError(error as Error);
    }
  };

  startStream();

  return controller;
}
