import { useEffect } from 'react';
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useLocation,
} from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { Toaster } from 'react-hot-toast';
import { TTSProvider } from './contexts/TTSContext';
import Spinner from './components/ui/Spinner';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import LanguageCatalogPage from './pages/LanguageCatalogPage';
import CourseCatalogPage from './pages/CourseCatalogPage';
import CourseDetailPage from './pages/CourseDetailPage';
import SessionListPage from './pages/SessionListPage';
import ChatPage from './pages/ChatPage';
import VocabularyPage from './pages/VocabularyPage';
import ProfilePage from './pages/ProfilePage';
import AdminSummaryPage from './pages/AdminSummaryPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminUserDetailPage from './pages/AdminUserDetailPage';
import CreateCustomTutorPage from './pages/CreateCustomTutorPage';
import ErrorPatternsPage from './pages/ErrorPatternsPage';
import SubscriptionPage from './pages/SubscriptionPage';
import CourseManagementPage from './pages/CourseManagementPage';
import CourseEditorPage from './pages/CourseEditorPage';
import ErrorBoundary from './components/ui/ErrorBoundary';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!user) {
    // Save intended destination
    const redirect = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  return <>{children}</>;
}

function App() {
  const loadUser = useAuthStore((state) => state.loadUser);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  return (
    <ErrorBoundary>
      <TTSProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route
              path="/languages"
              element={
                <ProtectedRoute>
                  <LanguageCatalogPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/languages/:code/courses"
              element={
                <ProtectedRoute>
                  <CourseCatalogPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/courses/:id"
              element={
                <ProtectedRoute>
                  <CourseDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/sessions"
              element={
                <ProtectedRoute>
                  <SessionListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/chat/:sessionId"
              element={
                <ProtectedRoute>
                  <ChatPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/vocabulary"
              element={
                <ProtectedRoute>
                  <VocabularyPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/error-patterns"
              element={
                <ProtectedRoute>
                  <ErrorPatternsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/subscription"
              element={
                <ProtectedRoute>
                  <SubscriptionPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin/summaries"
              element={
                <ProtectedRoute>
                  <AdminSummaryPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute>
                  <AdminUsersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/users/:userId"
              element={
                <ProtectedRoute>
                  <AdminUserDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/tutors/create"
              element={
                <ProtectedRoute>
                  <CreateCustomTutorPage />
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/courses/manage"
              element={
                <ProtectedRoute>
                  <CourseManagementPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/courses/create"
              element={
                <ProtectedRoute>
                  <CourseEditorPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/courses/edit/:courseId"
              element={
                <ProtectedRoute>
                  <CourseEditorPage />
                </ProtectedRoute>
              }
            />

            {/* Default to sessions page */}
            <Route path="/" element={<Navigate to="/sessions" replace />} />
          </Routes>
        </BrowserRouter>
        <Toaster position="top-right" />
      </TTSProvider>
    </ErrorBoundary>
  );
}

export default App;
