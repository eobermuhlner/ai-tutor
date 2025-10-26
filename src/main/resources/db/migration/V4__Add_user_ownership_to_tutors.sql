-- Add user ownership tracking to tutor_profiles table
-- This enables both global tutors (seed data) and user-specific custom tutors

-- Add created_by_user_id column (nullable - null means global/seed tutor)
ALTER TABLE tutor_profiles ADD COLUMN created_by_user_id UUID;

-- Add is_global flag as nullable first (to handle existing data)
ALTER TABLE tutor_profiles ADD COLUMN is_global BOOLEAN;

-- Set all existing tutors to global (seed data tutors)
UPDATE tutor_profiles SET is_global = TRUE WHERE is_global IS NULL;

-- Now make the column NOT NULL with default
ALTER TABLE tutor_profiles ALTER COLUMN is_global SET NOT NULL;
ALTER TABLE tutor_profiles ALTER COLUMN is_global SET DEFAULT TRUE;

-- Add foreign key constraint to users table
ALTER TABLE tutor_profiles ADD CONSTRAINT fk_tutor_profiles_created_by_user
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Add index for efficient filtering by creator
CREATE INDEX idx_tutor_profiles_created_by_user_id ON tutor_profiles(created_by_user_id);

-- Add composite index for common query pattern (language + active + global/owned)
CREATE INDEX idx_tutor_profiles_language_active_global ON tutor_profiles(target_language_code, is_active, is_global);
