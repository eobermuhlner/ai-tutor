import { useEffect, useState } from 'react';
import { getRateLimitStatus } from '../../api/rateLimits';
import type { RateLimitStatus } from '../../api/rateLimits';

interface RateLimitIndicatorProps {
  forceRefresh?: number; // Trigger refresh when this value changes
}

export default function RateLimitIndicator({ forceRefresh }: RateLimitIndicatorProps) {
  const [status, setStatus] = useState<RateLimitStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRateLimitStatus = async () => {
    try {
      setLoading(true);
      const data = await getRateLimitStatus();
      setStatus(data);
      setError(null);
    } catch (err) {
      console.error('Failed to load rate limit status:', err);
      setError('Failed to load rate limit information');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRateLimitStatus();
  }, []);

  // Refresh when forceRefresh changes
  useEffect(() => {
    if (forceRefresh !== undefined) {
      loadRateLimitStatus();
    }
  }, [forceRefresh]);

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-4">
        <h3 className="text-sm font-semibold mb-3 text-slate-900">Rate Limits</h3>
        <p className="text-slate-500 text-sm">Loading...</p>
      </div>
    );
  }

  if (error || !status) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-4">
        <h3 className="text-sm font-semibold mb-3 text-slate-900">Rate Limits</h3>
        <p className="text-red-600 text-sm">{error || 'No data available'}</p>
      </div>
    );
  }

  const getProgressColor = (used: number, total: number) => {
    const percentageUsed = total > 0 ? (used / total) * 100 : 0;
    if (percentageUsed < 50) return 'bg-green-500';
    if (percentageUsed < 80) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  const hourlyUsed = status.hourlyLimit - status.hourlyRemaining;
  const dailyUsed = status.dailyLimit - status.dailyRemaining;

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h3 className="text-sm font-semibold mb-3 text-slate-900">Rate Limits</h3>

      <div className="space-y-4">
        {/* Plan Badge */}
        <div>
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
            {status.planName}
          </span>
        </div>

        {/* Hourly Rate Limit */}
        <div>
          <div className="flex justify-between text-xs text-slate-600 mb-1.5">
            <span>Hourly</span>
            <span>{hourlyUsed}/{status.hourlyLimit}</span>
          </div>
          <div className="w-full bg-slate-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full transition-all duration-300 ${getProgressColor(hourlyUsed, status.hourlyLimit)}`}
              style={{ width: `${status.hourlyLimit > 0 ? (hourlyUsed / status.hourlyLimit) * 100 : 0}%` }}
            ></div>
          </div>
        </div>

        {/* Daily Rate Limit */}
        <div>
          <div className="flex justify-between text-xs text-slate-600 mb-1.5">
            <span>Daily</span>
            <span>{dailyUsed}/{status.dailyLimit}</span>
          </div>
          <div className="w-full bg-slate-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full transition-all duration-300 ${getProgressColor(dailyUsed, status.dailyLimit)}`}
              style={{ width: `${status.dailyLimit > 0 ? (dailyUsed / status.dailyLimit) * 100 : 0}%` }}
            ></div>
          </div>
        </div>

        {/* Available Messages */}
        <div className="pt-2 border-t border-slate-200">
          <p className="text-xs text-slate-600 mb-1">
            Available
          </p>
          <p className="text-lg font-bold text-slate-900">
            {status.availableTokens}
          </p>
        </div>

        {/* Warning if low */}
        {(status.dailyRemaining < 5 || status.hourlyRemaining < 2) && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-md p-2.5 mt-2">
            <p className="text-xs text-yellow-800">
              Low on messages. Consider waiting for reset or upgrading.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
