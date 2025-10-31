// Enums
export enum CEFRLevel {
  None = 'None',
  A1 = 'A1',
  A2 = 'A2',
  B1 = 'B1',
  B2 = 'B2',
  C1 = 'C1',
  C2 = 'C2',
}

export enum LanguageProficiencyType {
  Native = 'Native',
  Learning = 'Learning',
}

export enum ConversationPhase {
  FREE = 'FREE',
  CORRECTION = 'CORRECTION',
  DRILL = 'DRILL',
  AUTO = 'AUTO',
}

export enum MessageRole {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT',
  SYSTEM = 'SYSTEM',
}

export enum CourseCategory {
  GENERAL = 'GENERAL',
  BUSINESS = 'BUSINESS',
  TRAVEL = 'TRAVEL',
  ACADEMIC = 'ACADEMIC',
  EXAM_PREP = 'EXAM_PREP',
}

export enum Difficulty {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
}

export enum TutorPersonality {
  Encouraging = 'Encouraging',
  Strict = 'Strict',
  Casual = 'Casual',
  Professional = 'Professional',
  Academic = 'Academic',
}

export enum TeachingStyle {
  Reactive = 'Reactive',
  Guided = 'Guided',
  Directive = 'Directive',
}

export enum TutorGender {
  Male = 'Male',
  Female = 'Female',
  Neutral = 'Neutral',
}

export enum ErrorType {
  GRAMMAR = 'GRAMMAR',
  SPELLING = 'SPELLING',
  VOCABULARY = 'VOCABULARY',
  WORD_ORDER = 'WORD_ORDER',
  VERB_FORM = 'VERB_FORM',
  ARTICLE = 'ARTICLE',
  PREPOSITION = 'PREPOSITION',
  PUNCTUATION = 'PUNCTUATION',
  OTHER = 'OTHER',
}

export enum ErrorSeverity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

// Domain Models
export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  roles: string[];
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string | null;
  lastLoginAt: string | null;
}

export interface Message {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  timestamp: string;
  metadata?: MessageMetadata;
  errorMessage?: string;
}

export interface Session {
  id: string;
  userId: string;
  courseId: string;
  courseName: string;
  targetLanguageCode: string;
  userLevel: CEFRLevel;
  phase: ConversationPhase;
  effectivePhase: ConversationPhase;
  tutorTeachingStyle?: TeachingStyle;
  tutorAge?: number;
  tutorImage?: string | null;
  tutorEmoji?: string;
  tutorProfileId?: string;
  tutorName?: string;
  tutorPersona?: string;
  currentTopic: string | null;
  vocabularyReviewMode: boolean;
  // Skill-specific CEFR levels
  cefrGrammar?: CEFRLevel | null;
  cefrVocabulary?: CEFRLevel | null;
  cefrFluency?: CEFRLevel | null;
  cefrComprehension?: CEFRLevel | null;
  lastAssessmentAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Correction {
  startIndex: number;
  endIndex: number;
  originalText: string;
  correctedText: string;
  errorType: ErrorType;
  severity: ErrorSeverity;
  explanation?: string;
}

export interface WordCard {
  titleSourceLanguage: string;
  titleTargetLanguage: string;
  descriptionSourceLanguage: string;
  descriptionTargetLanguage: string;
  conceptName?: string | null;
  imageUrl?: string | null;
}

export interface CharacterCard {
  character: string;
  pronunciation: string;
  description: string;
}

export interface MessageMetadata {
  corrections: Correction[];
  phase: ConversationPhase;
  wordCards?: WordCard[];
  characterCards?: CharacterCard[];
}

export interface Language {
  code: string;
  name: string;
  flagEmoji: string;
  nativeName: string;
  difficulty: Difficulty;
  description: string;
  courseCount: number;
}

export interface Course {
  id: string;
  languageCode: string;
  name: string;
  shortDescription: string;
  description?: string;
  category: CourseCategory;
  targetAudience: string;
  startingLevel: CEFRLevel;
  targetLevel: CEFRLevel;
  difficulty?: Difficulty;
  userLevel?: CEFRLevel;
  estimatedWeeks: number | null;
  displayOrder: number;
}

export interface CourseDetail extends Course {
  description: string;
  suggestedTutors: Tutor[];
  defaultPhase: ConversationPhase;
  topicSequence: string[] | null;
  learningGoals: string[];
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Tutor {
  id: string;
  name: string;
  emoji: string;
  persona: string;
  domain: string;
  personality: TutorPersonality;
  teachingStyle: TeachingStyle;
  description: string;
  targetLanguageCode: string;
  culturalBackground: string | null;
  age: number;
  gender: TutorGender | null;
  imageUrl: string | null;
  displayOrder: number;
  location?: string;
}

export interface TutorDetail extends Tutor {
  createdAt: string;
  updatedAt: string;
}

export interface VocabularyItem {
  id: string;
  lemma: string;
  lang: string;
  exposures: number;
  lastSeenAt: string;
  createdAt: string;
  imageUrl: string | null;
  conceptName: string | null;
  nextReviewAt: string | null;
  reviewStage: number;
  isDue: boolean;
}

export interface VocabularyContext {
  context: string;
  turnId: string | null;
}

// User language proficiency
export interface LanguageProficiency {
  id: string;
  userId: string;
  languageCode: string;
  proficiencyType: LanguageProficiencyType;
  cefrLevel: CEFRLevel | null;
  isNative: boolean;
  isPrimary: boolean;
  selfAssessed: boolean;
  lastAssessedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SessionProgress {
  sessionId: string;
  totalMessages: number;
  topicsCovered: Array<{
    topic: string;
    turnCount: number;
  }>;
  errorsByType: Record<ErrorType, number>;
  vocabularyCount: number;
  timeByPhase: Record<ConversationPhase, number>;
}

// Session summary types
export interface SummaryLevelInfo {
  level: number;
  count: number;
  totalTokens: number;
  coveredSequences: { start: number; end: number } | null;
}

export interface SessionSummaryInfo {
  sessionId: string;
  totalMessages: number;
  summaryLevels: SummaryLevelInfo[];
  lastSummarizedSequence: number | null;
  estimatedTokenSavings: number;
  compressionRatio: number;
}

export interface SummaryDetail {
  id: string;
  summaryLevel: number;
  startSequence: number;
  endSequence: number;
  summaryText: string;
  tokenCount: number;
  sourceType: string;
  sourceIds: string[];
  supersededById: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface GlobalSummaryStats {
  totalSessions: number;
  totalSummaries: number;
  averageCompressionRatio: number;
  totalTokensSaved: number;
}

// Custom tutor creation request
export interface CreateTutorRequest {
  name: string;
  emoji: string;
  personaEnglish: string;
  domainEnglish: string;
  descriptionEnglish: string;
  personality: TutorPersonality;
  teachingStyle: TeachingStyle;
  targetLanguageCode: string;
  culturalBackground?: string;
  location?: string;
  age?: number;
  gender?: TutorGender;
  isActive?: boolean;
  displayOrder?: number;
}

// Vocabulary review types
export interface DueCountResponse {
  count: number;
}

// Error analytics types
export enum ErrorTrend {
  IMPROVING = 'IMPROVING',
  STABLE = 'STABLE',
  WORSENING = 'WORSENING',
  INSUFFICIENT_DATA = 'INSUFFICIENT_DATA',
}

export interface ErrorPattern {
  errorType: string;
  totalCount: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  weightedScore: number;
  firstSeenAt: string;
  lastSeenAt: string;
}

export interface ErrorTrendResponse {
  errorType: string;
  trend: ErrorTrend;
}

export interface ErrorSample {
  id: string;
  errorType: string;
  severity: string;
  errorSpan: string | null;
  occurredAt: string;
}

// Assessment types
export interface SkillBreakdown {
  overall: string;
  grammar: string;
  vocabulary: string;
  fluency: string;
  comprehension: string;
  lastAssessedAt: string | null;
  assessmentCount: number;
}

// Lesson types
export interface LessonContent {
  id: string;
  title: string;
  weekNumber: number | null;
  estimatedDuration: string | null;
  focusAreas: string[];
  targetCEFR: CEFRLevel;
  goals: string[];
  grammarPoints: GrammarPoint[];
  essentialVocabulary: VocabEntry[];
  conversationScenarios: Scenario[];
  practicePatterns: string[];
  commonMistakes: string[];
  fullMarkdown: string;
}

export interface GrammarPoint {
  title: string;
  rule: string;
  examples: string[];
  patterns: string[];
}

export interface VocabEntry {
  word: string;
  translation: string;
  contextExample: string | null;
}

export interface Scenario {
  title: string;
  dialogue: string;
}

// TTS (Text-to-Speech) types
export enum TTSVoice {
  Warm = 'Warm',
  Professional = 'Professional',
  Energetic = 'Energetic',
  Calm = 'Calm',
  Authoritative = 'Authoritative',
  Friendly = 'Friendly',
}

export interface VoicesResponse {
  abstractVoices: string[];
  voiceMappings: Record<string, string>;
  defaultVoice: string;
}

export interface SynthesizeRequest {
  text: string;
  voiceId: string;
  speed?: number;
}

export interface TTSPreferences {
  autoPlay: boolean;
  defaultSpeed: number;
  wifiOnly: boolean;
  enabled: boolean;
}

// Tutor-initiated message types
export type InitiateMessageContext = 'welcome' | 'reengage';

export interface InitiateTutorMessageRequest {
  context: InitiateMessageContext;
}
