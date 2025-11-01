import { useEffect, useRef, useState, useCallback } from 'react';
import { Bot, User, RotateCcw, Volume2, VolumeX, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import type { Message } from '../../types';
import { MessageRole, ConversationPhase } from '../../types';
import { CorrectedText } from './CorrectedText';
import WordCardList from './WordCardList';
import CharacterCardList from './CharacterCardList';
import MarkdownMessage from './MarkdownMessage';
import TutorImage from '../tutor/TutorImage';
import { useTTS } from '../../contexts/TTSContext';

interface MessageListProps {
  messages: Message[];
  isStreaming?: boolean;
  currentPhase?: ConversationPhase;
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
  console.log('🎨 MessageList: Props received', {
    tutorProfileId,
    tutorImage,
    tutorEmoji,
    tutorName,
    messageCount: messages.length
  });
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { available: ttsAvailable, playMessageAudio, isPlaying, currentMessageId, preferences } = useTTS();
  const [loadingAudio, setLoadingAudio] = useState<string | null>(null);
  const [selectedSpeed, setSelectedSpeed] = useState<Record<string, number>>({});
  const lastMessageIdRef = useRef<string | null>(null);
  const isInitialLoadRef = useRef(true);

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

  const handleRetry = (index: number) => {
    const userMessage = findPreviousUserMessage(index);
    if (userMessage) {
      onRetry(userMessage);
    }
  };

  const handlePlayAudio = useCallback(async (messageId: string) => {
    setLoadingAudio(messageId);
    try {
      const speed = selectedSpeed[messageId] || preferences.defaultSpeed;
      await playMessageAudio(sessionId, messageId, speed);
    } catch (error) {
      console.error('Failed to play audio:', error);
      toast.error('Could not play audio. Please try again.');
    } finally {
      setLoadingAudio(null);
    }
  }, [sessionId, selectedSpeed, preferences.defaultSpeed, playMessageAudio]);

  // Auto-play new assistant messages
  useEffect(() => {
    if (!ttsAvailable || !preferences.autoPlay || !preferences.enabled || messages.length === 0) {
      return;
    }

    // Skip on initial load
    if (isInitialLoadRef.current) {
      isInitialLoadRef.current = false;
      if (messages.length > 0) {
        const lastMessage = messages[messages.length - 1];
        lastMessageIdRef.current = lastMessage.id;
      }
      return;
    }

    // Get the last message
    const lastMessage = messages[messages.length - 1];

    // Check if it's a new ASSISTANT message
    if (
      lastMessage.role === MessageRole.ASSISTANT &&
      lastMessage.id !== lastMessageIdRef.current &&
      !isStreaming // Don't auto-play while still streaming
    ) {
      lastMessageIdRef.current = lastMessage.id;

      // Auto-play with a slight delay to ensure message is rendered
      const timeoutId = setTimeout(() => {
        handlePlayAudio(lastMessage.id);
      }, 300);

      return () => clearTimeout(timeoutId);
    } else if (lastMessage.role === MessageRole.ASSISTANT && isStreaming) {
      // Don't update lastMessageId while streaming an ASSISTANT message
      // We need to wait until streaming finishes to auto-play
    } else {
      // Update lastMessageId for USER messages or non-streaming cases
      lastMessageIdRef.current = lastMessage.id;
    }
  }, [messages, isStreaming, ttsAvailable, preferences.autoPlay, preferences.enabled, handlePlayAudio]);

  const handleSpeedChange = (messageId: string, speed: number) => {
    setSelectedSpeed(prev => ({ ...prev, [messageId]: speed }));
  };

  const getMessageSpeed = (messageId: string) => {
    return selectedSpeed[messageId] || preferences.defaultSpeed;
  };

  return (
    <div className="flex-1 space-y-6 overflow-y-auto p-4 sm:p-6 bg-gradient-to-b from-slate-50 to-white">
      {messages.map((message, index) => (
        <div
          key={message.id}
          className={`flex gap-3 ${
            message.role === MessageRole.USER ? 'justify-end' : 'justify-start'
          } animate-in fade-in duration-300`}
        >
          {/* Avatar for Assistant */}
          {message.role === MessageRole.ASSISTANT && (
            <div className="flex-shrink-0">
              {tutorProfileId ? (
                <TutorImage
                  tutorId={tutorProfileId}
                  tutorImageUrl={tutorImage}
                  tutorEmoji={tutorEmoji || ''}
                  tutorName={tutorName || 'Tutor'}
                  size="small"
                  rounded="full"
                  disableExpand={true}
                />
              ) : (
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-400 to-slate-500 flex items-center justify-center shadow-md">
                  <Bot className="w-5 h-5 text-white" />
                </div>
              )}
            </div>
          )}

          {/* Message Content */}
          <div className="flex flex-col gap-3 max-w-[85%]">
            {/* Message Bubble */}
            <div
              className={`px-4 py-3 shadow-soft ${message.errorMessage
                ? 'bg-red-50 text-red-900 border border-red-200 rounded-2xl rounded-tl-sm'
                : message.role === MessageRole.USER
                  ? 'bg-blue-50 text-slate-900 border border-blue-200 rounded-2xl rounded-tr-sm'
                  : message.role === MessageRole.ASSISTANT
                    ? 'bg-gray-100 text-slate-900 border border-gray-200 rounded-2xl rounded-tl-sm'
                    : 'bg-amber-50 text-slate-900 border border-amber-200 rounded-2xl'
              }`}
            >
              <div className="whitespace-pre-wrap break-words leading-relaxed">
                {message.errorMessage ? (
                  <div className="flex items-start gap-2">
                    <div className="text-red-500">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                    </div>
                    <span className="text-red-700">{message.errorMessage}</span>
                  </div>
                ) : message.role === MessageRole.USER &&
                  message.metadata?.corrections &&
                  message.metadata.corrections.length > 0 ? (
                  <CorrectedText
                    text={message.content}
                    corrections={message.metadata.corrections}
                  />
                ) : (
                  <MarkdownMessage content={message.content} />
                )}
              </div>

              {/* Audio Controls for Assistant Messages */}
              {message.role === MessageRole.ASSISTANT && ttsAvailable && (
                <div className="mt-3 flex items-center gap-3 pt-2 border-t border-gray-300">
                  <button
                    onClick={() => handlePlayAudio(message.id)}
                    disabled={loadingAudio === message.id}
                    className="flex items-center gap-1.5 px-2 py-1 text-xs font-medium text-slate-700 hover:text-slate-900 hover:bg-gray-200 rounded-md transition-colors disabled:opacity-50"
                    aria-label={
                      isPlaying && currentMessageId === message.id
                        ? 'Pause audio'
                        : 'Play audio'
                    }
                  >
                    {loadingAudio === message.id ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : isPlaying && currentMessageId === message.id ? (
                      <VolumeX className="w-3.5 h-3.5" />
                    ) : (
                      <Volume2 className="w-3.5 h-3.5" />
                    )}
                    <span className="hidden sm:inline">
                      {isPlaying && currentMessageId === message.id ? 'Stop' : 'Listen'}
                    </span>
                  </button>

                  {/* Speed Control */}
                  <div className="flex items-center gap-1">
                    {[0.75, 1.0, 1.25].map((speed) => (
                      <button
                        key={speed}
                        onClick={() => handleSpeedChange(message.id, speed)}
                        className={`px-2 py-0.5 text-xs font-medium rounded transition-colors ${
                          getMessageSpeed(message.id) === speed
                            ? 'bg-slate-700 text-white'
                            : 'text-slate-600 hover:bg-gray-200'
                        }`}
                        aria-label={`Set speed to ${speed}x`}
                      >
                        {speed}x
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div
                className={`mt-2 flex justify-between items-center text-xs ${
                  message.role === MessageRole.USER
                    ? 'text-slate-500'
                    : 'text-slate-500'
                }`}
              >
                <span>
                  {new Date(message.timestamp).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
                {message.errorMessage && (
                  <button
                    onClick={() => handleRetry(index)}
                    className="text-xs text-red-600 hover:text-red-800 flex items-center gap-1"
                    title="Retry message"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Retry</span>
                  </button>
                )}
              </div>
            </div>

            {/* Word Cards - outside bubble for full width */}
            {message.metadata?.wordCards && message.metadata.wordCards.length > 0 && (
              <WordCardList wordCards={message.metadata.wordCards} sessionId={sessionId} />
            )}

            {/* Character Cards - outside bubble for full width, after word cards */}
            {message.metadata?.characterCards && message.metadata.characterCards.length > 0 && (
              <CharacterCardList characterCards={message.metadata.characterCards} />
            )}
          </div>

          {/* Avatar for User */}
          {message.role === MessageRole.USER && (
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gradient-to-br from-slate-600 to-slate-700 flex items-center justify-center shadow-md">
              <User className="w-5 h-5 text-white" />
            </div>
          )}
        </div>
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
