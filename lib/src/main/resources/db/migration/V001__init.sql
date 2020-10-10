-- Initial DB schema creation for Tapis Apps Service
-- postgres commands to create all tables, indices and other database artifacts required.
-- Prerequisites:
-- Create DB named tapisappdb and user named tapis_app
--   CREATE DATABASE tapisappdb ENCODING='UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';
--   CREATE USER tapis_app WITH ENCRYPTED PASSWORD '<password>'
--   GRANT ALL PRIVILEGES ON DATABASE tapisappdb TO tapis_app;
-- Fast way to check for table. Might use this at startup during an init phase.
--   SELECT to_regclass('tapis_app.apps');
--
--
-- TIMEZONE Convention
----------------------
-- All tables in this application conform to the same timezone usage rule:
--
--   All dates, times and timestamps are stored as UTC WITHOUT TIMEZONE information.
--
-- All temporal values written to the database are required to be UTC, all temporal
-- values read from the database can be assumed to be UTC.

-- NOTES for jOOQ
--   When a POJO has a default constructor (which is needed for jersey's SelectableEntityFilteringFeature)
--     then column names must match POJO attributes (with convention an_attr -> anAttr)
--     in order for jOOQ to set the attribute during Record.into()
--     Possibly another option would be to create a custom mapper to be used by Record.into()
--
-- Create the schema and set the search path
CREATE SCHEMA IF NOT EXISTS tapis_app AUTHORIZATION tapis_app;
ALTER ROLE tapis_app SET search_path = 'tapis_app';
SET search_path TO tapis_app;
-- SET search_path TO public;

-- Set permissions
-- GRANT CONNECT ON DATABASE tapisappdb TO tapis_app;
-- GRANT USAGE ON SCHEMA tapis_app TO tapis_app;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA tapis_app TO tapis_app;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA tapis_app TO tapis_app;

-- Types
CREATE TYPE app_type_type AS ENUM ('BATCH', 'INTERACTIVE');
CREATE TYPE operation_type AS ENUM ('create', 'modify', 'softDelete', 'hardDelete', 'changeOwner',
                                    'grantPerms', 'revokePerms');
CREATE TYPE capability_category_type AS ENUM ('SCHEDULER', 'OS', 'HARDWARE', 'SOFTWARE', 'JOB', 'CONTAINER', 'MISC', 'CUSTOM');

-- ----------------------------------------------------------------------------------------
--                                     APPS
-- ----------------------------------------------------------------------------------------
-- Apps table
-- Basic app attributes
CREATE TABLE apps
(
  id          SERIAL PRIMARY KEY,
  tenant      VARCHAR(24) NOT NULL,
  name        VARCHAR(256) NOT NULL,
  description VARCHAR(2048),
  app_type app_type_type NOT NULL,
  owner       VARCHAR(60) NOT NULL,
  enabled     BOOLEAN NOT NULL DEFAULT true,
  tags       TEXT[] NOT NULL,
  notes      JSONB NOT NULL,
  import_ref_id VARCHAR(256),
  deleted    BOOLEAN NOT NULL DEFAULT false,
  created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  UNIQUE (tenant,name)
);
ALTER TABLE apps OWNER TO tapis;
CREATE INDEX app_tenant_name_idx ON apps (tenant, name);
COMMENT ON COLUMN apps.id IS 'App id';
COMMENT ON COLUMN apps.tenant IS 'Tenant name associated with app';
COMMENT ON COLUMN apps.name IS 'Unique name for the app';
COMMENT ON COLUMN apps.description IS 'App description';
COMMENT ON COLUMN apps.app_type IS 'Type of app';
COMMENT ON COLUMN apps.owner IS 'User name of app owner';
COMMENT ON COLUMN apps.enabled IS 'Indicates if app is currently active and available for use';
COMMENT ON COLUMN apps.tags IS 'Tags for user supplied key:value pairs';
COMMENT ON COLUMN apps.notes IS 'Notes for general information stored as JSON';
COMMENT ON COLUMN apps.import_ref_id IS 'Optional reference ID for apps created via import';
COMMENT ON COLUMN apps.deleted IS 'Indicates if app has been soft deleted';
COMMENT ON COLUMN apps.created IS 'UTC time for when record was created';
COMMENT ON COLUMN apps.updated IS 'UTC time for when record was last updated';

-- App updates table
-- Track update requests for apps
CREATE TABLE app_updates
(
    id SERIAL PRIMARY KEY,
    app_id SERIAL REFERENCES apps(id) ON DELETE CASCADE,
    user_name VARCHAR(60) NOT NULL,
    user_tenant VARCHAR(24) NOT NULL,
    operation operation_type NOT NULL,
    upd_json JSONB NOT NULL,
    upd_text VARCHAR,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE app_updates OWNER TO tapis;
COMMENT ON COLUMN app_updates.id IS 'App update request id';
COMMENT ON COLUMN app_updates.app_id IS 'Id of app being updated';
COMMENT ON COLUMN app_updates.user_name IS 'Name of user who requested the update';
COMMENT ON COLUMN app_updates.user_tenant IS 'Tenant of user who requested the update';
COMMENT ON COLUMN app_updates.operation IS 'Type of update operation';
COMMENT ON COLUMN app_updates.upd_json IS 'JSON representing the update - with secrets scrubbed';
COMMENT ON COLUMN app_updates.upd_text IS 'Text data supplied by client - secrets should be scrubbed';
COMMENT ON COLUMN app_updates.created IS 'UTC time for when record was created';

-- ----------------------------------------------------------------------------------------
--                               CAPABILITIES
-- ----------------------------------------------------------------------------------------
-- Capabilities table
-- Capabilities associated with an app
-- All columns are specified NOT NULL to make queries easier. <col> = null is not the same as <col> is null
CREATE TABLE capabilities
(
    id     SERIAL PRIMARY KEY,
    app_id SERIAL REFERENCES apps(id) ON DELETE CASCADE,
    category capability_category_type NOT NULL,
    name   VARCHAR(256) NOT NULL DEFAULT '',
    value  VARCHAR(256) NOT NULL DEFAULT '',
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (app_id, category, name)
);
ALTER TABLE capabilities OWNER TO tapis;
COMMENT ON COLUMN capabilities.id IS 'Capability id';
COMMENT ON COLUMN capabilities.app_id IS 'Id of app supporting the capability';
COMMENT ON COLUMN capabilities.category IS 'Category for grouping of capabilities';
COMMENT ON COLUMN capabilities.name IS 'Name of capability';
COMMENT ON COLUMN capabilities.value IS 'Value for the capability';
COMMENT ON COLUMN capabilities.created IS 'UTC time for when record was created';
COMMENT ON COLUMN capabilities.updated IS 'UTC time for when record was last updated';

-- ******************************************************************************
--                         PROCEDURES and TRIGGERS
-- ******************************************************************************

-- Auto update of updated column
CREATE OR REPLACE FUNCTION trigger_set_updated() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER app_updated
  BEFORE UPDATE ON apps
  EXECUTE PROCEDURE trigger_set_updated();
CREATE TRIGGER capability_updated
    BEFORE UPDATE ON capabilities
EXECUTE PROCEDURE trigger_set_updated();
