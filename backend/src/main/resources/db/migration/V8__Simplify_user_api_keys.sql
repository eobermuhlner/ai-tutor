-- Simplify API key schema from provider-specific fields to generic fields
-- Supports single active LLM provider configured system-wide

-- Drop provider-specific columns
ALTER TABLE users
DROP COLUMN openai_api_key_encrypted;

ALTER TABLE users
DROP COLUMN azure_openai_api_key_encrypted;

ALTER TABLE users
DROP COLUMN azure_openai_endpoint;

ALTER TABLE users
DROP COLUMN anthropic_api_key_encrypted;

ALTER TABLE users
DROP COLUMN preferred_provider;

-- Add generic API key columns
ALTER TABLE users
ADD COLUMN api_key_encrypted VARCHAR(512);

ALTER TABLE users
ADD COLUMN endpoint VARCHAR(255);
