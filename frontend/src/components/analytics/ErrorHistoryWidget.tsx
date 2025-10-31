import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { getRecentErrorSamples } from '../../api/analytics';
import Spinner from '../ui/Spinner';
import type { ErrorSample } from '../../types';

export default function ErrorHistoryWidget() {
  const [samples, setSamples] = useState<ErrorSample[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function loadSamples() {
      setIsLoading(true);
      try {
        const samplesData = await getRecentErrorSamples(10);
        setSamples(samplesData);
      } catch (error) {
        console.error('Failed to load error samples:', error);
        toast.error('Failed to load recent errors');
      } finally {
        setIsLoading(false);
      }
    }

    loadSamples();
  }, []);

  const getSeverityColor = (severity: string): string => {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
        return 'bg-red-600';
      case 'HIGH':
        return 'bg-orange-500';
      case 'MEDIUM':
        return 'bg-yellow-500';
      case 'LOW':
        return 'bg-blue-500';
      default:
        return 'bg-gray-400';
    }
  };

  const formatErrorType = (errorType: string): string => {
    return errorType
      .split('_')
      .map(word => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  };

  const formatTime = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Recent Errors</h3>
        <div className="flex items-center justify-center py-8">
          <Spinner size="md" />
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Recent Errors</h3>

      {samples.length === 0 ? (
        <div className="text-center py-8">
          <div className="mx-auto w-12 h-12 rounded-full bg-green-100 flex items-center justify-center mb-3">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p className="text-sm text-slate-600">No recent errors</p>
        </div>
      ) : (
        <div className="space-y-3">
          {samples.map((sample) => (
            <div
              key={sample.id}
              className="p-3 rounded-lg border border-slate-200 hover:border-brand-300 hover:shadow-sm transition-all cursor-pointer"
            >
              {/* Error Type and Severity */}
              <div className="flex items-center gap-2 mb-2">
                <span
                  className={`w-2 h-2 rounded-full ${getSeverityColor(sample.severity)}`}
                  title={sample.severity}
                />
                <span className="text-sm font-medium text-slate-900">
                  {formatErrorType(sample.errorType)}
                </span>
                <span className="text-xs text-slate-500 ml-auto">
                  {formatTime(sample.occurredAt)}
                </span>
              </div>

              {/* Error Span */}
              {sample.errorSpan && (
                <div className="text-sm text-slate-700 bg-slate-50 px-2 py-1 rounded">
                  "{sample.errorSpan}"
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
