import { useState, useCallback } from 'react';
import { Bot, User, RotateCcw, Volume2, VolumeX, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { MessageRole } from '../../types';
import type { Message } from '../../types';
import { CorrectedText } from './CorrectedText';
import WordCardList from './WordCardList';
import CharacterCardList from './CharacterCardList';
import MarkdownMessage from './MarkdownMessage';
import TutorImage from '../tutor/TutorImage';
import { useTTS } from '../../contexts/TTSContext';
import { TTS_SPEED_OPTIONS } from '../../constants/chat';

interface MessageItemProps {
  message: Message;
  sessionId: string;
  onRetry?: (text: string) => void;
  previousUserMessage?: string | null;
  tutorProfileId?: string;
  tutorImage?: string | null;
  tutorEmoji?: string;
  tutorName?: string;
  userAvatarUrl?: string | null;
}

export default function MessageItem({
  message,
  sessionId,
  onRetry,
  previousUserMessage,
  tutorProfileId,
  tutorImage,
  tutorEmoji,
  tutorName,
  userAvatarUrl,
}: MessageItemProps) {
  const { available: ttsAvailable, playMessageAudio, isPlaying, currentMessageId, preferences } = useTTS();
  const [loadingAudio, setLoadingAudio] = useState(false);
  const [selectedSpeed, setSelectedSpeed] = useState<number>(preferences.defaultSpeed);

  const handlePlayAudio = useCallback(async () => {
    setLoadingAudio(true);
    try {
      await playMessageAudio(sessionId, message.id, selectedSpeed);
    } catch (error) {
      console.error('Failed to play audio:', error);
      toast.error('Could not play audio. Please try again.');
    } finally {
      setLoadingAudio(false);
    }
  }, [sessionId, message.id, selectedSpeed, playMessageAudio]);

  const handleRetry = () => {
    if (onRetry && previousUserMessage) {
      onRetry(previousUserMessage);
    }
  };

  const isAssistant = message.role === MessageRole.ASSISTANT;
  const isUser = message.role === MessageRole.USER;

  return (
    <div
      className={`flex gap-3 ${
        isUser ? 'justify-end' : 'justify-start'
      } animate-in fade-in duration-300`}
    >
      {/* Avatar for Assistant */}
      {isAssistant && (
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
            : isUser
              ? 'bg-blue-50 text-slate-900 border border-blue-200 rounded-2xl rounded-tr-sm'
              : isAssistant
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
            ) : isUser && message.metadata?.corrections && message.metadata.corrections.length > 0 ? (
              <CorrectedText
                text={message.content}
                corrections={message.metadata.corrections}
              />
            ) : (
              <MarkdownMessage content={message.content} />
            )}
          </div>

          {/* Audio Controls for Assistant Messages */}
          {isAssistant && ttsAvailable && (
            <div className="mt-3 flex items-center gap-3 pt-2 border-t border-gray-300">
              <button
                onClick={handlePlayAudio}
                disabled={loadingAudio}
                className="flex items-center gap-1.5 px-2 py-1 text-xs font-medium text-slate-700 hover:text-slate-900 hover:bg-gray-200 rounded-md transition-colors disabled:opacity-50"
                aria-label={
                  isPlaying && currentMessageId === message.id
                    ? 'Pause audio'
                    : 'Play audio'
                }
              >
                {loadingAudio ? (
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
                {TTS_SPEED_OPTIONS.map((speed) => (
                  <button
                    key={speed}
                    onClick={() => setSelectedSpeed(speed)}
                    className={`px-2 py-0.5 text-xs font-medium rounded transition-colors ${
                      selectedSpeed === speed
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
              isUser ? 'text-slate-500' : 'text-slate-500'
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
                onClick={handleRetry}
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
      {isUser && (
        <div className="flex-shrink-0">
          {userAvatarUrl ? (
            <img
              src={userAvatarUrl}
              alt="User Avatar"
              className="w-8 h-8 rounded-full object-cover shadow-md border border-slate-300"
            />
          ) : (
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-600 to-slate-700 flex items-center justify-center shadow-md">
              <User className="w-5 h-5 text-white" />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
