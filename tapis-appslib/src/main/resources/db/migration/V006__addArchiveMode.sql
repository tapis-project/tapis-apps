-- Add JobAttributes archiveMode attribute to support skipping ARCHIVING phase. Represented in code as an enum.
-- Values are: ALWAYS, SKIP_ON_FAIL, NEVER
ALTER TABLE apps_versions ADD COLUMN IF NOT EXISTS archive_mode TEXT;
