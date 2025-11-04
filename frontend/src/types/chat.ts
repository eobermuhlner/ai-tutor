import type { CEFRLevel, TeachingStyle, ConversationPhase, Message, Language } from '../types';

/**
 * Grouped tutor information to reduce prop drilling
 */
export interface TutorInfo {
  profileId: string;
  name: string;
  image: string | null;
  emoji: string;
  age: number | null;
}

/**
 * Conversation phase information (user preference vs effective phase)
 */
export interface PhaseInfo {
  /** User-selected phase (Auto/Free/Correction/Drill) */
  current: ConversationPhase;
  /** Actual active phase (what's being used right now) */
  effective: ConversationPhase;
}

/**
 * Skill-specific CEFR levels
 */
export interface SkillLevels {
  grammar?: CEFRLevel | null;
  vocabulary?: CEFRLevel | null;
  fluency?: CEFRLevel | null;
  comprehension?: CEFRLevel | null;
}

/**
 * Complete chat session state
 */
export interface ChatSessionState {
  // Session identity
  sessionId: string;
  courseId?: string;
  courseName: string;

  // Messages
  messages: Message[];
  isLoading: boolean;
  isSending: boolean;

  // Language
  targetLanguageCode: string;
  languages: Language[];

  // Learning state
  userLevel: CEFRLevel;
  skillLevels: SkillLevels;
  phase: PhaseInfo;
  teachingStyle: TeachingStyle;
  currentTopic: string | null;

  // Tutor
  tutor: TutorInfo;

  // Vocabulary review
  vocabularyReviewMode: boolean;
  dueCount: number;

  // UI refresh triggers
  summaryRefreshKey: number;
  rateLimitRefreshTrigger: number;
}

/**
 * Actions available in the chat session context
 */
export interface ChatSessionActions {
  sendMessage: (text: string) => Promise<void>;
  updatePhase: (phase: ConversationPhase) => Promise<void>;
  updateTopic: (topic: string | null) => Promise<void>;
  updateTeachingStyle: (style: TeachingStyle) => Promise<void>;
  updateVocabularyReviewMode: (enabled: boolean) => Promise<void>;
  refreshDueCount: () => Promise<void>;
  handleReengage: (message: Message) => void;
  addMessage: (message: Message) => void;
}

/**
 * Complete chat session context value
 */
export interface ChatSessionContextValue extends ChatSessionState, ChatSessionActions {
  sessionLoaded: boolean;
}
