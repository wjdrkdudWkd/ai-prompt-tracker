-- V4: Add was_truncated column to calls table
-- Tracks whether response body was truncated due to size limits

ALTER TABLE calls
ADD COLUMN was_truncated BOOLEAN DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN calls.was_truncated IS 'Whether the response was truncated due to size limits (maxResponseBytes or maxInMemoryBytes)';
