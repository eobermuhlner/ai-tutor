-- Add age field to tutor_profiles table
ALTER TABLE tutor_profiles ADD COLUMN age INT NOT NULL DEFAULT 30;

-- Add tutorAge field to chat_sessions table
ALTER TABLE chat_sessions ADD COLUMN tutor_age INT NOT NULL DEFAULT 30;
