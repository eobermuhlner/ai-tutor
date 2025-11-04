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
import apiClient from './client';
import type { Session, Message, SessionProgress, ConversationPhase, TeachingStyle } from '../types';

// Define the mock functions
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPatch = vi.fn();
const mockDelete = vi.fn();

vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    patch: mockPatch,
    delete: mockDelete,
  },
}));

describe('chat API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createSessionFromCourse', () => {
    it('should create a new session from course', async () => {
      const request = {
        courseId: 'course1',
        targetLanguageCode: 'es',
        userLevel: 'A1'
      };
      
      const mockSession: Session = {
        id: 'session1',
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: 'A1',
        phase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: null,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockSession });
      
      const result = await createSessionFromCourse(request);
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/chat/sessions/from-course', {
        courseTemplateId: request.courseId,
        targetLanguageCode: request.targetLanguageCode,
        sourceLanguageCode: 'en', // default value if not provided
        userLevel: request.userLevel,
        customName: undefined,
        tutorProfileId: undefined
      });
      expect(result).toEqual(mockSession);
    });
  });

  describe('getSessions', () => {
    it('should fetch user sessions', async () => {
      const userId = 'user1';
      const mockResponse = [
        {
          session: {
            id: 'session1',
            userId: userId,
            courseTemplateId: 'course1',
            courseTemplateName: 'Spanish for Beginners',
            targetLanguageCode: 'es',
            sourceLanguageCode: 'en',
            userLevel: 'A1',
            conversationPhase: 'FREE',
            effectivePhase: 'FREE',
            currentTopic: null,
            vocabularyReviewMode: false,
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
      
      const mockSessions = [
        {
          id: 'session1',
          userId: userId,
          courseId: 'course1',
          courseName: 'Spanish for Beginners',
          targetLanguageCode: 'es',
          sourceLanguageCode: 'en',
          userLevel: 'A1',
          phase: 'FREE',
          effectivePhase: 'FREE',
          currentTopic: null,
          vocabularyReviewMode: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockResponse });
      
      const result = await getSessions(userId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/chat/sessions', { params: { userId } });
      expect(result).toEqual(mockSessions);
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
            courseTemplateName: 'Spanish for Beginners',
            targetLanguageCode: 'es',
            sourceLanguageCode: 'en',
            userLevel: 'A1',
            conversationPhase: 'FREE',
            effectivePhase: 'FREE',
            currentTopic: null,
            vocabularyReviewMode: false,
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
      
      const mockSessions = [
        {
          id: 'session1',
          userId: userId,
          courseId: 'course1',
          courseName: 'Spanish for Beginners',
          targetLanguageCode: 'es',
          sourceLanguageCode: 'en',
          userLevel: 'A1',
          phase: 'FREE',
          effectivePhase: 'FREE',
          currentTopic: null,
          vocabularyReviewMode: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockResponse });
      
      const result = await getActiveSessions(userId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/chat/sessions/active', { params: { userId } });
      expect(result).toEqual(mockSessions);
    });
  });

  describe('getSession', () => {
    it('should fetch a specific session with messages', async () => {
      const sessionId = 'session1';
      const mockBackendSession = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        courseTemplateName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        conversationPhase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: null,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        messages: [
          {
            id: 'msg1',
            role: 'ASSISTANT',
            content: 'Hello! How can I help you today?',
            timestamp: new Date().toISOString(),
            metadata: undefined,
            errorMessage: undefined,
          }
        ]
      };
      
      const mockSession = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        phase: 'FREE' as ConversationPhase,
        effectivePhase: 'FREE' as ConversationPhase,
        currentTopic: null,
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        messages: [
          {
            id: 'msg1',
            sessionId: sessionId,
            role: 'ASSISTANT',
            content: 'Hello! How can I help you today?',
            timestamp: new Date().toISOString(),
            metadata: undefined,
            errorMessage: undefined,
          }
        ]
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockBackendSession });
      
      const result = await getSession(sessionId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/chat/sessions/${sessionId}`);
      expect(result).toEqual(mockSession);
    });
  });

  describe('getSessionProgress', () => {
    it('should fetch session progress', async () => {
      const sessionId = 'session1';
      const mockProgress: SessionProgress = {
        sessionId: sessionId,
        totalMessages: 25,
        correctionsReceived: 5,
        vocabularyLearned: 10,
        currentCEFRLevel: 'A1',
        timeSpent: 1200, // seconds
        lastActive: new Date().toISOString(),
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockProgress });
      
      const result = await getSessionProgress(sessionId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/progress`);
      expect(result).toEqual(mockProgress);
    });
  });

  describe('updatePhase', () => {
    it('should update conversation phase', async () => {
      const sessionId = 'session1';
      const phase: ConversationPhase = 'CORRECTION';
      const mockBackendSession = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        courseTemplateName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        conversationPhase: 'CORRECTION',
        effectivePhase: 'CORRECTION',
        currentTopic: 'greetings',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        phase: 'CORRECTION',
        effectivePhase: 'CORRECTION',
        currentTopic: 'greetings',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.patch as any).mockResolvedValue({ data: mockBackendSession });
      
      const result = await updatePhase(sessionId, phase);
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/phase`, { phase: 'Correction' });
      expect(result).toEqual(mockSession);
    });
  });

  describe('updateTeachingStyle', () => {
    it('should update teaching style', async () => {
      const sessionId = 'session1';
      const teachingStyle: TeachingStyle = 'Guided';
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        userLevel: 'A1',
        phase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'greetings',
        tutorTeachingStyle: 'Guided',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.patch as any).mockResolvedValue({ data: mockSession });
      
      const result = await updateTeachingStyle(sessionId, teachingStyle);
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/teaching-style`, { teachingStyle });
      expect(result).toEqual(mockSession);
    });
  });

  describe('updateTopic', () => {
    it('should update conversation topic', async () => {
      const sessionId = 'session1';
      const topic = 'food';
      const mockBackendSession = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        courseTemplateName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        conversationPhase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'food',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        phase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'food',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.patch as any).mockResolvedValue({ data: mockBackendSession });
      
      const result = await updateTopic(sessionId, topic);
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/topic`, { currentTopic: topic });
      expect(result).toEqual(mockSession);
    });
  });

  describe('updateVocabularyReviewMode', () => {
    it('should update vocabulary review mode', async () => {
      const sessionId = 'session1';
      const vocabularyReviewMode = true;
      const mockBackendSession = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        courseTemplateName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        conversationPhase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'greetings',
        vocabularyReviewMode: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        phase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'greetings',
        vocabularyReviewMode: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.patch as any).mockResolvedValue({ data: mockBackendSession });
      
      const result = await updateVocabularyReviewMode(sessionId, vocabularyReviewMode);
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/vocabulary-review-mode`, { enabled: vocabularyReviewMode });
      expect(result).toEqual(mockSession);
    });
  });

  describe('deleteSession', () => {
    it('should delete a session', async () => {
      const sessionId = 'session1';
      
      (mockApiClient.delete as any).mockResolvedValue({});
      
      await deleteSession(sessionId);
      
      expect(mockApiClient.delete).toHaveBeenCalledWith(`/chat/sessions/${sessionId}`);
    });
  });

  describe('navigateLesson', () => {
    it('should navigate to a specific lesson', async () => {
      const sessionId = 'session1';
      const lessonIndex = 2;
      const mockBackendSession = {
        id: sessionId,
        userId: 'user1',
        courseTemplateId: 'course1',
        courseTemplateName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        conversationPhase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'greetings',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      const mockSession: Session = {
        id: sessionId,
        userId: 'user1',
        courseId: 'course1',
        courseName: 'Spanish for Beginners',
        targetLanguageCode: 'es',
        sourceLanguageCode: 'en',
        userLevel: 'A1',
        phase: 'FREE',
        effectivePhase: 'FREE',
        currentTopic: 'greetings',
        vocabularyReviewMode: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBackendSession });
      
      const result = await navigateLesson(sessionId, lessonIndex);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/navigate`, { lessonIndex });
      expect(result).toEqual(mockSession);
    });
  });

  describe('sendChatMessage', () => {
    it('should send a chat message', async () => {
      const sessionId = 'session1';
      const messageText = 'Hello, how are you?';
      const mockBackendMessage = {
        id: 'msg1',
        role: 'USER',
        content: messageText,
        timestamp: new Date().toISOString(),
        metadata: undefined,
        errorMessage: undefined,
      };
      
      const mockMessage: Message = {
        id: 'msg1',
        sessionId: sessionId,
        role: 'USER',
        content: messageText,
        timestamp: new Date().toISOString(),
        metadata: undefined,
        errorMessage: undefined,
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBackendMessage });
      
      const result = await sendChatMessage(sessionId, messageText);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/messages`, { 
        content: messageText 
      });
      expect(result).toEqual(mockMessage);
    });
  });

  describe('initiateTutorMessage', () => {
    it('should initiate a tutor message', async () => {
      const sessionId = 'session1';
      const context = 'welcome';
      const mockBackendMessage = {
        id: 'msg1',
        role: 'ASSISTANT',
        content: 'Hello! Welcome to your language learning session.',
        timestamp: new Date().toISOString(),
        metadata: undefined,
        errorMessage: undefined,
      };
      
      const mockMessage: Message = {
        id: 'msg1',
        sessionId: sessionId,
        role: 'ASSISTANT',
        content: 'Hello! Welcome to your language learning session.',
        timestamp: new Date().toISOString(),
        metadata: undefined,
        errorMessage: undefined,
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBackendMessage });
      
      const result = await initiateTutorMessage(sessionId, context);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/chat/sessions/${sessionId}/messages/initiate`, { 
        context 
      });
      expect(result).toEqual(mockMessage);
    });
  });
});