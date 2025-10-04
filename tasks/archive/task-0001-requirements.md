# Task 0001: User Management & Authentication Requirements

**Status:** Requirements Definition
**Priority:** High
**Created:** 2025-10-02
**Author:** Eric

## Executive Summary

Add comprehensive user management and authentication system to AI Tutor, transforming it from a UUID-based multi-user system to a fully authenticated application with credential-based login, role-based access control, and self-service onboarding.

## Background

**Current State:**
- Users identified by UUID (`userId` parameter in requests)
- No authentication or authorization
- Any client can access any user's data by providing their UUID
- No user profile management
- ChatSessionEntity and VocabularyItemEntity reference `userId: UUID` directly

**Problem:**
- Security: No protection of user data
- Trust: No verification of user identity
- Management: No admin capabilities to manage users
- Onboarding: Manual UUID generation required

## Goals

1. **Security**: Protect user data with authentication and authorization
2. **User Experience**: Enable self-service registration and login
3. **Scalability**: Prepare for OAuth2 social login (Google, etc.)
4. **Administration**: Provide admin tools for user management
5. **Backward Compatibility**: Minimize disruption to existing functionality

---

## Functional Requirements

### FR-1: User Entity & Data Model

**FR-1.1: User Entity**
- **Fields:**
  - `id: UUID` (primary key, auto-generated)
  - `username: String` (unique, 3-32 chars, alphanumeric + underscore/dash)
  - `email: String` (unique, valid email format)
  - `passwordHash: String` (bcrypt/argon2 hashed, nullable for OAuth2-only users)
  - `firstName: String?` (optional, max 64 chars)
  - `lastName: String?` (optional, max 64 chars)
  - `roles: Set<UserRole>` (USER, ADMIN - stored as join table or JSON)
  - `enabled: Boolean` (default: true, admin can disable accounts)
  - `locked: Boolean` (default: false, security lockout)
  - `accountExpired: Boolean` (default: false, future use)
  - `credentialsExpired: Boolean` (default: false, password expiry)
  - `emailVerified: Boolean` (default: false, future email verification)
  - `provider: AuthProvider` (CREDENTIALS, GOOGLE, GITHUB, etc.)
  - `providerId: String?` (external OAuth2 user ID)
  - `createdAt: Instant` (timestamp)
  - `updatedAt: Instant` (timestamp)
  - `lastLoginAt: Instant?` (optional, track last login)

**FR-1.2: UserRole Enum**
```kotlin
enum class UserRole {
    USER,   // Normal user - can access own data
    ADMIN   // Administrator - can manage users
}
```

**FR-1.3: AuthProvider Enum**
```kotlin
enum class AuthProvider {
    CREDENTIALS,  // Username/password
    GOOGLE,       // Google OAuth2 (future)
    GITHUB,       // GitHub OAuth2 (future)
    FACEBOOK,     // Facebook OAuth2 (future)
}
```

### FR-2: Authentication

**FR-2.1: Credential-Based Login**
- **Endpoint:** `POST /api/v1/auth/login`
- **Request:**
  ```json
  {
    "username": "string",  // or email
    "password": "string"
  }
  ```
- **Response:**
  ```json
  {
    "accessToken": "string",  // JWT token
    "refreshToken": "string", // Optional refresh token
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "firstName": "string",
      "lastName": "string",
      "roles": ["USER"]
    }
  }
  ```
- **Authentication:** Allow login with either username OR email
- **Password Validation:** Verify bcrypt/argon2 hash
- **Token Generation:** Issue JWT with user ID, username, roles
- **Error Handling:**
  - 401 Unauthorized for invalid credentials
  - 403 Forbidden for disabled/locked accounts
  - Rate limiting on login attempts (future)

**FR-2.2: Token Refresh**
- **Endpoint:** `POST /api/v1/auth/refresh`
- **Request:**
  ```json
  {
    "refreshToken": "string"
  }
  ```
- **Response:** Same as login response with new tokens

**FR-2.3: Logout**
- **Endpoint:** `POST /api/v1/auth/logout`
- **Authentication:** Required (Bearer token)
- **Behavior:**
  - Invalidate refresh token (if stored server-side)
  - Client discards access token
  - Return 204 No Content

**FR-2.4: Current User Profile**
- **Endpoint:** `GET /api/v1/auth/me`
- **Authentication:** Required
- **Response:** User profile of authenticated user

**FR-2.5: Password Change**
- **Endpoint:** `POST /api/v1/auth/password`
- **Authentication:** Required
- **Request:**
  ```json
  {
    "currentPassword": "string",
    "newPassword": "string"
  }
  ```
- **Validation:**
  - Verify current password
  - Validate new password strength (min 8 chars, complexity rules)
  - Hash new password with bcrypt/argon2
- **Response:** 204 No Content on success

### FR-3: Self-Service Registration

**FR-3.1: User Registration**
- **Endpoint:** `POST /api/v1/auth/register`
- **Authentication:** None (public endpoint)
- **Request:**
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string",
    "firstName": "string",  // optional
    "lastName": "string"    // optional
  }
  ```
- **Response:**
  ```json
  {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "message": "Registration successful. Please verify your email." // future
  }
  ```
- **Validation:**
  - Username: unique, 3-32 chars, alphanumeric + underscore/dash, not reserved words
  - Email: unique, valid format, not disposable domains (future)
  - Password: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit
  - Hash password with bcrypt (cost factor 12) or argon2
- **Default Values:**
  - `roles = [USER]`
  - `enabled = true`
  - `emailVerified = false`
  - `provider = CREDENTIALS`
- **Error Handling:**
  - 400 Bad Request for validation failures
  - 409 Conflict for duplicate username/email

**FR-3.2: Email Verification (Future Phase)**
- Send verification email with token
- Endpoint: `GET /api/v1/auth/verify?token={token}`
- Set `emailVerified = true` on success

### FR-4: Authorization & Access Control

**FR-4.1: Role-Based Access Control (RBAC)**

| Resource | Endpoint Pattern | USER | ADMIN |
|----------|-----------------|------|-------|
| Own Sessions | GET/POST/DELETE `/api/v1/chat/sessions?userId={self}` | ✓ | ✓ |
| Other's Sessions | GET/POST/DELETE `/api/v1/chat/sessions?userId={other}` | ✗ | ✓ |
| Own Vocabulary | GET `/api/v1/vocabulary?userId={self}` | ✓ | ✓ |
| Other's Vocabulary | GET `/api/v1/vocabulary?userId={other}` | ✗ | ✓ |
| User Management | `/api/v1/users/**` | ✗ | ✓ |
| User Registration | POST `/api/v1/auth/register` | ✓ (public) | ✓ |
| Login | POST `/api/v1/auth/login` | ✓ (public) | ✓ |

**FR-4.2: Data Filtering**
- **Automatic User Context Injection:**
  - Controllers automatically inject authenticated user's ID
  - Services enforce data isolation by user ID
  - Repository queries include user ID filter
- **Example:**
  ```kotlin
  // Before (insecure):
  fun getUserSessions(@RequestParam userId: UUID)

  // After (secure):
  fun getUserSessions(@AuthenticationPrincipal user: UserDetails)
  // or keep parameter but validate it matches authenticated user
  ```

**FR-4.3: Admin Capabilities**
- View all users' sessions and vocabulary
- Create/update/delete users
- Change user roles
- Enable/disable accounts

### FR-5: User Management (Admin)

**FR-5.1: List Users**
- **Endpoint:** `GET /api/v1/users`
- **Authentication:** ADMIN role required
- **Query Parameters:**
  - `page: Int` (default: 0)
  - `size: Int` (default: 20, max: 100)
  - `sort: String` (default: "createdAt,desc")
  - `search: String?` (search username/email)
  - `role: UserRole?` (filter by role)
  - `enabled: Boolean?` (filter by enabled status)
- **Response:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "username": "string",
        "email": "string",
        "firstName": "string",
        "lastName": "string",
        "roles": ["USER"],
        "enabled": true,
        "locked": false,
        "emailVerified": false,
        "provider": "CREDENTIALS",
        "createdAt": "2025-01-01T00:00:00Z",
        "lastLoginAt": "2025-01-02T12:00:00Z"
      }
    ],
    "pageable": {...},
    "totalElements": 100,
    "totalPages": 5
  }
  ```

**FR-5.2: Get User**
- **Endpoint:** `GET /api/v1/users/{userId}`
- **Authentication:** ADMIN role required
- **Response:** Full user details (exclude passwordHash)

**FR-5.3: Create User (Admin)**
- **Endpoint:** `POST /api/v1/users`
- **Authentication:** ADMIN role required
- **Request:**
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string",  // optional, can be null for OAuth2-only users
    "firstName": "string",
    "lastName": "string",
    "roles": ["USER", "ADMIN"],
    "enabled": true
  }
  ```
- **Response:** Created user details

**FR-5.4: Update User**
- **Endpoint:** `PUT /api/v1/users/{userId}`
- **Authentication:** ADMIN role required
- **Request:** Same as create (all fields optional)
- **Capabilities:**
  - Update user profile fields
  - Change roles (grant/revoke ADMIN)
  - Enable/disable account
  - Lock/unlock account
  - Reset password (admin sets temporary password)

**FR-5.5: Delete User**
- **Endpoint:** `DELETE /api/v1/users/{userId}`
- **Authentication:** ADMIN role required
- **Behavior:**
  - Soft delete (set `enabled = false, deletedAt = now()`) OR
  - Hard delete (cascade delete sessions, vocabulary, messages)
- **Protection:** Cannot delete own admin account
- **Response:** 204 No Content

**FR-5.6: Change User Roles**
- **Endpoint:** `PATCH /api/v1/users/{userId}/roles`
- **Authentication:** ADMIN role required
- **Request:**
  ```json
  {
    "roles": ["USER", "ADMIN"]
  }
  ```
- **Protection:** Cannot revoke own ADMIN role

### FR-6: Default Admin User

**FR-6.1: Bootstrap Admin Account**
- **Username:** `admin`
- **Email:** `admin@localhost` (or configured via env var)
- **Password:**
  - Option A: Configured via environment variable `ADMIN_PASSWORD`
  - Option B: Generated random password printed to logs on first startup
  - Option C: Default password `admin123!` (force change on first login)
- **Roles:** `[ADMIN, USER]`
- **Creation:**
  - Database initialization (Liquibase/Flyway migration) OR
  - `ApplicationRunner` bean on startup (check if exists, create if not)

**FR-6.2: Admin Password Reset**
- **CLI Command:** `./gradlew resetAdminPassword` (future)
- **Environment Override:** Restart with `ADMIN_PASSWORD` env var to reset

### FR-7: OAuth2 Social Login (Future Phase)

**FR-7.1: Google OAuth2**
- **Endpoint:** `GET /api/v1/auth/oauth2/google`
- **Flow:**
  1. Redirect to Google OAuth2 consent screen
  2. Google redirects back with authorization code
  3. Backend exchanges code for access token
  4. Fetch user profile from Google
  5. Create user if not exists (email as username, `provider = GOOGLE`)
  6. Issue JWT token
- **User Mapping:**
  - `email` from Google profile (primary identifier)
  - `firstName`, `lastName` from profile
  - `providerId` = Google user ID
  - `passwordHash = null` (OAuth2-only user)
  - `emailVerified = true` (Google verifies email)

**FR-7.2: Other Providers**
- GitHub, Facebook, Microsoft, etc.
- Same flow as Google
- Support multiple providers per user (future: link accounts)

---

## Technical Requirements

### TR-1: Spring Security Configuration

**TR-1.1: Dependencies**
- Add to `build.gradle`:
  ```gradle
  implementation 'org.springframework.boot:spring-boot-starter-security'
  implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
  runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
  runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
  ```

**TR-1.2: Security Configuration**
- **Class:** `SecurityConfig.kt`
- **Configuration:**
  ```kotlin
  @Configuration
  @EnableWebSecurity
  @EnableMethodSecurity
  class SecurityConfig {
      @Bean
      fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
          http {
              csrf { disable() }  // Disable for stateless JWT API
              cors { }            // Configure CORS as needed
              authorizeHttpRequests {
                  // Public endpoints
                  authorize("/api/v1/auth/register", permitAll)
                  authorize("/api/v1/auth/login", permitAll)
                  authorize("/api/v1/auth/refresh", permitAll)
                  authorize("/h2-console/**", permitAll)  // Dev only

                  // Admin endpoints
                  authorize("/api/v1/users/**", hasRole("ADMIN"))

                  // Authenticated endpoints
                  authorize("/api/v1/**", authenticated)

                  // Deny all others
                  authorize(anyRequest, denyAll)
              }
              sessionManagement {
                  sessionCreationPolicy = SessionCreationPolicy.STATELESS
              }
              addFilterBefore<UsernamePasswordAuthenticationFilter>(
                  jwtAuthenticationFilter
              )
          }
          return http.build()
      }
  }
  ```

**TR-1.3: JWT Configuration**
- **Class:** `JwtProperties.kt`
  ```kotlin
  @ConfigurationProperties("jwt")
  data class JwtProperties(
      val secret: String,
      val expirationMs: Long = 3600000,      // 1 hour
      val refreshExpirationMs: Long = 2592000000  // 30 days
  )
  ```
- **application.yml:**
  ```yaml
  jwt:
    secret: ${JWT_SECRET:changeme-generate-secure-random-key-min-256-bits}
    expiration-ms: 3600000
    refresh-expiration-ms: 2592000000
  ```

**TR-1.4: JWT Token Service**
- **Class:** `JwtTokenService.kt`
- **Methods:**
  - `generateAccessToken(user: UserEntity): String`
  - `generateRefreshToken(user: UserEntity): String`
  - `validateToken(token: String): Boolean`
  - `getUserIdFromToken(token: String): UUID`
  - `getUsernameFromToken(token: String): String`
  - `getRolesFromToken(token: String): List<UserRole>`

**TR-1.5: JWT Authentication Filter**
- **Class:** `JwtAuthenticationFilter.kt`
- **Responsibility:**
  - Extract JWT from `Authorization: Bearer {token}` header
  - Validate token signature and expiration
  - Load user details from token claims (avoid DB lookup per request)
  - Set authentication in SecurityContext

**TR-1.6: Password Encoder**
- **Bean:** `BCryptPasswordEncoder(12)` or `Argon2PasswordEncoder`
- **Usage:** Hash passwords on registration/change, verify on login

**TR-1.7: UserDetailsService**
- **Class:** `CustomUserDetailsService.kt`
- **Implementation:**
  ```kotlin
  @Service
  class CustomUserDetailsService(
      private val userRepository: UserRepository
  ) : UserDetailsService {
      override fun loadUserByUsername(username: String): UserDetails {
          val user = userRepository.findByUsername(username)
              ?: userRepository.findByEmail(username)
              ?: throw UsernameNotFoundException("User not found: $username")

          return CustomUserDetails(user)
      }
  }

  class CustomUserDetails(private val user: UserEntity) : UserDetails {
      override fun getAuthorities() = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
      override fun getPassword() = user.passwordHash
      override fun getUsername() = user.username
      override fun isAccountNonExpired() = !user.accountExpired
      override fun isAccountNonLocked() = !user.locked
      override fun isCredentialsNonExpired() = !user.credentialsExpired
      override fun isEnabled() = user.enabled
  }
  ```

### TR-2: Database Schema

**TR-2.1: User Table**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(32) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),  -- Nullable for OAuth2-only users
    first_name VARCHAR(64),
    last_name VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider VARCHAR(32) NOT NULL DEFAULT 'CREDENTIALS',
    provider_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP  -- Soft delete
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_id ON users(provider, provider_id);
```

**TR-2.2: User Roles Table**
```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
```

**TR-2.3: Refresh Tokens Table (Optional)**
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user_id ON user_roles(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

**TR-2.4: Migration Strategy**
- **Existing Data:**
  - `chat_sessions.user_id` remains UUID (foreign key to `users.id`)
  - `vocabulary_items.user_id` remains UUID (foreign key to `users.id`)
- **Migration:**
  - Option A: Require manual user creation for existing UUIDs
  - Option B: Auto-create "guest" users with existing UUIDs (username = `guest_{uuid}`)
- **Foreign Keys:**
  - Add foreign key constraints after migration (or make nullable temporarily)

### TR-3: Package Structure

**New Packages:**
```
ch.obermuhlner.aitutor
├── auth/                      # Authentication & authorization
│   ├── controller/            # AuthController (/api/v1/auth)
│   ├── service/               # AuthService, JwtTokenService
│   ├── filter/                # JwtAuthenticationFilter
│   ├── config/                # SecurityConfig, JwtProperties
│   ├── domain/                # (reuse user/ domain)
│   └── dto/                   # LoginRequest, LoginResponse, RegisterRequest,
│                              # RefreshTokenRequest, ChangePasswordRequest
├── user/                      # User management
│   ├── controller/            # UserController (/api/v1/users)
│   ├── service/               # UserService, CustomUserDetailsService
│   ├── repository/            # UserRepository, RefreshTokenRepository
│   ├── domain/                # UserEntity, RefreshTokenEntity, UserRole, AuthProvider
│   └── dto/                   # UserResponse, CreateUserRequest, UpdateUserRequest,
│                              # UserPageResponse
└── (existing packages unchanged)
```

### TR-4: Testing Requirements

**TR-4.1: Unit Tests**
- `JwtTokenServiceTest` - token generation, validation, claims extraction
- `AuthServiceTest` - login, registration, password change logic
- `UserServiceTest` - user CRUD operations, role management
- `CustomUserDetailsServiceTest` - load user by username/email

**TR-4.2: Integration Tests**
- `AuthControllerTest` - login, register, refresh endpoints
- `UserControllerTest` - admin user management endpoints
- `SecurityConfigTest` - endpoint access control rules

**TR-4.3: Security Tests**
- Verify unauthorized access returns 401
- Verify forbidden access returns 403
- Verify users cannot access other users' data
- Verify ADMIN can access all data
- Verify password hashing (never stored plaintext)
- Verify JWT expiration enforcement

**TR-4.4: Test Users**
- Create `TestDataFactory` fixtures for users:
  - Test user: `testuser` / `test@example.com` / `password123`
  - Test admin: `testadmin` / `admin@example.com` / `admin123`

**TR-4.5: Test Configuration**
- Mock JWT secret in test profile
- Use in-memory H2 database
- Pre-populate test users via `@Sql` scripts or `@BeforeEach`

### TR-5: Error Handling

**TR-5.1: Custom Exceptions**
- `UserNotFoundException`
- `DuplicateUsernameException`
- `DuplicateEmailException`
- `InvalidCredentialsException`
- `InvalidTokenException`
- `ExpiredTokenException`
- `InsufficientPermissionsException`

**TR-5.2: Global Exception Handler**
- `@RestControllerAdvice` class `AuthExceptionHandler`
- Map exceptions to appropriate HTTP status codes:
  - `UserNotFoundException` → 404
  - `DuplicateUsernameException` → 409
  - `InvalidCredentialsException` → 401
  - `InvalidTokenException` → 401
  - `InsufficientPermissionsException` → 403

**TR-5.3: Error Response Format**
```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/v1/auth/login"
}
```

### TR-6: Configuration

**TR-6.1: Environment Variables**
- `JWT_SECRET` - JWT signing secret (min 256 bits, base64 encoded)
- `JWT_EXPIRATION_MS` - Access token expiration (default: 3600000 = 1 hour)
- `JWT_REFRESH_EXPIRATION_MS` - Refresh token expiration (default: 2592000000 = 30 days)
- `ADMIN_USERNAME` - Default admin username (default: `admin`)
- `ADMIN_PASSWORD` - Default admin password (required on first startup)
- `ADMIN_EMAIL` - Default admin email (default: `admin@localhost`)

**TR-6.2: Application Properties**
```yaml
jwt:
  secret: ${JWT_SECRET:changeme}
  expiration-ms: ${JWT_EXPIRATION_MS:3600000}
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:2592000000}

app:
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:}  # Must be set
    email: ${ADMIN_EMAIL:admin@localhost}
```

---

## Security Considerations

### SEC-1: Password Security
- **Hashing:** Use bcrypt (cost 12) or Argon2id
- **Validation:** Minimum 8 characters, complexity rules
- **Storage:** Never store plaintext, never log passwords
- **Transmission:** Only accept passwords over HTTPS

### SEC-2: JWT Security
- **Secret:** Min 256 bits, cryptographically random, stored securely
- **Algorithm:** HS256 (HMAC-SHA256) or RS256 (RSA)
- **Claims:** Include `sub` (user ID), `iat`, `exp`, `roles`
- **Expiration:** Short-lived access tokens (1 hour), refresh tokens (30 days)
- **Revocation:** Store refresh tokens in database for revocation

### SEC-3: CORS Configuration
- Configure allowed origins for production
- Restrict in development to known frontend URLs
- Allow credentials for cookie-based auth (if used)

### SEC-4: Rate Limiting (Future)
- Limit login attempts (5 per 15 min per IP)
- Limit registration (10 per hour per IP)
- Lock account after repeated failed logins

### SEC-5: Input Validation
- Sanitize all user inputs
- Validate username format (no SQL injection, XSS)
- Validate email format
- Check for common password patterns (e.g., "password", "123456")

### SEC-6: Audit Logging (Future)
- Log authentication events (login, logout, failed attempts)
- Log authorization failures
- Log admin actions (user creation, role changes)
- Store logs securely, comply with GDPR/privacy laws

---

## API Endpoints Summary

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | None | Register new user (self-service) |
| POST | `/login` | None | Login with username/email + password |
| POST | `/logout` | User | Logout (invalidate refresh token) |
| POST | `/refresh` | None | Refresh access token |
| GET | `/me` | User | Get current user profile |
| POST | `/password` | User | Change own password |

### User Management (`/api/v1/users`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | Admin | List all users (paginated, searchable) |
| GET | `/{userId}` | Admin | Get user by ID |
| POST | `/` | Admin | Create new user |
| PUT | `/{userId}` | Admin | Update user |
| DELETE | `/{userId}` | Admin | Delete user |
| PATCH | `/{userId}/roles` | Admin | Change user roles |

### Modified Endpoints

**Chat Sessions (`/api/v1/chat`)**
- Remove `?userId={uuid}` query parameter
- Inject authenticated user ID from SecurityContext
- Admin can still query by userId with elevated permissions

**Vocabulary (`/api/v1/vocabulary`)**
- Remove `?userId={uuid}` query parameter
- Inject authenticated user ID from SecurityContext
- Admin can still query by userId with elevated permissions

---

## Implementation Phases

### Phase 1: Core Authentication (Milestone 1)
**Goal:** Basic login/register with JWT

**Tasks:**
1. Add Spring Security dependencies
2. Create User entity, repository, service
3. Create UserRole enum, RefreshToken entity (optional)
4. Implement BCrypt password encoder
5. Create JwtTokenService (generate, validate)
6. Create JwtAuthenticationFilter
7. Configure SecurityConfig (JWT, CORS, CSRF)
8. Create AuthController (login, register, refresh, me)
9. Create CustomUserDetailsService
10. Database migration (user table, roles table)
11. Unit tests for JwtTokenService, AuthService
12. Integration tests for AuthController
13. Update application.yml with JWT config
14. Create default admin user on startup
15. Documentation update (README, CLAUDE.md, HTTP tests)

**Acceptance Criteria:**
- Users can register with username/email/password
- Users can login and receive JWT token
- JWT tokens expire after configured time
- Refresh tokens work
- Admin user exists with correct roles
- All tests pass
- Postman/HTTP tests demonstrate functionality

### Phase 2: Authorization & Data Isolation (Milestone 2)
**Goal:** Protect user data, enforce RBAC

**Tasks:**
1. Update ChatController to use authenticated user ID
2. Update VocabularyController to use authenticated user ID
3. Add method security annotations (`@PreAuthorize`)
4. Create authorization service for admin overrides
5. Update ChatService to validate user ownership
6. Update VocabularyService to validate user ownership
7. Add foreign key constraints (users → sessions, vocabulary)
8. Create admin user management endpoints (UserController)
9. Update tests to authenticate requests
10. Security tests for access control
11. Update HTTP test files with authentication headers
12. Documentation update

**Acceptance Criteria:**
- Users can only access their own sessions/vocabulary
- Admin can access all users' data
- Unauthorized requests return 401
- Forbidden requests return 403
- All existing functionality works with authentication
- All tests pass

### Phase 3: User Management (Milestone 3)
**Goal:** Admin tools for user management

**Tasks:**
1. Create UserController (CRUD endpoints)
2. Implement pagination, search, filtering
3. Add user enable/disable functionality
4. Add role management (grant/revoke ADMIN)
5. Add password reset (admin sets temporary password)
6. Protect against admin self-deletion
7. Unit tests for UserService
8. Integration tests for UserController
9. Documentation update

**Acceptance Criteria:**
- Admin can list, create, update, delete users
- Admin can change user roles
- Admin can enable/disable accounts
- Admin cannot delete own account
- Pagination and search work correctly
- All tests pass

### Phase 4: OAuth2 Social Login (Future)
**Goal:** Google/GitHub/etc. login

**Tasks:**
1. Add Spring Security OAuth2 dependencies
2. Configure OAuth2 client properties
3. Create OAuth2 success handler
4. Map OAuth2 user to local user
5. Support multiple providers per user
6. Link/unlink accounts
7. Tests for OAuth2 flow
8. Documentation update

**Acceptance Criteria:**
- Users can login with Google
- OAuth2 users created automatically
- Email verified for OAuth2 users
- Multiple providers supported

---

## Migration Strategy

### Existing Data
**Problem:** Existing `chat_sessions` and `vocabulary_items` tables have `user_id` UUID columns, but no corresponding `users` table.

**Options:**

**Option A: Require Manual User Creation**
- Deploy new version with user management
- Admin manually creates users with known UUIDs
- Existing data remains valid (foreign keys enforced)
- **Pros:** Clean, explicit
- **Cons:** Manual work, downtime for data association

**Option B: Auto-Create Guest Users**
- Migration script finds all distinct `user_id` UUIDs
- Creates "guest" user for each:
  ```sql
  INSERT INTO users (id, username, email, password_hash, roles, enabled, provider)
  VALUES (
    '{existing-uuid}',
    'guest_{uuid}',
    'guest_{uuid}@localhost',
    NULL,  -- No password, admin must reset
    '["USER"]',
    TRUE,
    'CREDENTIALS'
  );
  ```
- Admin later updates guest users with real details
- **Pros:** No data loss, seamless migration
- **Cons:** Placeholder accounts need cleanup

**Option C: Temporary Nullable Foreign Keys**
- Add `users` table without foreign key constraints
- Allow `chat_sessions.user_id` to reference non-existent users temporarily
- Admin creates users over time
- Add foreign key constraints later
- **Pros:** Gradual migration
- **Cons:** Data integrity risk, complexity

**Recommendation:** **Option B (Auto-Create Guest Users)** with admin notification to update accounts.

### Migration Script (Liquibase/Flyway)
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(32) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    first_name VARCHAR(64),
    last_name VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider VARCHAR(32) NOT NULL DEFAULT 'CREDENTIALS',
    provider_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Auto-create guest users from existing sessions
INSERT INTO users (id, username, email, enabled, provider, created_at, updated_at)
SELECT DISTINCT
    user_id,
    'guest_' || REPLACE(CAST(user_id AS VARCHAR), '-', ''),
    'guest_' || REPLACE(CAST(user_id AS VARCHAR), '-', '') || '@localhost',
    TRUE,
    'CREDENTIALS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM chat_sessions
WHERE user_id NOT IN (SELECT id FROM users);

-- Assign USER role to all guest users
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username LIKE 'guest_%';

-- Create default admin user
INSERT INTO users (id, username, email, password_hash, enabled, provider, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'admin',
    'admin@localhost',
    '$2a$12$...', -- BCrypt hash of configured ADMIN_PASSWORD
    TRUE,
    'CREDENTIALS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role)
VALUES
    ('00000000-0000-0000-0000-000000000000', 'USER'),
    ('00000000-0000-0000-0000-000000000000', 'ADMIN');

-- Add foreign key constraints
ALTER TABLE chat_sessions
ADD CONSTRAINT fk_chat_sessions_user
FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE vocabulary_items
ADD CONSTRAINT fk_vocabulary_items_user
FOREIGN KEY (user_id) REFERENCES users(id);
```

---

## Testing Checklist

### Authentication Tests
- [ ] Register new user with valid data
- [ ] Register fails with duplicate username
- [ ] Register fails with duplicate email
- [ ] Register fails with weak password
- [ ] Login with username succeeds
- [ ] Login with email succeeds
- [ ] Login fails with wrong password
- [ ] Login fails with disabled account
- [ ] JWT token includes correct claims
- [ ] JWT token expires after configured time
- [ ] Refresh token generates new access token
- [ ] Refresh token expires after configured time
- [ ] Invalid JWT returns 401
- [ ] Expired JWT returns 401

### Authorization Tests
- [ ] Unauthenticated request to protected endpoint returns 401
- [ ] USER can access own sessions
- [ ] USER cannot access other users' sessions
- [ ] USER can access own vocabulary
- [ ] USER cannot access other users' vocabulary
- [ ] ADMIN can access all users' sessions
- [ ] ADMIN can access all users' vocabulary
- [ ] USER cannot access admin endpoints
- [ ] ADMIN can access admin endpoints

### User Management Tests
- [ ] Admin can list all users
- [ ] Admin can search users by username/email
- [ ] Admin can filter users by role
- [ ] Admin can create new user
- [ ] Admin can update user profile
- [ ] Admin can enable/disable user
- [ ] Admin can change user roles
- [ ] Admin can delete user
- [ ] Admin cannot delete own account
- [ ] Admin cannot revoke own ADMIN role
- [ ] Deleted user's sessions are handled correctly (cascade or prevent)

### Integration Tests
- [ ] Full user journey: register → login → create session → send message → logout
- [ ] Admin journey: login → create user → assign ADMIN → list users
- [ ] Password change journey: login → change password → logout → login with new password

---

## Documentation Updates

### Files to Update
1. **README.md**
   - Add authentication section
   - Update API endpoints table
   - Add environment variables
   - Update quick start with registration/login examples

2. **CLAUDE.md**
   - Add auth/ and user/ packages to structure
   - Update REST API layer section
   - Add security notes to development guidelines

3. **src/test/http/http-client-requests.http**
   - Add authentication examples (register, login, refresh)
   - Add Bearer token to all protected endpoints
   - Add admin user management examples

4. **http-client.env.json**
   - Add `accessToken` variable
   - Add `adminToken` variable

5. **New file: SECURITY.md**
   - Document security best practices
   - JWT secret generation
   - Password policies
   - OAuth2 setup (future)

---

## Open Questions

1. **JWT Storage:**
   - Should refresh tokens be stored in database for revocation? (Recommendation: Yes)
   - Should we implement token blacklisting for logout? (Recommendation: Yes for refresh, No for access)

2. **Email Verification:**
   - Should we require email verification before allowing login? (Recommendation: Optional, configurable)
   - Should we send welcome emails? (Recommendation: Future phase)

3. **Password Policies:**
   - Should we enforce password expiration? (Recommendation: No for MVP, optional later)
   - Should we prevent password reuse? (Recommendation: Future phase)

4. **Account Lockout:**
   - After how many failed login attempts? (Recommendation: 5 attempts, 15 min lockout)
   - Should we implement CAPTCHA? (Recommendation: Future phase)

5. **Multi-Factor Authentication (MFA):**
   - Should we support TOTP/SMS codes? (Recommendation: Future phase)

6. **User Deletion:**
   - Soft delete or hard delete? (Recommendation: Soft delete with GDPR-compliant hard delete option)
   - Should we anonymize data instead of deleting? (Recommendation: Yes for compliance)

7. **Session Management:**
   - Should we allow multiple concurrent sessions per user? (Recommendation: Yes)
   - Should we show active sessions to users? (Recommendation: Future phase)

8. **OAuth2 Account Linking:**
   - Can users link multiple OAuth2 providers? (Recommendation: Yes, future phase)
   - Can users convert OAuth2 account to credential account? (Recommendation: Yes, future phase)

---

## Success Metrics

1. **Functionality:**
   - All authentication endpoints working correctly
   - All authorization rules enforced
   - Admin user management operational
   - Zero security vulnerabilities in initial audit

2. **Performance:**
   - Login response time < 200ms
   - JWT validation overhead < 10ms per request
   - User data isolation with no performance degradation

3. **Testing:**
   - 100% test coverage for auth and user services
   - All security tests passing
   - No failing tests after implementation

4. **Documentation:**
   - Complete API documentation with examples
   - Security best practices documented
   - Migration guide for existing deployments

---

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Spring Security OAuth2 Login](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Bcrypt Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

---

## Approval & Sign-off

**Requirements Author:** Eric
**Technical Review:** _Pending_
**Security Review:** _Pending_
**Product Owner Approval:** _Pending_

**Version:** 1.0
**Last Updated:** 2025-10-02
