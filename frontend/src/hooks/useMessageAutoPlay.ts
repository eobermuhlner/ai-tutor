import { useEffect, useRef } from 'react';
import { MessageRole } from '../types';
import type { Message } from '../types';

interface UseMessageAutoPlayOptions {
  messages: Message[];
  isStreaming: boolean;
  ttsAvailable: boolean;
  autoPlayEnabled: boolean;
  ttsEnabled: boolean;
  onAutoPlay: (messageId: string) => void;
}

const AUTO_PLAY_DELAY = 300; // ms - delay before auto-playing to ensure message is rendered

/**
 * Custom hook to handle automatic audio playback for new assistant messages
 */
export function useMessageAutoPlay({
  messages,
  isStreaming,
  ttsAvailable,
  autoPlayEnabled,
  ttsEnabled,
  onAutoPlay,
}: UseMessageAutoPlayOptions) {
  const lastMessageIdRef = useRef<string | null>(null);
  const isInitialLoadRef = useRef(true);

  useEffect(() => {
    if (!ttsAvailable || !autoPlayEnabled || !ttsEnabled || messages.length === 0) {
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
        onAutoPlay(lastMessage.id);
      }, AUTO_PLAY_DELAY);

      return () => clearTimeout(timeoutId);
    } else if (lastMessage.role === MessageRole.ASSISTANT && isStreaming) {
      // Don't update lastMessageId while streaming an ASSISTANT message
      // We need to wait until streaming finishes to auto-play
    } else {
      // Update lastMessageId for USER messages or non-streaming cases
      lastMessageIdRef.current = lastMessage.id;
    }
  }, [messages, isStreaming, ttsAvailable, autoPlayEnabled, ttsEnabled, onAutoPlay]);
}
