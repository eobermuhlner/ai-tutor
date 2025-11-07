import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { getSession, sendChatMessage, updatePhase as updatePhaseAPI, updateTopic as updateTopicAPI, updateTeachingStyle as updateTeachingStyleAPI, updateVocabularyReviewMode as updateVocabularyReviewModeAPI } from '../api/chat';
import { getLanguages } from '../api/catalog';
import { getDueCount } from '../api/vocabulary';
import { getErrorMessage } from '../utils/errorHandling';
import { CEFRLevel, TeachingStyle } from '../types';
import type { Message, ConversationPhase, MessageRole, Language } from '../types';
import type { ChatSessionContextValue, TutorInfo, PhaseInfo, SkillLevels } from '../types/chat';

const ChatSessionContext = createContext<ChatSessionContextValue | null>(null);

interface ChatSessionProviderProps {
  children: ReactNode;
  sessionId: string;
}

export function ChatSessionProvider({ children, sessionId }: ChatSessionProviderProps) {
  const navigate = useNavigate();

  // Session state
  const [sessionLoaded, setSessionLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  
  // Session identity
  const [courseId, setCourseId] = useState<string | undefined>(undefined);
  const [courseName, setCourseName] = useState('');

  // Messages
  const [messages, setMessages] = useState<Message[]>([]);

  // Language
  const [targetLanguageCode, setTargetLanguageCode] = useState<string>('');
  const [languages, setLanguages] = useState<Language[]>([]);

  // Learning state
  const [userLevel, setUserLevel] = useState<CEFRLevel>(CEFRLevel.None);
  const [skillLevels, setSkillLevels] = useState<SkillLevels>({});
  const [currentPhase, setCurrentPhase] = useState<ConversationPhase>('FREE' as ConversationPhase);
  const [effectivePhase, setEffectivePhase] = useState<ConversationPhase>('FREE' as ConversationPhase);
  const [teachingStyle, setTeachingStyle] = useState<TeachingStyle>(TeachingStyle.Reactive);
  const [currentTopic, setCurrentTopic] = useState<string | null>(null);

  // Tutor info
  const [tutorName, setTutorName] = useState<string>('');
  const [tutorProfileId, setTutorProfileId] = useState<string>('');
  const [tutorImage, setTutorImage] = useState<string | null>(null);
  const [tutorAge, setTutorAge] = useState<number | null>(null);
  const [tutorEmoji, setTutorEmoji] = useState<string>('');

  // Vocabulary review
  const [vocabularyReviewMode, setVocabularyReviewMode] = useState(false);
  const [dueCount, setDueCount] = useState<number>(0);

  // UI refresh triggers
  const [summaryRefreshKey, setSummaryRefreshKey] = useState(0);
  const [rateLimitRefreshTrigger, setRateLimitRefreshTrigger] = useState(0);

  // Load session data
  useEffect(() => {
    if (!sessionId) return;

    setSessionLoaded(false);

    const loadSession = async () => {
      try {
        const [session, languagesData] = await Promise.all([
          getSession(sessionId),
          getLanguages(),
        ]);

        setMessages(session.messages || []);
        setCurrentPhase(session.phase);
        setEffectivePhase(session.effectivePhase);
        setCourseName(session.courseName);
        setCourseId(session.courseId || undefined);
        setCurrentTopic(session.currentTopic);
        setTargetLanguageCode(session.targetLanguageCode);
        setUserLevel(session.userLevel || CEFRLevel.None);
        setTutorName(session.tutorName || '');
        setTutorProfileId(session.tutorProfileId || '');
        setTutorImage(session.tutorImage || null);
        setTutorAge(session.tutorAge || null);
        setTutorEmoji(session.tutorEmoji || '');
        setTeachingStyle(session.tutorTeachingStyle || TeachingStyle.Reactive);
        setVocabularyReviewMode(session.vocabularyReviewMode || false);
        setSkillLevels({
          grammar: session.cefrGrammar || null,
          vocabulary: session.cefrVocabulary || null,
          fluency: session.cefrFluency || null,
          comprehension: session.cefrComprehension || null,
        });
        setLanguages(languagesData);

        // Load due count if vocabulary review mode is enabled
        if (session.vocabularyReviewMode) {
          try {
            const dueCountData = await getDueCount(session.targetLanguageCode);
            setDueCount(dueCountData.count);
          } catch {
            console.error('Failed to load due count');
          }
        }

        setSessionLoaded(true);
      } catch {
        toast.error('Failed to load session');
        navigate('/sessions');
      } finally {
        setIsLoading(false);
      }
    };

    loadSession();
  }, [sessionId, navigate]);

  // Actions
  const sendMessage = async (text: string) => {
    if (!sessionId) return;

    // Optimistically add user message
    const userMessage: Message = {
      id: crypto.randomUUID(),
      sessionId,
      role: 'USER' as MessageRole,
      content: text,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsSending(true);

    try {
      const assistantMessage = await sendChatMessage(sessionId, text, text);

      // If assistant message has corrections, move them to the user message
      if (assistantMessage.metadata?.corrections && assistantMessage.metadata.corrections.length > 0) {
        setMessages((prev) => {
          const updatedMessages = [...prev];
          const lastUserMsgIndex = updatedMessages.length - 1;
          const lastUserMsg = updatedMessages[lastUserMsgIndex];

          const updatedUserMsg = {
            ...lastUserMsg,
            metadata: {
              corrections: assistantMessage.metadata!.corrections,
              phase: assistantMessage.metadata!.phase,
            },
          };

          updatedMessages[lastUserMsgIndex] = updatedUserMsg;

          // Remove corrections from assistant message but keep wordCards
          const assistantWithoutCorrections = {
            ...assistantMessage,
            metadata: assistantMessage.metadata?.wordCards || assistantMessage.metadata?.characterCards
              ? {
                  corrections: [],
                  phase: assistantMessage.metadata.phase,
                  wordCards: assistantMessage.metadata.wordCards,
                  characterCards: assistantMessage.metadata.characterCards,
                }
              : undefined,
          };
          return [...updatedMessages, assistantWithoutCorrections];
        });
      } else {
        setMessages((prev) => [...prev, assistantMessage]);
      }

      // Refresh summary panel
      setSummaryRefreshKey((prev) => prev + 1);

      // Refresh overall CEFR level
      try {
        const updatedSession = await getSession(sessionId);
        setUserLevel(updatedSession.userLevel || CEFRLevel.None);
      } catch {
        console.error('Failed to refresh overall CEFR level');
      }

      // Refresh vocabulary due count if review mode is enabled
      if (vocabularyReviewMode && targetLanguageCode) {
        try {
          const dueCountData = await getDueCount(targetLanguageCode);
          setDueCount(dueCountData.count);
        } catch (error) {
          console.error('Failed to refresh due count:', error);
        }
      }

      // Trigger rate limit refresh
      setRateLimitRefreshTrigger((prev) => prev + 1);
    } catch (error) {
      const errorMessage = getErrorMessage(error, 'Failed to send message');
      toast.error(errorMessage);
      // Remove the user message on error
      setMessages((prev) => prev.slice(0, -1));
    } finally {
      setIsSending(false);
    }
  };

  const cancelSendMessage = () => {
    // In HTTP-based implementation, we can't truly cancel an in-flight request
    // But we can provide feedback to the user
    toast('Cannot cancel messages with current implementation', { icon: 'ℹ️' });
  };

  const updatePhase = async (phase: ConversationPhase) => {
    if (!sessionId || isSending) return;

    try {
      const updatedSession = await updatePhaseAPI(sessionId, phase);
      setCurrentPhase(updatedSession.phase);
      setEffectivePhase(updatedSession.effectivePhase);

      const phaseNames: Record<ConversationPhase, string> = {
        AUTO: 'Adaptive mode',
        FREE: 'Free conversation',
        CORRECTION: 'Correction mode',
        DRILL: 'Practice mode',
      };
      toast.success(`Switched to ${phaseNames[phase]}`);
    } catch {
      toast.error('Failed to change phase');
    }
  };

  const updateTopic = async (newTopic: string | null) => {
    if (!sessionId) return;

    try {
      await updateTopicAPI(sessionId, newTopic);
      setCurrentTopic(newTopic);
      toast.success('Topic updated');
    } catch (error) {
      toast.error('Failed to update topic');
      throw error;
    }
  };

  const updateTeachingStyle = async (style: TeachingStyle) => {
    if (!sessionId || isSending) return;

    try {
      await updateTeachingStyleAPI(sessionId, style);
      setTeachingStyle(style);

      const styleNames: Record<TeachingStyle, string> = {
        [TeachingStyle.Reactive]: 'Reactive style',
        [TeachingStyle.Guided]: 'Guided style',
        [TeachingStyle.Directive]: 'Directive style',
      };
      toast.success(`Switched to ${styleNames[style]}`);
    } catch {
      toast.error('Failed to change teaching style');
    }
  };

  const updateVocabularyReviewMode = async (enabled: boolean) => {
    if (!sessionId || isSending) return;

    try {
      await updateVocabularyReviewModeAPI(sessionId, enabled);
      setVocabularyReviewMode(enabled);

      // Load due count if enabling
      if (enabled && targetLanguageCode) {
        const dueCountData = await getDueCount(targetLanguageCode);
        setDueCount(dueCountData.count);
      }

      toast.success(enabled ? 'Vocabulary review mode enabled' : 'Vocabulary review mode disabled');
    } catch {
      toast.error('Failed to change vocabulary review mode');
    }
  };

  const refreshDueCount = async () => {
    if (targetLanguageCode) {
      try {
        const dueCountData = await getDueCount(targetLanguageCode);
        setDueCount(dueCountData.count);
      } catch (error) {
        console.error('Failed to refresh due count:', error);
      }
    }
  };

  const updateUserLevel = (newLevel: CEFRLevel) => {
    setUserLevel(newLevel);
  };

  const handleReengage = (message: Message) => {
    setMessages((prev) => [...prev, message]);
  };

  const addMessage = (message: Message) => {
    setMessages((prev) => [...prev, message]);
  };

  // Build tutor info object
  const tutor: TutorInfo = {
    profileId: tutorProfileId,
    name: tutorName,
    image: tutorImage,
    emoji: tutorEmoji,
    age: tutorAge,
  };

  // Build phase info object
  const phase: PhaseInfo = {
    current: currentPhase,
    effective: effectivePhase,
  };

  const value: ChatSessionContextValue = {
    // State
    sessionId,
    courseId,
    courseName,
    messages,
    isLoading,
    isSending,
    targetLanguageCode,
    languages,
    userLevel,
    skillLevels,
    phase,
    teachingStyle,
    currentTopic,
    tutor,
    vocabularyReviewMode,
    dueCount,
    summaryRefreshKey,
    rateLimitRefreshTrigger,
    sessionLoaded,

    // Actions
    sendMessage,
    updatePhase,
    updateTopic,
    updateTeachingStyle,
    updateVocabularyReviewMode,
    updateUserLevel,
    refreshDueCount,
    handleReengage,
    addMessage,
    cancelSendMessage,
  };

  return (
    <ChatSessionContext.Provider value={value}>
      {children}
    </ChatSessionContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useChatSession(): ChatSessionContextValue {
  const context = useContext(ChatSessionContext);
  if (!context) {
    throw new Error('useChatSession must be used within a ChatSessionProvider');
  }
  return context;
}
