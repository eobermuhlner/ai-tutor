import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { setTokens } from '../utils/storage';
import Spinner from '../components/ui/Spinner';
import toast from 'react-hot-toast';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const loadUser = useAuthStore((state) => state.loadUser);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Extract tokens from URL fragment
        const hash = window.location.hash.substring(1); // Remove '#'
        const params = new URLSearchParams(hash);

        const accessToken = params.get('access_token');
        const refreshToken = params.get('refresh_token');

        if (!accessToken || !refreshToken) {
          throw new Error('Missing authentication tokens');
        }

        // Store tokens
        setTokens(accessToken, refreshToken);

        // Load user data
        await loadUser();

        toast.success('Successfully signed in with Google!');

        // Redirect to sessions page
        navigate('/sessions', { replace: true });
      } catch (error) {
        console.error('OAuth2 callback error:', error);
        toast.error(
          error instanceof Error
            ? error.message
            : 'Failed to complete Google sign-in. Please try again.'
        );
        navigate('/login', { replace: true });
      }
    };

    handleCallback();
  }, [navigate, loadUser]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-white to-brand-50/30">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-slate-600">Completing sign-in...</p>
      </div>
    </div>
  );
}
