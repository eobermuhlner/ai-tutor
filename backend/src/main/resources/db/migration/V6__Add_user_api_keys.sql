-- Add API key fields to users table for BYOK (Bring Your Own Key) support
-- Allows users to provide their own LLM provider API keys

ALTER TABLE users
ADD COLUMN openai_api_key_encrypted VARCHAR(512);

ALTER TABLE users
ADD COLUMN azure_openai_api_key_encrypted VARCHAR(512);

ALTER TABLE users
ADD COLUMN azure_openai_endpoint VARCHAR(255);

ALTER TABLE users
ADD COLUMN anthropic_api_key_encrypted VARCHAR(512);

ALTER TABLE users
ADD COLUMN preferred_provider VARCHAR(32);
