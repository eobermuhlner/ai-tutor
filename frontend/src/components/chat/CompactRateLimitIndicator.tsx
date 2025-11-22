import { useEffect, useState } from 'react';
import { getRateLimitStatus } from '../../api/rateLimits';
import type { RateLimitStatus } from '../../api/rateLimits';

interface CompactRateLimitIndicatorProps {
  forceRefresh?: number;
}

export default function CompactRateLimitIndicator({ forceRefresh }: CompactRateLimitIndicatorProps) {
  const [status, setStatus] = useState<RateLimitStatus | null>(null);

  const loadRateLimitStatus = async () => {
    try {
      const data = await getRateLimitStatus();
      setStatus(data);
    } catch (err) {
      console.error('Failed to load rate limit status:', err);
    }
  };

  useEffect(() => {
    loadRateLimitStatus();
  }, []);

  useEffect(() => {
    if (forceRefresh !== undefined) {
      loadRateLimitStatus();
    }
  }, [forceRefresh]);

  if (!status) {
    return null;
  }

  const getColorClass = () => {
    const percentUsed = status.percentageUsed;
    if (percentUsed < 50) return 'text-green-600';
    if (percentUsed < 80) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getDotColor = () => {
    const percentUsed = status.percentageUsed;
    if (percentUsed < 50) return 'bg-green-500';
    if (percentUsed < 80) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  return (
    <div className="flex items-center gap-1.5 text-xs">
      <div className={`w-1.5 h-1.5 rounded-full ${getDotColor()}`}></div>
      <span className={`font-medium ${getColorClass()}`}>
        {status.availableTokens}
      </span>
      <span className="text-slate-400">left</span>
    </div>
  );
}
