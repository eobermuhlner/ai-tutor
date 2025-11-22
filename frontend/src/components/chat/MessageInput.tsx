import { useState, useRef, useEffect } from 'react';
import type { KeyboardEvent } from 'react';
import Button from '../ui/Button';
import VirtualKeyboard from './VirtualKeyboard';
import { hasKeyboardLayout } from '../../utils/keyboardLayouts';
import { useMobileDetection } from '../../hooks/useMobileDetection';

interface MessageInputProps {
  onSend: (message: string) => void;
  disabled?: boolean;
  onCancel?: () => void;
  languageCode?: string;
}

export default function MessageInput({
  onSend,
  disabled,
  onCancel,
  languageCode,
}: MessageInputProps) {
  const [message, setMessage] = useState('');
  const [isKeyboardOpen, setIsKeyboardOpen] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const isMobile = useMobileDetection();

  useEffect(() => {
    if (!disabled && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [disabled]);

  const handleSend = () => {
    if (message.trim() && !disabled) {
      onSend(message.trim());
      setMessage('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter') {
      if (isMobile) {
        // Mobile: Enter always inserts newline (don't prevent default)
        // User must use Send button to send message
        return;
      } else {
        // Desktop: Enter sends message, Shift+Enter inserts newline
        if (!e.shiftKey) {
          e.preventDefault();
          handleSend();
        }
      }
    }
  };

  const insertCharacterAtCursor = (char: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const newValue = message.slice(0, start) + char + message.slice(end);

    setMessage(newValue);

    // Restore cursor position after character
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + char.length, start + char.length);
    }, 0);
  };

  const showKeyboard = languageCode && hasKeyboardLayout(languageCode);

  return (
    <div className="border-t border-slate-200 bg-white/90 backdrop-blur-sm p-4 sm:p-6 relative">
      {showKeyboard && languageCode && (
        <VirtualKeyboard
          languageCode={languageCode}
          textareaRef={textareaRef}
          onCharacterInsert={insertCharacterAtCursor}
          isOpen={isKeyboardOpen}
          onClose={() => setIsKeyboardOpen(false)}
        />
      )}
      <div className="max-w-4xl mx-auto flex items-end gap-3">
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={
              isMobile
                ? 'Type your message...'
                : 'Type your message... (Shift+Enter for newline)'
            }
            disabled={disabled}
            rows={3}
            className="w-full resize-none rounded-xl border-2 border-slate-200 px-4 py-3 pr-12 text-slate-900 placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 disabled:bg-slate-50 disabled:text-slate-400 disabled:cursor-not-allowed transition-all shadow-sm hover:shadow-md focus:shadow-md"
          />
          {showKeyboard && (
            <button
              onClick={() => setIsKeyboardOpen(!isKeyboardOpen)}
              disabled={disabled}
              className="absolute right-3 top-3 p-2 text-slate-400 hover:text-brand-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              aria-label="Virtual Keyboard"
              title="Virtual Keyboard"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"
                />
              </svg>
            </button>
          )}
        </div>
        <div className="flex flex-col gap-2">
          {onCancel && disabled ? (
            <Button onClick={onCancel} variant="outline" size="md">
              Cancel
            </Button>
          ) : (
            <Button
              onClick={handleSend}
              disabled={disabled || !message.trim()}
              size="md"
              variant="primary"
            >
              Send
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
