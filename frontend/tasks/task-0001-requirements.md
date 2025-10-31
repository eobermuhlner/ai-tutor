# Task 0001: AI Tutor Frontend - Modern UI Design Requirements

## Overview
Design and implement a modern, intuitive web interface for the AI Tutor language learning platform. The frontend connects to a Spring Boot REST API backend with conversational AI tutoring and vocabulary tracking.

## User Personas

### Primary Users
- **Beginner Learner** (A1-A2 CEFR): Needs simple, guided experience with clear visual feedback
- **Intermediate Learner** (B1-B2 CEFR): Wants structured learning with error correction and progress tracking
- **Advanced Learner** (C1-C2 CEFR): Seeks natural conversation practice with subtle corrections

## Core User Flows

### 1. Authentication Flow
- **Entry Point**: Landing page with clear value proposition
- **Register**: Simple form with email, password, and optional profile setup
- **Login**: Email/password with "Remember me" and password reset options
- **User Profile**: View and edit profile, manage language proficiencies

### 2. Language & Course Selection Flow
- **Browse Languages**: Visual catalog of available target languages
- **Select Proficiency**: Choose CEFR level (A1-C2) for each language
- **Browse Courses**: Filter by language, level, category, difficulty
- **Course Details**: View course description, topics, tutor personality
- **Browse Tutors**: Alternative path to select tutor personality directly

### 3. Learning Session Flow
- **Create Session**: From course template or custom tutor selection
- **Chat Interface**: Real-time conversation with AI tutor
- **Phase Awareness**: Visual indicator of current phase (Free/Correction/Drill/Auto)
- **Error Display**: Hover over corrected text to see error details and severity
- **Topic Tracking**: Current topic displayed, with topic history
- **Phase Control**: Manual override to switch conversation phases
- **Session Management**: View active sessions, resume or delete sessions

### 4. Vocabulary Review Flow
- **Vocabulary List**: View all saved vocabulary items
- **Filter by Language**: Filter vocabulary by target language
- **Item Details**: Click to see all contexts where word appeared
- **Exposure Count**: Track how many times learner has encountered each word

## Feature Requirements

### 1. Authentication & User Management

#### Landing Page
- Clean, modern hero section explaining AI Tutor value proposition
- Clear CTAs for "Get Started" and "Sign In"
- Brief feature highlights (conversational AI, adaptive learning, vocabulary tracking)
- Responsive design for mobile/tablet/desktop

#### Registration Screen
- Email and password fields with validation
- Optional: display name, preferred UI language
- Password strength indicator
- Terms of service agreement
- "Already have an account?" link to login

#### Login Screen
- Email and password fields
- "Remember me" checkbox
- "Forgot password?" link
- "Create account" link to registration
- JWT token management (access + refresh tokens)

#### User Profile
- Display current user info (email, name)
- List of user's language proficiencies with CEFR levels
- Add/edit/remove language proficiencies
- Set primary learning language
- Change password functionality
- Logout button

### 2. Language & Course Catalog

#### Language Selection
- Visual grid/cards of available languages
- Flag icons and language names (in user's UI language)
- Filter by source language (e.g., "as an English speaker")
- Click to browse courses for that language

#### Course Browsing
- Card-based layout with course thumbnails
- Display: course name, category, difficulty, brief description
- Filter controls:
  - Source language dropdown
  - User level (A1-C2)
  - Category tags (Business, Travel, Academic, etc.)
  - Difficulty (Beginner, Intermediate, Advanced)
- Search bar for course names
- Sort options (newest, popularity, level)

#### Course Details Page
- Full course description
- Target language and recommended level
- List of topics covered
- Tutor personality preview
- "Start Learning" CTA button
- Back to catalog link

#### Tutor Selection (Alternative Flow)
- Similar card layout for tutor personalities
- Display: tutor name, personality traits, teaching style
- Filter by target language
- "Start Session with [Tutor]" button

### 3. Chat Interface (Core Feature)

#### Session Dashboard
- List of active learning sessions
- Display: course name, language, last activity timestamp
- Session progress indicators (messages count, topics covered)
- "Continue" and "Delete" actions
- "Start New Session" button

#### Chat Screen Layout
```
┌─────────────────────────────────────────────┐
│ Header: Course Name | Phase | Topic         │
├─────────────────────────────────────────────┤
│                                              │
│  Chat Messages Area                          │
│  - User messages (right-aligned)             │
│  - Tutor messages (left-aligned)             │
│  - Error corrections (hover tooltips)        │
│  - Timestamps                                │
│                                              │
│  [Streaming indicator for AI responses]      │
│                                              │
├─────────────────────────────────────────────┤
│ Input: [Text input] [Send button]            │
└─────────────────────────────────────────────┘
```

#### Phase Indicator
- Visual badge/pill showing current phase:
  - **Free**: Green - "Free Conversation" (no error tracking)
  - **Correction**: Blue - "Correction Mode" (errors tracked, shown on hover)
  - **Drill**: Orange - "Practice Mode" (explicit error discussion)
  - **Auto**: Purple - "Adaptive Mode" (automatic phase selection)
- Dropdown or buttons to manually switch phases
- Tooltip explaining what each phase means

#### Topic Display
- Current topic shown prominently (e.g., "Topic: Travel Planning")
- Option to suggest new topic or let tutor choose
- Topic history accessible via expandable panel

#### Error Correction Display
- User messages containing errors have subtle indicator (underline, color)
- Hover over corrected text reveals tooltip:
  - Original text
  - Corrected text
  - Error type (grammar, spelling, vocabulary, etc.)
  - Severity indicator (color-coded: Critical, High, Medium, Low)
  - Brief explanation (optional)
- Corrections only visible in Correction/Drill modes, hidden in Free mode

#### Message Streaming
- Real-time streaming of AI responses using SSE
- Typing indicator while AI is generating response
- Smooth text animation as response streams in

#### Session Controls
- Back to dashboard button
- Session menu with:
  - Change phase
  - Change topic
  - View progress
  - End session
  - Delete session

### 4. Session Progress

#### Progress View
- Total messages exchanged
- Topics covered (list with turn counts)
- Errors by type (chart or list)
- Vocabulary learned (count)
- Time spent in each phase
- Link to vocabulary from this session

### 5. Vocabulary Management

#### Vocabulary List
- Table or card view of all vocabulary items
- Columns/fields:
  - Target word/phrase
  - Translation (if available)
  - Language
  - Exposure count (how many times seen)
  - First seen date
- Filter by language
- Sort by exposure count, date added, alphabetical
- Search functionality

#### Vocabulary Item Details
- Click item to see modal/detail page
- Display all contexts where word appeared:
  - Original sentence
  - Corrected sentence (if applicable)
  - Session reference
  - Timestamp
- Translation and example usage
- Option to mark as "mastered" or add notes (future feature)

## UI Design Principles

### Visual Design
- **Modern & Clean**: Minimalist design with ample whitespace
- **Color Palette**:
  - Primary: Professional blue/teal for trust and learning
  - Secondary: Warm accent color for CTAs and highlights
  - Semantic colors: Green (success), Orange (warning), Red (error)
  - Phase colors: Distinct but harmonious for each conversation phase
- **Typography**: Clear, readable sans-serif fonts (system fonts or Google Fonts)
- **Icons**: Consistent icon set (e.g., Heroicons, Lucide React)

### Responsive Design
- Mobile-first approach
- Breakpoints for mobile (< 640px), tablet (640-1024px), desktop (> 1024px)
- Chat interface optimized for mobile typing
- Collapsible sidebars/menus on mobile

### Accessibility
- WCAG 2.1 AA compliance
- Semantic HTML
- Keyboard navigation support
- ARIA labels and roles
- Color contrast ratios meet standards
- Screen reader friendly

### User Experience
- **Intuitive Navigation**: Clear hierarchy, breadcrumbs where appropriate
- **Feedback**: Loading states, success/error messages, confirmation dialogs
- **Performance**: Fast page loads, optimistic UI updates
- **Onboarding**: First-time user guidance (tooltips, walkthroughs)
- **Error Handling**: Graceful error messages with recovery suggestions

## Technical Constraints

### Frontend Stack
- **Framework**: React 19.1.1
- **Language**: TypeScript 5.9.3
- **Build Tool**: Vite 5.4.20
- **Styling**: Tailwind CSS 3.4.18
- **State Management**: React Context API + hooks (or Zustand/Redux if needed)
- **HTTP Client**: Fetch API or Axios
- **Routing**: React Router v6
- **Forms**: React Hook Form + Zod validation
- **Real-time**: EventSource API for SSE streaming

### Backend Integration

#### REST API Base URL
- Development: `http://localhost:8080/api/v1`
- Production: TBD

#### Key Endpoints

**Authentication** (`/api/v1/auth/*`)
- POST `/register` - Register new user
- POST `/login` - Login, receive JWT tokens
- POST `/refresh` - Refresh access token
- POST `/logout` - Logout
- GET `/me` - Get current user
- POST `/password` - Change password

**Catalog** (`/api/v1/catalog/*`)
- GET `/languages?sourceLanguage={lang}` - List languages
- GET `/languages/{code}/courses?sourceLanguage={lang}&userLevel={level}` - List courses
- GET `/languages/{code}/tutors?sourceLanguage={lang}` - List tutors
- GET `/courses/{id}?sourceLanguage={lang}` - Course details
- GET `/tutors/{id}?sourceLanguage={lang}` - Tutor details

**Chat** (`/api/v1/chat/*`)
- POST `/sessions/from-course` - Create session from course
- GET `/sessions/active?userId={id}` - Get active sessions with progress
- GET `/sessions?userId={id}` - List sessions
- GET `/sessions/{id}` - Get session with messages
- GET `/sessions/{id}/progress` - Session progress
- PATCH `/sessions/{id}/phase` - Update phase
- PATCH `/sessions/{id}/topic` - Update topic
- GET `/sessions/{id}/topics/history` - Topic history
- POST `/sessions/{id}/messages/stream` - Send message with SSE streaming
- DELETE `/sessions/{id}` - Delete session

**User Languages** (`/api/v1/users/{userId}/languages/*`)
- GET `/` - Get language proficiencies
- POST `/` - Add language proficiency
- PATCH `/{languageCode}` - Update proficiency level
- PATCH `/{languageCode}/set-primary` - Set primary language
- DELETE `/{languageCode}` - Remove proficiency

**Vocabulary** (`/api/v1/vocabulary/*`)
- GET `/?userId={id}&lang={lang}` - Get vocabulary items
- GET `/{itemId}` - Get item with all contexts

#### Authentication
- JWT-based with access token (short-lived) and refresh token (long-lived)
- Include `Authorization: Bearer {accessToken}` header in requests
- Implement token refresh logic before access token expires
- Handle 401 responses (redirect to login)

#### Error Handling
- Handle HTTP status codes appropriately
- Display user-friendly error messages
- Log errors for debugging
- Retry logic for network failures

## Performance Requirements
- Initial page load < 2 seconds
- Time to interactive < 3 seconds
- Smooth 60fps animations
- Lazy load images and non-critical components
- Code splitting for route-based chunks
- Optimize bundle size (< 500KB initial load)

## Security Requirements
- Store JWT tokens securely (httpOnly cookies or secure localStorage)
- Sanitize user input to prevent XSS
- HTTPS only in production
- CORS properly configured
- No sensitive data in URL parameters
- Implement CSRF protection if using cookies

## Future Enhancements (Out of Scope for v1)
- Speech recognition for voice input
- Text-to-speech for AI responses
- Gamification (badges, streaks, leaderboards)
- Social features (share progress, compete with friends)
- Offline mode for reviewing vocabulary
- Mobile native apps (React Native)
- Advanced analytics dashboard
- Spaced repetition for vocabulary review
- Custom vocabulary flashcards

## Success Metrics
- User engagement: session duration, messages per session
- Learning outcomes: vocabulary retention, error reduction over time
- User satisfaction: NPS, app store ratings
- Technical performance: page load times, error rates

## Deliverables
1. **Design mockups** (Figma/Sketch) for key screens
2. **Component library** (reusable UI components)
3. **Responsive web application** (mobile, tablet, desktop)
4. **Integration with backend API** (all endpoints functional)
5. **Documentation** (setup, deployment, API integration guide)
6. **Test coverage** (unit tests for critical components)
