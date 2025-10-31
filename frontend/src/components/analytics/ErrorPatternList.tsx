import { useState, useEffect } from 'react';
import { getErrorTrend } from '../../api/analytics';
import { ErrorTrend } from '../../types';
import type { ErrorPattern } from '../../types';

interface ErrorPatternListProps {
  patterns: ErrorPattern[];
  language: string;
}

export default function ErrorPatternList({ patterns, language }: ErrorPatternListProps) {
  const [trends, setTrends] = useState<Record<string, ErrorTrend>>({});
  const [loadingTrends, setLoadingTrends] = useState(true);

  useEffect(() => {
    async function loadTrends() {
      setLoadingTrends(true);
      const trendPromises = patterns.map(async (pattern) => {
        try {
          const trendData = await getErrorTrend(pattern.errorType, language);
          return { errorType: pattern.errorType, trend: trendData.trend };
        } catch (error) {
          console.error(`Failed to load trend for ${pattern.errorType}:`, error);
          return { errorType: pattern.errorType, trend: ErrorTrend.INSUFFICIENT_DATA };
        }
      });

      const trendResults = await Promise.all(trendPromises);
      const trendsMap = trendResults.reduce((acc, { errorType, trend }) => {
        acc[errorType] = trend;
        return acc;
      }, {} as Record<string, ErrorTrend>);

      setTrends(trendsMap);
      setLoadingTrends(false);
    }

    if (patterns.length > 0) {
      loadTrends();
    } else {
      setLoadingTrends(false);
    }
  }, [patterns, language]);

  const getTrendDisplay = (trend: ErrorTrend) => {
    switch (trend) {
      case ErrorTrend.IMPROVING:
        return {
          icon: '↓',
          text: 'Improving',
          color: 'text-green-600',
          bgColor: 'bg-green-50',
          borderColor: 'border-green-200',
        };
      case ErrorTrend.STABLE:
        return {
          icon: '−',
          text: 'Stable',
          color: 'text-yellow-600',
          bgColor: 'bg-yellow-50',
          borderColor: 'border-yellow-200',
        };
      case ErrorTrend.WORSENING:
        return {
          icon: '↑',
          text: 'Worsening',
          color: 'text-red-600',
          bgColor: 'bg-red-50',
          borderColor: 'border-red-200',
        };
      case ErrorTrend.INSUFFICIENT_DATA:
      default:
        return {
          icon: '?',
          text: 'Insufficient data',
          color: 'text-gray-500',
          bgColor: 'bg-gray-50',
          borderColor: 'border-gray-200',
        };
    }
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const formatErrorType = (errorType: string): string => {
    // Convert WORD_ORDER to "Word Order", VERB_FORM to "Verb Form", etc.
    return errorType
      .split('_')
      .map(word => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <div className="space-y-4">
      {patterns.map((pattern, index) => {
        const trend = trends[pattern.errorType] || ErrorTrend.INSUFFICIENT_DATA;
        const trendDisplay = getTrendDisplay(trend);

        return (
          <div
            key={pattern.errorType}
            className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 hover:shadow-md transition-shadow"
          >
            {/* Header with Rank and Error Type */}
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-start gap-4">
                {/* Rank Badge */}
                <div className="flex-shrink-0 w-10 h-10 rounded-full bg-brand-100 flex items-center justify-center">
                  <span className="text-lg font-bold text-brand-700">#{index + 1}</span>
                </div>

                {/* Error Type and Stats */}
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-slate-900 mb-2">
                    {formatErrorType(pattern.errorType)}
                  </h3>

                  {/* Severity Breakdown */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {pattern.criticalCount > 0 && (
                      <span className="px-2 py-1 text-xs font-medium text-white bg-red-600 rounded-full">
                        {pattern.criticalCount} Critical
                      </span>
                    )}
                    {pattern.highCount > 0 && (
                      <span className="px-2 py-1 text-xs font-medium text-white bg-orange-500 rounded-full">
                        {pattern.highCount} High
                      </span>
                    )}
                    {pattern.mediumCount > 0 && (
                      <span className="px-2 py-1 text-xs font-medium text-white bg-yellow-500 rounded-full">
                        {pattern.mediumCount} Medium
                      </span>
                    )}
                    {pattern.lowCount > 0 && (
                      <span className="px-2 py-1 text-xs font-medium text-white bg-blue-500 rounded-full">
                        {pattern.lowCount} Low
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Trend Indicator */}
              {!loadingTrends && (
                <div
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg border-2 ${trendDisplay.bgColor} ${trendDisplay.borderColor}`}
                >
                  <span className={`text-2xl font-bold ${trendDisplay.color}`}>
                    {trendDisplay.icon}
                  </span>
                  <span className={`text-sm font-medium ${trendDisplay.color}`}>
                    {trendDisplay.text}
                  </span>
                </div>
              )}
            </div>

            {/* Metrics Row */}
            <div className="flex items-center gap-6 text-sm text-slate-600 border-t border-slate-100 pt-4">
              <div>
                <span className="font-medium">Total Count:</span>{' '}
                <span className="font-bold text-slate-900">{pattern.totalCount}</span>
              </div>
              <div>
                <span className="font-medium">Importance:</span>{' '}
                <span className="font-bold text-brand-600">
                  {pattern.weightedScore.toFixed(1)}
                </span>
              </div>
              <div>
                <span className="font-medium">First seen:</span>{' '}
                <span>{formatDate(pattern.firstSeenAt)}</span>
              </div>
              <div>
                <span className="font-medium">Last seen:</span>{' '}
                <span>{formatDate(pattern.lastSeenAt)}</span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
