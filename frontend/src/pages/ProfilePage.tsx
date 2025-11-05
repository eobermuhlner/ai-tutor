import { useState, useEffect, useCallback } from 'react';
import { Plus, User } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Spinner from '../components/ui/Spinner';
import LanguageProficiencyList from '../components/profile/LanguageProficiencyList';
import AddLanguageModal from '../components/profile/AddLanguageModal';
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

  // Profile name change state
  const [isEditingName, setIsEditingName] = useState(false);
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');
  const [isSubmittingName, setIsSubmittingName] = useState(false);
  
  // Pronunciation preference state
  const [isEditingPronunciation, setIsEditingPronunciation] = useState(false);
  const [pronunciationPreference, setPronunciationPreference] = useState(user?.pronunciationPreference || 'NONE');
  const [isSubmittingPronunciation, setIsSubmittingPronunciation] = useState(false);

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
      setFirstName(user.firstName || '');
      setLastName(user.lastName || '');
      setPronunciationPreference(user.pronunciationPreference);
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
      // Clean up URL and redirect to subscription page
      window.history.replaceState({}, document.title, window.location.pathname);
    } else if (paymentResult === 'cancel') {
      toast('Checkout canceled. You can upgrade anytime.');
      // Clean up URL and redirect to subscription page
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

  const handleUpdateName = async (e: React.FormEvent) => {
    e.preventDefault();

    setIsSubmittingName(true);
    try {
      await import('../api/auth').then(({ updateProfile }) => 
        updateProfile(firstName || null, lastName || null)
      );
      // Refresh user data to get updated name
      await useAuthStore.getState().refreshUser();
      toast.success('Name updated successfully');
      setIsEditingName(false);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      const errorMessage = error.response?.data?.message || 'Failed to update name';
      toast.error(errorMessage);
    } finally {
      setIsSubmittingName(false);
    }
  };

  const handleUpdatePronunciationPreference = async (e: React.FormEvent) => {
    e.preventDefault();

    setIsSubmittingPronunciation(true);
    try {
      await import('../api/auth').then(({ updatePronunciationPreference }) => 
        updatePronunciationPreference(pronunciationPreference)
      );
      // Refresh user data to get updated pronunciation preference
      await useAuthStore.getState().refreshUser();
      toast.success('Pronunciation preference updated successfully');
      setIsEditingPronunciation(false);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      const errorMessage = error.response?.data?.message || 'Failed to update pronunciation preference';
      toast.error(errorMessage);
    } finally {
      setIsSubmittingPronunciation(false);
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
            {!isEditingName ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-semibold text-slate-500">First Name</label>
                  <p className="text-slate-900 mt-1">{user.firstName || '-'}</p>
                </div>
                <div>
                  <label className="text-sm font-semibold text-slate-500">Last Name</label>
                  <div className="flex items-center gap-3 mt-1">
                    <p className="text-slate-900">{user.lastName || '-'}</p>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => {
                        setFirstName(user.firstName || '');
                        setLastName(user.lastName || '');
                        setIsEditingName(true);
                      }}
                    >
                      Edit
                    </Button>
                  </div>
                </div>
              </div>
            ) : (
              <form onSubmit={handleUpdateName} className="space-y-3 mt-2">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input
                    type="text"
                    label="First Name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    disabled={isSubmittingName}
                    placeholder="Your first name"
                  />
                  <Input
                    type="text"
                    label="Last Name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    disabled={isSubmittingName}
                    placeholder="Your last name"
                  />
                </div>
                <div className="flex gap-3">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setIsEditingName(false);
                      setFirstName(user.firstName || '');
                      setLastName(user.lastName || '');
                    }}
                    disabled={isSubmittingName}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    size="sm"
                    isLoading={isSubmittingName}
                  >
                    Save Name
                  </Button>
                </div>
              </form>
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

        {/* Pronunciation Preference */}
        <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
              <User className="w-6 h-6 text-white" />
            </div>
            <h2 className="text-xl font-semibold text-slate-900">
              Pronunciation Guide
            </h2>
          </div>
          <div className="space-y-4">
            {!isEditingPronunciation ? (
              <div>
                <label className="text-sm font-semibold text-slate-500">Pronunciation Preference</label>
                <div className="flex items-center gap-3 mt-1">
                  <p className="text-slate-900 capitalize">
                    {user.pronunciationPreference.replace('_', ' ').toLowerCase()}
                  </p>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setPronunciationPreference(user.pronunciationPreference);
                      setIsEditingPronunciation(true);
                    }}
                  >
                    Edit
                  </Button>
                </div>
                <p className="text-sm text-slate-600 mt-2">
                  Choose how pronunciation guidance is provided during conversations
                </p>
              </div>
            ) : (
              <form onSubmit={handleUpdatePronunciationPreference} className="space-y-3 mt-2">
                <select
                  value={pronunciationPreference}
                  onChange={(e) => setPronunciationPreference(e.target.value)}
                  disabled={isSubmittingPronunciation}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 disabled:opacity-50"
                >
                  <option value="NONE">No pronunciation guide</option>
                  <option value="IPA">Use International Phonetic Alphabet (IPA)</option>
                  <option value="SOURCE_LANGUAGE">Use source language pronunciation</option>
                  <option value="ENGLISH">Use simple English pronunciation</option>
                </select>
                <div className="flex gap-3">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setIsEditingPronunciation(false);
                      setPronunciationPreference(user.pronunciationPreference);
                    }}
                    disabled={isSubmittingPronunciation}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    size="sm"
                    isLoading={isSubmittingPronunciation}
                  >
                    Save Preference
                  </Button>
                </div>
              </form>
            )}
          </div>
        </section>

        {/* Language Proficiencies */}
        <section id="language-proficiencies" className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
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
