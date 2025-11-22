import { useEffect, useRef, useCallback } from 'react';
import { Bot } from 'lucide-react';
import toast from 'react-hot-toast';
import type { Message } from '../../types';
import { MessageRole } from '../../types';
import MessageItem from './MessageItem';
import MarkdownMessage from './MarkdownMessage';
import TutorImage from '../tutor/TutorImage';
import { useTTS } from '../../contexts/TTSContext';
import { useMessageAutoPlay } from '../../hooks/useMessageAutoPlay';
import { useAuthStore } from '../../store/authStore';

interface MessageListProps {
  messages: Message[];
  isStreaming?: boolean;
  currentPhase?: string;
  sessionId: string;
  onRetry: (text: string) => void;
  tutorProfileId?: string;
  tutorImage?: string | null;
  tutorEmoji?: string;
  tutorName?: string;
  streamingContent?: string;
}

export default function MessageList({
  messages,
  isStreaming,
  sessionId,
  onRetry,
  tutorProfileId,
  tutorImage,
  tutorEmoji,
  tutorName,
  streamingContent,
}: MessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { available: ttsAvailable, playMessageAudio, preferences } = useTTS();
  const user = useAuthStore((state) => state.user);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const findPreviousUserMessage = (currentIndex: number): string | null => {
    for (let i = currentIndex - 1; i >= 0; i--) {
      if (messages[i].role === MessageRole.USER) {
        return messages[i].content;
      }
    }
    return null;
  };

  const handlePlayAudio = useCallback(async (messageId: string) => {
    try {
      await playMessageAudio(sessionId, messageId, preferences.defaultSpeed);
    } catch (error) {
      console.error('Failed to play audio:', error);
      toast.error('Could not play audio. Please try again.');
    }
  }, [sessionId, preferences.defaultSpeed, playMessageAudio]);

  // Auto-play new assistant messages
  useMessageAutoPlay({
    messages,
    isStreaming: isStreaming || false,
    ttsAvailable,
    autoPlayEnabled: preferences.autoPlay,
    ttsEnabled: preferences.enabled,
    onAutoPlay: handlePlayAudio,
  });

  return (
    <div className="flex-1 space-y-6 overflow-y-auto p-4 sm:p-6 bg-gradient-to-b from-slate-50 to-white">
      {messages.map((message, index) => (
        <MessageItem
          key={message.id}
          message={message}
          sessionId={sessionId}
          onRetry={onRetry}
          previousUserMessage={findPreviousUserMessage(index)}
          tutorProfileId={tutorProfileId}
          tutorImage={tutorImage}
          tutorEmoji={tutorEmoji}
          tutorName={tutorName}
          userAvatarUrl={user?.avatarUrl}
        />
      ))}

      {/* Streaming Message or Typing Indicator */}
      {isStreaming && (
        <div className="flex gap-3 justify-start animate-in fade-in duration-300">
          {tutorProfileId ? (
            <div className="flex-shrink-0">
              <TutorImage
                tutorId={tutorProfileId}
                tutorImageUrl={tutorImage}
                tutorEmoji={tutorEmoji || ''}
                tutorName={tutorName || 'Tutor'}
                size="small"
                rounded="full"
                disableExpand={true}
              />
            </div>
          ) : (
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gradient-to-br from-slate-400 to-slate-500 flex items-center justify-center shadow-md">
              <Bot className="w-5 h-5 text-white" />
            </div>
          )}
          <div className="max-w-[85%] rounded-2xl rounded-tl-sm bg-gray-100 border border-gray-200 px-4 py-3 shadow-soft">
            {streamingContent ? (
              <div className="whitespace-pre-wrap break-words leading-relaxed">
                <MarkdownMessage content={streamingContent} />
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <div className="h-2 w-2 animate-bounce rounded-full bg-slate-500"></div>
                <div
                  className="h-2 w-2 animate-bounce rounded-full bg-slate-500"
                  style={{ animationDelay: '0.2s' }}
                ></div>
                <div
                  className="h-2 w-2 animate-bounce rounded-full bg-slate-500"
                  style={{ animationDelay: '0.4s' }}
                ></div>
              </div>
            )}
          </div>
        </div>
      )}
      <div ref={messagesEndRef} />
    </div>
  );
}
