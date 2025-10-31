import { useEffect, useState } from 'react';
import type { SessionSummaryInfo, SummaryDetail } from '../../types';
import { getSessionSummaryInfo, getSessionSummaryDetails } from '../../api/summaries';

interface SessionSummaryPanelProps {
  sessionId: string;
  optimisticMessageCount?: number;
}

export default function SessionSummaryPanel({
  sessionId,
  optimisticMessageCount,
}: SessionSummaryPanelProps) {
  const [summaryInfo, setSummaryInfo] = useState<SessionSummaryInfo | null>(
    null
  );
  const [summaryDetails, setSummaryDetails] = useState<SummaryDetail[]>([]);
  const [showDetails, setShowDetails] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSummaryInfo = async () => {
      try {
        setLoading(true);
        setError(null);
        const info = await getSessionSummaryInfo(sessionId);
        setSummaryInfo(info);
      } catch (err) {
        setError('Failed to load summary info');
        console.error('Error fetching summary info:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchSummaryInfo();
  }, [sessionId]);

  const handleToggleDetails = async () => {
    if (!showDetails && summaryDetails.length === 0) {
      // Fetch details on first open
      setLoadingDetails(true);
      try {
        const details = await getSessionSummaryDetails(sessionId);
        setSummaryDetails(details);
      } catch (err) {
        console.error('Error fetching summary details:', err);
      } finally {
        setLoadingDetails(false);
      }
    }
    setShowDetails(!showDetails);
  };

  if (loading) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="animate-pulse space-y-2">
          <div className="h-4 w-32 rounded bg-slate-200"></div>
          <div className="h-3 w-48 rounded bg-slate-200"></div>
        </div>
      </div>
    );
  }

  if (error || !summaryInfo) {
    return null; // Hide on error
  }

  const compressionPercent = Math.round((1 - summaryInfo.compressionRatio) * 100);
  const hasSummaries = summaryInfo.summaryLevels.length > 0;
  const hasPositiveCompression = summaryInfo.estimatedTokenSavings > 0;

  // Use optimistic count if provided, otherwise use actual count
  const displayMessageCount = optimisticMessageCount ?? summaryInfo.totalMessages;

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-sm font-semibold text-slate-700">
        Session Summary Stats
      </h3>
      <div className="space-y-2 text-sm">
        <div className="flex items-center justify-between">
          <span className="text-slate-600">Total Messages:</span>
          <span className="font-medium text-slate-900">
            {displayMessageCount}
          </span>
        </div>
        {hasSummaries ? (
          <>
            {hasPositiveCompression ? (
              <>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Compression:</span>
                  <span className="font-medium text-emerald-600">
                    {compressionPercent}% reduction
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Compression Ratio:</span>
                  <span className="font-medium text-slate-700">
                    {summaryInfo.compressionRatio.toFixed(2)}x
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-600">Tokens Saved:</span>
                  <span className="font-medium text-blue-600">
                    {summaryInfo.estimatedTokenSavings.toLocaleString()}
                  </span>
                </div>
                {summaryInfo.lastSummarizedSequence !== null && (
                  <div className="flex items-center justify-between">
                    <span className="text-slate-600">Last Summarized:</span>
                    <span className="font-medium text-slate-700">
                      Seq {summaryInfo.lastSummarizedSequence}
                    </span>
                  </div>
                )}
              </>
            ) : (
              <div className="rounded-md bg-amber-50 p-2">
                <p className="text-xs text-amber-700">
                  Summaries are being built. Compression stats will show when conversation grows.
                </p>
              </div>
            )}
            <div className="mt-3 border-t border-slate-100 pt-2">
              <div className="mb-1 flex items-center justify-between">
                <div className="text-xs font-medium text-slate-600">
                  Summary Levels
                </div>
                <button
                  onClick={handleToggleDetails}
                  className="text-xs text-brand-600 hover:text-brand-700"
                >
                  {showDetails ? 'Hide' : 'View'}
                </button>
              </div>
              <div className="space-y-1">
                {summaryInfo.summaryLevels.map((level) => (
                  <div
                    key={level.level}
                    className="flex items-center justify-between text-xs"
                  >
                    <span className="text-slate-500">Level {level.level}:</span>
                    <span className="text-slate-700">
                      {level.count} summaries ({level.totalTokens} tokens)
                    </span>
                  </div>
                ))}
              </div>

              {showDetails && (
                <div className="mt-3 space-y-2">
                  {loadingDetails ? (
                    <div className="text-xs text-slate-500">Loading summaries...</div>
                  ) : summaryDetails.length > 0 ? (
                    summaryDetails
                      .filter(detail => detail.isActive)
                      .sort((a, b) => a.summaryLevel - b.summaryLevel || a.startSequence - b.startSequence)
                      .map((detail) => (
                        <div
                          key={detail.id}
                          className="rounded-md border border-slate-200 bg-slate-50 p-2"
                        >
                          <div className="mb-1 flex items-center gap-2">
                            <span className="rounded bg-brand-100 px-1.5 py-0.5 text-xs font-medium text-brand-700">
                              L{detail.summaryLevel}
                            </span>
                            <span className="text-xs text-slate-500">
                              Seq {detail.startSequence}–{detail.endSequence}
                            </span>
                            <span className="text-xs text-slate-400">
                              {detail.tokenCount}t
                            </span>
                          </div>
                          <p className="text-xs text-slate-700 whitespace-pre-wrap leading-relaxed">
                            {detail.summaryText}
                          </p>
                        </div>
                      ))
                  ) : (
                    <div className="text-xs text-slate-500">No summary details available</div>
                  )}
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="rounded-md bg-slate-50 p-2">
            <p className="text-xs text-slate-500">
              No summaries yet. Summaries are generated automatically as conversations grow.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
