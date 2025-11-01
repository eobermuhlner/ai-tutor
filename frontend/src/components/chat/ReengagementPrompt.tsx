import { useState } from 'react';
import { initiateTutorMessage } from '../../api/chat';
import Button from '../ui/Button';
import type { Message } from '../../types';

interface ReengagementPromptProps {
  sessionId: string;
  lastMessageAt: string;
  onReengage: (message: Message) => void;
  onDismiss: () => void;
}

function calculateDaysSince(timestamp: string): number {
  const lastDate = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - lastDate.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

export default function ReengagementPrompt({
  sessionId,
  lastMessageAt,
  onReengage,
  onDismiss,
}: ReengagementPromptProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const daysSinceLastMessage = calculateDaysSince(lastMessageAt);

  async function handleReengage() {
    setIsLoading(true);
    setError(null);

    try {
      const message = await initiateTutorMessage(sessionId, 'reengage');
      onReengage(message);
    } catch (err: unknown) {
      console.error('Re-engagement failed:', err);
      setError((err as Error).message || 'Failed to restart conversation. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="mx-4 my-6 rounded-xl border-2 border-brand-200 bg-gradient-to-r from-brand-50 to-blue-50 p-6 shadow-lg">
      <div className="flex items-start gap-4">
        {/* Wave Icon */}
        <div className="flex-shrink-0">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-100 text-2xl">
            👋
          </div>
        </div>

        {/* Content */}
        <div className="flex-1">
          <h3 className="mb-2 text-xl font-semibold text-slate-900">
            Welcome back!
          </h3>
          <p className="mb-4 text-slate-700">
            It's been {daysSinceLastMessage} day{daysSinceLastMessage !== 1 ? 's' : ''} since your last message.
            Ready to continue your learning journey?
          </p>

          {error && (
            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="flex items-center gap-3">
            <Button
              onClick={handleReengage}
              isLoading={isLoading}
              disabled={isLoading}
              variant="primary"
              size="md"
            >
              {isLoading ? 'Restarting...' : 'Restart Conversation'}
            </Button>
            <button
              onClick={onDismiss}
              disabled={isLoading}
              className="text-sm text-slate-600 hover:text-slate-900 transition-colors disabled:opacity-50"
            >
              Dismiss
            </button>
          </div>
        </div>

        {/* Close button */}
        <button
          onClick={onDismiss}
          disabled={isLoading}
          className="flex-shrink-0 text-slate-400 hover:text-slate-600 transition-colors disabled:opacity-50"
          aria-label="Dismiss"
        >
          <svg
            className="h-5 w-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}
