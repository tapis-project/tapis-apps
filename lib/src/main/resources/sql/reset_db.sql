-- Reset DB for Tapis Applications Service by dropping and re-creating the schema
-- This prepares the DB for flyway to create the initial tables when the service is first started.
DROP SCHEMA IF EXISTS tapis_app CASCADE;
CREATE SCHEMA IF NOT EXISTS tapis_app AUTHORIZATION tapis_app;
ALTER ROLE tapis_app SET search_path = 'tapis_app';
SET search_path TO tapis_app;
