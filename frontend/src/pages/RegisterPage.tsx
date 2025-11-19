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
                  {lang.name}
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
