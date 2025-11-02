import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Layout from '../components/layout/Layout';
import MessageList from '../components/chat/MessageList';
import MessageInput from '../components/chat/MessageInput';
import PhaseSelector from '../components/chat/PhaseSelector';
import TeachingStyleSelector from '../components/chat/TeachingStyleSelector';
import EditableTopic from '../components/chat/EditableTopic';
import SessionSummaryPanel from '../components/chat/SessionSummaryPanel';
import VocabularyReviewPanel from '../components/vocabulary/VocabularyReviewPanel';
import SkillBreakdownBadge from '../components/assessment/SkillBreakdownBadge';
import LessonPanel from '../components/chat/LessonPanel';
import TTSSettings from '../components/chat/TTSSettings';
import RateLimitIndicator from '../components/profile/RateLimitIndicator';
import TutorImage from '../components/tutor/TutorImage';
import Spinner from '../components/ui/Spinner';
import Button from '../components/ui/Button';
import ReengagementPrompt from '../components/chat/ReengagementPrompt';
import { getSession, sendChatMessage, updatePhase, updateTopic, updateTeachingStyle, updateVocabularyReviewMode } from '../api/chat';
import { getLanguages } from '../api/catalog';
import { getDueCount } from '../api/vocabulary';
import { triggerReassessment } from '../api/assessment';
import { formatCompactLanguageDisplay } from '../utils/languageDisplay';
import { getErrorMessage } from '../utils/errorHandling';
import { useWelcomeMessage } from '../hooks/useWelcomeMessage';
import { TeachingStyle } from '../types';
import { CEFRLevel } from '../types';
import type { Message, ConversationPhase, MessageRole, Language } from '../types';

function calculateDaysSince(timestamp: string): number {
  const lastDate = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - lastDate.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

export default function ChatPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [messages, setMessages] = useState<Message[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionLoaded, setSessionLoaded] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [currentPhase, setCurrentPhase] = useState<ConversationPhase>('FREE' as ConversationPhase);
  const [effectivePhase, setEffectivePhase] = useState<ConversationPhase>('FREE' as ConversationPhase);
  const [courseName, setCourseName] = useState('');
  const [courseId, setCourseId] = useState<string | undefined>(undefined);
  const [currentTopic, setCurrentTopic] = useState<string | null>(null);
  const [targetLanguageCode, setTargetLanguageCode] = useState<string>('');
  const [userLevel, setUserLevel] = useState<CEFRLevel>(CEFRLevel.None);
  const [tutorName, setTutorName] = useState<string>('');
  const [tutorProfileId, setTutorProfileId] = useState<string>('');
  const [tutorImage, setTutorImage] = useState<string | null>(null);
  const [tutorAge, setTutorAge] = useState<number | null>(null);
  const [tutorEmoji, setTutorEmoji] = useState<string>('');

  const [teachingStyle, setTeachingStyle] = useState<TeachingStyle>(TeachingStyle.Reactive);
  const [summaryRefreshKey, setSummaryRefreshKey] = useState(0);
  const [showSummaryPanel, setShowSummaryPanel] = useState(true);
  const [vocabularyReviewMode, setVocabularyReviewMode] = useState(false);
  const [dueCount, setDueCount] = useState<number>(0);
  const [sidebarTab, setSidebarTab] = useState<'summary' | 'review' | 'settings' | 'lesson'>('settings');
  const [skillLevels, setSkillLevels] = useState<{
    grammar?: CEFRLevel | null;
    vocabulary?: CEFRLevel | null;
    fluency?: CEFRLevel | null;
    comprehension?: CEFRLevel | null;
  }>({});
  const [showReengagePrompt, setShowReengagePrompt] = useState(false);

  // Calculate if we should show re-engagement prompt (7+ days inactive)
  const shouldShowReengage = useMemo(() => {
    if (messages.length === 0) return false;
    const lastMessage = messages[messages.length - 1];
    const daysSince = calculateDaysSince(lastMessage.timestamp);
    return daysSince >= 7;
  }, [messages]);

  // Use welcome message hook for new sessions
  // Using non-streaming for simplicity and reliability
  // Wait for session to load before checking if we need welcome message
  const { isStreaming: isWelcomeStreaming, streamingContent: welcomeStreamingContent, error: welcomeError, retry: retryWelcome } = useWelcomeMessage({
    sessionId: sessionId || '',
    hasMessages: messages.length > 0,
    sessionLoaded, // Only initiate after we know if session has messages
    useStreaming: false, // Non-streaming is simpler and sufficient for welcome messages
    onComplete: (message) => {
      setMessages((prev) => [...prev, message]);
    },
  });

  useEffect(() => {
    if (!sessionId) return;

    // Reset session loaded state when sessionId changes
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

        // Mark session as loaded after all state is set
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

  // Update re-engagement prompt visibility based on inactivity
  useEffect(() => {
    setShowReengagePrompt(shouldShowReengage);
  }, [shouldShowReengage]);

  const handleReengage = (message: Message) => {
    setMessages((prev) => [...prev, message]);
    setShowReengagePrompt(false);
  };

  const handleSendMessage = async (text: string) => {
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

      // If assistant message has corrections, they're for the user's message
      // Move them to the user message instead
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
            metadata: assistantMessage.metadata?.wordCards
              ? {
                  corrections: [],
                  phase: assistantMessage.metadata.phase,
                  wordCards: assistantMessage.metadata.wordCards,
                }
              : undefined,
          };
          return [...updatedMessages, assistantWithoutCorrections];
        });
      } else {
        setMessages((prev) => [...prev, assistantMessage]);
      }

      // Refresh summary panel again after assistant response (for compression stats)
      setSummaryRefreshKey(prev => prev + 1);

      // Refresh overall CEFR level after message (overall level still updates after each message)
      // but don't update detailed skill levels (grammar, vocabulary, fluency, comprehension)
      try {
        const updatedSession = await getSession(sessionId);
        // Only update the overall user level, not the detailed skill levels
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
    } catch (error) {
      const errorMessage = getErrorMessage(error, 'Failed to send message');
      toast.error(errorMessage);
      // Remove the user message on error
      setMessages((prev) => prev.slice(0, -1));
    } finally {
      setIsSending(false);
    }
  };

  const handlePhaseChange = async (phase: ConversationPhase) => {
    if (!sessionId || isSending) return;

    try {
      const updatedSession = await updatePhase(sessionId, phase);
      setCurrentPhase(updatedSession.phase);
      setEffectivePhase(updatedSession.effectivePhase);

      // Show appropriate message
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

  const handleTopicChange = async (newTopic: string | null) => {
    if (!sessionId) return;

    try {
      await updateTopic(sessionId, newTopic);
      setCurrentTopic(newTopic);
      toast.success('Topic updated');
    } catch (error) {
      toast.error('Failed to update topic');
      throw error; // Re-throw to let EditableTopic handle it
    }
  };

  const handleTeachingStyleChange = async (style: TeachingStyle) => {
    if (!sessionId || isSending) return;

    try {
      await updateTeachingStyle(sessionId, style);
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

  const handleVocabularyReviewModeChange = async (enabled: boolean) => {
    if (!sessionId || isSending) return;

    try {
      await updateVocabularyReviewMode(sessionId, enabled);
      setVocabularyReviewMode(enabled);

      // Load due count if enabling
      if (enabled && targetLanguageCode) {
        const dueCountData = await getDueCount(targetLanguageCode);
        setDueCount(dueCountData.count);
        // Switch to review tab when enabling
        setSidebarTab('review');
        setShowSummaryPanel(true);
      } else {
        // Switch back to summary tab when disabling
        setSidebarTab('summary');
      }

      toast.success(enabled ? 'Vocabulary review mode enabled' : 'Vocabulary review mode disabled');
    } catch {
      toast.error('Failed to change vocabulary review mode');
    }
  };

  const handleReviewComplete = async () => {
    // Refresh due count after a review
    if (targetLanguageCode) {
      try {
        const dueCountData = await getDueCount(targetLanguageCode);
        setDueCount(dueCountData.count);
      } catch (error) {
        console.error('Failed to refresh due count:', error);
      }
    }
  };

  // @ts-expect-error - Function may be used in future updates
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleManualReassessment = async () => {
    if (!sessionId) return;

    try {
      const skillBreakdown = await triggerReassessment(sessionId);

      // Update the skill levels state using the data from reassessment
      setSkillLevels({
        grammar: skillBreakdown.grammar as CEFRLevel | null,
        vocabulary: skillBreakdown.vocabulary as CEFRLevel | null,
        fluency: skillBreakdown.fluency as CEFRLevel | null,
        comprehension: skillBreakdown.comprehension as CEFRLevel | null,
      });

      // Show success message
      toast.success('Skills assessment updated successfully');
    } catch (error) {
      console.error('Failed to trigger reassessment:', error);
      toast.error('Failed to update skills assessment');
    }
  };



  if (isLoading) {
    return (
      <Layout>
        <div className="flex min-h-[60vh] items-center justify-center">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  const currentLanguage = languages.find((lang) => lang.code === targetLanguageCode);
  const languageDisplay = currentLanguage
    ? formatCompactLanguageDisplay(currentLanguage)
    : (targetLanguageCode ? targetLanguageCode.toUpperCase() : '');

  return (
    <Layout>
      <div className="relative flex gap-4">
        <div className="flex-1 flex h-[calc(100vh-8rem)] flex-col rounded-2xl border border-slate-200 bg-white shadow-soft-lg overflow-hidden">
          {/* Header */}
          <div className="relative z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 backdrop-blur-sm px-6 py-4">
            <div className="flex items-center gap-4">
              <Button onClick={() => navigate('/sessions')} variant="ghost" size="sm">
                ← Back
              </Button>
              <div className="flex items-center gap-3">
                {tutorProfileId && (
                  <div className="flex-shrink-0">
                    <TutorImage
                      tutorId={tutorProfileId}
                      tutorImageUrl={tutorImage}
                      tutorEmoji={tutorEmoji}
                      tutorName={tutorName}
                      size="medium"
                      rounded="lg"
                    />
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-3">
                    {languageDisplay && (
                      <>
                        <span className="text-sm font-medium text-slate-600">
                          {languageDisplay}
                        </span>
                        <span className="text-slate-300">•</span>
                      </>
                    )}
                    <h1 className="text-xl font-semibold text-slate-900">{tutorName || courseName}</h1>
                    {tutorAge && (
                      <>
                        <span className="text-slate-300">•</span>
                        <span className="text-sm text-slate-600">Age {tutorAge}</span>
                      </>
                    )}
                  </div>
                  {(userLevel || currentTopic !== undefined) && (
                    <div className="flex items-center gap-3 mt-1">
                      {userLevel && (
                        <SkillBreakdownBadge
                          overall={userLevel}
                          grammar={skillLevels.grammar}
                          vocabulary={skillLevels.vocabulary}
                          fluency={skillLevels.fluency}
                          comprehension={skillLevels.comprehension}
                        />
                      )}
                      <>
                        {userLevel && <span className="text-slate-300">•</span>}
                        <EditableTopic
                          topic={currentTopic}
                          onSave={handleTopicChange}
                          disabled={isSending}
                        />
                      </>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Messages Container */}
          <div className="flex-1 flex flex-col overflow-hidden">
            {/* Re-engagement Prompt */}
            {showReengagePrompt && messages.length > 0 && (
              <ReengagementPrompt
                sessionId={sessionId || ''}
                lastMessageAt={messages[messages.length - 1]?.timestamp}
                onReengage={handleReengage}
                onDismiss={() => setShowReengagePrompt(false)}
              />
            )}

            {/* Messages */}
            <MessageList
              messages={messages}
              isStreaming={isSending || isWelcomeStreaming}
              currentPhase={currentPhase}
              sessionId={sessionId || ''}
              onRetry={handleSendMessage}
              tutorProfileId={tutorProfileId}
              tutorImage={tutorImage}
              tutorEmoji={tutorEmoji}
              tutorName={tutorName}
              streamingContent={welcomeStreamingContent}
            />

            {/* Welcome Error */}
            {welcomeError && !messages.length && (
              <div className="mx-4 mb-4 rounded-lg border border-red-200 bg-red-50 p-4">
                <div className="flex items-start gap-3">
                  <svg className="h-5 w-5 flex-shrink-0 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <div className="flex-1">
                    <h3 className="text-sm font-semibold text-red-800">Couldn't load welcome message</h3>
                    <p className="mt-1 text-sm text-red-700">{welcomeError}</p>
                    <div className="mt-3 flex gap-3">
                      <button
                        onClick={retryWelcome}
                        className="text-sm font-medium text-red-600 hover:text-red-800"
                      >
                        Try Again
                      </button>
                      <button
                        onClick={() => {/* Skip and allow user to start typing */}}
                        className="text-sm font-medium text-red-600 hover:text-red-800"
                      >
                        Skip & Start Chatting
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Input */}
          <MessageInput
            onSend={handleSendMessage}
            disabled={isSending || isWelcomeStreaming}
            languageCode={targetLanguageCode}
          />
        </div>

        {/* Sidebar with Tabs - Slide-out design */}
        {sessionId && (
          <div
            className={`flex-shrink-0 transition-all duration-300 ease-in-out ${
              showSummaryPanel ? 'w-80 opacity-100' : 'w-0 opacity-0'
            } overflow-hidden`}
          >
            <div className="h-full flex flex-col rounded-2xl border border-slate-200 bg-white shadow-soft-lg">
              {/* Tab Navigation */}
              <div className="flex border-b border-slate-200">
                <button
                  onClick={() => setSidebarTab('settings')}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                    sidebarTab === 'settings'
                      ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Settings
                </button>
                <button
                  onClick={() => setSidebarTab('lesson')}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                    sidebarTab === 'lesson'
                      ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Lesson
                </button>
                <button
                  onClick={() => setSidebarTab('summary')}
                  className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                    sidebarTab === 'summary'
                      ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Summary
                </button>
                {vocabularyReviewMode && (
                  <button
                    onClick={() => setSidebarTab('review')}
                    className={`flex-1 px-4 py-3 text-sm font-medium transition-colors relative ${
                      sidebarTab === 'review'
                        ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    }`}
                  >
                    Review
                    {dueCount > 0 && (
                      <span className="absolute top-2 right-2 inline-flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-brand-600 rounded-full">
                        {dueCount}
                      </span>
                    )}
                  </button>
                )}
              </div>

              {/* Tab Content */}
              <div className="flex-1 overflow-y-auto p-4">
                {sidebarTab === 'lesson' && (
                  <LessonPanel sessionId={sessionId || ''} courseId={courseId} />
                )}
                {sidebarTab === 'summary' && (
                  <SessionSummaryPanel
                    key={summaryRefreshKey}
                    sessionId={sessionId}
                    optimisticMessageCount={messages.length}
                  />
                )}
                {sidebarTab === 'review' && vocabularyReviewMode && (
                  <VocabularyReviewPanel
                    targetLanguageCode={targetLanguageCode}
                    isVisible={true}
                    onReviewComplete={handleReviewComplete}
                  />
                )}
                {sidebarTab === 'settings' && (
                  <div className="space-y-6">
                    <div>
                      <h3 className="text-sm font-semibold text-slate-900 mb-3">Conversation Mode</h3>
                      <PhaseSelector
                        currentPhase={currentPhase}
                        effectivePhase={effectivePhase}
                        onPhaseChange={handlePhaseChange}
                        disabled={isSending}
                      />
                    </div>

                    <div className="border-t border-slate-200 pt-6">
                      <h3 className="text-sm font-semibold text-slate-900 mb-3">Teaching Style</h3>
                      <TeachingStyleSelector
                        currentStyle={teachingStyle}
                        onStyleChange={handleTeachingStyleChange}
                        disabled={isSending}
                      />
                    </div>

                    <div className="border-t border-slate-200 pt-6">
                      <h3 className="text-sm font-semibold text-slate-900 mb-3">Vocabulary Review</h3>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-sm text-slate-700">Enable review mode</span>
                          {vocabularyReviewMode && dueCount > 0 && (
                            <span className="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none text-white bg-brand-600 rounded-full">
                              {dueCount} due
                            </span>
                          )}
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                          <input
                            type="checkbox"
                            checked={vocabularyReviewMode}
                            onChange={(e) => handleVocabularyReviewModeChange(e.target.checked)}
                            disabled={isSending}
                            className="sr-only peer"
                          />
                          <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
                        </label>
                      </div>
                    </div>

                    <div className="border-t border-slate-200 pt-6">
                      <TTSSettings />
                    </div>

                    <div className="border-t border-slate-200 pt-6">
                      <RateLimitIndicator />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Toggle Tab - Always visible on right edge */}
        {sessionId && (
          <button
            onClick={() => setShowSummaryPanel(!showSummaryPanel)}
            className="absolute right-0 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1.5 rounded-l-lg border border-r-0 border-slate-200 bg-white px-2.5 py-3 text-slate-600 hover:bg-slate-50 hover:text-brand-600 transition-all shadow-md z-10"
            title={showSummaryPanel ? 'Hide summary stats' : 'Show summary stats'}
            style={{ right: showSummaryPanel ? '320px' : '0' }}
          >
            {/* Chart/Stats Icon */}
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
              />
            </svg>
            {/* Chevron */}
            <svg
              className={`w-3 h-3 transition-transform ${showSummaryPanel ? 'rotate-0' : 'rotate-180'}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        )}
      </div>
    </Layout>
  );
}
