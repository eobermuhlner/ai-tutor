import apiClient from './client';
import { CEFRLevel, TeachingStyle, ConversationPhase, ErrorType, ErrorSeverity, MessageRole } from '../types';
import type { Correction } from '../types';
import type { Session, Message, SessionProgress, InitiateMessageContext } from '../types';
import * as storage from '../utils/storage';
import { API_BASE_URL } from '../utils/constants';

const BASE_URL = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8080';

// Backend response format (different from frontend Message type)
interface BackendCorrection {
  span: string;
  errorType: string;
  severity: string;
  correctedTargetLanguage: string;
  whySourceLanguage: string;
  whyTargetLanguage: string;
}

interface BackendMessageResponse {
  id: string;
  role: string;
  content: string;
  corrections: BackendCorrection[] | null;
  newVocabulary: NewVocabulary[] | null;
  wordCards: BackendWordCard[] | null;
  characterCards: BackendCharacterCard[] | null;
  createdAt: string; // ISO timestamp from backend
  errorMessage?: string;
}

interface NewVocabulary {
  word: string;
  translation: string;
  exampleTarget: string;
  exampleTranslation: string;
  frequency: number;
  difficulty: string;
}

interface BackendWordCard {
  titleSourceLanguage: string;
  titleTargetLanguage: string;
  descriptionSourceLanguage: string;
  descriptionTargetLanguage: string;
  conceptName?: string | null;
  imageUrl?: string | null;
}

interface BackendCharacterCard {
  character: string;
  pronunciation: string;
  meaning: string;
  example: string;
  strokeOrder?: number;
}



// Transform backend correction to frontend format
function transformCorrection(backendCorrection: BackendCorrection, userText: string): Correction | null {
  const span = backendCorrection.span;
  const startIndex = userText.indexOf(span);

  if (startIndex === -1) {
    return null;
  }

  const endIndex = startIndex + span.length;

  // Map severity from backend format
  const severityMap: Record<string, string> = {
    'Low': 'LOW',
    'Medium': 'MEDIUM',
    'High': 'HIGH',
    'Critical': 'CRITICAL',
  };

  // Map error type from backend format
  const errorTypeMap: Record<string, string> = {
    'Typography': 'SPELLING',
    'Grammar': 'GRAMMAR',
    'Vocabulary': 'VOCABULARY',
    'WordOrder': 'WORD_ORDER',
    'VerbForm': 'VERB_FORM',
    'Article': 'ARTICLE',
    'Preposition': 'PREPOSITION',
    'Punctuation': 'PUNCTUATION',
  };

  return {
    startIndex,
    endIndex,
    originalText: span,
    correctedText: backendCorrection.correctedTargetLanguage,
    errorType: errorTypeMap[backendCorrection.errorType] as ErrorType || 'OTHER' as ErrorType,
    severity: severityMap[backendCorrection.severity] as ErrorSeverity || 'MEDIUM' as ErrorSeverity,
    explanation: backendCorrection.whySourceLanguage,
  };
}

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
    courseName: item.session.tutorName,
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
  // and move corrections from assistant messages to the preceding user messages
  const messages: Message[] = [];
  for (let i = 0; i < response.data.messages.length; i++) {
    const msg = response.data.messages[i];
    const frontendMsg: Message = {
      id: msg.id,
      sessionId,
      role: msg.role as MessageRole,
      content: msg.content,
      timestamp: msg.createdAt,
      errorMessage: msg.errorMessage,
      metadata: undefined,
    };

    // If this is an assistant message with corrections, attach them to the previous user message
    if (msg.role === 'ASSISTANT' && msg.corrections && msg.corrections.length > 0 && messages.length > 0) {
      const lastMsg = messages[messages.length - 1];
      if (lastMsg.role === 'USER') {
        // Transform corrections using the previous user message content
        const transformedCorrections = msg.corrections
          .map(c => transformCorrection(c, lastMsg.content))
          .filter(c => c !== null);

        if (transformedCorrections.length > 0) {
          lastMsg.metadata = {
            corrections: transformedCorrections,
            phase: 'CORRECTION' as ConversationPhase,
          };
        }
      }
    } else if (msg.role !== 'ASSISTANT' && msg.corrections && msg.corrections.length > 0) {
      // If corrections are on a non-assistant message (shouldn't happen, but handle it)
      const transformedCorrections = msg.corrections
        .map(c => transformCorrection(c, msg.content))
        .filter(c => c !== null);

      if (transformedCorrections.length > 0) {
        frontendMsg.metadata = {
          corrections: transformedCorrections,
          phase: 'CORRECTION' as ConversationPhase,
        };
      }
    }

    // Add word cards if present
    if (msg.wordCards && msg.wordCards.length > 0) {
      // Transform imageUrl to ensure it's a complete URL (check if already a full URL before adding base)
      const transformedWordCards = msg.wordCards.map((card: BackendWordCard) => ({
        titleSourceLanguage: card.titleSourceLanguage,
        titleTargetLanguage: card.titleTargetLanguage,
        descriptionSourceLanguage: card.descriptionSourceLanguage,
        descriptionTargetLanguage: card.descriptionTargetLanguage,
        imageUrl: card.imageUrl ?
          card.imageUrl.startsWith('http') ? card.imageUrl : `${BASE_URL}${card.imageUrl}`
          : null,
        conceptName: card.conceptName,
      }));
      frontendMsg.metadata = {
        ...frontendMsg.metadata,
        corrections: frontendMsg.metadata?.corrections || [],
        phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
        wordCards: transformedWordCards,
      };
    }

    // Add character cards if present
    if (msg.characterCards && msg.characterCards.length > 0) {
      // Transform backend character cards to frontend format
      const transformedCharacterCards = msg.characterCards.map((card: BackendCharacterCard) => ({
        character: card.character,
        pronunciation: card.pronunciation,
        description: card.meaning || card.example || card.character, // Use meaning, example, or character as description
      }));
      
      frontendMsg.metadata = {
        ...frontendMsg.metadata,
        corrections: frontendMsg.metadata?.corrections || [],
        phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
        characterCards: transformedCharacterCards,
      };
    }

    messages.push(frontendMsg);
  }

  const session = backend.session;

  const result: Session & { messages: Message[] } = {
    id: session.id,
    userId: session.userId,
    courseId: session.courseTemplateId,
    courseName: session.customName || 'Conversation', // Use custom name or fallback
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
  userText: string // The user's message text for correction mapping
): Promise<Message> {
  const response = await apiClient.post<BackendMessageResponse>(
    `/chat/sessions/${sessionId}/messages`,
    { content: message }
  );

  // Transform backend response to frontend Message format
  const backendMsg = response.data;

  // Transform corrections if present
  let transformedCorrections = undefined;
  if (backendMsg.corrections && backendMsg.corrections.length > 0) {
    transformedCorrections = backendMsg.corrections
      .map(c => transformCorrection(c, userText))
      .filter(c => c !== null);
  }

  const frontendMsg: Message = {
    id: backendMsg.id,
    sessionId,
    role: backendMsg.role as MessageRole,
    content: backendMsg.content,
    timestamp: backendMsg.createdAt,
    errorMessage: backendMsg.errorMessage,
    metadata: transformedCorrections && transformedCorrections.length > 0
      ? { corrections: transformedCorrections, phase: 'CORRECTION' as ConversationPhase }
      : undefined,
  };

  // Add word cards if present
  if (backendMsg.wordCards && backendMsg.wordCards.length > 0) {
    // Transform imageUrl to ensure it's a complete URL (check if already a full URL before adding base)
    const transformedWordCards = backendMsg.wordCards.map((card: BackendWordCard) => ({
      titleSourceLanguage: card.titleSourceLanguage,
      titleTargetLanguage: card.titleTargetLanguage,
      descriptionSourceLanguage: card.descriptionSourceLanguage,
      descriptionTargetLanguage: card.descriptionTargetLanguage,
      imageUrl: card.imageUrl ?
        card.imageUrl.startsWith('http') ? card.imageUrl : `${BASE_URL}${card.imageUrl}`
        : null,
      conceptName: card.conceptName,
    }));
    frontendMsg.metadata = {
      ...frontendMsg.metadata,
      corrections: frontendMsg.metadata?.corrections || [],
      phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
      wordCards: transformedWordCards,
    };
  }

  // Add character cards if present
  if (backendMsg.characterCards && backendMsg.characterCards.length > 0) {
    // Transform backend character cards to frontend format
    const transformedCharacterCards = backendMsg.characterCards.map((card: BackendCharacterCard) => ({
      character: card.character,
      pronunciation: card.pronunciation,
      description: card.meaning || card.example || card.character, // Use meaning, example, or character as description
    }));
    
    frontendMsg.metadata = {
      ...frontendMsg.metadata,
      corrections: frontendMsg.metadata?.corrections || [],
      phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
      characterCards: transformedCharacterCards,
    };
  }

  return frontendMsg;
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

  const backendMsg = response.data;

  // Transform backend response to frontend Message format
  const frontendMsg: Message = {
    id: backendMsg.id,
    sessionId,
    role: backendMsg.role as MessageRole,
    content: backendMsg.content,
    timestamp: backendMsg.createdAt,
    errorMessage: backendMsg.errorMessage,
    metadata: undefined,
  };

  // Add word cards if present
  if (backendMsg.wordCards && backendMsg.wordCards.length > 0) {
    const transformedWordCards = backendMsg.wordCards.map((card: BackendWordCard) => ({
      titleSourceLanguage: card.titleSourceLanguage,
      titleTargetLanguage: card.titleTargetLanguage,
      descriptionSourceLanguage: card.descriptionSourceLanguage,
      descriptionTargetLanguage: card.descriptionTargetLanguage,
      imageUrl: card.imageUrl ?
        card.imageUrl.startsWith('http') ? card.imageUrl : `${BASE_URL}${card.imageUrl}`
        : null,
      conceptName: card.conceptName,
    }));
    frontendMsg.metadata = {
      corrections: [],
      phase: 'FREE' as ConversationPhase,
      wordCards: transformedWordCards,
    };
  }

  // Add character cards if present
  if (backendMsg.characterCards && backendMsg.characterCards.length > 0) {
    // Transform backend character cards to frontend format
    const transformedCharacterCards = backendMsg.characterCards.map((card: BackendCharacterCard) => ({
      character: card.character,
      pronunciation: card.pronunciation,
      description: card.meaning || card.example || card.character, // Use meaning, example, or character as description
    }));
    
    frontendMsg.metadata = {
      ...frontendMsg.metadata,
      corrections: frontendMsg.metadata?.corrections || [],
      phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
      characterCards: transformedCharacterCards,
    };
  }

  return frontendMsg;
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
        setTimeout(() => reject(new Error('SSE fetch timeout after 10 seconds')), 10000);
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
                const frontendMsg: Message = {
                  id: backendMsg.id,
                  sessionId,
                  role: backendMsg.role as MessageRole,
                  content: backendMsg.content,
                  timestamp: backendMsg.createdAt,
                  errorMessage: backendMsg.errorMessage,
                  metadata: undefined,
                };

                // Add word cards if present
                if (backendMsg.wordCards && backendMsg.wordCards.length > 0) {
                  const transformedWordCards = backendMsg.wordCards.map((card: BackendWordCard) => ({
                    titleSourceLanguage: card.titleSourceLanguage,
                    titleTargetLanguage: card.titleTargetLanguage,
                    descriptionSourceLanguage: card.descriptionSourceLanguage,
                    descriptionTargetLanguage: card.descriptionTargetLanguage,
                    imageUrl: card.imageUrl ?
                      card.imageUrl.startsWith('http') ? card.imageUrl : `${BASE_URL}${card.imageUrl}`
                      : null,
                    conceptName: card.conceptName,
                  }));
                  frontendMsg.metadata = {
                    corrections: [],
                    phase: 'FREE' as ConversationPhase,
                    wordCards: transformedWordCards,
                  };
                }

                // Add character cards if present
                if (backendMsg.characterCards && backendMsg.characterCards.length > 0) {
                  // Transform backend character cards to frontend format
                  const transformedCharacterCards = backendMsg.characterCards.map((card: BackendCharacterCard) => ({
                    character: card.character,
                    pronunciation: card.pronunciation,
                    description: card.meaning || card.example || card.character, // Use meaning, example, or character as description
                  }));
                  
                  frontendMsg.metadata = {
                    ...frontendMsg.metadata,
                    corrections: frontendMsg.metadata?.corrections || [],
                    phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
                    characterCards: transformedCharacterCards,
                  };
                }

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
