-- Add new columns for lesson progress with default values
ALTER TABLE chat_sessions ADD COLUMN lesson_progress_turn_count INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE chat_sessions ADD COLUMN lesson_progress_goals_completed BOOLEAN DEFAULT FALSE NOT NULL;

-- Drop the old JSON column (data will be defaulted for existing records)
ALTER TABLE chat_sessions DROP COLUMN lesson_progress_json;