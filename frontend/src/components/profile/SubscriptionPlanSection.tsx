import { useState, useEffect } from 'react';
import { DollarSign, Check, Loader2 } from 'lucide-react';
import Button from '../ui/Button';
import { getRateLimitStatus, updateUserSubscriptionPlan, type UpdateUserSubscriptionPlanRequest, type RateLimitStatus } from '../../api/rateLimits';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';

// Define subscription plan types
type SubscriptionPlan = 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10';

interface PlanOption {
  id: SubscriptionPlan;
  name: string;
  description: string;
  price: string;
  hourlyLimit: number;
  dailyLimit: number;
  features: string[];
  selectable: boolean; // Whether this plan can be manually selected by the user
}

const planOptions: PlanOption[] = [
  {
    id: 'FREE',
    name: 'Free',
    description: 'Basic access for trying out the platform',
    price: 'Free',
    hourlyLimit: 10,
    dailyLimit: 50,
    features: [
      '50 messages per day',
      '10 messages per hour',
      'Standard AI models',
      'Basic language courses'
    ],
    selectable: true
  },
  {
    id: 'FREE_BYOK',
    name: 'Free + BYOK',
    description: 'Higher limits when using your own API key',
    price: 'Free (your API costs)',
    hourlyLimit: 60,
    dailyLimit: 300,
    features: [
      '300 messages per day',
      '60 messages per hour',
      'Use your own API key',
      'Access to premium models',
      'All language courses'
    ],
    selectable: false // This plan is automatically applied when user has an API key
  },
  {
    id: 'SUBSCRIPTION_10',
    name: 'Premium',
    description: 'Unlimited access for regular learners',
    price: '$10/month',
    hourlyLimit: 100,
    dailyLimit: 500,
    features: [
      '500 messages per day',
      '100 messages per hour',
      'Priority access',
      'All premium features',
      'All language courses',
      'Custom tutors'
    ],
    selectable: true
  }
];

export default function SubscriptionPlanSection() {
  const user = useAuthStore((state) => state.user);
  const [status, setStatus] = useState<RateLimitStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingPlan, setUpdatingPlan] = useState<SubscriptionPlan | null>(null);

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

  // Refresh rate limit status when user data changes (e.g., after API key changes)
  useEffect(() => {
    if (user) {
      // Only reload if the subscription plan has changed
      loadRateLimitStatus();
    }
  }, [user?.subscriptionPlan]); // Only trigger when subscriptionPlan changes

  const handlePlanChange = async (planId: SubscriptionPlan) => {
    if (!user) return;

    // Don't allow selecting FREE_BYOK directly since it's automatically managed by API key status
    if (planId === 'FREE_BYOK') {
      toast.error('Free + BYOK plan is automatically activated when you configure an API key in the settings below.');
      return;
    }

    // Only allow upgrading to higher tier plans, not downgrading to FREE
    if (planId === 'FREE') {
      toast.error('Downgrading to Free plan is not allowed. Please select a higher tier plan.');
      return;
    }

    if (!confirm(`Are you sure you want to change your subscription plan to ${planOptions.find(p => p.id === planId)?.name}?`)) {
      return;
    }

    setUpdatingPlan(planId);
    try {
      const result = await updateUserSubscriptionPlan(planId as 'SUBSCRIPTION_10'); // Only allow premium plans
      
      // Update user in store
      useAuthStore.getState().updateUser({ ...user, subscriptionPlan: planId });
      
      // Update status with the new data
      setStatus(result);
      
      toast.success(`Subscription plan updated to ${planOptions.find(p => p.id === planId)?.name}`);
    } catch (err: any) {
      console.error('Failed to update subscription plan:', err);
      const errorMessage = err.response?.data?.message || 'Failed to update subscription plan. Please try again.';
      toast.error(errorMessage);
    } finally {
      setUpdatingPlan(null);
    }
  };

  if (loading) {
    return (
      <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
            <DollarSign className="w-6 h-6 text-white" />
          </div>
          <h2 className="text-xl font-semibold text-slate-900">Subscription Plan</h2>
        </div>
        
        <div className="flex items-center justify-center py-8">
          <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
        </div>
      </section>
    );
  }

  if (error || !status) {
    return (
      <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
            <DollarSign className="w-6 h-6 text-white" />
          </div>
          <h2 className="text-xl font-semibold text-slate-900">Subscription Plan</h2>
        </div>
        
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 text-sm">{error || 'No data available'}</p>
        </div>
      </section>
    );
  }

  const currentPlan = planOptions.find(p => p.id === (status.subscriptionPlan as SubscriptionPlan)) || planOptions[0];
  const hourlyUsed = status.hourlyLimit - status.hourlyRemaining;
  const dailyUsed = status.dailyLimit - status.dailyRemaining;

  const getProgressColor = (used: number, total: number) => {
    const percentageUsed = total > 0 ? (used / total) * 100 : 0;
    if (percentageUsed < 50) return 'bg-green-500';
    if (percentageUsed < 80) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  return (
    <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
          <DollarSign className="w-6 h-6 text-white" />
        </div>
        <h2 className="text-xl font-semibold text-slate-900">Subscription Plan</h2>
      </div>

      <div className="space-y-6">
        {/* Current Plan Status */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex justify-between items-start mb-3">
            <div>
              <h3 className="font-semibold text-blue-900 flex items-center gap-2">
                Current Plan: {currentPlan.name}
                <span className="text-sm font-normal bg-blue-100 text-blue-800 px-2 py-0.5 rounded-full">
                  {status.planName}
                </span>
              </h3>
              <p className="text-sm text-blue-700 mt-1">{currentPlan.description}</p>
            </div>
          </div>

          {/* BYOK Status Indicator for FREE plan */}
          {status.subscriptionPlan === 'FREE' && (
            <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-md">
              <div className="flex items-start">
                <div className="flex-shrink-0 mt-0.5">
                  <span className="text-yellow-600">💡</span>
                </div>
                <div className="ml-2">
                  <p className="text-sm text-yellow-800">
                    <span className="font-medium">Increase your limits!</span> Configure your own API key to get the <strong>Free + BYOK</strong> plan with higher limits (300/day instead of 50/day).
                  </p>
                  <Button 
                    variant="link" 
                    className="mt-1 px-0 text-yellow-700 hover:text-yellow-900 text-sm font-medium"
                    onClick={() => document.getElementById('api-key-settings-section')?.scrollIntoView({ behavior: 'smooth' })}
                  >
                    Set up API key →
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* BYOK Active Indicator for FREE_BYOK plan */}
          {status.subscriptionPlan === 'FREE_BYOK' && (
            <div className="mt-3 p-3 bg-green-50 border border-green-200 rounded-md">
              <div className="flex items-start">
                <div className="flex-shrink-0 mt-0.5">
                  <Check className="w-4 h-4 text-green-600" />
                </div>
                <div className="ml-2">
                  <p className="text-sm text-green-800">
                    <span className="font-medium">BYOK Active!</span> You're using the Free + BYOK plan with higher limits (300/day) because you have an API key configured.
                  </p>
                  <Button 
                    variant="link" 
                    className="mt-1 px-0 text-green-700 hover:text-green-900 text-sm font-medium"
                    onClick={() => document.getElementById('api-key-settings-section')?.scrollIntoView({ behavior: 'smooth' })}
                  >
                    Manage API key →
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Rate Limits Display */}
          <div className="space-y-3 mt-4 border-t border-blue-100 pt-4">
            {/* Hourly Rate Limit */}
            <div>
              <div className="flex justify-between text-xs text-blue-600 mb-1">
                <span>Hourly Usage: {hourlyUsed}/{status.hourlyLimit}</span>
                <span>{Math.round((hourlyUsed / status.hourlyLimit) * 100)}%</span>
              </div>
              <div className="w-full bg-blue-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${getProgressColor(hourlyUsed, status.hourlyLimit)}`}
                  style={{ width: `${status.hourlyLimit > 0 ? (hourlyUsed / status.hourlyLimit) * 100 : 0}%` }}
                ></div>
              </div>
            </div>

            {/* Daily Rate Limit */}
            <div>
              <div className="flex justify-between text-xs text-blue-600 mb-1">
                <span>Daily Usage: {dailyUsed}/{status.dailyLimit}</span>
                <span>{Math.round((dailyUsed / status.dailyLimit) * 100)}%</span>
              </div>
              <div className="w-full bg-blue-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${getProgressColor(dailyUsed, status.dailyLimit)}`}
                  style={{ width: `${status.dailyLimit > 0 ? (dailyUsed / status.dailyLimit) * 100 : 0}%` }}
                ></div>
              </div>
            </div>

            {/* Available Messages */}
            <div className="pt-2">
              <p className="text-xs text-blue-600 mb-1">Available Tokens</p>
              <p className="text-lg font-bold text-blue-900">{status.availableTokens}</p>
            </div>
          </div>

          {/* Warning if low */}
          {(status.dailyRemaining < 5 || status.hourlyRemaining < 2) && (
            <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-md p-2.5 mt-3">
              <p className="text-xs text-yellow-800 dark:text-yellow-200">
                Low on messages. Consider upgrading your plan.
              </p>
            </div>
          )}
        </div>

        {/* Plan Options */}
        <div>
          <h3 className="font-semibold text-slate-900 mb-4">Choose a Plan</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {planOptions
              .filter(plan => plan.selectable) // Only show selectable plans
              .map((plan) => {
                const isCurrent = plan.id === (status.subscriptionPlan as SubscriptionPlan);
                const isUpdating = updatingPlan === plan.id;
                
                // Special handling for the Free plan to show BYOK status
                const isFreePlan = plan.id === 'FREE';
                const showBYOKInfo = isFreePlan && status.subscriptionPlan === 'FREE_BYOK';

                return (
                  <div
                    key={plan.id}
                    className={`border rounded-lg p-4 ${
                      isCurrent 
                        ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200' 
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="font-semibold text-slate-900">
                        {plan.name}
                        {showBYOKInfo && (
                          <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            Active (BYOK)
                          </span>
                        )}
                      </h4>
                      {isCurrent && !showBYOKInfo && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          Current
                        </span>
                      )}
                    </div>
                    
                    <p className="text-sm text-slate-600 mb-2">{plan.description}</p>
                    
                    <div className="mb-4">
                      <span className="text-xl font-bold text-slate-900">{plan.price}</span>
                      
                      {/* Show actual limits for Free plan when BYOK is active */}
                      {isFreePlan && showBYOKInfo && (
                        <div className="mt-2 text-sm">
                          <p className="text-green-700 font-medium">Using BYOK Enhanced Limits:</p>
                          <p className="text-green-600">300 messages per day, 60 per hour</p>
                        </div>
                      )}
                    </div>
                    
                    <ul className="space-y-1 mb-4 text-sm text-slate-700">
                      <li className="flex items-center">
                        <Check className="w-4 h-4 text-green-500 mr-2" />
                        <span>
                          {isFreePlan && showBYOKInfo 
                            ? '300 messages per day (with BYOK)' 
                            : `${plan.dailyLimit} messages per day`}
                        </span>
                      </li>
                      <li className="flex items-center">
                        <Check className="w-4 h-4 text-green-500 mr-2" />
                        <span>
                          {isFreePlan && showBYOKInfo 
                            ? '60 messages per hour (with BYOK)' 
                            : `${plan.hourlyLimit} messages per hour`}
                        </span>
                      </li>
                    </ul>
                    
                    {isFreePlan && (
                      <div className="mb-3 p-2 bg-blue-50 border border-blue-200 rounded-md">
                        <p className="text-xs text-blue-700">
                          {showBYOKInfo 
                            ? 'API key is configured. Higher limits active.' 
                            : 'Configure your own API key to increase limits to 300/day.'}
                        </p>
                        <Button 
                          variant="link" 
                          className="px-0 text-blue-700 hover:text-blue-900 text-xs font-medium mt-1"
                          onClick={() => document.getElementById('api-key-settings-section')?.scrollIntoView({ behavior: 'smooth' })}
                        >
                          {showBYOKInfo ? 'Manage API Key' : 'Set up API Key →'}
                        </Button>
                      </div>
                    )}
                    
                    <Button
                      variant={isCurrent ? "secondary" : "primary"}
                      className="w-full"
                      onClick={() => handlePlanChange(plan.id)}
                      disabled={isCurrent || isUpdating}
                    >
                      {isUpdating ? (
                        <>
                          <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          Processing...
                        </>
                      ) : isCurrent ? (
                        "Current Plan"
                      ) : (
                        "Select Plan"
                      )}
                    </Button>
                  </div>
                );
              })}
          </div>
        </div>
      </div>
    </section>
  );
}