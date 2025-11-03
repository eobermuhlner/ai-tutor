import { useEffect } from 'react';
import Layout from '../components/layout/Layout';
import SubscriptionPlanSection from '../components/profile/SubscriptionPlanSection';
import ApiKeySettingsSection from '../components/profile/ApiKeySettingsSection';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';

export default function SubscriptionPage() {
  // Handle payment result from Stripe
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentResult = params.get('payment');

    if (paymentResult === 'success') {
      toast.success('Subscription activated! Welcome to AI Tutor.');
      // Refresh user data to get updated subscription plan
      useAuthStore.getState().refreshUser();
      // Clean up URL
      window.history.replaceState({}, document.title, window.location.pathname);
    } else if (paymentResult === 'cancel') {
      toast('Checkout canceled. You can upgrade anytime.');
      // Clean up URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            Subscription Plan
          </h1>
          <p className="mt-2 text-slate-600">
            Manage your subscription and usage limits
          </p>
        </div>

        <SubscriptionPlanSection />
        
        <div className="mt-8">
          <ApiKeySettingsSection />
        </div>
      </div>
    </Layout>
  );
}