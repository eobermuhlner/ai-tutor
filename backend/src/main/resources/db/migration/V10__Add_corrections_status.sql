-- Add corrections_status column to track async correction generation
ALTER TABLE chat_messages ADD COLUMN corrections_status VARCHAR(16);

-- Create index for querying messages with pending corrections
CREATE INDEX idx_chat_messages_corrections_status ON chat_messages(corrections_status);
