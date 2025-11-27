-- Update all curriculum rules to COMPLETION_BASED
UPDATE curriculum_rules
SET progression_mode = 'COMPLETION_BASED'
WHERE progression_mode = 'TIME_BASED';

-- Drop the minimum_days column from lesson_content
ALTER TABLE lesson_content DROP COLUMN IF EXISTS minimum_days;

-- Add constraint to enforce only COMPLETION_BASED
-- Note: H2 doesn't support adding CHECK constraints with ALTER TABLE
-- So we'll leave this as a soft constraint enforced by application code
