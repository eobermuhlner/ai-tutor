import Layout from '../components/layout/Layout';
import SubscriptionPlanSection from '../components/profile/SubscriptionPlanSection';
import ApiKeySettingsSection from '../components/profile/ApiKeySettingsSection';

export default function SubscriptionPage() {
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