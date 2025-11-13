# Task 0015: User Authentication & Management Security Enhancements

**Status:** Planning
**Priority:** Critical
**Created:** 2025-01-09

## Overview

Comprehensive security enhancements for user authentication and management system. The current implementation has a solid JWT foundation but lacks critical security features for production deployment.

---

## 🔴 PHASE 1: CRITICAL SECURITY (Must Have for Production)

### 1. Email Verification System
**Priority:** Critical | **Effort:** Medium | **Risk:** High if not implemented

**Current State:**
- ✅ Database field exists: `UserEntity.emailVerified`
- ✅ Field returned in API: `UserResponse.emailVerified`
- ✅ Reset on email change
- ❌ No verification token generation
- ❌ No email sending
- ❌ No verification endpoint
- ❌ Not enforced (users can use app with unverified email)

**Implementation Tasks:**
- [ ] Create `EmailVerificationToken` entity (token, userId, expiresAt, createdAt)
- [ ] Add email sending service (SMTP configuration)
  - Options: SendGrid, AWS SES, Mailgun, or SMTP relay
- [ ] Create email templates (HTML + plain text)
- [ ] Add endpoint: `POST /api/v1/auth/send-verification-email`
- [ ] Add endpoint: `POST /api/v1/auth/verify-email?token={token}`
- [ ] Send verification email on registration
- [ ] Option: Restrict features until email verified
- [ ] Add resend verification email functionality

**Security Benefits:**
- Prevents spam/fake accounts
- Ensures valid contact information
- Reduces account takeover risk

**Files to Modify:**
- `AuthService.kt` - Add verification logic
- `AuthController.kt` - Add verification endpoints
- `application.yml` - Add email configuration

**Files to Create:**
- `EmailVerificationTokenEntity.kt`
- `EmailVerificationTokenRepository.kt`
- `EmailService.kt`
- Email templates in `resources/templates/email/`

---

### 2. Password Reset Flow
**Priority:** Critical | **Effort:** Medium | **Risk:** High if not implemented

**Current State:**
- ✅ Placeholder endpoint: `POST /api/v1/admin/users/{userId}/reset-password` (admin only)
- ❌ No self-service password reset
- ❌ No reset token generation
- ❌ No reset emails
- ❌ Users locked out cannot recover accounts

**Implementation Tasks:**
- [ ] Create `PasswordResetToken` entity (token, userId, expiresAt, createdAt, used)
- [ ] Add endpoint: `POST /api/v1/auth/forgot-password` (public)
  - Input: email address
  - Generate secure token
  - Send reset email
- [ ] Add endpoint: `POST /api/v1/auth/reset-password` (public)
  - Input: token, newPassword
  - Validate token (not expired, not used)
  - Update password
  - Revoke all refresh tokens
  - Mark token as used
- [ ] Create password reset email template
- [ ] Token expiration: 1 hour recommended
- [ ] Add rate limiting (3 reset requests per hour per email)

**Security Benefits:**
- Self-service account recovery
- Reduces support burden
- Prevents account abandonment

**Files to Modify:**
- `AuthService.kt` - Add reset logic
- `AuthController.kt` - Add reset endpoints

**Files to Create:**
- `PasswordResetTokenEntity.kt`
- `PasswordResetTokenRepository.kt`
- `resources/templates/email/password-reset.html`

---

### 3. Failed Login Tracking & Auto-Lock
**Priority:** Critical | **Effort:** Small | **Risk:** Critical if not implemented

**Current State:**
- ✅ Database field exists: `UserEntity.locked`
- ✅ Admins can manually lock/unlock
- ❌ No failed login attempt tracking
- ❌ No automatic locking
- ❌ No auto-unlock mechanism
- ❌ Unlimited brute force attempts possible

**Implementation Tasks:**
- [ ] Add database fields to `UserEntity`:
  - `failedLoginAttempts: Int = 0`
  - `lastFailedLoginAt: Instant?`
  - `lockedUntil: Instant?`
- [ ] Modify login flow in `AuthService.kt`:
  - Increment counter on failed login
  - Reset counter on successful login
  - Auto-lock after 5 failed attempts
  - Set `lockedUntil` timestamp (30 minutes)
- [ ] Check `lockedUntil` before allowing login attempt
- [ ] Auto-unlock if current time > `lockedUntil`
- [ ] Send lockout notification email
- [ ] Add admin endpoint: `POST /api/v1/admin/users/{userId}/unlock`
- [ ] Add user endpoint: `GET /api/v1/auth/me/lockout-status`

**Configuration:**
- Max failed attempts: 5 (configurable)
- Lockout duration: 30 minutes (configurable)
- Reset counter after: 24 hours of no attempts

**Security Benefits:**
- **Prevents brute force attacks**
- Detects credential stuffing
- Alerts users to unauthorized access attempts

**Files to Modify:**
- `UserEntity.kt` - Add new fields
- `AuthService.kt` - Add tracking logic
- `AdminController.kt` - Add unlock endpoint

---

### 4. Rate Limiting on Auth Endpoints
**Priority:** Critical | **Effort:** Small | **Risk:** High if not implemented

**Current State:**
- ✅ Rate limiting exists for API calls (`RateLimitingService.kt`)
- ❌ No rate limiting on `/auth/login`
- ❌ No rate limiting on `/auth/register`
- ❌ No IP-based limiting
- ❌ No protection against distributed attacks

**Implementation Tasks:**
- [ ] Extend `RateLimitingService` or create `AuthRateLimitingService`
- [ ] Add IP-based rate limiting (use Spring's `HttpServletRequest`)
- [ ] Configure limits:
  - `/auth/login`: 5 attempts per 15 minutes per IP
  - `/auth/register`: 3 attempts per hour per IP
  - `/auth/forgot-password`: 3 attempts per hour per email
- [ ] Store in Redis or in-memory cache (Caffeine)
- [ ] Return HTTP 429 (Too Many Requests) when exceeded
- [ ] Add header: `Retry-After: {seconds}`
- [ ] Optional: Add CAPTCHA after N failed attempts

**Security Benefits:**
- Prevents automated attacks
- Reduces server load from bots
- Protects against credential stuffing

**Files to Modify:**
- `AuthController.kt` - Add rate limit checks

**Files to Create:**
- `AuthRateLimitingService.kt`
- `RateLimitExceededException.kt` (if not exists)

---

## 🟠 PHASE 2: ESSENTIAL FEATURES (High Priority)

### 5. Email Notification System
**Priority:** High | **Effort:** Medium

**Missing Notifications:**
- [ ] Welcome email (new user registration)
- [ ] Email verification
- [ ] Password reset
- [ ] Password changed (security alert)
- [ ] Email changed (confirmation to both old and new email)
- [ ] Account locked (failed login attempts)
- [ ] New device login (security alert)
- [ ] Suspicious activity detected
- [ ] Admin actions affecting user (subscription change, role change)

**Implementation Tasks:**
- [ ] Setup email service (already started in task 1)
- [ ] Create email templates (Thymeleaf or Freemarker)
- [ ] Add async email sending (Spring's `@Async`)
- [ ] Add email queue for reliability
- [ ] Add retry logic for failed sends
- [ ] Add unsubscribe mechanism for non-critical emails
- [ ] Track email delivery status

**Files to Create:**
- `EmailTemplateService.kt`
- `AsyncEmailService.kt`
- Multiple email templates in `resources/templates/email/`

---

### 6. Audit Logging
**Priority:** High | **Effort:** Medium

**Current State:**
- ✅ SLF4J logging to console/files
- ❌ No database audit trail
- ❌ Admin actions not tracked
- ❌ No compliance reporting

**Implementation Tasks:**
- [ ] Create `AuditLogEntity`:
  - `id`, `timestamp`, `userId`, `adminId`, `action`, `resourceType`, `resourceId`
  - `details` (JSON), `ipAddress`, `userAgent`, `success`
- [ ] Create `AuditLogRepository`
- [ ] Create `AuditService` with logging methods
- [ ] Add aspect-oriented programming (AOP) for automatic logging
- [ ] Log all admin actions:
  - User updates, role changes, subscription changes
  - Force logout, password reset triggers
  - User list views
- [ ] Log critical user actions:
  - Password changes, email changes
  - Account deletion requests
- [ ] Add admin endpoint: `GET /api/v1/admin/audit-logs`
  - Pagination, filtering (userId, action, dateRange)
  - Export to CSV
- [ ] Add retention policy (e.g., keep 1 year)

**Compliance Benefits:**
- GDPR compliance (access tracking)
- SOC 2 compliance
- Forensic investigation
- Admin accountability

**Files to Create:**
- `AuditLogEntity.kt`
- `AuditLogRepository.kt`
- `AuditService.kt`
- `AuditController.kt`
- `AuditLogAspect.kt` (for AOP)

---

### 7. Session/Device Management
**Priority:** High | **Effort:** Medium

**Current State:**
- ✅ Refresh token tracking in database
- ✅ `lastLoginAt` field
- ❌ No device information
- ❌ No session list for users
- ❌ Cannot revoke individual sessions

**Implementation Tasks:**
- [ ] Extend `RefreshTokenEntity`:
  - `userAgent` (browser/device info)
  - `ipAddress` (location tracking)
  - `deviceName` (friendly name, e.g., "Chrome on Windows")
  - `lastUsedAt` (updated on token refresh)
  - `createdAt` (session start time)
- [ ] Add endpoints:
  - `GET /api/v1/auth/sessions` - List active sessions
  - `DELETE /api/v1/auth/sessions/{id}` - Revoke specific session
  - `POST /api/v1/auth/sessions/logout-others` - Logout all except current
- [ ] Parse User-Agent for device info
- [ ] GeoIP lookup for location (optional)
- [ ] Display in UI:
  - Device type and browser
  - Last activity timestamp
  - Location (city, country)
  - "Current session" indicator
- [ ] Send email on new device login

**Security Benefits:**
- Users can detect unauthorized access
- Quick response to account compromise
- Better session visibility

**Files to Modify:**
- `RefreshTokenEntity.kt` - Add new fields
- `AuthService.kt` - Capture device info
- `AuthController.kt` - Add session endpoints

**Files to Create:**
- `SessionResponse.kt` (DTO)
- `UserAgentParser.kt` (utility)

---

## 🟡 PHASE 3: ENHANCED SECURITY (Medium Priority)

### 8. Password History
**Priority:** Medium | **Effort:** Small

**Implementation Tasks:**
- [ ] Create `PasswordHistoryEntity` (userId, passwordHash, changedAt)
- [ ] Store last 5-10 password hashes
- [ ] Check against history on password change
- [ ] Prevent password reuse
- [ ] Add configuration: `password.history.limit=5`

**Files to Create:**
- `PasswordHistoryEntity.kt`
- `PasswordHistoryRepository.kt`

---

### 9. Password Expiration Policy
**Priority:** Medium | **Effort:** Small

**Implementation Tasks:**
- [ ] Add field: `lastPasswordChangedAt: Instant` to `UserEntity`
- [ ] Set on password creation/change
- [ ] Check during login: `if (now - lastPasswordChangedAt > 90 days)`
- [ ] Set `credentialsExpired = true` (field exists)
- [ ] Force password change on next login
- [ ] Send warning emails (7 days before expiration)
- [ ] Add configuration: `password.expiration.days=90`

**Files to Modify:**
- `UserEntity.kt` - Add field
- `AuthService.kt` - Add expiration check

---

### 10. Enhanced Password Policy
**Priority:** Medium | **Effort:** Trivial

**Implementation Tasks:**
- [ ] Update `validatePassword()` in `AuthService.kt`
- [ ] Add special character requirement: `!@#$%^&*()_+-=[]{}|;:,.<>?`
- [ ] Update error message

---

### 11. Self-Service Account Deletion
**Priority:** Medium | **Effort:** Small

**Implementation Tasks:**
- [ ] Add endpoint: `DELETE /api/v1/auth/me`
- [ ] Require password confirmation
- [ ] Implement soft delete (set `deletedAt`)
- [ ] Send confirmation email
- [ ] Add scheduled job: hard delete after 30 days
- [ ] Add endpoint: `POST /api/v1/auth/me/undelete` (within 30 days)
- [ ] GDPR: Allow immediate hard delete if requested

**Files to Modify:**
- `AuthController.kt` - Add delete endpoint
- `UserService.kt` - Implement soft delete

**Files to Create:**
- `AccountDeletionScheduler.kt` (Spring @Scheduled)

---

## 🟢 PHASE 4: ADVANCED FEATURES (Low Priority)

### 12. Two-Factor Authentication (2FA)
**Priority:** Low | **Effort:** Large

**Implementation Tasks:**
- [ ] Add dependency: `google-authenticator` library
- [ ] Create `TwoFactorAuthEntity` (userId, secret, backupCodes, enabled, createdAt)
- [ ] Add enrollment endpoints:
  - `POST /api/v1/auth/2fa/enroll` - Generate secret, QR code
  - `POST /api/v1/auth/2fa/verify-enrollment` - Verify TOTP code
  - `POST /api/v1/auth/2fa/disable` - Disable 2FA
- [ ] Generate backup codes (10 one-time use codes)
- [ ] Update login flow:
  - Check if user has 2FA enabled
  - Return intermediate response requiring TOTP
  - Add `POST /api/v1/auth/2fa/verify` endpoint
- [ ] Add recovery flow using backup codes
- [ ] Store used backup codes to prevent reuse

**Files to Create:**
- `TwoFactorAuthEntity.kt`
- `TwoFactorAuthRepository.kt`
- `TwoFactorAuthService.kt`
- `TwoFactorAuthController.kt`

---

### 13. Admin User Impersonation
**Priority:** Low | **Effort:** Medium

**Implementation Tasks:**
- [ ] Add endpoint: `POST /api/v1/admin/users/{userId}/impersonate`
- [ ] Generate special JWT with claims: `originalAdminId`, `impersonatedUserId`
- [ ] Add endpoint: `POST /api/v1/admin/impersonate/exit`
- [ ] Log all impersonation events in audit log
- [ ] Add middleware to detect impersonation
- [ ] UI: Display banner "Impersonating user X"
- [ ] Restrict impersonation: cannot impersonate other admins

---

### 14. Bulk User Operations
**Priority:** Low | **Effort:** Medium

**Implementation Tasks:**
- [ ] Add endpoint: `GET /api/v1/admin/users/export?format=csv`
- [ ] Add endpoint: `POST /api/v1/admin/users/import` (multipart CSV)
- [ ] Add endpoint: `POST /api/v1/admin/users/bulk-update`
- [ ] Implement CSV parsing and validation
- [ ] Return import results (success/failure counts, error details)
- [ ] Add bulk operations: enable, disable, change subscription, assign role

---

### 15. Login History & Analytics
**Priority:** Low | **Effort:** Medium

**Implementation Tasks:**
- [ ] Create `LoginHistoryEntity` (userId, timestamp, ipAddress, userAgent, success, failureReason, location)
- [ ] Log every login attempt (success and failure)
- [ ] Add endpoint: `GET /api/v1/auth/login-history` (user's own history)
- [ ] Add admin endpoint: `GET /api/v1/admin/users/{userId}/login-history`
- [ ] Implement suspicious activity detection:
  - New country login
  - Unusual time of day
  - Multiple IPs in short time
- [ ] Send security alerts for suspicious logins
- [ ] Add analytics dashboard for admins

**Files to Create:**
- `LoginHistoryEntity.kt`
- `LoginHistoryRepository.kt`
- `LoginHistoryService.kt`

---

## 📊 CURRENT STATE SUMMARY

### ✅ What Works Well:
- JWT authentication (access + refresh tokens)
- Strong password hashing (BCrypt work factor 12)
- Password complexity requirements (8 chars, upper, lower, digit)
- Role-based access control (USER, ADMIN)
- Admin user management UI (search, filter, edit)
- Session management via refresh tokens
- Rate limiting for API calls (not auth endpoints)
- Security headers (HSTS, CSP, X-Frame-Options)
- CORS configuration
- Account status flags (enabled, locked, accountExpired, credentialsExpired)

### ❌ Critical Gaps:
1. **No email verification** - Field exists but no implementation
2. **No password reset** - Admin endpoint is placeholder
3. **No failed login tracking** - Unlimited brute force attempts
4. **No rate limiting on auth endpoints** - Vulnerable to automated attacks
5. **No email notification system** - No SMTP configuration
6. **No audit logging** - Only console logs, no database trail

### ⚠️ Security Risks:
- **Brute Force Attacks**: Unlimited login attempts with no throttling
- **Account Recovery**: Users cannot reset passwords
- **Spam Accounts**: No email verification enforcement
- **Compliance**: No audit trail for admin actions
- **Account Takeover**: No alerts for suspicious activity
- **Credential Stuffing**: No protection against automated login attempts

---

## 💡 RECOMMENDED IMPLEMENTATION ORDER

### Week 1-2: Email Infrastructure
1. Email Verification System (Task 1)
2. Email Notification System (Task 5)

### Week 3-4: Password Security
3. Password Reset Flow (Task 2)
4. Failed Login Tracking & Auto-Lock (Task 3)

### Week 5-6: Attack Prevention
5. Rate Limiting on Auth Endpoints (Task 4)
6. Audit Logging (Task 6)

### Week 7-8: Session Management
7. Session/Device Management (Task 7)
8. Password History (Task 8)

### Week 9-10: Policy Enforcement
9. Password Expiration Policy (Task 9)
10. Enhanced Password Policy (Task 10)
11. Self-Service Account Deletion (Task 11)

### Week 11-16: Advanced Features (Optional)
12. Two-Factor Authentication (Task 12)
13. Admin User Impersonation (Task 13)
14. Bulk User Operations (Task 14)
15. Login History & Analytics (Task 15)

---

## 🎯 Success Criteria

### Phase 1 Complete When:
- [ ] Users can verify email addresses
- [ ] Users can reset forgotten passwords
- [ ] Accounts auto-lock after 5 failed login attempts
- [ ] Auth endpoints have rate limiting
- [ ] All 4 critical features tested and deployed

### Phase 2 Complete When:
- [ ] Email notifications sent for security events
- [ ] Admin actions logged to audit table
- [ ] Users can view and manage active sessions
- [ ] All 3 high-priority features tested

### Production-Ready Criteria:
- [ ] All Phase 1 features implemented
- [ ] All Phase 2 features implemented
- [ ] Security testing completed (penetration test)
- [ ] Load testing completed
- [ ] Documentation updated
- [ ] Privacy policy updated (email handling, data retention)

---

## 📚 Technical References

### Libraries to Add:
- Email: Spring Boot Starter Mail
- Templates: Thymeleaf
- 2FA: `com.warrenstrange:googleauth:1.5.0`
- User Agent Parsing: `eu.bitwalker:UserAgentUtils:1.21`
- GeoIP (optional): `com.maxmind.geoip2:geoip2:4.0.0`

### Configuration Examples:

**Email (application.yml):**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

**Security:**
```yaml
app:
  security:
    max-failed-login-attempts: 5
    account-lockout-duration-minutes: 30
    password-expiration-days: 90
    password-history-limit: 5
    password-reset-token-expiration-hours: 1
    email-verification-token-expiration-hours: 24
```

---

## 🔗 Related Documentation

- Spring Security Reference: https://docs.spring.io/spring-security/reference/
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- NIST Digital Identity Guidelines: https://pages.nist.gov/800-63-3/
- JWT Best Practices: https://datatracker.ietf.org/doc/html/rfc8725

---

## ✅ Acceptance Testing Checklist

### Email Verification:
- [ ] User receives verification email on registration
- [ ] Email contains valid link with token
- [ ] Token expires after 24 hours
- [ ] Can resend verification email
- [ ] Verified status shown in profile

### Password Reset:
- [ ] User receives reset email58
- [ ] Reset link works and shows form
- [ ] Token expires after 1 hour
- [ ] Token can only be used once
- [ ] All sessions invalidated after reset

### Failed Login Protection:
- [ ] Account locks after 5 failed attempts
- [ ] Lockout email sent to user
- [ ] Auto-unlock after 30 minutes
- [ ] Admin can manually unlock
- [ ] Counter resets after successful login

### Rate Limiting:
- [ ] Login blocked after 5 attempts in 15 minutes
- [ ] Registration blocked after 3 attempts in 1 hour
- [ ] Returns HTTP 429 with Retry-After header
- [ ] Limits reset after time window

### Audit Logging:
- [ ] Admin actions logged with timestamp, admin ID, user ID
- [ ] Logs include IP address and user agent
- [ ] Admin can search/filter logs
- [ ] Logs exported to CSV

### Session Management:
- [ ] User can view all active sessions
- [ ] Sessions show device, browser, location
- [ ] User can revoke individual sessions
- [ ] "Logout all other devices" works
- [ ] Email sent on new device login
