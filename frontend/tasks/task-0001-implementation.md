# Task 0001: Implementation Plan

## Principles
- **Keep it simple**: Start with minimal functionality, add features incrementally
- **No over-engineering**: Use standard patterns, avoid premature abstractions
- **Ship early**: Get working features in front of users quickly
- **Iterate**: Improve based on real usage, not speculation

## Phase 1: Foundation

### Dependencies
```bash
npm install react-router-dom axios lucide-react zustand react-hot-toast
npm install -D prettier eslint-config-prettier @testing-library/react @testing-library/jest-dom vitest jsdom
```

### Folder Structure
```
src/
├── api/
│   ├── client.ts           # Axios instance with interceptors + token refresh
│   ├── auth.ts             # Auth API calls
│   ├── catalog.ts          # Catalog API calls
│   ├── chat.ts             # Chat API calls + SSE streaming
│   ├── vocabulary.ts       # Vocabulary API calls
│   └── userLanguages.ts    # User language proficiency API calls
├── components/
│   ├── ui/                 # Reusable primitives
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Card.tsx
│   │   ├── Spinner.tsx
│   │   ├── ErrorBoundary.tsx
│   │   └── EmptyState.tsx
│   ├── layout/
│   │   ├── Header.tsx
│   │   └── Layout.tsx
│   ├── chat/               # Chat-specific components
│   │   ├── MessageList.tsx
│   │   ├── MessageInput.tsx
│   │   ├── PhaseIndicator.tsx
│   │   └── CorrectedText.tsx
│   ├── catalog/
│   │   ├── LanguageCard.tsx
│   │   ├── CourseCard.tsx
│   │   └── FilterBar.tsx
│   ├── vocabulary/
│   │   ├── VocabularyTable.tsx
│   │   └── VocabularyDetail.tsx
│   └── profile/
│       ├── LanguageProficiencyList.tsx
│       └── AddLanguageModal.tsx
├── pages/
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── SessionListPage.tsx
│   ├── ChatPage.tsx
│   ├── LanguageCatalogPage.tsx
│   ├── CourseCatalogPage.tsx
│   ├── CourseDetailPage.tsx
│   ├── VocabularyPage.tsx
│   └── ProfilePage.tsx
├── store/
│   └── authStore.ts        # Zustand store for auth state
├── utils/
│   ├── storage.ts          # localStorage wrapper
│   └── constants.ts        # App constants
├── types.ts                # All TypeScript types
└── App.tsx                 # Router setup
```

### Tasks
- [ ] Install dependencies
- [ ] Create folder structure
- [ ] Setup TypeScript strict mode in `tsconfig.json`
- [ ] Add Prettier config (`.prettierrc`)
- [ ] Add `.env.example` with `VITE_API_URL=http://localhost:8080/api/v1`
- [ ] Create `.env` from `.env.example`
- [ ] Setup API client with token refresh (`src/api/client.ts`)
- [ ] Define all types in `src/types.ts`
- [ ] Setup storage utility (`src/utils/storage.ts`)
- [ ] Setup Router in `App.tsx`
- [ ] Add basic ErrorBoundary component

## Phase 2: Authentication

### Components to Build
- `src/components/ui/Button.tsx` - Reusable button
- `src/components/ui/Input.tsx` - Input with label
- `src/components/layout/Header.tsx` - App header with logout
- `src/components/layout/Layout.tsx` - Page wrapper with header
- `src/store/authStore.ts` - Zustand auth store
- `src/pages/LoginPage.tsx` - Login form
- `src/pages/RegisterPage.tsx` - Register form

### API Endpoints
- `src/api/auth.ts`:
  - `register(email, password)`
  - `login(email, password)` → returns `{ accessToken, refreshToken, user }`
  - `refreshToken(refreshToken)` → returns new `{ accessToken, refreshToken }`
  - `getMe()`
  - `logout()`

### Flow
1. User visits `/login`
2. Enters email/password, submits form (validate: min 8 chars, has uppercase, lowercase, number)
3. Call `login()` → store tokens in localStorage (via storage utils)
4. Save intended destination from query param (`?redirect=/chat/123`)
5. Redirect to intended destination or `/languages` (catalog as default)
6. On app load, check for token → call `getMe()` → set user in Zustand
7. If access token expired, use refresh token to get new access token
8. If refresh token expired, redirect to login

### Token Refresh Strategy
- Intercept 401 responses in axios
- Attempt token refresh with refresh token
- Retry original request with new access token
- If refresh fails, clear tokens and redirect to login
- Queue multiple failed requests during refresh to avoid race conditions

### Done When
- [ ] User can register with password validation
- [ ] User can login
- [ ] User can logout
- [ ] Access token auto-refreshes before expiration
- [ ] Token persists across page reloads
- [ ] Protected routes redirect to login with return URL
- [ ] Login redirects to intended destination
- [ ] Expired refresh token clears state and shows login

## Phase 3: Course Catalog (MOVED UP - needed before chat)

### Components to Build
- `src/pages/LanguageCatalogPage.tsx` - Grid of languages
- `src/pages/CourseCatalogPage.tsx` - List of courses with filters
- `src/pages/CourseDetailPage.tsx` - Course detail + "Start Learning"
- `src/components/catalog/LanguageCard.tsx`
- `src/components/catalog/CourseCard.tsx`
- `src/components/catalog/FilterBar.tsx`
- `src/components/ui/Spinner.tsx` - Loading indicator
- `src/components/ui/EmptyState.tsx` - No results state

### API Endpoints
- `src/api/catalog.ts`:
  - `getLanguages(sourceLanguage?)`
  - `getCourses(languageCode, sourceLanguage?, userLevel?, category?)`
  - `getCourse(courseId, sourceLanguage?)`

### Flow
1. User visits `/languages` (default after login)
2. Shows grid of language cards with flags/names
3. Clicks language → navigate to `/languages/:code/courses`
4. Shows courses with filters (level/category)
5. Clicks course → navigate to `/courses/:id`
6. Shows course details (description, topics, tutor personality)
7. Clicks "Start Learning" → create session → navigate to `/chat/:sessionId`

### Error Handling
- Show spinner while loading
- Show error toast if API fails with retry button
- Show empty state if no courses found
- Disable "Start Learning" button while creating session

### Done When
- [ ] User can browse languages with loading state
- [ ] User can browse courses for a language
- [ ] User can filter courses by level/category
- [ ] User can view course details
- [ ] User can start session from course (creates session, navigates to chat)
- [ ] All loading states and error handling work
- [ ] Empty states show helpful messages

## Phase 4: Chat Interface

### Components to Build
- `src/pages/SessionListPage.tsx` - List sessions + progress indicators
- `src/pages/ChatPage.tsx` - Chat interface with streaming
- `src/components/chat/MessageList.tsx` - Display messages with auto-scroll
- `src/components/chat/MessageInput.tsx` - Input + send button + keyboard support
- `src/components/chat/PhaseIndicator.tsx` - Badge showing phase with dropdown

### API Endpoints
- `src/api/chat.ts`:
  - `createSessionFromCourse(courseId)` → returns session
  - `getSessions(userId)` → list all sessions
  - `getActiveSessions(userId)` → sessions with progress
  - `getSession(sessionId)` → session with messages
  - `streamChatResponse(sessionId, message, signal)` → AsyncGenerator for streaming
  - `updatePhase(sessionId, phase)`
  - `deleteSession(sessionId)`

### SSE Streaming Implementation
**CRITICAL: EventSource doesn't support POST or custom headers**

Use fetch with ReadableStream in `src/api/chat.ts`:
```typescript
// src/api/chat.ts
export async function* streamChatResponse(
  sessionId: string,
  message: string,
  signal?: AbortSignal
): AsyncGenerator<string, void, unknown> {
  const token = storage.getAccessToken();
  const response = await fetch(
    `${API_URL}/chat/sessions/${sessionId}/messages/stream`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ message }),
      signal,
    }
  );

  if (!response.ok) throw new Error(`Stream failed: ${response.status}`);
  if (!response.body) throw new Error('No response body');

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      yield chunk;
    }
  } finally {
    reader.releaseLock();
  }
}
```

### Flow
1. User navigates to `/sessions` to see session list
2. Shows sessions with last activity, message count, topics covered
3. Clicks "Continue" → navigate to `/chat/:sessionId`
4. Load session with `getSession()` → display messages
5. User types message, clicks send (or presses Enter)
6. Disable input, show typing indicator
7. Inline streaming logic in ChatPage:
   ```typescript
   const [isStreaming, setIsStreaming] = useState(false);
   const abortControllerRef = useRef<AbortController | null>(null);

   async function handleSend(message: string) {
     setIsStreaming(true);
     abortControllerRef.current = new AbortController();

     try {
       for await (const chunk of streamChatResponse(sessionId, message, abortControllerRef.current.signal)) {
         // Update message state with chunk
       }
     } catch (error) {
       toast.error('Failed to send message');
     } finally {
       setIsStreaming(false);
     }
   }
   ```
8. Stream chunks into message as they arrive
9. On stream complete, enable input, scroll to bottom
10. Phase indicator shows current phase with tooltip
11. User can click phase dropdown to manually switch phases

### Error Handling & UX
- **Loading**: Show spinner while fetching session
- **Streaming**: Show typing indicator, disable input, show cancel button
- **Error**: Show error toast, allow retry
- **Cancel**: Abort fetch, enable input
- **Rate limiting**: Disable send button while streaming
- **Auto-scroll**: Scroll to bottom when new message arrives
- **Keyboard**: Enter to send, Shift+Enter for newline
- **Mobile**: Fixed input at bottom, use `dvh` instead of `vh`, handle safe areas

### Done When
- [ ] User sees list of sessions with progress
- [ ] User can create new session from course
- [ ] User can open session and see messages
- [ ] User can send message with Enter key
- [ ] AI response streams in real-time using POST + ReadableStream
- [ ] Input disabled while streaming, shows cancel button
- [ ] Phase indicator displays current phase with dropdown
- [ ] User can manually switch phases
- [ ] Auto-scroll works correctly
- [ ] Mobile keyboard doesn't cover input
- [ ] All loading/error states work
- [ ] Session list shows empty state when no sessions

## Phase 5: Error Corrections

### Error Data Structure
```typescript
// Add to src/types.ts
export enum ErrorType {
  GRAMMAR = 'GRAMMAR',
  SPELLING = 'SPELLING',
  VOCABULARY = 'VOCABULARY',
  WORD_ORDER = 'WORD_ORDER',
  VERB_FORM = 'VERB_FORM',
  ARTICLE = 'ARTICLE',
  PREPOSITION = 'PREPOSITION',
  PUNCTUATION = 'PUNCTUATION',
  OTHER = 'OTHER',
}

export enum ErrorSeverity {
  CRITICAL = 'CRITICAL',   // Red
  HIGH = 'HIGH',           // Orange
  MEDIUM = 'MEDIUM',       // Yellow
  LOW = 'LOW',             // Blue
}

export interface Correction {
  startIndex: number;
  endIndex: number;
  originalText: string;
  correctedText: string;
  errorType: ErrorType;
  severity: ErrorSeverity;
  explanation?: string;
}

export interface MessageMetadata {
  corrections: Correction[];
  phase: ConversationPhase;
}
```

### Components to Build
- `src/components/chat/CorrectedText.tsx` - Text with clickable/hoverable error spans
- `src/components/chat/CorrectionTooltip.tsx` - Tooltip/popover with correction details

### Implementation Details
- Parse corrections from message metadata (backend provides as JSON)
- Split message text into segments based on correction indices
- Render segments with different styles:
  - Normal text: no styling
  - Error text: underline with color based on severity
    - Critical: `border-b-2 border-red-500`
    - High: `border-b-2 border-orange-500`
    - Medium: `border-b-2 border-yellow-500`
    - Low: `border-b-2 border-blue-500`
- **Desktop**: Show tooltip on hover
- **Mobile/Touch**: Show popover on tap/click
- **Keyboard**: Focus with Tab, show with Space/Enter
- Show corrections in all modes when available
- Tooltip shows:
  - Original text (strikethrough)
  - Corrected text (green)
  - Error type badge
  - Severity indicator (colored dot)
  - Explanation (if available)

### Accessibility
- Use `role="button"` and `tabIndex={0}` on error spans
- Add `aria-label` with correction summary
- Keyboard navigation (Tab to focus, Space/Enter to toggle)
- Screen reader announces "Error: [type], press Enter for details"
- Sufficient color contrast (don't rely on color alone)
- Touch targets minimum 44x44px on mobile

### Done When
- [ ] Errors highlighted in user messages with correct colors
- [ ] Desktop: Hover shows correction tooltip
- [ ] Mobile: Tap shows correction popover
- [ ] Keyboard: Tab focus + Space/Enter shows details
- [ ] Corrections always shown when available
- [ ] Accessible to screen readers
- [ ] Works on mobile with proper touch targets

## Phase 6: Vocabulary

### Components to Build
- `src/pages/VocabularyPage.tsx` - List of vocabulary items
- `src/components/vocabulary/VocabularyTable.tsx`
- `src/components/vocabulary/VocabularyDetail.tsx` - Modal/detail view

### API Endpoints
- `src/api/vocabulary.ts`:
  - `getVocabulary(language?)`
  - `getVocabularyItem(itemId)`

### Flow
1. User visits `/vocabulary`
2. Sees table of all vocabulary items
3. Can filter by language
4. Clicks item → see all contexts

### Done When
- [ ] User can view vocabulary list
- [ ] User can filter by language
- [ ] User can click item to see contexts

## Phase 7: User Profile & Settings

### Components to Build
- `src/pages/ProfilePage.tsx` - User profile and settings
- `src/components/profile/LanguageProficiencyList.tsx`
- `src/components/profile/AddLanguageModal.tsx`

### API Endpoints
- `src/api/userLanguages.ts`:
  - `getLanguageProficiencies(userId)`
  - `addLanguageProficiency(userId, languageCode, level)`
  - `updateLanguageProficiency(userId, languageCode, level)`
  - `setPrimaryLanguage(userId, languageCode)`
  - `removeLanguageProficiency(userId, languageCode)`
- `src/api/auth.ts`:
  - `changePassword(oldPassword, newPassword)`

### Done When
- [ ] User can view their profile
- [ ] User can add/edit/remove language proficiencies
- [ ] User can set primary language
- [ ] User can change password
- [ ] All forms have proper validation

## Phase 8: Testing & Quality Assurance

### Testing Setup (Configure Vitest)
- [ ] Configure Vitest in `vite.config.ts`
- [ ] Add React Testing Library setup
- [ ] Create test utilities and mocks

### Critical Path Tests
- [ ] **Auth flow**: Login → token storage → getMe → protected route access
- [ ] **Token refresh**: Mock 401 → refresh token called → request retried
- [ ] **Message streaming**: Mock fetch + ReadableStream → chunks processed
- [ ] **Correction parsing**: Given correction data → text segments rendered correctly

### Code Quality
- [ ] All components have proper TypeScript types
- [ ] No `any` types except where truly necessary
- [ ] ESLint passes with no warnings
- [ ] Prettier formatting applied
- [ ] No console errors or warnings in browser
- [ ] All API calls have error handling
- [ ] All forms have validation

### Manual Testing Checklist
- [ ] Complete happy path from requirements (register → login → browse → chat → logout)
- [ ] Test on mobile viewport (responsive design)
- [ ] Test keyboard navigation (Tab, Enter, Esc)
- [ ] Test with slow 3G network (loading states, timeouts)
- [ ] Test error states (wrong password, network failure, 401 redirect)

### Done When
- [ ] Auth flow test passes
- [ ] Token refresh test passes
- [ ] Streaming test passes
- [ ] Correction parsing test passes
- [ ] Code quality checks pass
- [ ] Manual testing complete
- [ ] No critical bugs

## Phase 9: Deploy

### Tasks
- [ ] Test all flows manually
- [ ] Fix critical bugs
- [ ] Add environment variables for production API URL
- [ ] Build: `npm run build`
- [ ] Deploy to Vercel/Netlify
- [ ] Add basic README with setup instructions

### Done When
- [ ] App deployed and accessible
- [ ] Basic documentation complete

---

## Code Patterns

### API Client Setup with Token Refresh
```typescript
// src/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import * as storage from '../utils/storage';
import * as authApi from './auth';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
});

// Queue to hold failed requests during token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor: add access token
apiClient.interceptors.request.use((config) => {
  const token = storage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 and refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Wait for token refresh to complete
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = storage.getRefreshToken();
      if (!refreshToken) {
        storage.clearTokens();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const { accessToken, refreshToken: newRefreshToken } =
          await authApi.refreshToken(refreshToken);
        storage.setTokens(accessToken, newRefreshToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        processQueue(null, accessToken);
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as AxiosError, null);
        storage.clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### Storage Utility
```typescript
// src/utils/storage.ts
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
```

### Zustand Auth Store
```typescript
// src/store/authStore.ts
import { create } from 'zustand';
import * as authApi from '../api/auth';
import * as storage from '../utils/storage';

interface User {
  id: string;
  email: string;
  name?: string;
}

interface AuthState {
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  loadUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: true,

  login: async (email: string, password: string) => {
    const { accessToken, refreshToken, user } = await authApi.login(
      email,
      password
    );
    storage.setTokens(accessToken, refreshToken);
    set({ user });
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // Logout anyway even if API call fails
    }
    storage.clearTokens();
    set({ user: null });
  },

  loadUser: async () => {
    const token = storage.getAccessToken();
    if (token) {
      try {
        const userData = await authApi.getMe();
        set({ user: userData, isLoading: false });
      } catch {
        storage.clearTokens();
        set({ user: null, isLoading: false });
      }
    } else {
      set({ isLoading: false });
    }
  },
}));
```

### Usage in ChatPage (Inline Streaming)
```typescript
// src/pages/ChatPage.tsx (excerpt)
import { useState, useRef } from 'react';
import { streamChatResponse } from '../api/chat';
import toast from 'react-hot-toast';

export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const handleSendMessage = async (text: string) => {
    // Optimistically add user message
    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: text,
      timestamp: new Date().toISOString(),
    };

    // Add placeholder for assistant message
    const assistantMessage: Message = {
      id: crypto.randomUUID(),
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage, assistantMessage]);
    setIsStreaming(true);
    abortControllerRef.current = new AbortController();

    try {
      for await (const chunk of streamChatResponse(
        sessionId,
        text,
        abortControllerRef.current.signal
      )) {
        setMessages((prev) => {
          const updated = [...prev];
          updated[updated.length - 1].content += chunk;
          return updated;
        });
      }
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        toast.error('Message cancelled');
      } else {
        toast.error('Failed to send message');
      }
    } finally {
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  };

  const handleCancel = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
  };

  return (
    <div>
      <MessageList messages={messages} />
      <MessageInput
        onSend={handleSendMessage}
        disabled={isStreaming}
        onCancel={isStreaming ? handleCancel : undefined}
      />
    </div>
  );
}
```

### Router Setup with Redirect
```typescript
// src/App.tsx
import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { Toaster } from 'react-hot-toast';
import { Spinner } from './components/ui/Spinner';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SessionListPage from './pages/SessionListPage';
import ChatPage from './pages/ChatPage';
import LanguageCatalogPage from './pages/LanguageCatalogPage';
import CourseCatalogPage from './pages/CourseCatalogPage';
import CourseDetailPage from './pages/CourseDetailPage';
import VocabularyPage from './pages/VocabularyPage';
import ProfilePage from './pages/ProfilePage';
import ErrorBoundary from './components/ui/ErrorBoundary';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
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
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />

          {/* Default to languages catalog */}
          <Route path="/" element={<Navigate to="/languages" replace />} />
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" />
    </ErrorBoundary>
  );
}

export default App;
```

### Type Definitions (All in src/types.ts)
```typescript
// src/types.ts - Single file with all types

// Enums
export enum CEFRLevel {
  A1 = 'A1',
  A2 = 'A2',
  B1 = 'B1',
  B2 = 'B2',
  C1 = 'C1',
  C2 = 'C2',
}

export enum ConversationPhase {
  FREE = 'FREE',
  CORRECTION = 'CORRECTION',
  DRILL = 'DRILL',
  AUTO = 'AUTO',
}

export enum MessageRole {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT',
  SYSTEM = 'SYSTEM',
}

export enum CourseCategory {
  GENERAL = 'GENERAL',
  BUSINESS = 'BUSINESS',
  TRAVEL = 'TRAVEL',
  ACADEMIC = 'ACADEMIC',
  EXAM_PREP = 'EXAM_PREP',
}

export enum Difficulty {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
}

export enum TutorPersonality {
  FRIENDLY = 'FRIENDLY',
  PROFESSIONAL = 'PROFESSIONAL',
  ENCOURAGING = 'ENCOURAGING',
  STRICT = 'STRICT',
}

export enum ErrorType {
  GRAMMAR = 'GRAMMAR',
  SPELLING = 'SPELLING',
  VOCABULARY = 'VOCABULARY',
  WORD_ORDER = 'WORD_ORDER',
  VERB_FORM = 'VERB_FORM',
  ARTICLE = 'ARTICLE',
  PREPOSITION = 'PREPOSITION',
  PUNCTUATION = 'PUNCTUATION',
  OTHER = 'OTHER',
}

export enum ErrorSeverity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

// Domain Models
export interface User {
  id: string;
  email: string;
  name?: string;
}

export interface Message {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  timestamp: string;
  metadata?: MessageMetadata;
}

export interface Session {
  id: string;
  userId: string;
  courseId: string;
  courseName: string;
  targetLanguageCode: string;
  userLevel: CEFRLevel;
  phase: ConversationPhase;
  currentTopic: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Correction {
  startIndex: number;
  endIndex: number;
  originalText: string;
  correctedText: string;
  errorType: ErrorType;
  severity: ErrorSeverity;
  explanation?: string;
}

export interface MessageMetadata {
  corrections: Correction[];
  phase: ConversationPhase;
}

// API Request/Response Types (add as needed throughout implementation)
// ... Add specific API types as you build each feature
```

---

## Testing Checklist

### Happy Path
- [ ] Register new user
- [ ] Login
- [ ] Browse languages
- [ ] Browse courses
- [ ] Start session from course
- [ ] Send message, see AI response stream
- [ ] See error corrections
- [ ] View vocabulary
- [ ] Logout

### Edge Cases
- [ ] Login with wrong password
- [ ] Register with existing email
- [ ] Send empty message
- [ ] Network error during API call
- [ ] Token expires (401 redirect)
- [ ] Mobile layout
- [ ] Empty states (no sessions, no vocabulary)
