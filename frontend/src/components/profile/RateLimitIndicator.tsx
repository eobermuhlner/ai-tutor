import { useEffect, useState } from 'react';
import { getRateLimitStatus } from '../../api/rateLimits';
import type { RateLimitStatus } from '../../api/rateLimits';

interface RateLimitIndicatorProps {
  onRefresh?: () => void;
  forceRefresh?: number; // Trigger refresh when this value changes
}

export default function RateLimitIndicator({ onRefresh, forceRefresh }: RateLimitIndicatorProps) {
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
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Rate Limits</h3>
        <p className="text-gray-500">Loading...</p>
      </div>
    );
  }

  if (error || !status) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Rate Limits</h3>
        <p className="text-red-500">{error || 'No data available'}</p>
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
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Rate Limits</h3>

      <div className="space-y-6">
        {/* Plan Badge */}
        <div>
          <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
            {status.planName}
          </span>
        </div>

        {/* Hourly Rate Limit */}
        <div>
          <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-2">
            <span>Messages Used (Hourly)</span>
            <span>
              {hourlyUsed} / {status.hourlyLimit} → {status.hourlyRemaining} remaining
            </span>
          </div>
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
            <div
              className={`h-3 rounded-full transition-all duration-300 ${getProgressColor(hourlyUsed, status.hourlyLimit)}`}
              style={{ width: `${status.hourlyLimit > 0 ? (hourlyUsed / status.hourlyLimit) * 100 : 0}%` }}
            ></div>
          </div>
        </div>

        {/* Daily Rate Limit */}
        <div>
          <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-2">
            <span>Messages Used (Daily)</span>
            <span>
              {dailyUsed} / {status.dailyLimit} → {status.dailyRemaining} remaining
            </span>
          </div>
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
            <div
              className={`h-3 rounded-full transition-all duration-300 ${getProgressColor(dailyUsed, status.dailyLimit)}`}
              style={{ width: `${status.dailyLimit > 0 ? (dailyUsed / status.dailyLimit) * 100 : 0}%` }}
            ></div>
          </div>
        </div>

        {/* Limits Info */}
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-gray-500 dark:text-gray-400">Hourly Limit</p>
            <p className="font-semibold">{status.hourlyLimit} messages</p>
          </div>
          <div>
            <p className="text-gray-500 dark:text-gray-400">Daily Limit</p>
            <p className="font-semibold">{status.dailyLimit} messages</p>
          </div>
        </div>

        {/* Available Messages */}
        <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Available Messages
          </p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white">
            {status.availableTokens}
          </p>
        </div>

        {/* Warning if low */}
        {(status.dailyRemaining < 5 || status.hourlyRemaining < 2) && (
          <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-3">
            <p className="text-sm text-yellow-800 dark:text-yellow-200">
              You're running low on messages. Consider waiting for the reset or upgrading your plan.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
