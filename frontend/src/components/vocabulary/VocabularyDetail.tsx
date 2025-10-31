import type { VocabularyItem, VocabularyContext } from '../../types';

interface VocabularyDetailProps {
  item: VocabularyItem;
  contexts: VocabularyContext[];
  onClose: () => void;
}

export default function VocabularyDetail({
  item,
  contexts,
  onClose,
}: VocabularyDetailProps) {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-start">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">{item.lemma}</h2>
            <div className="flex items-center gap-4 mt-2">
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                {item.lang}
              </span>
              <span className="text-sm text-gray-500">
                Exposures: {item.exposures}
              </span>
              <span className="text-sm text-gray-500">
                Last seen: {formatDate(item.lastSeenAt)}
              </span>
              <span className="text-sm text-gray-500">
                Created: {formatDate(item.createdAt)}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg
              className="w-6 h-6"
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

        {/* Contexts */}
        <div className="px-6 py-4">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Contexts ({contexts.length})
          </h3>

          {contexts.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No contexts available for this word
            </div>
          ) : (
            <div className="space-y-4">
              {contexts.map((context, index) => (
                <div
                  key={index}
                  className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="space-y-2">
                    {/* Context */}
                    <div>
                      <p className="text-gray-900">{context.context}</p>
                    </div>

                    {/* Metadata */}
                    {context.turnId && (
                      <div className="flex items-center gap-4 text-xs text-gray-500 pt-2 border-t border-gray-100">
                        <span>Turn ID: {context.turnId.slice(0, 8)}...</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-gray-50 px-6 py-4 border-t border-gray-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
