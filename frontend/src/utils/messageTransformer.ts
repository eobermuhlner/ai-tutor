import { ConversationPhase, MessageRole } from '../types';
import type { Message, Correction, WordCard, CharacterCard, ErrorType, ErrorSeverity } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8080';
const BASE_URL = API_BASE_URL;

/**
 * Backend response formats
 */
export interface BackendCorrection {
  span: string;
  errorType: string;
  severity: string;
  correctedTargetLanguage: string;
  whySourceLanguage: string;
  whyTargetLanguage: string;
}

export interface BackendWordCard {
  titleSourceLanguage: string;
  titleTargetLanguage: string;
  descriptionSourceLanguage: string;
  descriptionTargetLanguage: string;
  conceptName?: string | null;
  imageUrl?: string | null;
}

export interface BackendCharacterCard {
  character: string;
  pronunciation: string;
  meaning: string;
  example: string;
  strokeOrder?: number;
}

export interface BackendMessageResponse {
  id: string;
  role: string;
  content: string;
  corrections: BackendCorrection[] | null;
  newVocabulary: any[] | null;
  wordCards: BackendWordCard[] | null;
  characterCards: BackendCharacterCard[] | null;
  createdAt: string;
  errorMessage?: string;
}

/**
 * Transform backend correction to frontend format
 */
export function transformCorrection(backendCorrection: BackendCorrection, userText: string): Correction | null {
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

/**
 * Transform backend word cards to frontend format
 */
export function transformWordCards(backendCards: BackendWordCard[]): WordCard[] {
  return backendCards.map((card) => ({
    titleSourceLanguage: card.titleSourceLanguage,
    titleTargetLanguage: card.titleTargetLanguage,
    descriptionSourceLanguage: card.descriptionSourceLanguage,
    descriptionTargetLanguage: card.descriptionTargetLanguage,
    imageUrl: card.imageUrl ?
      card.imageUrl.startsWith('http') ? card.imageUrl : `${BASE_URL}${card.imageUrl}`
      : null,
    conceptName: card.conceptName,
  }));
}

/**
 * Transform backend character cards to frontend format
 */
export function transformCharacterCards(backendCards: BackendCharacterCard[]): CharacterCard[] {
  return backendCards.map((card) => ({
    character: card.character,
    pronunciation: card.pronunciation,
    description: card.meaning || card.example || card.character,
  }));
}

/**
 * Transform backend message response to frontend Message format
 * @param backendMsg - Backend message response
 * @param sessionId - Session ID to associate with the message
 * @param userText - Optional user text for correction mapping (only for assistant messages with corrections)
 */
export function transformBackendMessage(
  backendMsg: BackendMessageResponse,
  sessionId: string,
  userText?: string
): Message {
  // Transform corrections if present
  let transformedCorrections: Correction[] | undefined = undefined;
  if (backendMsg.corrections && backendMsg.corrections.length > 0 && userText) {
    transformedCorrections = backendMsg.corrections
      .map(c => transformCorrection(c, userText))
      .filter((c): c is Correction => c !== null);
  }

  // Base message object
  const frontendMsg: Message = {
    id: backendMsg.id,
    sessionId,
    role: backendMsg.role as MessageRole,
    content: backendMsg.content,
    timestamp: backendMsg.createdAt,
    errorMessage: backendMsg.errorMessage,
    metadata: undefined,
  };

  // Add corrections if present
  if (transformedCorrections && transformedCorrections.length > 0) {
    frontendMsg.metadata = {
      corrections: transformedCorrections,
      phase: 'CORRECTION' as ConversationPhase,
    };
  }

  // Add word cards if present
  if (backendMsg.wordCards && backendMsg.wordCards.length > 0) {
    const transformedWordCards = transformWordCards(backendMsg.wordCards);
    frontendMsg.metadata = {
      ...frontendMsg.metadata,
      corrections: frontendMsg.metadata?.corrections || [],
      phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
      wordCards: transformedWordCards,
    };
  }

  // Add character cards if present
  if (backendMsg.characterCards && backendMsg.characterCards.length > 0) {
    const transformedCharacterCards = transformCharacterCards(backendMsg.characterCards);
    frontendMsg.metadata = {
      ...frontendMsg.metadata,
      corrections: frontendMsg.metadata?.corrections || [],
      phase: frontendMsg.metadata?.phase || 'FREE' as ConversationPhase,
      characterCards: transformedCharacterCards,
    };
  }

  return frontendMsg;
}
