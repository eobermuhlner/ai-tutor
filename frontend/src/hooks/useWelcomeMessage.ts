import { useState, useEffect, useRef } from 'react';
import { initiateTutorMessage, initiateTutorMessageStream } from '../api/chat';
import type { Message, InitiateMessageContext } from '../types';

interface UseWelcomeMessageOptions {
  sessionId: string;
  hasMessages: boolean;
  sessionLoaded: boolean; // Wait for session to load before initiating
  onComplete: (message: Message) => void;
  context?: InitiateMessageContext;
  useStreaming?: boolean;
}

interface UseWelcomeMessageResult {
  isStreaming: boolean;
  streamingContent: string;
  error: string | null;
  retry: () => void;
}

export function useWelcomeMessage({
  sessionId,
  hasMessages,
  sessionLoaded,
  onComplete,
  context = 'welcome',
  useStreaming = false,
}: UseWelcomeMessageOptions): UseWelcomeMessageResult {
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  const [error, setError] = useState<string | null>(null);
  const initiatedRef = useRef(false);
  const controllerRef = useRef<AbortController | null>(null);
  const isCleanupSafeRef = useRef(false);

  async function initiateNonStreaming() {
    initiatedRef.current = true;
    setIsStreaming(true);
    setStreamingContent('');
    setError(null);

    try {
      const message = await initiateTutorMessage(sessionId, context);
      setIsStreaming(false);
      onComplete(message);
    } catch (err: any) {
      setIsStreaming(false);
      setError(err.message || 'Failed to load welcome message');
    }
  }

  function initiateStreaming() {
    console.log('✨ Starting welcome message stream...');
    initiatedRef.current = true;
    isCleanupSafeRef.current = false; // Not safe to cleanup yet
    setIsStreaming(true);
    setStreamingContent('');
    setError(null);

    controllerRef.current = initiateTutorMessageStream(
      sessionId,
      context,
      // onChunk
      (chunk) => {
        isCleanupSafeRef.current = true; // Now we're actually streaming, safe to cleanup
        console.log('📝 Chunk received:', chunk);
        setStreamingContent((prev) => prev + chunk);
      },
      // onComplete
      (message) => {
        console.log('✅ Welcome message complete:', message);
        setIsStreaming(false);
        setStreamingContent('');
        onComplete(message);
        controllerRef.current = null;
        isCleanupSafeRef.current = false;
      },
      // onError
      (err) => {
        // Ignore AbortError from React Strict Mode cleanup
        if (err.name === 'AbortError' && !isCleanupSafeRef.current) {
          console.log('⚠️ Ignoring AbortError from React Strict Mode');
          initiatedRef.current = false; // Allow retry
          return;
        }
        console.error('❌ Welcome message error:', err);
        setIsStreaming(false);
        setStreamingContent('');
        setError(err.message || 'Failed to load welcome message');
        controllerRef.current = null;
        isCleanupSafeRef.current = false;
      }
    );
  }

  function initiate() {
    console.log('🎯 useWelcomeMessage.initiate called:', {
      sessionId,
      hasMessages,
      alreadyInitiated: initiatedRef.current,
      context,
      useStreaming
    });

    if (initiatedRef.current || hasMessages) {
      console.log('⏭️ Skipping initiate:', { alreadyInitiated: initiatedRef.current, hasMessages });
      return;
    }

    if (useStreaming) {
      initiateStreaming();
    } else {
      initiateNonStreaming();
    }
  }

  function retry() {
    initiatedRef.current = false;
    initiate();
  }

  // Reset initiated flag when sessionId changes
  useEffect(() => {
    initiatedRef.current = false;
    isCleanupSafeRef.current = false;
  }, [sessionId]);

  useEffect(() => {
    // Wait for session to load before checking if we need to initiate
    if (!sessionLoaded) {
      return;
    }

    // Don't initiate if already done or has messages
    if (initiatedRef.current || hasMessages) {
      return;
    }

    // Small delay to let React Strict Mode settle
    const timeoutId = setTimeout(() => {
      initiate();
    }, 10);

    // Cleanup: only abort if we're actually streaming
    return () => {
      clearTimeout(timeoutId);
      if (controllerRef.current && isCleanupSafeRef.current) {
        controllerRef.current.abort();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId, hasMessages, sessionLoaded]);

  return {
    isStreaming,
    streamingContent,
    error,
    retry,
  };
}
