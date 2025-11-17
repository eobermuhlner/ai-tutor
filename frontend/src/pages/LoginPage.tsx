import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { addLanguageProficiency } from '../api/userLanguages';
import { getSessions } from '../api/chat';
import { LanguageProficiencyType } from '../types';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>(
    {}
  );

  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuthStore();

  const validateForm = (): boolean => {
    const newErrors: { email?: string; password?: string } = {};

    if (!email) {
      newErrors.email = 'Email or username is required';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    try {
      const user = await login(email, password);
      toast.success('Login successful!');

      // Check if there's a pending native language from registration
      const pendingLanguage = localStorage.getItem('pendingNativeLanguage');
      if (pendingLanguage && user) {
        try {
          await addLanguageProficiency(
            user.id,
            pendingLanguage,
            LanguageProficiencyType.Native
          );
          localStorage.removeItem('pendingNativeLanguage');
        } catch (langError) {
          console.error('Failed to set native language:', langError);
          // Don't fail login if language setting fails
        }
      }

      // Redirect to intended destination or determine default page
      const redirect = searchParams.get('redirect');
      if (redirect) {
        navigate(decodeURIComponent(redirect));
      } else {
        // Check if user has any sessions
        try {
          const sessions = await getSessions(user.id);
          // If user has sessions, go to sessions page; otherwise go to languages
          navigate(sessions.length > 0 ? '/sessions' : '/languages');
        } catch (error) {
          console.error('Failed to fetch sessions:', error);
          // Default to sessions page on error
          navigate('/sessions');
        }
      }
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : 'Login failed. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-white to-brand-50/30 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-soft-lg border border-slate-100">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-brand-600 to-brand-700 bg-clip-text text-transparent">
            AI Tutor
          </h1>
          <p className="mt-2 text-slate-600">Welcome back! Please sign in to continue.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <Input
            id="email"
            type="text"
            label="Email or Username"
            placeholder="you@example.com or username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={errors.email}
            disabled={isLoading}
          />

          <Input
            id="password"
            type="password"
            label="Password"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            disabled={isLoading}
          />

          <Button
            type="submit"
            className="w-full"
            isLoading={isLoading}
            disabled={isLoading}
          >
            Sign In
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-600">
          Don't have an account?{' '}
          <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700 transition-colors">
            Create account
          </Link>
        </div>
      </div>
    </div>
  );
}
