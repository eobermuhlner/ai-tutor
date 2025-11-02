# Stripe Payment Integration Setup Guide

This guide walks you through completing the Stripe payment integration for the AI Tutor application.

## Table of Contents

1. [Stripe Dashboard Setup](#stripe-dashboard-setup)
2. [Environment Configuration](#environment-configuration)
3. [Local Development & Testing](#local-development--testing)
4. [Production Deployment](#production-deployment)
5. [Testing the Integration](#testing-the-integration)
6. [Troubleshooting](#troubleshooting)

---

## Stripe Dashboard Setup

### 1. Create or Sign In to Stripe Account

- Visit https://dashboard.stripe.com
- Create a new account or sign in to existing account
- Make sure you're in **Test Mode** (toggle in top-right corner)

### 2. Create the Premium Product

Navigate to **Products** → **Add product**:

**Product Details:**
- **Name**: `AI Tutor Premium`
- **Description**: `Premium subscription with 500 messages per day`
- **Image**: (optional) Upload a logo or product image

**Pricing:**
- **Pricing model**: `Standard pricing`
- **Price**: `10.00`
- **Billing period**: `Monthly`
- **Currency**: `USD` (or your preferred currency)

**Advanced Options:**
- **Payment behavior**: `Charge automatically`
- **Usage type**: `Licensed` (per user)

Click **Save product**

### 3. Copy the Price ID

After creating the product:
1. Click on the product in the Products list
2. Find the price entry (should show "$10.00/month")
3. Click on the price to see details
4. Copy the **Price ID** - it looks like: `price_1234567890abcdef`

**Save this Price ID** - you'll need it for environment variables.

### 4. Get Your API Keys

Navigate to **Developers** → **API keys**:

1. **Secret Key (for backend)**:
   - Find the "Secret key" section
   - Click **Reveal test key**
   - Copy the key (format: `sk_test_...`)
   - **IMPORTANT**: Never commit this to version control!

2. **Publishable Key (not needed for this integration)**:
   - We're using Stripe Checkout (hosted), so we don't need the publishable key

### 5. Set Up Webhooks

Navigate to **Developers** → **Webhooks** → **Add endpoint**:

**For Local Development:**
- **Endpoint URL**: You'll use Stripe CLI (see [Local Development](#local-development--testing))
- Skip this for now, we'll configure it with Stripe CLI

**For Production:**
- **Endpoint URL**: `https://your-domain.com/api/v1/webhooks/stripe`
- **Description**: `AI Tutor subscription webhooks`
- **Events to send**:
  - `customer.subscription.created`
  - `customer.subscription.updated`
  - `customer.subscription.deleted`
  - `invoice.payment_failed`
- Click **Add endpoint**
- Copy the **Signing secret** (format: `whsec_...`)

---

## Environment Configuration

> **Note**: Stripe integration is **optional** and disabled by default. See [STRIPE_CONFIGURATION.md](./STRIPE_CONFIGURATION.md) for details on the profile-based configuration system.

### Backend Environment Variables

**Important**: To enable Stripe, you must use the `dev-stripe` or `prod` profile.

**For development (local):**

Set environment variables:

```bash
# Stripe Configuration
export STRIPE_SECRET_KEY=sk_test_YOUR_SECRET_KEY_HERE
export STRIPE_WEBHOOK_SECRET=whsec_YOUR_WEBHOOK_SECRET_HERE
export STRIPE_PRICE_ID_SUBSCRIPTION_10=price_YOUR_PRICE_ID_HERE

# Frontend URL for redirects
export FRONTEND_URL=http://localhost:5173
```

**For Windows (PowerShell):**

```powershell
$env:STRIPE_SECRET_KEY="sk_test_YOUR_SECRET_KEY_HERE"
$env:STRIPE_WEBHOOK_SECRET="whsec_YOUR_WEBHOOK_SECRET_HERE"
$env:STRIPE_PRICE_ID_SUBSCRIPTION_10="price_YOUR_PRICE_ID_HERE"
$env:FRONTEND_URL="http://localhost:5173"
```

**For production:**

Set these as environment variables in your deployment platform (Heroku, AWS, etc.):

```bash
STRIPE_SECRET_KEY=sk_live_YOUR_LIVE_SECRET_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_LIVE_WEBHOOK_SECRET
STRIPE_PRICE_ID_SUBSCRIPTION_10=price_YOUR_LIVE_PRICE_ID
FRONTEND_URL=https://your-production-domain.com
```

### Verify Configuration

The backend reads these values from `application.yml`:

```yaml
stripe:
  api-key: ${STRIPE_SECRET_KEY:}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:}
  price-id-subscription-10: ${STRIPE_PRICE_ID_SUBSCRIPTION_10:}
  success-url: ${FRONTEND_URL:http://localhost:5173}/profile?payment=success
  cancel-url: ${FRONTEND_URL:http://localhost:5173}/profile?payment=cancel
```

---

## Local Development & Testing

### 1. Install Stripe CLI

**macOS:**
```bash
brew install stripe/stripe-cli/stripe
```

**Windows:**
```powershell
scoop install stripe
```

**Linux:**
```bash
# Download from https://github.com/stripe/stripe-cli/releases/latest
```

Or visit: https://stripe.com/docs/stripe-cli#install

### 2. Authenticate Stripe CLI

```bash
stripe login
```

This will open a browser window to authenticate.

### 3. Forward Webhooks to Local Backend

Open a **new terminal window** and run:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

You should see:
```
> Ready! Your webhook signing secret is whsec_... (^C to quit)
```

**Copy this webhook signing secret** and set it as your `STRIPE_WEBHOOK_SECRET` environment variable.

**Keep this terminal running** while testing locally.

### 4. Start Backend Server

In another terminal, **using the dev-stripe profile**:

```bash
# Windows
.\gradlew :backend:bootRun --args="--spring.profiles.active=dev-stripe"

# macOS/Linux
./gradlew :backend:bootRun --args='--spring.profiles.active=dev-stripe'
```

> **Important**: Without the `dev-stripe` profile, Stripe is disabled and payment features will return errors.

### 5. Start Frontend Dev Server

In another terminal:

```bash
cd frontend
npm run dev
```

### 6. Test the Flow

1. Open browser to http://localhost:5173
2. Log in (or register a new account)
3. Navigate to Profile page
4. Click **Upgrade to Premium** button
5. You'll be redirected to Stripe Checkout (test mode)
6. Use test card: `4242 4242 4242 4242`
   - Any future expiry date (e.g., 12/34)
   - Any 3-digit CVC (e.g., 123)
   - Any billing details
7. Complete payment
8. You'll be redirected back to `/profile?payment=success`
9. Check the Stripe CLI terminal - you should see webhook events
10. Check your backend logs - subscription should be activated

### Test Cards Reference

| Card Number         | Scenario                    |
|---------------------|----------------------------|
| 4242 4242 4242 4242 | Successful payment         |
| 4000 0025 0000 3155 | Requires authentication    |
| 4000 0000 0000 9995 | Payment declined           |
| 4000 0000 0000 0341 | Card attached successfully but charge fails |

Full list: https://stripe.com/docs/testing#cards

---

## Production Deployment

### Before Going Live

1. **Switch to Live Mode** in Stripe Dashboard
2. **Create production product** with same details as test product
3. **Get live API keys**:
   - Live Secret Key: `sk_live_...`
   - Live Price ID: `price_...` (from live product)
4. **Set up production webhook endpoint**:
   - URL: `https://your-production-domain.com/api/v1/webhooks/stripe`
   - Same events as before
   - Get live webhook signing secret: `whsec_...`

### Production Environment Variables

Set these in your production environment:

```bash
STRIPE_SECRET_KEY=sk_live_YOUR_LIVE_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_LIVE_WEBHOOK_SECRET
STRIPE_PRICE_ID_SUBSCRIPTION_10=price_YOUR_LIVE_PRICE_ID
FRONTEND_URL=https://your-production-domain.com
```

### Security Checklist

- [ ] Never commit API keys to version control
- [ ] Use environment variables for all secrets
- [ ] Verify webhook signatures (already implemented)
- [ ] Use HTTPS for production webhooks
- [ ] Enable Stripe webhook signature validation
- [ ] Set up proper error monitoring (Sentry, etc.)
- [ ] Test webhook delivery in production
- [ ] Set up alerts for failed payments

### SSL/HTTPS Requirement

**CRITICAL**: Stripe webhooks in production **require HTTPS**. Make sure your production backend is served over HTTPS.

---

## Testing the Integration

### Manual Testing Checklist

#### Happy Path

- [ ] User can click "Upgrade to Premium"
- [ ] Stripe Checkout page loads correctly
- [ ] Test card payment succeeds
- [ ] User is redirected back to profile with success message
- [ ] User's subscription plan updates to SUBSCRIPTION_10
- [ ] Rate limits update to 100/hour, 500/day
- [ ] "Manage Subscription" button appears
- [ ] Clicking "Manage Subscription" opens Stripe Billing Portal
- [ ] User can cancel subscription in Billing Portal
- [ ] After cancellation, subscription shows "Cancels on [date]"
- [ ] On cancellation date, user downgrades to FREE plan

#### Error Cases

- [ ] Payment fails with declined card → User returned to profile with error
- [ ] User cancels checkout → User returned to profile with cancel message
- [ ] Duplicate webhook events are ignored (idempotency)
- [ ] Missing environment variables show clear error messages

#### Edge Cases

- [ ] User already has SUBSCRIPTION_10 → Shows "Current Plan" button
- [ ] User with FREE_BYOK plan → Can still upgrade to SUBSCRIPTION_10
- [ ] Multiple rapid clicks on "Upgrade" → Only one checkout session created
- [ ] User logs out during payment → Payment still completes, activates on login

### Webhook Testing

Use Stripe CLI to trigger test webhooks:

```bash
# Test subscription created
stripe trigger customer.subscription.created

# Test subscription updated
stripe trigger customer.subscription.updated

# Test subscription deleted (cancellation)
stripe trigger customer.subscription.deleted

# Test payment failure
stripe trigger invoice.payment_failed
```

Check your backend logs to verify webhook processing.

### Monitoring

Check these in production:

1. **Stripe Dashboard** → **Developers** → **Webhooks**
   - View webhook delivery attempts
   - Check for failed deliveries
   - Retry failed webhooks if needed

2. **Backend Logs**
   - Look for: `"Processing Stripe event: customer.subscription.*"`
   - Look for: `"Activated subscription for user X"`
   - Look for: `"Deactivated subscription for user X"`

3. **Database**
   - Check `stripe_subscriptions` table for user subscription records
   - Check `payment_events` table for webhook audit trail
   - Verify `users.subscription_plan` updates correctly

---

## Troubleshooting

### Common Issues

#### 1. "Invalid API Key" Error

**Symptom**: Backend throws exception when creating checkout session

**Solution**:
- Verify `STRIPE_SECRET_KEY` is set correctly
- Make sure key starts with `sk_test_` (test mode) or `sk_live_` (production)
- Don't use publishable key (`pk_`) - use secret key only

#### 2. Webhook Signature Validation Fails

**Symptom**: Backend returns 400 "Invalid signature" for webhooks

**Solution**:
- Verify `STRIPE_WEBHOOK_SECRET` matches Stripe CLI output or Dashboard value
- For local testing, use the secret from `stripe listen` output
- For production, use the secret from Webhook settings in Dashboard

#### 3. User Not Upgraded After Payment

**Symptom**: Payment succeeds but user still shows FREE plan

**Solution**:
- Check Stripe CLI terminal - did webhook arrive?
- Check backend logs - was webhook processed?
- Verify webhook contains `metadata.user_id` field
- Check `payment_events` table - was event logged?
- Check `stripe_subscriptions` table - is subscription record present?

#### 4. Redirect URL Not Working

**Symptom**: After payment, user sees 404 or wrong page

**Solution**:
- Verify `FRONTEND_URL` environment variable is correct
- Check Stripe Checkout session `success_url` and `cancel_url`
- Make sure frontend is running on the configured URL

#### 5. "Manage Subscription" Button Not Appearing

**Symptom**: User has SUBSCRIPTION_10 but doesn't see management button

**Solution**:
- Check `GET /api/v1/payment/subscription-status` response
- Verify `hasActiveSubscription` is `true`
- Check `stripe_subscriptions` table for user's subscription record
- Verify subscription status is "active" not "canceled"

#### 6. Database Migration Fails

**Symptom**: Application won't start, mentions "stripe_subscriptions" table

**Solution**:
- Check that `V9__Add_stripe_integration.sql` migration exists
- Verify Flyway migration table (`flyway_schema_history`)
- If needed, manually run migration or reset H2 database

### Debug Mode

Enable detailed logging:

**application.yml:**
```yaml
logging:
  level:
    ch.obermuhlner.aitutor.payment: DEBUG
    com.stripe: DEBUG
```

This will show all Stripe API calls and webhook processing details.

### Getting Help

1. **Stripe Logs**: https://dashboard.stripe.com/logs
   - See all API requests and responses
   - Webhook delivery attempts and errors

2. **Stripe Documentation**: https://stripe.com/docs
   - API reference
   - Webhook events reference
   - Common integration patterns

3. **Stripe Support**: Available in Dashboard (bottom-right chat icon)

---

## Next Steps After Setup

Once Stripe is working:

1. **Add Email Notifications**:
   - Send confirmation email after successful payment
   - Notify users before subscription renewal
   - Alert on payment failures

2. **Add Analytics**:
   - Track conversion rate (FREE → SUBSCRIPTION_10)
   - Monitor churn rate
   - Revenue metrics

3. **Add More Plans**:
   - Create additional pricing tiers
   - Implement annual billing (discount)
   - Add team/business plans

4. **Improve UX**:
   - Show payment history in profile
   - Allow changing payment method
   - Display usage statistics vs plan limits

5. **Tax Compliance**:
   - Enable Stripe Tax for automatic tax calculation
   - Configure tax rates for your regions
   - Generate tax invoices

---

## Resources

- **Stripe Dashboard**: https://dashboard.stripe.com
- **Stripe Documentation**: https://stripe.com/docs
- **Stripe Testing Guide**: https://stripe.com/docs/testing
- **Stripe CLI**: https://stripe.com/docs/stripe-cli
- **Webhook Events Reference**: https://stripe.com/docs/api/events/types
- **Checkout Session API**: https://stripe.com/docs/api/checkout/sessions
- **Billing Portal**: https://stripe.com/docs/billing/subscriptions/integrating-customer-portal

---

**Last Updated**: 2025-01-02

For questions or issues, please refer to the [Troubleshooting](#troubleshooting) section or consult Stripe's documentation.
