-- Reset DB for Tapis Applications Service by dropping and re-creating the schema
DROP SCHEMA IF EXISTS tapis_app CASCADE;
CREATE SCHEMA IF NOT EXISTS tapis_app AUTHORIZATION tapis;
ALTER ROLE tapis SET search_path = 'tapis_app';
SET search_path TO tapis_app;
