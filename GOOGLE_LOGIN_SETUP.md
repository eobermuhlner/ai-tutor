# Google OAuth Login Configuration Guide

## Overview

The AI Tutor application supports Google OAuth 2.0 authentication, allowing users to sign in with their Google accounts. This feature provides:

- **Auto-account creation**: New users are automatically registered on first Google login
- **Account linking**: Existing email/password accounts can be linked to Google
- **Google One Tap**: Auto-popup on login page for seamless sign-in
- **Secure authentication**: Server-side token verification using Google's official API
- **Profile auto-fill**: Automatically populates user profile with Google account data

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Google Cloud Console Setup](#google-cloud-console-setup)
3. [Backend Configuration](#backend-configuration)
4. [Frontend Configuration](#frontend-configuration)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)
7. [Security Considerations](#security-considerations)
8. [Production Deployment](#production-deployment)

---

## Prerequisites

Before configuring Google OAuth login, ensure you have:

- A Google account (Gmail or Google Workspace)
- Access to [Google Cloud Console](https://console.cloud.google.com/)
- Backend running on `http://localhost:8080` (or your configured port)
- Frontend running on `http://localhost:5173` (or your configured port)

---

## Google Cloud Console Setup

### Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click the project dropdown in the top navigation bar
3. Click **"New Project"**
4. Enter project details:
   - **Project name**: `AI Tutor` (or your preferred name)
   - **Organization**: Leave as default or select your organization
   - **Location**: Leave as default
5. Click **"Create"**
6. Wait for the project to be created (takes ~10-30 seconds)
7. Select the newly created project from the project dropdown

### Step 2: Enable Required APIs

1. In the left sidebar, navigate to **"APIs & Services"** → **"Library"**
2. Search for **"Google+ API"** (or "Google Identity")
3. Click on **"Google+ API"**
4. Click **"Enable"**
5. Wait for the API to be enabled

**Alternative**: The API may be enabled automatically when you create OAuth credentials.

### Step 3: Configure OAuth Consent Screen

This screen is shown to users when they sign in with Google.

1. Navigate to **"APIs & Services"** → **"OAuth consent screen"**
2. Choose user type:
   - **Internal**: Only for Google Workspace users in your organization (not available for personal Gmail accounts)
   - **External**: For anyone with a Google account (**recommended for most cases**)
3. Click **"Create"**

#### Configure App Information

**App information:**
- **App name**: `AI Tutor`
- **User support email**: Select your email from the dropdown
- **App logo**: (Optional) Upload a 120x120px logo

**App domain:**
- **Application home page**: `http://localhost:5173` (for development)
- **Application privacy policy link**: (Optional) Your privacy policy URL
- **Application terms of service link**: (Optional) Your terms of service URL

**Authorized domains:**
- For development: Leave empty or add `localhost`
- For production: Add your domain (e.g., `example.com`)

**Developer contact information:**
- **Email addresses**: Enter your email address(es)

Click **"Save and Continue"**

#### Configure Scopes

1. Click **"Add or Remove Scopes"**
2. Select the following scopes:
   - `.../auth/userinfo.email` - View your email address
   - `.../auth/userinfo.profile` - View your basic profile info
   - `openid` - Associate you with your personal info on Google
3. Click **"Update"**
4. Click **"Save and Continue"**

#### Add Test Users (External User Type Only)

If you selected "External" user type and haven't published the app:

1. Click **"Add Users"**
2. Enter email addresses of users who can test the app
3. Click **"Add"**
4. Click **"Save and Continue"**

**Note**: Test users are required during development. To allow anyone to sign in, you must publish the app (see Production Deployment section).

#### Review and Finish

1. Review your OAuth consent screen configuration
2. Click **"Back to Dashboard"**

### Step 4: Create OAuth 2.0 Credentials

1. Navigate to **"APIs & Services"** → **"Credentials"**
2. Click **"Create Credentials"** (top of page)
3. Select **"OAuth client ID"**

#### Configure OAuth Client

**Application type:**
- Select **"Web application"**

**Name:**
- Enter a descriptive name: `AI Tutor Web Client`

**Authorized JavaScript origins:**
Add the origins where your frontend is hosted:
- Development: `http://localhost:5173`
- Production: `https://yourdomain.com`

Click **"Add URI"** for each origin.

**Authorized redirect URIs:**
While not strictly required for Google One Tap, add these for broader OAuth compatibility:
- Development: `http://localhost:5173`
- Production: `https://yourdomain.com`

Click **"Add URI"** for each redirect.

**Example configuration:**
```
Authorized JavaScript origins:
  - http://localhost:5173

Authorized redirect URIs:
  - http://localhost:5173
```

#### Save and Copy Credentials

1. Click **"Create"**
2. A popup will appear with your credentials:
   - **Client ID**: `1234567890-abcdefghijk.apps.googleusercontent.com`
   - **Client secret**: `GOCSPX-abcdefghijk1234567890`
3. Click **"Download JSON"** (optional, for backup)
4. **Copy both values** - you'll need them for configuration
5. Click **"OK"**

**⚠️ Important**: Keep the Client Secret secure! Never commit it to version control or expose it in frontend code.

---

## Backend Configuration

### Step 1: Set Environment Variables

The backend requires two environment variables for Google OAuth.

#### Option A: Environment Variables (Recommended)

**Linux/macOS:**
```bash
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="GOCSPX-your-client-secret"
```

**Windows (Command Prompt):**
```cmd
set GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
set GOOGLE_CLIENT_SECRET=GOCSPX-your-client-secret
```

**Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="GOCSPX-your-client-secret"
```

#### Option B: IntelliJ IDEA Run Configuration

1. Open IntelliJ IDEA
2. Go to **Run** → **Edit Configurations**
3. Select **"AiTutor Server (OpenAI)"** (or your active configuration)
4. Under **Environment variables**, click the folder icon
5. Add the following variables:
   - Name: `GOOGLE_CLIENT_ID`, Value: `your-client-id.apps.googleusercontent.com`
   - Name: `GOOGLE_CLIENT_SECRET`, Value: `GOCSPX-your-client-secret`
6. Click **"OK"**
7. Apply and run the configuration

#### Option C: application.yml (Not Recommended for Secrets)

Edit `backend/src/main/resources/application.yml`:

```yaml
google:
  oauth:
    client-id: your-client-id.apps.googleusercontent.com
    client-secret: GOCSPX-your-client-secret
```

**⚠️ Warning**: This method hardcodes secrets in your configuration file. Only use for local development and **never commit secrets to Git**.

### Step 2: Verify Backend Configuration

1. Start the backend:
   ```bash
   ./gradlew :backend:bootRun
   ```

2. Check the logs for errors related to Google OAuth configuration

3. Verify the endpoint is available:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/oauth/google \
     -H "Content-Type: application/json" \
     -d '{"googleToken":"invalid"}'
   ```

   Expected response (token is invalid, but endpoint works):
   ```json
   {
     "timestamp": "2025-11-20T10:00:00.000+00:00",
     "status": 400,
     "error": "Bad Request",
     "message": "Invalid Google ID token"
   }
   ```

---

## Frontend Configuration

### Step 1: Create Environment File

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Create a `.env` file (if it doesn't exist):
   ```bash
   touch .env
   ```

3. Add the Google Client ID:
   ```env
   VITE_API_URL=http://localhost:8080/api/v1
   VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```

**⚠️ Important**:
- Only use the **Client ID** in the frontend (not the Client Secret!)
- The `.env` file is gitignored by default
- For team development, share the Client ID via secure channels

### Step 2: Verify Frontend Configuration

1. Start the frontend dev server:
   ```bash
   npm run dev
   ```

2. Open your browser to `http://localhost:5173`

3. Navigate to the login page

4. Check the browser console for errors:
   - Press **F12** (or **Cmd+Option+I** on Mac)
   - Look for Google-related errors in the Console tab

5. Verify the Google Sign-In button appears on the login page

### Step 3: Environment File Template

The repository includes `.env.example` with all required variables:

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

Copy and rename this file to get started:
```bash
cp .env.example .env
```

Then edit `.env` with your actual Client ID.

---

## Testing

### Test Scenario 1: New User Registration via Google

**Objective**: Verify that new users can create an account using Google login.

1. Ensure you're logged out of the application
2. Navigate to `http://localhost:5173/login`
3. Click **"Sign in with Google"**
4. Select a Google account that has **never** been used with this app
5. Grant permissions when prompted
6. Verify:
   - ✅ You're redirected to the application (sessions or languages page)
   - ✅ Your profile shows your name from Google
   - ✅ Your email is marked as verified
   - ✅ You can access protected pages

**Backend verification**:
```bash
# Check the backend logs for:
Successfully verified Google ID token for user: user@gmail.com
Creating new user from Google account: user@gmail.com
User logged in successfully via Google: user_123 (id: abc-123-def)
```

### Test Scenario 2: Account Linking

**Objective**: Verify that existing email/password accounts can be linked to Google.

1. Create an account with email/password:
   - Navigate to `http://localhost:5173/register`
   - Register with email: `test@gmail.com`, username: `testuser`, password: `Test1234`

2. Log out

3. Navigate to `http://localhost:5173/login`

4. Click **"Sign in with Google"**

5. Select the Google account with email `test@gmail.com`

6. Verify:
   - ✅ You're logged in successfully
   - ✅ Your username is still `testuser` (not changed)
   - ✅ Your account now has Google linked
   - ✅ You can log in with either Google or email/password

**Backend verification**:
```bash
# Check the backend logs for:
Verified Google token for email: test@gmail.com
Linking Google account to existing user: testuser
User logged in successfully via Google: testuser (id: abc-123-def)
```

### Test Scenario 3: Existing Google User Login

**Objective**: Verify that users with Google accounts can log in repeatedly.

1. Complete Test Scenario 1 or 2 first

2. Log out

3. Navigate to `http://localhost:5173/login`

4. Click **"Sign in with Google"**

5. Select your Google account

6. Verify:
   - ✅ You're logged in immediately (no registration)
   - ✅ Profile data is preserved
   - ✅ Sessions and data are intact

### Test Scenario 4: Google One Tap

**Objective**: Verify that Google One Tap auto-popup works.

1. Ensure you're logged into Google in your browser

2. Log out of the AI Tutor application

3. Navigate to `http://localhost:5173/login`

4. Verify:
   - ✅ Google One Tap popup appears automatically (may take 1-2 seconds)
   - ✅ You can select your Google account from the popup
   - ✅ You're logged in without clicking the button

**Note**: One Tap may not appear if:
- You've dismissed it recently (cooldown period)
- You're in incognito/private browsing
- You're not signed into Google
- The Client ID is incorrect

### Test Scenario 5: Email/Password Login Still Works

**Objective**: Verify backward compatibility with existing login.

1. Navigate to `http://localhost:5173/login`

2. Enter credentials:
   - Email/Username: `demo`
   - Password: `demo`

3. Click **"Sign In"**

4. Verify:
   - ✅ Login succeeds as before
   - ✅ No Google-related errors
   - ✅ All functionality works normally

---

## Troubleshooting

### Issue: "Invalid Google ID token" Error

**Symptoms**: Backend returns 400 error with message "Invalid Google ID token"

**Possible Causes**:
1. **Client ID mismatch**: Backend `GOOGLE_CLIENT_ID` doesn't match the credentials used to generate the token
2. **Expired token**: Google ID tokens expire after 1 hour
3. **Network issues**: Backend can't reach Google's verification servers

**Solutions**:
1. Verify `GOOGLE_CLIENT_ID` in backend matches the Client ID from Google Cloud Console
2. Check backend logs for detailed error messages
3. Ensure backend has internet access to reach `https://oauth2.googleapis.com`
4. Try logging in again to get a fresh token

**Debug**:
```bash
# Check backend environment variable
echo $GOOGLE_CLIENT_ID

# Restart backend with verbose logging
./gradlew :backend:bootRun --debug
```

### Issue: "Origin not allowed" Error

**Symptoms**: Browser console shows "Not a valid origin for the client"

**Possible Causes**:
1. Frontend URL not added to "Authorized JavaScript origins" in Google Cloud Console
2. Port mismatch (e.g., running on `:5174` but configured for `:5173`)

**Solutions**:
1. Go to Google Cloud Console → Credentials
2. Edit your OAuth 2.0 Client ID
3. Add the exact origin to "Authorized JavaScript origins":
   - Example: `http://localhost:5173`
   - No trailing slash
   - Include the port number
4. Save and wait 5-10 minutes for changes to propagate
5. Clear browser cache and try again

### Issue: Google Sign-In Button Not Appearing

**Symptoms**: Login page loads but no Google button is visible

**Possible Causes**:
1. `VITE_GOOGLE_CLIENT_ID` not set in frontend `.env`
2. Invalid Client ID format
3. JavaScript error preventing component from rendering

**Solutions**:
1. Check `frontend/.env` exists and contains `VITE_GOOGLE_CLIENT_ID`
2. Verify Client ID format: `[numbers]-[alphanumeric].apps.googleusercontent.com`
3. Check browser console for errors (F12)
4. Restart frontend dev server:
   ```bash
   npm run dev
   ```

**Debug**:
```bash
# Check environment variable is loaded
cd frontend
cat .env

# Restart with clean cache
npm run dev -- --force
```

### Issue: "Access blocked: This app's request is invalid"

**Symptoms**: Google shows error page when trying to sign in

**Possible Causes**:
1. OAuth consent screen not configured
2. App not published (for External user type)
3. Test user not added (for External user type in development)

**Solutions**:
1. Complete OAuth consent screen configuration (see Step 3)
2. Add your Google account as a test user:
   - Google Cloud Console → OAuth consent screen
   - Scroll to "Test users"
   - Click "Add Users"
   - Add your email address
3. For production, publish the app:
   - OAuth consent screen → "Publish App"

### Issue: Account Created with Random Username

**Symptoms**: New Google users get usernames like `john_doe_1`, `john_doe_2`

**Explanation**: This is expected behavior when:
- Multiple users have the same email prefix (e.g., `john.doe@gmail.com`)
- Username from email is already taken

**How it works**:
1. Username generated from email prefix: `john.doe@gmail.com` → `john_doe`
2. If `john_doe` exists, append counter: `john_doe_1`, `john_doe_2`, etc.
3. User can change username later in profile settings

**Not a bug**: This prevents username conflicts and ensures unique usernames.

### Issue: "Failed to verify Google ID token" in Logs

**Symptoms**: Backend logs show verification failure

**Possible Causes**:
1. Google API client library not installed
2. Network connectivity issues
3. Google's verification servers are down (rare)

**Solutions**:
1. Verify dependency in `backend/build.gradle`:
   ```gradle
   implementation 'com.google.api-client:google-api-client:2.2.0'
   ```

2. Rebuild backend:
   ```bash
   ./gradlew :backend:clean :backend:build
   ```

3. Check network connectivity:
   ```bash
   curl https://www.googleapis.com/oauth2/v3/certs
   ```

4. Check Google API status: https://status.cloud.google.com/

---

## Security Considerations

### Client Secret Protection

**❌ NEVER**:
- Commit `GOOGLE_CLIENT_SECRET` to Git
- Include it in frontend code
- Share it in public channels
- Hardcode it in `application.yml` in production

**✅ ALWAYS**:
- Use environment variables
- Store in secure secret management systems (AWS Secrets Manager, HashiCorp Vault, etc.)
- Rotate secrets periodically
- Restrict access to production credentials

### Token Validation

The backend **always** validates Google ID tokens server-side:
1. Verifies token signature using Google's public keys
2. Checks token expiration
3. Validates audience (Client ID) matches
4. Confirms issuer is Google

**Never** trust tokens from the frontend without server-side verification.

### HTTPS in Production

**⚠️ Critical**: Always use HTTPS in production.

Google requires HTTPS for:
- Authorized JavaScript origins (except localhost)
- Authorized redirect URIs (except localhost)

HTTP is only allowed for `localhost` and `127.0.0.1` during development.

### CORS Configuration

The backend allows specific origins for CORS:

**Development** (`application-dev.yml`):
```yaml
cors:
  allowed-origins:
    - http://localhost:5173
```

**Production** (environment variables):
```bash
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

**Never** use `*` for `allowed-origins` in production.

### Rate Limiting

Consider implementing rate limiting for OAuth endpoints to prevent abuse:
- Limit login attempts per IP
- Throttle token verification requests
- Monitor for suspicious patterns

### Data Privacy

**User data collected**:
- Email address (required)
- First name (optional)
- Last name (optional)
- Google user ID (for account linking)
- Email verification status

**Not collected**:
- Google password
- Access tokens (only short-lived ID tokens)
- Google contacts, calendar, or other data

**Compliance**:
- Include Google login in your privacy policy
- Disclose what data is accessed from Google
- Provide option to unlink Google account
- Allow account deletion

---

## Production Deployment

### Step 1: Publish OAuth Consent Screen

1. Go to Google Cloud Console → OAuth consent screen
2. Review all information for accuracy
3. Click **"Publish App"**
4. If prompted, submit for verification (required for certain scopes)
5. Wait for approval (can take several days to weeks)

**Note**: Until published, only test users can sign in.

### Step 2: Update Authorized Origins

1. Go to Google Cloud Console → Credentials
2. Edit your OAuth 2.0 Client ID
3. Add production origins:
   ```
   Authorized JavaScript origins:
     - https://yourdomain.com
     - https://www.yourdomain.com

   Authorized redirect URIs:
     - https://yourdomain.com
     - https://www.yourdomain.com
   ```
4. Save changes
5. Wait 5-10 minutes for propagation

### Step 3: Configure Production Backend

**Use secure secret management**:

**AWS:**
```bash
# Store in AWS Secrets Manager
aws secretsmanager create-secret \
  --name ai-tutor/google-oauth \
  --secret-string '{"client_id":"...","client_secret":"..."}'

# Retrieve in application
GOOGLE_CLIENT_ID=$(aws secretsmanager get-secret-value --secret-id ai-tutor/google-oauth --query SecretString --output text | jq -r .client_id)
```

**Docker:**
```bash
docker run -d \
  -e GOOGLE_CLIENT_ID=your-client-id \
  -e GOOGLE_CLIENT_SECRET=your-client-secret \
  ai-tutor-backend
```

**Kubernetes:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: google-oauth
type: Opaque
data:
  client-id: base64-encoded-client-id
  client-secret: base64-encoded-client-secret
```

### Step 4: Configure Production Frontend

**Build with environment variable**:
```bash
# Set environment variable
export VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com

# Build
npm run build

# Verify environment variable is embedded
grep -r "your-client-id" dist/
```

**Or use build-time configuration** (`.env.production`):
```env
VITE_API_URL=https://api.yourdomain.com/api/v1
VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

### Step 5: Enable HTTPS

**Backend** (Spring Boot with Let's Encrypt):
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

**Frontend** (Nginx):
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    location / {
        root /var/www/ai-tutor;
        try_files $uri $uri/ /index.html;
    }
}
```

### Step 6: Test Production Configuration

1. Deploy to production environment
2. Navigate to `https://yourdomain.com/login`
3. Complete all test scenarios (see Testing section)
4. Monitor backend logs for errors
5. Check browser console for errors

---

## Additional Resources

### Official Documentation

- [Google Identity Documentation](https://developers.google.com/identity)
- [Google Sign-In for Web](https://developers.google.com/identity/gsi/web)
- [@react-oauth/google Library](https://www.npmjs.com/package/@react-oauth/google)
- [Google API Client for Java](https://googleapis.github.io/google-api-java-client/)

### Support

For issues related to:
- **Google OAuth**: [Stack Overflow](https://stackoverflow.com/questions/tagged/google-oauth) or [Google Identity Support](https://support.google.com/cloud/answer/6158849)
- **AI Tutor application**: Create an issue in the repository

### Useful Commands

**Check environment variables**:
```bash
# Backend
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET

# Frontend
cat frontend/.env
```

**Reset Google OAuth session**:
1. Go to https://myaccount.google.com/permissions
2. Find "AI Tutor" in the list
3. Click "Remove Access"
4. Try signing in again (will show consent screen)

**Clear browser cache**:
- Chrome/Edge: `Ctrl+Shift+Delete` (or `Cmd+Shift+Delete` on Mac)
- Select "Cookies and other site data"
- Select "Cached images and files"
- Click "Clear data"

---

## FAQ

### Q: Can users have both email/password and Google login?

**A**: Yes! Account linking is supported. If a user registers with email/password and later signs in with Google using the same email, the accounts are automatically linked. They can then use either method to log in.

### Q: What happens if a user changes their Google email?

**A**: The user's account is linked to their Google user ID (not email), so changing the email in Google won't affect their account. However, they may need to update their email in the AI Tutor profile separately.

### Q: Can I disable email/password login and only use Google?

**A**: Yes, but this requires code changes. You would need to:
1. Remove the email/password form from `LoginPage.tsx`
2. Remove the `/auth/register` endpoint or make it private
3. Update the UI to reflect Google-only authentication

### Q: How do I revoke a user's Google access?

**A**: Users can revoke access themselves at https://myaccount.google.com/permissions. Alternatively, you can delete their account from the AI Tutor database, which will require them to re-authenticate.

### Q: Is Google login free?

**A**: Yes! Google OAuth is free for most use cases. However, Google may impose quotas or rate limits for very high-traffic applications. See [Google Cloud pricing](https://cloud.google.com/identity-platform/pricing) for details.

### Q: Can I use multiple OAuth providers (Google, GitHub, Facebook)?

**A**: The database schema already supports multiple providers via the `AuthProvider` enum (`GOOGLE`, `GITHUB`, `FACEBOOK`). You would need to implement similar services and endpoints for each provider.

### Q: What if Google's services are down?

**A**: If Google's OAuth services are unavailable:
- New users cannot sign in with Google
- Existing users with email/password can still log in
- The application remains functional for non-Google authentication

Monitor Google's status at https://status.cloud.google.com/

---

## Changelog

### v1.0.0 (2025-11-20)

- Initial implementation of Google OAuth login
- Auto-account creation for new users
- Account linking for existing users
- Google One Tap integration
- Server-side token verification
- Profile auto-fill from Google account

---

**Need help?** If you encounter issues not covered in this guide, please check the troubleshooting section or create an issue in the repository.
