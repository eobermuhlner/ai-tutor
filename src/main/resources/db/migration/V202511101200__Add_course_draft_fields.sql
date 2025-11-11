-- Add version, is_draft, published_at, last_edited_by columns to course_templates
-- This migration handles existing data properly

-- Add the new columns with default values
ALTER TABLE course_templates ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE course_templates ADD COLUMN IF NOT EXISTS is_draft BOOLEAN DEFAULT FALSE;
ALTER TABLE course_templates ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE course_templates ADD COLUMN IF NOT EXISTS last_edited_by UUID;

-- Update all existing rows to set default values
UPDATE course_templates SET version = 1 WHERE version IS NULL;
UPDATE course_templates SET is_draft = FALSE WHERE is_draft IS NULL;

-- Make the new columns non-nullable (after setting default values for existing rows)
ALTER TABLE course_templates ALTER COLUMN version SET NOT NULL;
ALTER TABLE course_templates ALTER COLUMN is_draft SET NOT NULL;