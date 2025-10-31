import { useState } from 'react';
import { Volume2, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { formatCompactLanguageDisplay } from '../../utils/languageDisplay';
import type { VocabularyItem, Language } from '../../types';
import { useTTS } from '../../contexts/TTSContext';

interface VocabularyTableProps {
  items: VocabularyItem[];
  onItemClick: (itemId: string) => void;
  languages: Language[];
}

type SortField = 'lemma' | 'exposures' | 'lastSeenAt' | 'reviewStage' | 'nextReviewAt';
type SortOrder = 'asc' | 'desc';

export default function VocabularyTable({
  items,
  onItemClick,
  languages,
}: VocabularyTableProps) {
  const [sortField, setSortField] = useState<SortField>('lastSeenAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [playingItemId, setPlayingItemId] = useState<string | null>(null);
  const { available: ttsAvailable, playText } = useTTS();

  const getLanguageDisplay = (code: string) => {
    const language = languages.find((lang) => lang.code === code);
    return language ? formatCompactLanguageDisplay(language) : code.toUpperCase();
  };

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const sortedItems = [...items].sort((a, b) => {
    let aValue: string | number;
    let bValue: string | number;

    if (sortField === 'lemma') {
      aValue = a.lemma.toLowerCase();
      bValue = b.lemma.toLowerCase();
    } else if (sortField === 'exposures') {
      aValue = a.exposures;
      bValue = b.exposures;
    } else if (sortField === 'reviewStage') {
      aValue = a.reviewStage;
      bValue = b.reviewStage;
    } else if (sortField === 'nextReviewAt') {
      // Sort by due date, with null values at the end
      aValue = a.nextReviewAt ? new Date(a.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER;
      bValue = b.nextReviewAt ? new Date(b.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER;
    } else {
      aValue = new Date(a.lastSeenAt).getTime();
      bValue = new Date(b.lastSeenAt).getTime();
    }

    if (aValue < bValue) return sortOrder === 'asc' ? -1 : 1;
    if (aValue > bValue) return sortOrder === 'asc' ? 1 : -1;
    return 0;
  });

  const getSortIcon = (field: SortField) => {
    if (sortField !== field) {
      return (
        <svg
          className="w-4 h-4 text-gray-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"
          />
        </svg>
      );
    }

    return sortOrder === 'asc' ? (
      <svg
        className="w-4 h-4 text-blue-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M5 15l7-7 7 7"
        />
      </svg>
    ) : (
      <svg
        className="w-4 h-4 text-blue-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M19 9l-7 7-7-7"
        />
      </svg>
    );
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatReviewDate = (dateString: string | null): string => {
    if (!dateString) return 'Not scheduled';

    const date = new Date(dateString);
    const now = new Date();
    const diffMs = date.getTime() - now.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return `${Math.abs(diffDays)} day${Math.abs(diffDays) > 1 ? 's' : ''} ago`;
    }
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Tomorrow';
    if (diffDays < 7) return `In ${diffDays} days`;
    return formatDate(dateString);
  };

  const handlePlayPronunciation = async (e: React.MouseEvent, item: VocabularyItem) => {
    e.stopPropagation(); // Prevent row click

    if (!ttsAvailable) {
      return;
    }

    setPlayingItemId(item.id);
    try {
      // Use slower speed for vocabulary learning
      await playText(item.lemma, 'Warm', 0.85);
    } catch (error) {
      console.error('Failed to play pronunciation:', error);
      toast.error('Could not play pronunciation');
    } finally {
      setPlayingItemId(null);
    }
  };

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <svg
          className="mx-auto h-12 w-12 text-gray-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
          />
        </svg>
        <h3 className="mt-2 text-sm font-medium text-gray-900">
          No vocabulary items
        </h3>
        <p className="mt-1 text-sm text-gray-500">
          Start a conversation to collect new vocabulary
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
              onClick={() => handleSort('lemma')}
            >
              <div className="flex items-center space-x-1">
                <span>Word/Phrase</span>
                {getSortIcon('lemma')}
              </div>
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              Language
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
              onClick={() => handleSort('exposures')}
            >
              <div className="flex items-center space-x-1">
                <span>Exposures</span>
                {getSortIcon('exposures')}
              </div>
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
              onClick={() => handleSort('reviewStage')}
            >
              <div className="flex items-center space-x-1">
                <span>Review Stage</span>
                {getSortIcon('reviewStage')}
              </div>
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
              onClick={() => handleSort('nextReviewAt')}
            >
              <div className="flex items-center space-x-1">
                <span>Next Review</span>
                {getSortIcon('nextReviewAt')}
              </div>
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
              onClick={() => handleSort('lastSeenAt')}
            >
              <div className="flex items-center space-x-1">
                <span>Last Seen</span>
                {getSortIcon('lastSeenAt')}
              </div>
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {sortedItems.map((item) => (
            <tr
              key={item.id}
              onClick={() => onItemClick(item.id)}
              className="hover:bg-gray-50 cursor-pointer transition-colors"
            >
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center gap-2">
                  <div className="text-sm font-medium text-gray-900">
                    {item.lemma}
                  </div>
                  {ttsAvailable && (
                    <button
                      onClick={(e) => handlePlayPronunciation(e, item)}
                      disabled={playingItemId === item.id}
                      className="p-1 text-slate-500 hover:text-slate-700 hover:bg-gray-100 rounded transition-colors disabled:opacity-50"
                      aria-label="Hear pronunciation"
                    >
                      {playingItemId === item.id ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Volume2 className="w-3.5 h-3.5" />
                      )}
                    </button>
                  )}
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                  {getLanguageDisplay(item.lang)}
                </span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-900">
                  {item.exposures}
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-indigo-100 text-indigo-800">
                  Stage {item.reviewStage}/5
                </span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-900">
                    {formatReviewDate(item.nextReviewAt)}
                  </span>
                  {item.isDue && (
                    <span className="px-2 py-0.5 text-xs font-bold text-white bg-red-600 rounded-full">
                      Due
                    </span>
                  )}
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {formatDate(item.lastSeenAt)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
