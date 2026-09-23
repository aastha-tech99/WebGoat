-- Add optimistic locking version column to user_progress for safe concurrent updates.
ALTER TABLE CONTAINER.user_progress ADD COLUMN version INTEGER;
