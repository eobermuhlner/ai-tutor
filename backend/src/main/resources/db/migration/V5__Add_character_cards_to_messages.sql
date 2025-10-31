-- Add character_cards_json column to chat_messages table
-- Character cards support teaching individual characters/symbols in special writing systems
-- (e.g., hiragana, katakana, hangul, cyrillic, kanji)

ALTER TABLE chat_messages
ADD COLUMN character_cards_json TEXT;
