-- Add subscription_plan column to users table
ALTER TABLE users
ADD COLUMN subscription_plan VARCHAR(32) NOT NULL DEFAULT 'FREE';

-- Add index for efficient querying by subscription plan
CREATE INDEX idx_users_subscription_plan ON users(subscription_plan);

-- Update existing users with BYOK to FREE_BYOK plan
UPDATE users
SET subscription_plan = 'FREE_BYOK'
WHERE openai_api_key_encrypted IS NOT NULL
   OR azure_openai_api_key_encrypted IS NOT NULL
   OR anthropic_api_key_encrypted IS NOT NULL;
