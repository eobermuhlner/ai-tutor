import { useEffect, useState } from 'react';
import type {
  GlobalSummaryStats,
  SummaryDetail,
} from '../../types';
import {
  getGlobalStats,
  getSessionSummaryDetails,
  triggerSummarization,
} from '../../api/summaries';

export default function SummaryDashboard() {
  const [globalStats, setGlobalStats] = useState<GlobalSummaryStats | null>(
    null
  );
  const [selectedSessionId, setSelectedSessionId] = useState('');
  const [summaryDetails, setSummaryDetails] = useState<SummaryDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [triggerLoading, setTriggerLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    const fetchGlobalStats = async () => {
      try {
        setLoading(true);
        setError(null);
        const stats = await getGlobalStats();
        setGlobalStats(stats);
      } catch (err) {
        setError('Failed to load global stats. Admin access required.');
        console.error('Error fetching global stats:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchGlobalStats();
  }, []);

  const handleViewDetails = async () => {
    if (!selectedSessionId.trim()) {
      setError('Please enter a session ID');
      return;
    }

    try {
      setDetailsLoading(true);
      setError(null);
      setSummaryDetails([]);
      const details = await getSessionSummaryDetails(selectedSessionId);
      setSummaryDetails(details);
    } catch (err) {
      setError('Failed to load session details. Check the session ID and your admin permissions.');
      console.error('Error fetching session details:', err);
    } finally {
      setDetailsLoading(false);
    }
  };

  const handleTriggerSummarization = async () => {
    if (!selectedSessionId.trim()) {
      setError('Please enter a session ID');
      return;
    }

    try {
      setTriggerLoading(true);
      setError(null);
      setSuccessMessage(null);
      const result = await triggerSummarization(selectedSessionId);
      setSuccessMessage(result.message);
    } catch (err) {
      setError('Failed to trigger summarization. Check your admin permissions.');
      console.error('Error triggering summarization:', err);
    } finally {
      setTriggerLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-64 rounded bg-slate-200"></div>
          <div className="h-32 rounded-lg bg-slate-200"></div>
        </div>
      </div>
    );
  }

  if (error && !globalStats) {
    return (
      <div className="mx-auto max-w-6xl p-6">
        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">
          Summarization Dashboard
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Monitor and manage progressive summarization across all sessions
        </p>
      </div>

      {/* Global Stats */}
      {globalStats && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="text-sm font-medium text-slate-600">
              Total Sessions
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">
              {(globalStats.totalSessions ?? 0).toLocaleString()}
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="text-sm font-medium text-slate-600">
              Total Summaries
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">
              {(globalStats.totalSummaries ?? 0).toLocaleString()}
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="text-sm font-medium text-slate-600">
              Avg Compression
            </div>
            <div className="mt-2 text-2xl font-bold text-emerald-600">
              {globalStats.averageCompressionRatio
                ? Math.round((1 - 1 / globalStats.averageCompressionRatio) * 100)
                : 0}%
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="text-sm font-medium text-slate-600">
              Total Tokens Saved
            </div>
            <div className="mt-2 text-2xl font-bold text-blue-600">
              {(globalStats.totalTokensSaved ?? 0).toLocaleString()}
            </div>
          </div>
        </div>
      )}

      {/* Session Management */}
      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-xl font-semibold text-slate-900">
          Session Management
        </h2>
        <div className="space-y-4">
          <div>
            <label
              htmlFor="sessionId"
              className="block text-sm font-medium text-slate-700"
            >
              Session ID
            </label>
            <input
              type="text"
              id="sessionId"
              value={selectedSessionId}
              onChange={(e) => setSelectedSessionId(e.target.value)}
              placeholder="Enter session UUID"
              className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleViewDetails}
              disabled={detailsLoading}
              className="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {detailsLoading ? 'Loading...' : 'View Details'}
            </button>
            <button
              onClick={handleTriggerSummarization}
              disabled={triggerLoading}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {triggerLoading ? 'Triggering...' : 'Trigger Summarization'}
            </button>
          </div>
          {error && (
            <div className="rounded-md border border-red-200 bg-red-50 p-3">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}
          {successMessage && (
            <div className="rounded-md border border-green-200 bg-green-50 p-3">
              <p className="text-sm text-green-800">{successMessage}</p>
            </div>
          )}
        </div>
      </div>

      {/* Summary Details */}
      {summaryDetails.length > 0 && (
        <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-xl font-semibold text-slate-900">
            Summary Details ({summaryDetails.length})
          </h2>
          <div className="space-y-4">
            {summaryDetails.map((detail) => (
              <div
                key={detail.id}
                className="rounded-lg border border-slate-200 bg-slate-50 p-4"
              >
                <div className="mb-2 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-brand-100 px-2 py-1 text-xs font-medium text-brand-700">
                      Level {detail.summaryLevel}
                    </span>
                    <span className="text-xs text-slate-500">
                      Seq {detail.startSequence}–{detail.endSequence}
                    </span>
                    <span className="text-xs text-slate-500">
                      {detail.tokenCount} tokens
                    </span>
                    {!detail.isActive && (
                      <span className="rounded-full bg-slate-200 px-2 py-1 text-xs font-medium text-slate-600">
                        Superseded
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-slate-400">
                    {new Date(detail.createdAt).toLocaleString()}
                  </span>
                </div>
                <p className="text-sm text-slate-700 whitespace-pre-wrap">
                  {detail.summaryText}
                </p>
                <div className="mt-2 text-xs text-slate-500">
                  Source: {detail.sourceType} ({detail.sourceIds.length} items)
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
