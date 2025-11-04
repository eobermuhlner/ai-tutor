import { useState, useEffect, useMemo } from 'react';
import MessageList from './MessageList';
import ReengagementPrompt from './ReengagementPrompt';
import { useChatSession } from '../../contexts/ChatSessionContext';
import { useWelcomeMessage } from '../../hooks/useWelcomeMessage';
import type { Message } from '../../types';

function calculateDaysSince(timestamp: string): number {
  const lastDate = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - lastDate.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

export default function ChatMessagesPanel() {
  const {
    sessionId,
    messages,
    isSending,
    tutor,
    sessionLoaded,
    sendMessage,
    handleReengage: handleReengageContext,
    addMessage,
  } = useChatSession();

  const [showReengagePrompt, setShowReengagePrompt] = useState(false);

  // Calculate if we should show re-engagement prompt (7+ days inactive)
  const shouldShowReengage = useMemo(() => {
    if (messages.length === 0) return false;
    const lastMessage = messages[messages.length - 1];
    const daysSince = calculateDaysSince(lastMessage.timestamp);
    return daysSince >= 7;
  }, [messages]);

  // Update re-engagement prompt visibility
  useEffect(() => {
    setShowReengagePrompt(shouldShowReengage);
  }, [shouldShowReengage]);

  // Welcome message hook
  const {
    isStreaming: isWelcomeStreaming,
    streamingContent: welcomeStreamingContent,
    error: welcomeError,
    retry: retryWelcome,
  } = useWelcomeMessage({
    sessionId,
    hasMessages: messages.length > 0,
    sessionLoaded,
    useStreaming: false,
    onComplete: (message: Message) => {
      addMessage(message);
    },
  });

  const handleReengage = (message: Message) => {
    handleReengageContext(message);
    setShowReengagePrompt(false);
  };

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Re-engagement Prompt */}
      {showReengagePrompt && messages.length > 0 && (
        <ReengagementPrompt
          sessionId={sessionId}
          lastMessageAt={messages[messages.length - 1]?.timestamp}
          onReengage={handleReengage}
          onDismiss={() => setShowReengagePrompt(false)}
        />
      )}

      {/* Messages */}
      <MessageList
        messages={messages}
        isStreaming={isSending || isWelcomeStreaming}
        currentPhase={useChatSession().phase.current}
        sessionId={sessionId}
        onRetry={sendMessage}
        tutorProfileId={tutor.profileId}
        tutorImage={tutor.image}
        tutorEmoji={tutor.emoji}
        tutorName={tutor.name}
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
  );
}
