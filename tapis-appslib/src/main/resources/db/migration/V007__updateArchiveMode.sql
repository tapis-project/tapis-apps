-- Update archiveMode for existing records where archiveMode is not already set
-- This should have been doing in previous upgrade
UPDATE apps_versions SET archive_mode = 'SKIP_ON_FAIL' WHERE archive_on_app_error = 'false' and archive_mode is null;
UPDATE apps_versions SET archive_mode = 'ALWAYS' WHERE archive_on_app_error = 'true' and archive_mode is null;