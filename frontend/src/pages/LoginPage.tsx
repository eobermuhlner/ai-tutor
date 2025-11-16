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

        {/* Google Sign In */}
        <Button
          type="button"
          variant="outline"
          className="w-full mb-4 flex items-center justify-center gap-3 bg-white hover:bg-slate-50 border border-slate-300"
          onClick={() => {
            // Extract base URL without /api/v1 suffix for OAuth2 endpoints
            let baseUrl;
            if (import.meta.env.VITE_API_BASE_URL) {
              // Remove /api/v1 suffix if present to get the base server URL
              baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/api\/v1$/, '');
            } else {
              // Use the same protocol as the current page (HTTP or HTTPS) to avoid mixed content errors
              const currentProtocol = window.location.protocol;
              const currentHost = window.location.host;
              baseUrl = `${currentProtocol}//${currentHost}`;
            }
            
            // Ensure OAuth2 URLs always use the same protocol as the current page to avoid mixed content
            const currentProtocol = window.location.protocol;
            // Parse the base URL and replace its protocol with the current page's protocol
            try {
              const urlObject = new URL(baseUrl);
              urlObject.protocol = currentProtocol;
              baseUrl = urlObject.toString().slice(0, -1); // Remove trailing slash added by URL.toString()
            } catch {
              // If URL parsing fails, fallback to protocol replacement approach
              baseUrl = baseUrl.replace(/^https?:\/\//, `${currentProtocol}//`);
            }
            
            window.location.href = `${baseUrl}/oauth2/authorization/google`;
          }}
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
          </svg>
          <span className="text-slate-700 font-medium">Continue with Google</span>
        </Button>

        <div className="relative my-6">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-300"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-4 bg-white text-slate-500">Or continue with email</span>
          </div>
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
