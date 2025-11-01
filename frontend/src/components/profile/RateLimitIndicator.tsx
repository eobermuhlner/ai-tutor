import { useEffect, useState } from 'react';
import { getRateLimitStatus } from '../../api/rateLimits';
import type { RateLimitStatus } from '../../api/rateLimits';

export default function RateLimitIndicator() {
  const [status, setStatus] = useState<RateLimitStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadRateLimitStatus();
  }, []);

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

  const percentageRemaining = 100 - status.percentageUsed;
  const getProgressColor = () => {
    if (percentageRemaining > 50) return 'bg-green-500';
    if (percentageRemaining > 20) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Rate Limits</h3>

      <div className="space-y-4">
        {/* Plan Badge */}
        <div>
          <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
            {status.planName}
          </span>
        </div>

        {/* Usage Progress Bar */}
        <div>
          <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-2">
            <span>Messages Used</span>
            <span>
              {status.dailyLimit - status.availableTokens} / {status.dailyLimit}
            </span>
          </div>
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2.5">
            <div
              className={`h-2.5 rounded-full transition-all duration-300 ${getProgressColor()}`}
              style={{ width: `${percentageRemaining}%` }}
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
        {percentageRemaining < 20 && (
          <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-3">
            <p className="text-sm text-yellow-800 dark:text-yellow-200">
              You're running low on messages. Consider upgrading your plan or waiting for the daily reset.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
