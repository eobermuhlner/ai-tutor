import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import TutorImage from '../components/tutor/TutorImage';
import { getActiveSessions, deleteSession, getSession } from '../api/chat';
import { getLanguages } from '../api/catalog';
import { getLanguageProficiencies } from '../api/userLanguages';
import { useAuthStore } from '../store/authStore';
import { formatCompactLanguageDisplay, getLanguageAriaLabel } from '../utils/languageDisplay';
import type { Session, Language, LanguageProficiency } from '../types';

export default function SessionListPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [languageProficiencies, setLanguageProficiencies] = useState<LanguageProficiency[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      if (!user?.id) return;

      try {
        // First get the active sessions list
        const [sessionsData, languagesData, proficienciesData] = await Promise.all([
          getActiveSessions(user.id),
          getLanguages(),
          getLanguageProficiencies(user.id),
        ]);
        setLanguages(languagesData);
        setLanguageProficiencies(proficienciesData);

        // Then fetch detailed information for each session to get tutor data
        // This is needed because the getActiveSessions API doesn't return tutor details
        const detailedSessions = await Promise.all(
          sessionsData.map(async (session) => {
            try {
              // Fetch detailed session info to get tutor profile data
              const detailedSession = await getSession(session.id);
              return detailedSession;
            } catch (error) {
              // If detailed session fetch fails, return the original session data
              console.error(`Failed to get detailed session info for ${session.id}:`, error);
              return session;
            }
          })
        );

        setSessions(detailedSessions);
      } catch (error) {
        console.error('Failed to load data:', error);
        toast.error('Failed to load sessions');
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, [user?.id]);

  const handleDeleteSession = async (sessionId: string) => {
    if (!confirm('Are you sure you want to delete this session?')) return;

    try {
      await deleteSession(sessionId);
      setSessions((prev) => prev.filter((s) => s.id !== sessionId));
      toast.success('Session deleted');
    } catch {
      toast.error('Failed to delete session');
    }
  };

  const getLanguage = (code: string) => {
    return languages.find((lang) => lang.code === code);
  };

  const getLanguageDisplay = (code: string) => {
    const language = getLanguage(code);
    return language ? formatCompactLanguageDisplay(language) : code.toUpperCase();
  };

  const getLanguageLabel = (code: string) => {
    const language = getLanguage(code);
    return language ? getLanguageAriaLabel(language) : code;
  };

  if (isLoading) {
    return (
      <Layout>
        <div className="flex min-h-[60vh] items-center justify-center">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            Learning Sessions
          </h1>
          <p className="mt-2 text-slate-600">
            Continue your active learning sessions or start a new one
          </p>
        </div>
        <Button onClick={() => navigate('/languages')}>Start New Session</Button>
      </div>

      {sessions.length === 0 ? (
        languageProficiencies.length === 0 ? (
          <EmptyState
            title="Welcome to AI Tutor!"
            message="First, set up your language proficiencies to start learning. Tell us what languages you know and want to learn."
            action={{
              label: 'Set Up Languages',
              onClick: () => navigate('/profile'),
            }}
          />
        ) : (
          <EmptyState
            title="Start Your Learning Journey"
            message="You haven't started any sessions yet. Choose a language and tutor to begin your first conversation!"
            action={{
              label: 'Start Your First Session',
              onClick: () => navigate('/languages'),
            }}
          />
        )
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {sessions.map((session) => (
            <div
              key={session.id}
              className="rounded-2xl border border-slate-200 bg-white p-6 shadow-soft transition-all hover:shadow-soft-lg hover:border-brand-200"
            >
              <div className="mb-4 flex gap-3 items-start">
                {session.tutorProfileId && (
                  <div className="flex-shrink-0">
                    <TutorImage
                      tutorId={session.tutorProfileId}
                      tutorEmoji={session.tutorEmoji || ''}
                      tutorName={session.tutorName || 'Tutor'}
                      size="medium"
                      rounded="lg"
                    />
                  </div>
                )}
                <div className="flex-1">
                  <h2 className="text-xl font-semibold text-slate-900">
                    {session.courseName}
                  </h2>
                  <p className="text-sm text-slate-600 mt-1" aria-label={getLanguageLabel(session.targetLanguageCode)}>
                    {getLanguageDisplay(session.targetLanguageCode)} · {session.userLevel}
                  </p>
                </div>
              </div>

              {session.currentTopic && (
                <div className="mb-4 p-3 bg-slate-50 rounded-lg">
                  <p className="text-sm text-slate-700">
                    <span className="font-semibold">Current topic:</span>{' '}
                    {session.currentTopic}
                  </p>
                </div>
              )}

              <div className="mb-4 text-xs text-slate-500 space-y-1">
                <p>Created: {new Date(session.createdAt).toLocaleDateString()}</p>
                <p>Last activity: {new Date(session.updatedAt).toLocaleString()}</p>
              </div>

              <div className="flex gap-2">
                <Button
                  onClick={() => navigate(`/chat/${session.id}`)}
                  size="sm"
                  className="flex-1"
                >
                  Continue
                </Button>
                <Button
                  onClick={() => handleDeleteSession(session.id)}
                  variant="danger"
                  size="sm"
                  aria-label="Delete session"
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </Layout>
  );
}
