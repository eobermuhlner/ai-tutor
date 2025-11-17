import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../api/auth';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import toast from 'react-hot-toast';

interface ValidationErrors {
  username?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  nativeLanguage?: string;
}

// Common languages for registration (subset of most common languages)
// Full list will be available after login in user profile
const COMMON_LANGUAGES = [
  { code: 'en', name: 'English', flagEmoji: '🇬🇧' },
  { code: 'es', name: 'Spanish', flagEmoji: '🇪🇸' },
  { code: 'fr', name: 'French', flagEmoji: '🇫🇷' },
  { code: 'de', name: 'German', flagEmoji: '🇩🇪' },
  { code: 'it', name: 'Italian', flagEmoji: '🇮🇹' },
  { code: 'pt', name: 'Portuguese', flagEmoji: '🇵🇹' },
  { code: 'ru', name: 'Russian', flagEmoji: '🇷🇺' },
  { code: 'ja', name: 'Japanese', flagEmoji: '🇯🇵' },
  { code: 'zh', name: 'Chinese', flagEmoji: '🇨🇳' },
  { code: 'ko', name: 'Korean', flagEmoji: '🇰🇷' },
  { code: 'ar', name: 'Arabic', flagEmoji: '🇸🇦' },
  { code: 'hi', name: 'Hindi', flagEmoji: '🇮🇳' },
  { code: 'nl', name: 'Dutch', flagEmoji: '🇳🇱' },
  { code: 'pl', name: 'Polish', flagEmoji: '🇵🇱' },
  { code: 'tr', name: 'Turkish', flagEmoji: '🇹🇷' },
];

// Helper function to detect browser language and map to supported language code
const detectBrowserLanguage = (): string => {
  const browserLang = navigator.language.split('-')[0]; // Get language code without region
  const supportedCodes = COMMON_LANGUAGES.map((lang) => lang.code);

  // Try exact match first
  if (supportedCodes.includes(browserLang)) {
    return browserLang;
  }

  // Default to English
  return 'en';
};

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [nativeLanguage, setNativeLanguage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<ValidationErrors>({});

  const navigate = useNavigate();

  // Detect browser language on mount
  useEffect(() => {
    const detectedLang = detectBrowserLanguage();
    setNativeLanguage(detectedLang);
  }, []);

  const validatePassword = (pwd: string): string | undefined => {
    if (!pwd) {
      return 'Password is required';
    }
    if (pwd.length < 8) {
      return 'Password must be at least 8 characters';
    }
    if (!/[A-Z]/.test(pwd)) {
      return 'Password must contain at least one uppercase letter';
    }
    if (!/[a-z]/.test(pwd)) {
      return 'Password must contain at least one lowercase letter';
    }
    if (!/[0-9]/.test(pwd)) {
      return 'Password must contain at least one number';
    }
    return undefined;
  };

  const validateForm = (): boolean => {
    const newErrors: ValidationErrors = {};

    if (!username) {
      newErrors.username = 'Username is required';
    } else if (username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    }

    if (!email) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Invalid email address';
    }

    const passwordError = validatePassword(password);
    if (passwordError) {
      newErrors.password = passwordError;
    }

    if (!confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (password !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    if (!nativeLanguage) {
      newErrors.nativeLanguage = 'Please select your native language';
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
      await register(username, email, password);

      // Store native language in localStorage to be set on first login
      localStorage.setItem('pendingNativeLanguage', nativeLanguage);

      toast.success('Registration successful! Please login.');
      navigate('/login');
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Registration failed. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-white to-brand-50/30 px-4 py-8">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-soft-lg border border-slate-100">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-brand-600 to-brand-700 bg-clip-text text-transparent">
            AI Tutor
          </h1>
          <p className="mt-2 text-slate-600">Create your account to start learning</p>
        </div>

        {/* Google Sign Up */}
        <Button
          type="button"
          variant="outline"
          className="w-full mb-4 flex items-center justify-center gap-3 bg-white hover:bg-slate-50 border border-slate-300"
          onClick={() => {
            // Use the same protocol as the current page (HTTP or HTTPS) to avoid mixed content errors
            const currentProtocol = window.location.protocol;
            const currentHost = window.location.host;
            const baseUrl = import.meta.env.VITE_API_BASE_URL || `${currentProtocol}//${currentHost}`;
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
            <span className="px-4 bg-white text-slate-500">Or register with email</span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            id="username"
            type="text"
            label="Username"
            placeholder="johndoe"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            error={errors.username}
            disabled={isLoading}
          />

          <Input
            id="email"
            type="email"
            label="Email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={errors.email}
            disabled={isLoading}
          />

          <Input
            id="password"
            type="password"
            label="Password"
            placeholder="Min 8 chars, uppercase, lowercase, number"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            disabled={isLoading}
          />

          <Input
            id="confirmPassword"
            type="password"
            label="Confirm Password"
            placeholder="Re-enter your password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            error={errors.confirmPassword}
            disabled={isLoading}
          />

          <div>
            <label
              htmlFor="nativeLanguage"
              className="block text-sm font-medium text-slate-700 mb-2"
            >
              Native Language
            </label>
            <select
              id="nativeLanguage"
              value={nativeLanguage}
              onChange={(e) => setNativeLanguage(e.target.value)}
              disabled={isLoading}
              className={`w-full px-4 py-2.5 rounded-lg border transition-all duration-200 ${
                errors.nativeLanguage
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                  : 'border-slate-200 focus:border-brand-500 focus:ring-brand-500'
              } focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:cursor-not-allowed`}
            >
              <option value="">Select your native language...</option>
              {COMMON_LANGUAGES.map((lang) => (
                <option key={lang.code} value={lang.code}>
                  {lang.flagEmoji} {lang.name}
                </option>
              ))}
            </select>
            {errors.nativeLanguage && (
              <p className="mt-1 text-sm text-red-600">{errors.nativeLanguage}</p>
            )}
            <p className="mt-1.5 text-xs text-slate-500">
              You can add more languages later in your profile
            </p>
          </div>

          <Button
            type="submit"
            className="w-full mt-6"
            isLoading={isLoading}
            disabled={isLoading}
          >
            Create Account
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-600">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700 transition-colors">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
