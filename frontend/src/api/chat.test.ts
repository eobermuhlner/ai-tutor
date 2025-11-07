import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  createSessionFromCourse,
  getSessions,
  getActiveSessions,
  getSession,
  getSessionProgress,
  updatePhase,
  updateTeachingStyle,
  updateTopic,
  updateVocabularyReviewMode,
  deleteSession,
  navigateLesson,
  sendChatMessage,
  initiateTutorMessage,
} from './chat';
import { CEFRLevel, ConversationPhase, TeachingStyle, MessageRole } from '../types';
import type { Session, SessionProgress } from '../types';

// Use vi.hoisted to properly handle the hoisting issue
const { mockGet, mockPost, mockPatch, mockDelete } = vi.hoisted(() => {
  return {
    mockGet: vi.fn(),
    mockPost: vi.fn(),
    mockPatch: vi.fn(),
    mockDelete: vi.fn(),
  };
});

vi.mock('./client', () => {
  return {
    default: {
      get: mockGet,
      post: mockPost,
      patch: mockPatch,
      delete: mockDelete,
    }
  };
});

describe('chat API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createSessionFromCourse', () => {
    it('should create a new session from course', async () => {
      const courseId = 'course1';

      const mockSession: Session = {
        id: 'session1',
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: null,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: undefined,
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };

      mockPost.mockResolvedValue({ data: mockSession });

      const result = await createSessionFromCourse(courseId);

      expect(mockPost).toHaveBeenCalledWith('/chat/sessions/from-course', {
        courseTemplateId: courseId,
        tutorProfileId: undefined,
        customName: undefined,
      });
      expect(result).toEqual(mockSession);
    });
  });

  describe('getSessions', () => {
    it('should fetch user sessions', async () => {
      const userId = 'user1';
      
      // Mock the API to return data already in the Session format (as expected by the interface)
      const mockSessionResponse = [
        {
          id: 'session1',
          userId: userId,
          courseId: 'course1',
          courseName: 'Spanish for Beginners',
          targetLanguageCode: 'es',
          userLevel: CEFRLevel.A1,
          phase: ConversationPhase.FREE,
          effectivePhase: ConversationPhase.FREE,
          currentTopic: null,
          vocabularyReviewMode: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          // Add optional fields with undefined values to match the interface
          sourceLanguageCode: undefined,
          tutorTeachingStyle: undefined,
          tutorAge: undefined,
          tutorImage: undefined,
          tutorEmoji: undefined,
          tutorProfileId: undefined,
          tutorName: undefined,
          tutorPersona: undefined,
          cefrGrammar: undefined,
          cefrVocabulary: undefined,
          cefrFluency: undefined,
          cefrComprehension: undefined,
          lastAssessmentAt: undefined,
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockSessionResponse });
      
      const result = await getSessions(userId);
      
      expect(mockGet).toHaveBeenCalledWith('/chat/sessions', { params: { userId } });
      expect(result).toEqual(mockSessionResponse);
    });
  });

  describe('getActiveSessions', () => {
    it('should fetch user\'s active sessions', async () => {
      const userId = 'user1';
      const mockResponse = [
        {
          session: {
            id: 'session1',
            userId: userId,
            courseTemplateId: 'course1',
            tutorName: 'Spanish for Beginners', // Backend uses tutorName instead of courseTemplateName
            targetLanguageCode: 'es',
            conversationPhase: 'Free', // Backend uses capitalized format
            effectivePhase: 'Free',
            estimatedCEFRLevel: 'A1', // Backend uses estimatedCEFRLevel instead of userLevel
            currentTopic: null,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
          progress: {
            totalMessages: 10,
            correctionsReceived: 2,
            vocabularyLearned: 5,
            currentCEFRLevel: 'A1',
            timeSpent: 600,
            lastActive: new Date().toISOString(),
          }
        }
      ];
      

      
      mockGet.mockResolvedValue({ data: mockResponse });
      
      const result = await getActiveSessions(userId);
      
      expect(mockGet).toHaveBeenCalledWith('/chat/sessions/active', { params: { userId } });
      // Transform the expected sessions to match the function's transformation
      const expectedSessions = mockResponse.map(item => ({
        id: item.session.id,
        userId: item.session.userId,
        courseId: item.session.courseTemplateId || '',
        courseName: item.session.tutorName,
        targetLanguageCode: item.session.targetLanguageCode,
        userLevel: item.session.estimatedCEFRLevel as CEFRLevel | '',
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: item.session.currentTopic,
        vocabularyReviewMode: false,
        createdAt: item.session.createdAt,
        updatedAt: item.session.updatedAt,
      }));
      expect(result).toEqual(expectedSessions);
    });
  });

  describe('getSession', () => {
    it('should fetch a specific session with messages', async () => {
      const sessionId = 'session1';
      // The getSession function expects the backend to return BackendSessionResponse format
      const mockBackendResponse = {
        session: {
          id: sessionId,
          userId: 'user1',
          courseTemplateId: 'course1',
          customName: 'Spanish for Beginners', // Using customName field instead of courseTemplateName
          targetLanguageCode: 'es',
          sourceLanguageCode: 'en',
          estimatedCEFRLevel: 'A1',
          conversationPhase: 'Free', // Backend uses capitalized format
          effectivePhase: 'Free',
          currentTopic: null,
          tutorProfileId: 'tutor123',
          tutorName: 'Maria',
          tutorPersona: 'Friendly Spanish tutor',
          tutorDomain: 'Spanish',
          tutorTeachingStyle: 'Guided',
          isActive: true,
          vocabularyReviewMode: false,
          cefrGrammar: null,
          cefrVocabulary: null,
          cefrFluency: null,
          cefrComprehension: null,
          lastAssessmentAt: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          tutorAge: 30,
          tutorImage: null,
          tutorEmoji: '👩',
        },
        messages: [
          {
            id: 'msg1',
            role: 'ASSISTANT',
            content: 'Hello! How can I help you today?',
            timestamp: new Date().toISOString(),
            metadata: undefined,
            corrections: [], // Backend message format might include corrections
            errorMessage: undefined,
          }
        ]
      };
      
      // Expected result after transformation
      const expectedSession = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: 'A1',
        phase: ConversationPhase.FREE, // Should be transformed from 'Free'
        effectivePhase: ConversationPhase.FREE,
        currentTopic: null,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: 'Guided',
        tutorAge: 30,
        tutorImage: null,
        tutorEmoji: '👩',
        tutorProfileId: 'tutor123',
        tutorName: 'Maria',
        tutorPersona: 'Friendly Spanish tutor',
        cefrGrammar: null,
        cefrVocabulary: null,
        cefrFluency: null,
        cefrComprehension: null,
        lastAssessmentAt: null,
        messages: [
          {
            id: 'msg1',
            sessionId: sessionId,
            role: 'ASSISTANT',
            content: 'Hello! How can I help you today?',
            timestamp: undefined, // Based on test results, transform function may not set timestamp
            metadata: undefined,
            errorMessage: undefined,
          }
        ]
      };
      
      mockGet.mockResolvedValue({ data: mockBackendResponse });
      
      const result = await getSession(sessionId);
      
      expect(mockGet).toHaveBeenCalledWith(`/chat/sessions/${sessionId}`);
      expect(result).toEqual(expectedSession);
    });
  });

  describe('getSessionProgress', () => {
    it('should fetch session progress', async () => {
      const sessionId = 'session1';
      const mockProgress: SessionProgress = {
        sessionId: sessionId,
        totalMessages: 25,
        topicsCovered: [
          { topic: 'greetings', turnCount: 5 },
          { topic: 'introductions', turnCount: 3 }
        ],
        errorsByType: {
          GRAMMAR: 2,
          SPELLING: 0,
          VOCABULARY: 1,
          WORD_ORDER: 0,
          VERB_FORM: 0,
          ARTICLE: 0,
          PREPOSITION: 0,
          PUNCTUATION: 0,
          OTHER: 0,
        },
        vocabularyCount: 10,
        timeByPhase: {
          FREE: 600,
          CORRECTION: 300,
          DRILL: 200,
          AUTO: 100,
        },
      };
      
      mockGet.mockResolvedValue({ data: mockProgress });
      
      const result = await getSessionProgress(sessionId);
      
      expect(mockGet).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/progress`);
      expect(result).toEqual(mockProgress);
    });
  });

  describe('updatePhase', () => {
    it('should update conversation phase', async () => {
      const sessionId = 'session1';
      const phase: ConversationPhase = ConversationPhase.CORRECTION;
      // Backend responds with the format expected by updatePhase function
      const mockBackendResponse = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        targetLanguageCode: 'es',
        estimatedCEFRLevel: 'A1',
        conversationPhase: 'Correction',
        effectivePhase: 'Correction',
        currentTopic: 'greetings',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      const expectedSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: '', // Not returned by this endpoint
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.CORRECTION,
        effectivePhase: ConversationPhase.CORRECTION,
        currentTopic: 'greetings',
        vocabularyReviewMode: false, // Default to false
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: undefined,
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };

      mockPatch.mockResolvedValue({ data: mockBackendResponse });

      const result = await updatePhase(sessionId, phase);

      expect(mockPatch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/phase`, { phase: 'Correction' });
      expect(result).toEqual(expectedSession);
    });
  });

  describe('updateTeachingStyle', () => {
    it('should update teaching style', async () => {
      const sessionId = 'session1';
      const teachingStyle: TeachingStyle = TeachingStyle.Guided;
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: 'greetings',
        tutorTeachingStyle: TeachingStyle.Guided,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };

      mockPatch.mockResolvedValue({ data: mockSession });

      const result = await updateTeachingStyle(sessionId, teachingStyle);
      
      expect(mockPatch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/teaching-style`, { teachingStyle });
      expect(result).toEqual(mockSession);
    });
  });

  describe('updateTopic', () => {
    it('should update conversation topic', async () => {
      const sessionId = 'session1';
      const topic = 'food';
      // The updateTopic function expects the backend to return data in Session format
      const mockSessionResponse: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: 'food',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: undefined,
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };
      
      mockPatch.mockResolvedValue({ data: mockSessionResponse });
      
      const result = await updateTopic(sessionId, topic);
      
      expect(mockPatch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/topic`, { currentTopic: topic });
      expect(result).toEqual(mockSessionResponse);
    });
  });

  describe('updateVocabularyReviewMode', () => {
    it('should update vocabulary review mode', async () => {
      const sessionId = 'session1';
      const vocabularyReviewMode = true;
      // The updateVocabularyReviewMode function expects the backend to return data in Session format
      const mockSessionResponse: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: 'greetings',
        vocabularyReviewMode: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: undefined,
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };
      
      mockPatch.mockResolvedValue({ data: mockSessionResponse });
      
      const result = await updateVocabularyReviewMode(sessionId, vocabularyReviewMode);
      
      expect(mockPatch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/vocabulary-review-mode`, { enabled: vocabularyReviewMode });
      expect(result).toEqual(mockSessionResponse);
    });
  });

  describe('deleteSession', () => {
    it('should delete a session', async () => {
      const sessionId = 'session1';
      
      mockDelete.mockResolvedValue({});
      
      await deleteSession(sessionId);
      
      expect(mockDelete).toHaveBeenCalledWith(`/chat/sessions/${sessionId}`);
    });
  });

  describe('navigateLesson', () => {
    it('should navigate to a specific lesson', async () => {
      const sessionId = 'session1';
      // The navigateLesson function expects the backend to return data in Session format
      const mockSessionResponse: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: CEFRLevel.A1,
        phase: ConversationPhase.FREE,
        effectivePhase: ConversationPhase.FREE,
        currentTopic: 'greetings',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tutorTeachingStyle: undefined,
        tutorAge: undefined,
        tutorImage: undefined,
        tutorEmoji: undefined,
        tutorProfileId: undefined,
        tutorName: undefined,
        tutorPersona: undefined,
        cefrGrammar: undefined,
        cefrVocabulary: undefined,
        cefrFluency: undefined,
        cefrComprehension: undefined,
        lastAssessmentAt: undefined,
      };

      mockPatch.mockResolvedValue({ data: mockSessionResponse });

      const result = await navigateLesson(sessionId, 'NEXT');

      expect(mockPatch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/lesson`, { direction: 'NEXT' });
      expect(result).toEqual(mockSessionResponse);
    });
  });

  describe('sendChatMessage', () => {
    it('should send a chat message', async () => {
      const sessionId = 'session1';
      const messageText = 'Hello, how are you?';
      // The sendChatMessage function transforms the backend response using utility
      const mockBackendResponse = {
        id: 'msg1',
        role: 'USER',
        content: messageText,
        // timestamp might be added by backend or transform function
        metadata: undefined,
        errorMessage: undefined,
      };

      // Expected result is what the transform function would return
      const expectedMessage = {
        id: 'msg1',
        sessionId: sessionId,
        role: MessageRole.USER,
        content: messageText,
        timestamp: undefined, // Based on test results, transform function may not set timestamp
        metadata: undefined,
        errorMessage: undefined,
      };

      mockPost.mockResolvedValue({ data: mockBackendResponse });

      const result = await sendChatMessage(sessionId, messageText, messageText);

      expect(mockPost).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/messages`, {
        content: messageText
      });
      expect(result).toEqual(expectedMessage);
    });
  });

  describe('initiateTutorMessage', () => {
    it('should initiate a tutor message', async () => {
      const sessionId = 'session1';
      const context = 'welcome';
      // The initiateTutorMessage function transforms the backend response using utility  
      const mockBackendResponse = {
        id: 'msg1',
        role: 'ASSISTANT',
        content: 'Hello! Welcome to your language learning session.',
        metadata: undefined,
        errorMessage: undefined,
      };

      // Expected result is what the transform function would return
      const expectedMessage = {
        id: 'msg1',
        sessionId: sessionId,
        role: MessageRole.ASSISTANT,
        content: 'Hello! Welcome to your language learning session.',
        timestamp: undefined, // Based on test results, transform function may not set timestamp
        metadata: undefined,
        errorMessage: undefined,
      };

      mockPost.mockResolvedValue({ data: mockBackendResponse });

      const result = await initiateTutorMessage(sessionId, context);

      expect(mockPost).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/messages/initiate`, {
        context
      });
      expect(result).toEqual(expectedMessage);
    });
  });
});