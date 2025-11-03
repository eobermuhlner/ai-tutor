import { useState, useEffect, useCallback } from 'react';
import { Plus, Lock, User } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Spinner from '../components/ui/Spinner';
import LanguageProficiencyList from '../components/profile/LanguageProficiencyList';
import AddLanguageModal from '../components/profile/AddLanguageModal';
import ApiKeySettingsSection from '../components/profile/ApiKeySettingsSection';
import SubscriptionPlanSection from '../components/profile/SubscriptionPlanSection';
import { useAuthStore } from '../store/authStore';
import {
  getLanguageProficiencies,
  addLanguageProficiency,
  updateLanguageProficiency,
  setPrimaryLanguage,
  removeLanguageProficiency,
} from '../api/userLanguages';
import { changePassword, changeEmail } from '../api/auth';
import { CEFRLevel, LanguageProficiencyType } from '../types';
import type { LanguageProficiency } from '../types';
import toast from 'react-hot-toast';

export default function ProfilePage() {
  const user = useAuthStore((state) => state.user);
  const [proficiencies, setProficiencies] = useState<LanguageProficiency[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editLanguageCode, setEditLanguageCode] = useState<string | undefined>();
  const [editCurrentLevel, setEditCurrentLevel] = useState<CEFRLevel | undefined>();
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isChangingEmail, setIsChangingEmail] = useState(false);

  // Password change form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmittingPassword, setIsSubmittingPassword] = useState(false);

  // Email change form state
  const [newEmail, setNewEmail] = useState('');
  const [isSubmittingEmail, setIsSubmittingEmail] = useState(false);

  const loadProficiencies = useCallback(async () => {
    if (!user) return;

    try {
      const data = await getLanguageProficiencies(user.id);
      setProficiencies(data);
    } catch {
      toast.error('Failed to load language proficiencies');
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      loadProficiencies();
    }
  }, [user, loadProficiencies]);

  // Handle payment result from Stripe
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentResult = params.get('payment');

    if (paymentResult === 'success') {
      toast.success('Subscription activated! Welcome to Premium.');
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

  const handleAddLanguage = async (
    languageCode: string,
    type: LanguageProficiencyType,
    cefrLevel?: CEFRLevel
  ) => {
    if (!user) return;

    try {
      const newProficiency = await addLanguageProficiency(
        user.id,
        languageCode,
        type,
        cefrLevel
      );
      setProficiencies([...proficiencies, newProficiency]);
      toast.success('Language added successfully');
    } catch (err) {
      toast.error('Failed to add language');
      throw err;
    }
  };

  const handleEditLanguage = async (languageCode: string, cefrLevel: CEFRLevel) => {
    if (!user) return;

    try {
      const updatedProficiency = await updateLanguageProficiency(
        user.id,
        languageCode,
        cefrLevel
      );
      setProficiencies(
        proficiencies.map((p) =>
          p.languageCode === languageCode ? updatedProficiency : p
        )
      );
      toast.success('Language level updated');
    } catch (err) {
      toast.error('Failed to update language level');
      throw err;
    }
  };

  const handleSetPrimary = async (languageCode: string) => {
    if (!user) return;

    try {
      await setPrimaryLanguage(user.id, languageCode);
      // Reload proficiencies to ensure consistency with server state
      await loadProficiencies();
      toast.success('Primary language updated');
    } catch {
      toast.error('Failed to set primary language');
    }
  };

  const handleRemoveLanguage = async (languageCode: string) => {
    if (!user) return;

    const proficiency = proficiencies.find((p) => p.languageCode === languageCode);
    if (proficiency?.isPrimary) {
      toast.error('Cannot remove primary language. Set another language as primary first.');
      return;
    }

    if (!confirm('Are you sure you want to remove this language?')) {
      return;
    }

    try {
      await removeLanguageProficiency(user.id, languageCode);
      setProficiencies(proficiencies.filter((p) => p.languageCode !== languageCode));
      toast.success('Language removed');
    } catch {
      toast.error('Failed to remove language');
    }
  };

  const handleOpenEditModal = (languageCode: string, currentLevel: CEFRLevel) => {
    setEditLanguageCode(languageCode);
    setEditCurrentLevel(currentLevel);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditLanguageCode(undefined);
    setEditCurrentLevel(undefined);
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }

    if (newPassword.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }

    setIsSubmittingPassword(true);
    try {
      await changePassword(currentPassword, newPassword);
      toast.success('Password changed successfully');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setIsChangingPassword(false);
    } catch {
      toast.error('Failed to change password. Please check your current password.');
    } finally {
      setIsSubmittingPassword(false);
    }
  };

  const handleChangeEmail = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newEmail.trim()) {
      toast.error('Email cannot be empty');
      return;
    }

    // Basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(newEmail)) {
      toast.error('Please enter a valid email address');
      return;
    }

    setIsSubmittingEmail(true);
    try {
      await changeEmail(newEmail);
      // Refresh user data to get updated email
      await useAuthStore.getState().refreshUser();
      toast.success('Email updated successfully');
      setNewEmail('');
      setIsChangingEmail(false);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      const errorMessage = error.response?.data?.message || 'Failed to update email';
      toast.error(errorMessage);
    } finally {
      setIsSubmittingEmail(false);
    }
  };

  if (!user) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <p className="text-gray-500">Please log in to view your profile</p>
        </div>
      </Layout>
    );
  }

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            Profile & Settings
          </h1>
          <p className="mt-2 text-slate-600">
            Manage your account and learning preferences
          </p>
        </div>

        {/* User Information */}
        <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
              <User className="w-6 h-6 text-white" />
            </div>
            <h2 className="text-xl font-semibold text-slate-900">
              User Information
            </h2>
          </div>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-semibold text-slate-500">Username</label>
              <p className="text-slate-900 mt-1">{user.username}</p>
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-500">Email</label>
              {!isChangingEmail ? (
                <div className="flex items-center gap-3 mt-1">
                  <p className="text-slate-900">{user.email}</p>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setNewEmail(user.email);
                      setIsChangingEmail(true);
                    }}
                  >
                    Edit
                  </Button>
                </div>
              ) : (
                <form onSubmit={handleChangeEmail} className="space-y-3 mt-2">
                  <Input
                    type="email"
                    label="New Email"
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    required
                    disabled={isSubmittingEmail}
                    placeholder="your.email@example.com"
                  />
                  <div className="flex gap-3">
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => {
                        setIsChangingEmail(false);
                        setNewEmail('');
                      }}
                      disabled={isSubmittingEmail}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      size="sm"
                      isLoading={isSubmittingEmail}
                    >
                      Save Email
                    </Button>
                  </div>
                </form>
              )}
            </div>
            {(user.firstName || user.lastName) && (
              <div>
                <label className="text-sm font-semibold text-slate-500">Name</label>
                <p className="text-slate-900 mt-1">
                  {[user.firstName, user.lastName].filter(Boolean).join(' ')}
                </p>
              </div>
            )}
            <div className="pt-4 border-t border-slate-100">
              <h3 className="text-sm font-semibold text-slate-500 mb-2">Account Security</h3>
              {!isChangingPassword ? (
                <Button
                  variant="secondary"
                  onClick={() => setIsChangingPassword(true)}
                >
                  Change Password
                </Button>
              ) : (
                <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
                  <Input
                    type="password"
                    label="Current Password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                    disabled={isSubmittingPassword}
                  />
                  <Input
                    type="password"
                    label="New Password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    minLength={8}
                    disabled={isSubmittingPassword}
                  />
                  <Input
                    type="password"
                    label="Confirm New Password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    minLength={8}
                    disabled={isSubmittingPassword}
                  />
                  <div className="flex gap-3">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => {
                        setIsChangingPassword(false);
                        setCurrentPassword('');
                        setNewPassword('');
                        setConfirmPassword('');
                      }}
                      disabled={isSubmittingPassword}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      isLoading={isSubmittingPassword}
                    >
                      Update Password
                    </Button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </section>



        {/* Language Proficiencies */}
        <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-slate-900">
              Language Proficiencies
            </h2>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setEditLanguageCode(undefined);
                setEditCurrentLevel(undefined);
                setIsModalOpen(true);
              }}
            >
              <Plus className="w-4 h-4 mr-1" />
              Add Language
            </Button>
          </div>

          <LanguageProficiencyList
            proficiencies={proficiencies}
            onSetPrimary={handleSetPrimary}
            onEdit={handleOpenEditModal}
            onRemove={handleRemoveLanguage}
          />
        </section>




      </div>

      {/* Add/Edit Language Modal */}
      <AddLanguageModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onAdd={handleAddLanguage}
        onEdit={handleEditLanguage}
        editLanguageCode={editLanguageCode}
        editCurrentLevel={editCurrentLevel}
        existingLanguageCodes={proficiencies.map((p) => p.languageCode)}
      />
    </Layout>
  );
}
