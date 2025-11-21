-- Drop corrections_status column and index (no longer needed with dedicated corrections endpoint)
DROP INDEX IF EXISTS idx_chat_messages_corrections_status;
ALTER TABLE chat_messages DROP COLUMN IF EXISTS corrections_status;
