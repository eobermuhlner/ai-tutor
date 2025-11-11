import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import type { User } from '../types';
import {
  getUser,
  updateUser,
  updateUserSubscriptionPlan,
  forceLogoutUser,
  resetUserPassword,
  unlockUser,
} from '../api/admin';
import {
  User as UserIcon,
  Mail,
  Calendar,
  Shield,
  ArrowLeft,
  Save,
  Lock,
  Key,
  LogOut,
  Crown,
  AlertTriangle,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { formatDistanceToNow } from 'date-fns';

export default function AdminUserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();

  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Edit state
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [locked, setLocked] = useState(false);
  const [roles, setRoles] = useState<string[]>([]);
  const [subscriptionPlan, setSubscriptionPlan] = useState<'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10'>('FREE');

  // Modal state
  const [showForceLogoutModal, setShowForceLogoutModal] = useState(false);
  const [showResetPasswordModal, setShowResetPasswordModal] = useState(false);
  const [showUnlockModal, setShowUnlockModal] = useState(false);

  useEffect(() => {
    if (!userId) {
      toast.error('User ID is required');
      navigate('/admin/users');
      return;
    }

    const fetchUser = async () => {
      try {
        setLoading(true);
        const userData = await getUser(userId);
        setUser(userData);
        setEmail(userData.email);
        setFirstName(userData.firstName || '');
        setLastName(userData.lastName || '');
        setEnabled(userData.enabled);
        setLocked(userData.locked);
        setRoles(userData.roles);
        setSubscriptionPlan(userData.subscriptionPlan);
      } catch (error) {
        console.error('Error fetching user:', error);
        toast.error('Failed to load user details');
        navigate('/admin/users');
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [userId, navigate]);

  const handleSaveProfile = async () => {
    if (!userId) return;

    try {
      setSaving(true);
      const updatedUser = await updateUser(userId, {
        email,
        firstName: firstName || undefined,
        lastName: lastName || undefined,
      });
      setUser(updatedUser);
      toast.success('Profile updated successfully');
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleEnabled = async () => {
    if (!userId) return;

    try {
      const updatedUser = await updateUser(userId, { enabled: !enabled });
      setUser(updatedUser);
      setEnabled(updatedUser.enabled);
      toast.success(`User ${updatedUser.enabled ? 'enabled' : 'disabled'} successfully`);
    } catch (error) {
      console.error('Error updating enabled status:', error);
      toast.error('Failed to update enabled status');
    }
  };

  const handleToggleLocked = async () => {
    if (!userId) return;

    try {
      const updatedUser = await updateUser(userId, { locked: !locked });
      setUser(updatedUser);
      setLocked(updatedUser.locked);
      toast.success(`User ${updatedUser.locked ? 'locked' : 'unlocked'} successfully`);
    } catch (error) {
      console.error('Error updating locked status:', error);
      toast.error('Failed to update locked status');
    }
  };

  const handleToggleRole = async (role: 'USER' | 'ADMIN') => {
    if (!userId) return;

    const newRoles = roles.includes(role)
      ? roles.filter((r) => r !== role)
      : [...roles, role];

    // Ensure at least USER role exists
    if (newRoles.length === 0) {
      toast.error('User must have at least one role');
      return;
    }

    try {
      const updatedUser = await updateUser(userId, { roles: newRoles });
      setUser(updatedUser);
      setRoles(updatedUser.roles);
      toast.success('Roles updated successfully');
    } catch (error) {
      console.error('Error updating roles:', error);
      toast.error('Failed to update roles');
    }
  };

  const handleUpdateSubscription = async () => {
    if (!userId) return;

    try {
      const updatedUser = await updateUserSubscriptionPlan(userId, subscriptionPlan);
      setUser(updatedUser);
      toast.success('Subscription plan updated successfully');
    } catch (error) {
      console.error('Error updating subscription:', error);
      toast.error('Failed to update subscription plan');
    }
  };

  const handleForceLogout = async () => {
    if (!userId) return;

    try {
      await forceLogoutUser(userId);
      toast.success('User logged out successfully');
      setShowForceLogoutModal(false);
    } catch (error) {
      console.error('Error forcing logout:', error);
      toast.error('Failed to force logout');
    }
  };

  const handleResetPassword = async () => {
    if (!userId) return;

    try {
      const result = await resetUserPassword(userId);
      toast.success(result.message);
      setShowResetPasswordModal(false);
    } catch (error) {
      console.error('Error resetting password:', error);
      toast.error('Failed to reset password');
    }
  };

  const handleUnlock = async () => {
    if (!userId) return;

    try {
      const result = await unlockUser(userId);
      toast.success(result.message);
      setShowUnlockModal(false);
      // Refresh user data to show updated lock status
      const updatedUser = await getUser(userId);
      setUser(updatedUser);
    } catch (error) {
      console.error('Error unlocking user:', error);
      toast.error('Failed to unlock user');
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="flex justify-center items-center h-64">
          <Spinner />
        </div>
      </Layout>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <Layout>
      <div className="max-w-5xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-6">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/admin/users')}
            className="mb-4"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Users
          </Button>
          <div className="flex items-center gap-3">
            <div className="w-16 h-16 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center text-white font-bold text-xl">
              {user.username.substring(0, 2).toUpperCase()}
            </div>
            <div>
              <h1 className="text-2xl font-semibold text-slate-900">{user.username}</h1>
              <p className="text-sm text-slate-600">User ID: {user.id}</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Profile Information */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
                <UserIcon className="w-5 h-5" />
                Profile Information
              </h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Email
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                      First Name
                    </label>
                    <input
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                      Last Name
                    </label>
                    <input
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                    />
                  </div>
                </div>
                <Button onClick={handleSaveProfile} isLoading={saving}>
                  <Save className="w-4 h-4 mr-2" />
                  Save Profile
                </Button>
              </div>
            </section>

            {/* Account Status */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
                <Shield className="w-5 h-5" />
                Account Status
              </h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium text-slate-900">Account Enabled</p>
                    <p className="text-sm text-slate-600">
                      {enabled ? 'User can log in and use the platform' : 'User cannot log in'}
                    </p>
                  </div>
                  <button
                    onClick={handleToggleEnabled}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      enabled ? 'bg-green-600' : 'bg-slate-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        enabled ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium text-slate-900">Account Locked</p>
                    <p className="text-sm text-slate-600">
                      {locked
                        ? 'User is temporarily locked out'
                        : 'User account is not locked'}
                    </p>
                  </div>
                  <button
                    onClick={handleToggleLocked}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      locked ? 'bg-orange-600' : 'bg-slate-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        locked ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </section>

            {/* Roles Management */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
                <Crown className="w-5 h-5" />
                Roles
              </h2>
              <div className="space-y-3">
                <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={roles.includes('USER')}
                    onChange={() => handleToggleRole('USER')}
                    className="w-4 h-4 text-brand-600 rounded focus:ring-brand-500"
                  />
                  <div>
                    <p className="font-medium text-slate-900">USER</p>
                    <p className="text-sm text-slate-600">Standard user access</p>
                  </div>
                </label>
                <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={roles.includes('ADMIN')}
                    onChange={() => handleToggleRole('ADMIN')}
                    className="w-4 h-4 text-brand-600 rounded focus:ring-brand-500"
                  />
                  <div>
                    <p className="font-medium text-slate-900">ADMIN</p>
                    <p className="text-sm text-slate-600">
                      Full administrative access
                    </p>
                  </div>
                </label>
              </div>
            </section>

            {/* Subscription Plan */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4">
                Subscription Plan
              </h2>
              <div className="space-y-4">
                <select
                  value={subscriptionPlan}
                  onChange={(e) => setSubscriptionPlan(e.target.value as 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10')}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                >
                  <option value="FREE">Free (10 msg/hr, 50 msg/day)</option>
                  <option value="FREE_BYOK">Free + BYOK (60 msg/hr, 300 msg/day)</option>
                  <option value="SUBSCRIPTION_10">Premium (100 msg/hr, 500 msg/day)</option>
                </select>
                {subscriptionPlan !== user.subscriptionPlan && (
                  <Button onClick={handleUpdateSubscription}>
                    Update Subscription
                  </Button>
                )}
              </div>
            </section>

            {/* Security Actions */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
                <Lock className="w-5 h-5" />
                Security Actions
              </h2>
              <div className="space-y-3">
                <Button
                  variant="outline"
                  onClick={() => setShowForceLogoutModal(true)}
                  className="w-full"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  Force Logout (Revoke All Sessions)
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowUnlockModal(true)}
                  className="w-full"
                >
                  <Unlock className="w-4 h-4 mr-2" />
                  Unlock Account (Clear Failed Attempts)
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowResetPasswordModal(true)}
                  className="w-full"
                >
                  <Key className="w-4 h-4 mr-2" />
                  Send Password Reset Email
                </Button>
              </div>
            </section>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Account Info */}
            <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
              <h3 className="text-sm font-semibold text-slate-900 mb-4">Account Details</h3>
              <div className="space-y-3 text-sm">
                <div>
                  <p className="text-slate-600">Username</p>
                  <p className="font-medium text-slate-900">{user.username}</p>
                </div>
                <div>
                  <p className="text-slate-600 flex items-center gap-1">
                    <Mail className="w-3 h-3" />
                    Email Verified
                  </p>
                  <p className="font-medium text-slate-900">
                    {user.emailVerified ? 'Yes' : 'No'}
                  </p>
                </div>
                <div>
                  <p className="text-slate-600 flex items-center gap-1">
                    <Calendar className="w-3 h-3" />
                    Created
                  </p>
                  <p className="font-medium text-slate-900">
                    {user.createdAt
                      ? formatDistanceToNow(new Date(user.createdAt), { addSuffix: true })
                      : 'Unknown'}
                  </p>
                </div>
                <div>
                  <p className="text-slate-600">Last Login</p>
                  <p className="font-medium text-slate-900">
                    {user.lastLoginAt
                      ? formatDistanceToNow(new Date(user.lastLoginAt), { addSuffix: true })
                      : 'Never'}
                  </p>
                </div>
              </div>
            </section>
          </div>
        </div>

        {/* Force Logout Modal */}
        {showForceLogoutModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full mx-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-full bg-orange-100 flex items-center justify-center">
                  <AlertTriangle className="w-6 h-6 text-orange-600" />
                </div>
                <h3 className="text-lg font-semibold text-slate-900">Force Logout User?</h3>
              </div>
              <p className="text-sm text-slate-600 mb-6">
                This will revoke all active sessions for <strong>{user.username}</strong>. They will
                need to log in again.
              </p>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowForceLogoutModal(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  onClick={handleForceLogout}
                  className="flex-1"
                >
                  Force Logout
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Reset Password Modal */}
        {showResetPasswordModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full mx-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
                  <Key className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-lg font-semibold text-slate-900">Reset Password?</h3>
              </div>
              <p className="text-sm text-slate-600 mb-6">
                This will send a password reset email to <strong>{user.email}</strong>. (Note: This
                is a placeholder feature - actual email functionality not yet implemented.)
              </p>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowResetPasswordModal(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleResetPassword}
                  className="flex-1"
                >
                  Send Reset Email
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Unlock Account Modal */}
        {showUnlockModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full mx-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
                  <Unlock className="w-6 h-6 text-green-600" />
                </div>
                <h3 className="text-lg font-semibold text-slate-900">Unlock Account?</h3>
              </div>
              <p className="text-sm text-slate-600 mb-6">
                This will clear all failed login attempts and remove any temporary lock for{' '}
                <strong>{user.username}</strong>. The user will be able to log in immediately.
              </p>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowUnlockModal(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleUnlock}
                  className="flex-1"
                >
                  Unlock Account
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
