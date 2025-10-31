import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { getDueVocabulary, recordReview } from '../../api/vocabulary';
import Spinner from '../ui/Spinner';
import type { VocabularyItem } from '../../types';

interface VocabularyReviewPanelProps {
  targetLanguageCode: string;
  isVisible: boolean;
  onReviewComplete?: () => void;
}

export default function VocabularyReviewPanel({
  targetLanguageCode,
  isVisible,
  onReviewComplete,
}: VocabularyReviewPanelProps) {
  const [dueItems, setDueItems] = useState<VocabularyItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [reviewingItemId, setReviewingItemId] = useState<string | null>(null);

  useEffect(() => {
    if (!isVisible || !targetLanguageCode) return;

    const loadDueItems = async () => {
      setIsLoading(true);
      try {
        const items = await getDueVocabulary(targetLanguageCode, 20);
        setDueItems(items);
      } catch (error) {
        console.error('Failed to load due vocabulary:', error);
        toast.error('Failed to load vocabulary for review');
      } finally {
        setIsLoading(false);
      }
    };

    loadDueItems();
  }, [targetLanguageCode, isVisible]);

  const handleReview = async (itemId: string, success: boolean) => {
    setReviewingItemId(itemId);
    try {
      await recordReview(itemId, success);

      // Remove the reviewed item from the list
      setDueItems((prev) => prev.filter((item) => item.id !== itemId));

      // Show success message
      toast.success(success ? 'Marked as learned' : 'Will review again soon');

      // Callback for parent component
      if (onReviewComplete) {
        onReviewComplete();
      }
    } catch (error) {
      console.error('Failed to record review:', error);
      toast.error('Failed to record review');
    } finally {
      setReviewingItemId(null);
    }
  };

  const getUrgencyColor = (item: VocabularyItem): string => {
    if (!item.nextReviewAt) return 'bg-green-50 border-green-200';

    const now = new Date();
    const reviewDate = new Date(item.nextReviewAt);
    const hoursDiff = (reviewDate.getTime() - now.getTime()) / (1000 * 60 * 60);

    if (hoursDiff < 0) return 'bg-red-50 border-red-200'; // Overdue
    if (hoursDiff < 24) return 'bg-yellow-50 border-yellow-200'; // Due today
    return 'bg-green-50 border-green-200'; // Upcoming
  };

  const getUrgencyBadge = (item: VocabularyItem): React.ReactElement | null => {
    if (!item.nextReviewAt) return null;

    const now = new Date();
    const reviewDate = new Date(item.nextReviewAt);
    const hoursDiff = (reviewDate.getTime() - now.getTime()) / (1000 * 60 * 60);

    if (hoursDiff < 0) {
      return <span className="px-2 py-0.5 text-xs font-medium text-red-700 bg-red-100 rounded-full">Overdue</span>;
    }
    if (hoursDiff < 24) {
      return <span className="px-2 py-0.5 text-xs font-medium text-yellow-700 bg-yellow-100 rounded-full">Due today</span>;
    }
    return null;
  };

  const formatReviewDate = (dateString: string | null): string => {
    if (!dateString) return 'Not scheduled';

    const date = new Date(dateString);
    const now = new Date();
    const diffMs = date.getTime() - now.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);

    if (diffHours < 0) {
      const absDays = Math.abs(diffDays);
      const absHours = Math.abs(diffHours);
      if (absDays > 0) return `${absDays} day${absDays > 1 ? 's' : ''} ago`;
      return `${absHours} hour${absHours > 1 ? 's' : ''} ago`;
    }

    if (diffDays === 0) {
      if (diffHours === 0) return 'Due now';
      return `In ${diffHours} hour${diffHours > 1 ? 's' : ''}`;
    }
    if (diffDays === 1) return 'Tomorrow';
    if (diffDays < 7) return `In ${diffDays} days`;
    return date.toLocaleDateString();
  };

  if (!isVisible) return null;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner size="md" />
      </div>
    );
  }

  if (dueItems.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="text-center">
          <div className="mx-auto w-12 h-12 rounded-full bg-green-100 flex items-center justify-center mb-3">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-slate-900 mb-1">All caught up!</h3>
          <p className="text-sm text-slate-600">No vocabulary items due for review right now.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-900">Due for Review</h3>
        <span className="text-sm text-slate-600">{dueItems.length} items</span>
      </div>

      {dueItems.map((item) => (
        <div
          key={item.id}
          className={`rounded-xl border-2 p-4 transition-all ${getUrgencyColor(item)}`}
        >
          <div className="flex items-start justify-between gap-3 mb-3">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h4 className="text-lg font-bold text-slate-900">{item.lemma}</h4>
                {getUrgencyBadge(item)}
              </div>
              <div className="flex items-center gap-3 text-xs text-slate-600">
                <span>Stage {item.reviewStage}/5</span>
                <span>•</span>
                <span>{formatReviewDate(item.nextReviewAt)}</span>
                <span>•</span>
                <span>{item.exposures} exposure{item.exposures !== 1 ? 's' : ''}</span>
              </div>
            </div>
            {item.imageUrl && (
              <img
                src={item.imageUrl}
                alt={item.lemma}
                className="w-12 h-12 rounded-lg object-cover"
              />
            )}
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => handleReview(item.id, false)}
              disabled={reviewingItemId === item.id}
              className="flex-1 px-4 py-2 text-sm font-medium text-red-700 bg-white border-2 border-red-200 rounded-lg hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {reviewingItemId === item.id ? <Spinner size="sm" /> : '✗ Need practice'}
            </button>
            <button
              onClick={() => handleReview(item.id, true)}
              disabled={reviewingItemId === item.id}
              className="flex-1 px-4 py-2 text-sm font-medium text-green-700 bg-white border-2 border-green-200 rounded-lg hover:bg-green-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {reviewingItemId === item.id ? <Spinner size="sm" /> : '✓ Know it'}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
