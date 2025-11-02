# Stripe Configuration Guide - Optional Payment Integration

This document explains how the Stripe payment integration is organized as an **optional feature** in the AI Tutor application.

## Overview

Stripe payment integration is now **disabled by default** and can be enabled via Spring profiles. This allows you to:

- **Run the application without Stripe** for local development or testing
- **Enable Stripe only when needed** (production, payment testing)
- **Avoid configuration errors** when Stripe credentials are not available
- **Keep the codebase clean** with conditional bean loading

## Architecture

### Profile-Based Configuration

The Stripe integration uses Spring Boot profiles:

| Profile         | Stripe Status | Use Case                           |
|-----------------|---------------|------------------------------------|
| `dev-ollama`    | ❌ Disabled   | Local development (default)        |
| `dev-openai`    | ❌ Disabled   | Development with OpenAI            |
| `dev-stripe`    | ✅ Enabled    | Development with Stripe testing    |
| `prod`          | ✅ Enabled    | Production deployment              |
| `test`          | ❌ Disabled   | Automated tests                    |

### File Structure

```
backend/src/main/resources/
├── application.yml              # Main config (Stripe removed)
├── application-stripe.yml       # Stripe-specific config (NEW)
└── db/migration/
    └── V9__Add_stripe_integration.sql

backend/src/main/kotlin/.../payment/
├── config/
│   └── StripeConfig.kt                # ⚙️ Conditional on stripe.enabled
├── service/
│   ├── StripeServiceInterface.kt      # Common interface (NEW)
│   ├── StripeService.kt               # ⚙️ Real implementation (conditional)
│   ├── NoOpStripeService.kt           # 🔄 No-op implementation (default, NEW)
│   └── SubscriptionService.kt         # Always loaded
└── controller/
    ├── PaymentController.kt           # Always loaded (uses interface)
    └── StripeWebhookController.kt     # ⚙️ Conditional on stripe.enabled
```

## Configuration Files

### application.yml (Main Config)

Stripe configuration **removed** from main file. It's now in `application-stripe.yml`.

Profile groups updated:
```yaml
spring:
  profiles:
    group:
      prod: prod,h2-file,ai-openai,prompts-large,stripe  # ← Includes 'stripe'
      dev-stripe: dev,h2-file,ai-openai,prompts-large,stripe  # ← New profile
```

### application-stripe.yml (Stripe Config)

**New file** with Stripe-specific configuration:

```yaml
# Only loaded when 'stripe' profile is active
stripe:
  enabled: true
  api-key: ${STRIPE_SECRET_KEY:}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:}
  price-id-subscription-10: ${STRIPE_PRICE_ID_SUBSCRIPTION_10:}
  success-url: ${FRONTEND_URL:http://localhost:5173}/profile?payment=success
  cancel-url: ${FRONTEND_URL:http://localhost:5173}/profile?payment=cancel
```

## Bean Conditional Loading

### StripeConfig

```kotlin
@Configuration
@ConfigurationProperties(prefix = "stripe")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeConfig {
    var enabled: Boolean = false
    // ... other properties
}
```

**Behavior**:
- ✅ **Loaded** when `stripe.enabled=true` (via `stripe` profile)
- ❌ **Not loaded** when property is missing or `false`

### StripeService (Real Implementation)

```kotlin
@Service
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeService(...) : StripeServiceInterface {
    // Real Stripe API calls
}
```

**Behavior**:
- ✅ **Loaded** when Stripe is enabled
- ❌ **Not loaded** otherwise

### StripeWebhookController

```kotlin
@RestController
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeWebhookController(...) {
    // Stripe webhook handling
}
```

**Behavior**:
- ✅ **Loaded** when Stripe is enabled
- ❌ **Not loaded** when Stripe is disabled
- Webhook endpoint `/api/v1/webhooks/stripe` only available when Stripe is enabled

### NoOpStripeService (Dummy Implementation)

```kotlin
@Service
@ConditionalOnProperty(
    name = ["stripe.enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class NoOpStripeService : StripeServiceInterface {
    override fun createCheckoutSession(...) {
        throw UnsupportedOperationException(
            "Stripe payment integration is not enabled. " +
            "Please configure Stripe or use the 'stripe' profile."
        )
    }
    // ... other methods
}
```

**Behavior**:
- ✅ **Loaded** when `stripe.enabled=false` or property is missing (default)
- ❌ **Not loaded** when Stripe is enabled
- **Returns errors** if user tries to access payment features

## Usage Examples

### 1. Running WITHOUT Stripe (Default)

```bash
# Standard development (no Stripe)
./gradlew :backend:bootRun

# Or explicitly
./gradlew :backend:bootRun --args='--spring.profiles.active=dev-openai'
```

**What happens**:
- `NoOpStripeService` is loaded
- Payment endpoints return error: "Stripe payment integration is not enabled"
- Frontend shows "Upgrade to Premium" button, but clicking it shows error message
- No Stripe API calls are made
- Application runs normally for non-payment features

### 2. Running WITH Stripe (Development Testing)

**Set environment variables first**:

```bash
# Windows PowerShell
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:STRIPE_WEBHOOK_SECRET="whsec_..."
$env:STRIPE_PRICE_ID_SUBSCRIPTION_10="price_..."
$env:FRONTEND_URL="http://localhost:5173"

# macOS/Linux
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export STRIPE_PRICE_ID_SUBSCRIPTION_10=price_...
export FRONTEND_URL=http://localhost:5173
```

**Run with `dev-stripe` profile**:

```bash
./gradlew :backend:bootRun --args='--spring.profiles.active=dev-stripe'
```

**What happens**:
- `StripeService` (real implementation) is loaded
- `StripeConfig` is loaded with environment variables
- Payment endpoints work normally
- Stripe API calls are made
- Webhooks can be received (use Stripe CLI)

### 3. Production Deployment

The `prod` profile automatically includes `stripe`:

```yaml
spring:
  profiles:
    group:
      prod: prod,h2-file,ai-openai,prompts-large,stripe
```

**Set environment variables in production**:

```bash
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_ID_SUBSCRIPTION_10=price_...
FRONTEND_URL=https://your-domain.com
```

**Run**:

```bash
./gradlew :backend:bootRun --args='--spring.profiles.active=prod'

# Or in deployment:
java -jar backend.jar --spring.profiles.active=prod
```

## User Experience

### When Stripe is DISABLED

**Profile Page**:
- User sees "Upgrade to Premium" button
- Clicking button triggers API call to `/api/v1/payment/checkout-session`
- Backend returns **500 error** with message:
  > "Stripe payment integration is not enabled. Please configure Stripe or use the 'stripe' profile."
- Frontend shows error toast
- User cannot upgrade (expected behavior)

**Use Case**: Local development, testing non-payment features

### When Stripe is ENABLED

**Profile Page**:
- User sees "Upgrade to Premium" button
- Clicking button creates Stripe Checkout session
- User is redirected to Stripe payment page
- After payment, subscription is activated
- User sees "Manage Subscription" button

**Use Case**: Production, payment testing

## Benefits of This Approach

### 1. Clean Separation of Concerns

- Payment logic is isolated in `application-stripe.yml`
- Main config remains clean and minimal
- Easy to see what's required for payments

### 2. No Configuration Errors

**Before** (all configs in main file):
```
❌ Application fails to start if STRIPE_SECRET_KEY is not set
❌ Bean creation errors when Stripe config is incomplete
❌ Developer must configure Stripe even for non-payment work
```

**After** (profile-based):
```
✅ Application starts successfully without Stripe config
✅ No bean errors when Stripe is disabled
✅ Developers can work on other features without Stripe setup
```

### 3. Easy Testing

```kotlin
// Tests can inject NoOpStripeService
@SpringBootTest
@ActiveProfiles("test")  // Stripe disabled
class SomeTest {
    @Autowired
    lateinit var stripeService: StripeServiceInterface  // Gets NoOpStripeService

    @Test
    fun testNonPaymentFeature() {
        // Test works without Stripe configuration
    }
}
```

### 4. Environment Parity

- Local dev: Stripe disabled (fast iteration)
- Staging: Stripe enabled (test payment flows)
- Production: Stripe enabled (real payments)

### 5. Clear Error Messages

When Stripe is disabled, users get helpful errors:

```
UnsupportedOperationException:
  Stripe payment integration is not enabled.
  Please configure Stripe or use the 'stripe' profile.
```

Instead of cryptic bean initialization errors.

## Migration Guide

If you previously had Stripe configured in `application.yml`:

### Before

```yaml
# application.yml
stripe:
  api-key: ${STRIPE_SECRET_KEY:}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:}
  price-id-subscription-10: ${STRIPE_PRICE_ID_SUBSCRIPTION_10:}
```

### After

1. **Remove** Stripe config from `application.yml` ✅ Already done
2. **Use** `application-stripe.yml` instead ✅ Already created
3. **Enable** Stripe profile when needed:

```bash
# Development with Stripe
./gradlew :backend:bootRun --args='--spring.profiles.active=dev-stripe'

# Production (already includes stripe)
./gradlew :backend:bootRun --args='--spring.profiles.active=prod'
```

## Troubleshooting

### Problem: "No qualifying bean of type StripeServiceInterface"

**Cause**: Neither `StripeService` nor `NoOpStripeService` is loaded.

**Solution**: This shouldn't happen due to `matchIfMissing = true` on `NoOpStripeService`.
Check that Spring Boot auto-configuration is working.

### Problem: "Payment integration is not enabled" error

**Cause**: You're trying to use payment features without the `stripe` profile.

**Solution**:
```bash
# Enable Stripe profile
./gradlew :backend:bootRun --args='--spring.profiles.active=dev-stripe'

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev-stripe
```

### Problem: Application fails to start with "lateinit property apiKey has not been initialized"

**Cause**: Stripe profile is active but environment variables are not set.

**Solution**: Set required environment variables:
```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export STRIPE_PRICE_ID_SUBSCRIPTION_10=price_...
```

## Related Documentation

- **[STRIPE_SETUP.md](./STRIPE_SETUP.md)**: Complete Stripe integration setup guide
- **[CLAUDE.md](./CLAUDE.md)**: General project documentation
- **[application-stripe.yml](./backend/src/main/resources/application-stripe.yml)**: Stripe configuration file

## Summary

✅ **Stripe is now optional** - disabled by default
✅ **Enable via profile** - use `dev-stripe` or `prod`
✅ **No-op fallback** - graceful degradation when disabled
✅ **Clean separation** - payment config isolated
✅ **Better DX** - no config errors in development

The application can now run perfectly fine without Stripe configuration, making it easier for developers to work on non-payment features while still supporting full payment integration when needed.
