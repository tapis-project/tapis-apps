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
CREATE TYPE app_type_type AS ENUM ('BATCH', 'DIRECT', 'BATCH_INTERACTIVE', 'DIRECT_INTERACTIVE');
CREATE TYPE operation_type AS ENUM ('create', 'modify', 'softDelete', 'hardDelete', 'changeOwner',
                                    'grantPerms', 'revokePerms');
CREATE TYPE container_runtime_type AS ENUM ('DOCKER', 'SINGULARITY');

-- ----------------------------------------------------------------------------------------
--                                     APPS
-- ----------------------------------------------------------------------------------------
-- Apps table
-- Basic app attributes
CREATE TABLE apps
(
  seq_id          SERIAL PRIMARY KEY,
  tenant      VARCHAR(24) NOT NULL,
  id        VARCHAR(80) NOT NULL,
  version     VARCHAR(64) NOT NULL,
  description VARCHAR(2048),
  app_type app_type_type NOT NULL,
  owner       VARCHAR(60) NOT NULL,
  enabled     BOOLEAN NOT NULL DEFAULT true,
  containerized BOOLEAN NOT NULL DEFAULT true,
  container_runtime container_runtime_type NOT NULL,
  container_runtime_version VARCHAR(128),
  container_image VARCHAR(128),
--  command VARCHAR(128),
--  exec_codes TEXT[] NOT NULL, -- TODO these will be FileInputs? need type in FileInputs table?
  max_jobs INTEGER NOT NULL DEFAULT -1,
  max_jobs_per_user INTEGER NOT NULL DEFAULT -1,
-- Start jobAttributes ==========================================
  job_description VARCHAR(2048),
  dynamic_exec_system BOOLEAN NOT NULL DEFAULT false,
  exec_system_constraints TEXT[] NOT NULL,
  exec_system_id VARCHAR(80) NOT NULL,
  exec_system_exec_dir VARCHAR(4096),
  exec_system_input_dir VARCHAR(4096),
  exec_system_output_dir VARCHAR(4096),
  exec_system_logical_queue VARCHAR(128),
  archive_system_id VARCHAR(80),
  archive_system_dir VARCHAR(4096),
  archive_on_app_error BOOLEAN NOT NULL DEFAULT true,
--   parameterSet location in jobAttributes ===================
--   parameterSet attributes flattened into this table ========
  env_variables TEXT[] NOT NULL,
  archive_includes TEXT[] NOT NULL,
  archive_excludes TEXT[] NOT NULL,
--   fileInputs location in jobAttributes =====================
  node_count INTEGER NOT NULL DEFAULT -1,
  cores_per_node INTEGER NOT NULL DEFAULT -1,
  memory_mb INTEGER NOT NULL DEFAULT -1,
  max_minutes INTEGER NOT NULL DEFAULT -1,
--   subscriptions location in jobAttributes ==================
  job_tags TEXT[] NOT NULL,
-- End jobAttributes ==========================================
  tags       TEXT[] NOT NULL,
  notes      JSONB NOT NULL,
  import_ref_id VARCHAR(80),
  deleted    BOOLEAN NOT NULL DEFAULT false,
  created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  UNIQUE (tenant,id,version)
);
ALTER TABLE apps OWNER TO tapis_app;
CREATE INDEX app_tenant_name_idx ON apps (tenant, id);
COMMENT ON COLUMN apps.seq_id IS 'Application sequence id';
COMMENT ON COLUMN apps.tenant IS 'Tenant name associated with the application';
COMMENT ON COLUMN apps.id IS 'Unique name for the application';
COMMENT ON COLUMN apps.version IS 'Application version';
COMMENT ON COLUMN apps.description IS 'Application description';
COMMENT ON COLUMN apps.app_type IS 'Type of application';
COMMENT ON COLUMN apps.owner IS 'User name of application owner';
COMMENT ON COLUMN apps.enabled IS 'Indicates if application is currently active and available for use';
COMMENT ON COLUMN apps.tags IS 'Tags for user supplied key:value pairs';
COMMENT ON COLUMN apps.notes IS 'Notes for general information stored as JSON';
COMMENT ON COLUMN apps.import_ref_id IS 'Optional reference ID for applications created via import';
COMMENT ON COLUMN apps.deleted IS 'Indicates if application has been soft deleted';
COMMENT ON COLUMN apps.created IS 'UTC time for when record was created';
COMMENT ON COLUMN apps.updated IS 'UTC time for when record was last updated';

-- App updates table
-- Track update requests for apps
CREATE TABLE app_updates
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    user_name VARCHAR(60) NOT NULL,
    user_tenant VARCHAR(24) NOT NULL,
    operation operation_type NOT NULL,
    upd_json JSONB NOT NULL,
    upd_text VARCHAR,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE app_updates OWNER TO tapis_app;
COMMENT ON COLUMN app_updates.seq_id IS 'Application update request id';
COMMENT ON COLUMN app_updates.app_seq_id IS 'Sequence id of application being updated';
COMMENT ON COLUMN app_updates.user_name IS 'Name of user who requested the update';
COMMENT ON COLUMN app_updates.user_tenant IS 'Tenant of user who requested the update';
COMMENT ON COLUMN app_updates.operation IS 'Type of update operation';
COMMENT ON COLUMN app_updates.upd_json IS 'JSON representing the update - with secrets scrubbed';
COMMENT ON COLUMN app_updates.upd_text IS 'Text data supplied by client - secrets should be scrubbed';
COMMENT ON COLUMN app_updates.created IS 'UTC time for when record was created';

-- ----------------------------------------------------------------------------------------
--                           FILE INPUTS
-- ----------------------------------------------------------------------------------------
-- TODO 2 tables? one for file inputs and one for exec codes?
-- File Inputs table
-- Inputs associated with an app
-- All columns are specified NOT NULL to make queries easier. <col> = null is not the same as <col> is null
CREATE TABLE file_inputs
(
    seq_id     SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    source_url VARCHAR(128) NOT NULL DEFAULT '',
    target_path VARCHAR(128) NOT NULL DEFAULT '',
    in_place BOOLEAN NOT NULL DEFAULT false,
    meta_name VARCHAR(128) NOT NULL DEFAULT '',
    meta_description VARCHAR(128) NOT NULL DEFAULT '',
    meta_required boolean NOT NULL DEFAULT true,
    meta_kv TEXT[] NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (app_seq_id, source_url, target_path)
);
ALTER TABLE file_inputs OWNER TO tapis_app;
COMMENT ON COLUMN file_inputs.seq_id IS 'File input sequence id';
COMMENT ON COLUMN file_inputs.app_seq_id IS 'Sequence id of application requiring the file input';

-- ----------------------------------------------------------------------------------------
--                           SUBSCRIPTIONS
-- ----------------------------------------------------------------------------------------
-- Notification subscriptions table
CREATE TABLE notification_subscriptions
(
    seq_id     SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    filter VARCHAR(128) NOT NULL DEFAULT '', -- TODO length?
    -- TODO
--     target_path VARCHAR(128) NOT NULL DEFAULT '',
--     in_place BOOLEAN NOT NULL DEFAULT false,
--     meta_name VARCHAR(128) NOT NULL DEFAULT '',
--     meta_description VARCHAR(128) NOT NULL DEFAULT '',
--     meta_required boolean NOT NULL DEFAULT true,
--     meta_kv TEXT[] NOT NULL,
--     created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
--     updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (app_seq_id)
);
ALTER TABLE file_inputs OWNER TO tapis_app;
COMMENT ON COLUMN file_inputs.seq_id IS 'File input sequence id';
COMMENT ON COLUMN file_inputs.app_seq_id IS 'Sequence id of application requiring the file input';

-- ----------------------------------------------------------------------------------------
--                           ARGS
-- ----------------------------------------------------------------------------------------
-- TODO could have one table with an arg_type column?
-- Container args table
-- Container arguments associated with an app
-- All columns are specified NOT NULL to make queries easier. <col> = null is not the same as <col> is null
CREATE TABLE container_args
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    arg_val VARCHAR(128) NOT NULL DEFAULT '',
    meta_name VARCHAR(128) NOT NULL DEFAULT '',
    meta_description VARCHAR(128) NOT NULL DEFAULT '',
    meta_required boolean NOT NULL DEFAULT true,
    meta_kv TEXT[] NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE container_args OWNER TO tapis_app;
COMMENT ON COLUMN container_args.seq_id IS 'Arg sequence id';
COMMENT ON COLUMN container_args.app_seq_id IS 'Sequence id of application';

-- App args table
-- App arguments associated with an app
-- All columns are specified NOT NULL to make queries easier. <col> = null is not the same as <col> is null
CREATE TABLE app_args
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    arg_val VARCHAR(128) NOT NULL DEFAULT '',
    meta_name VARCHAR(128) NOT NULL DEFAULT '',
    meta_description VARCHAR(128) NOT NULL DEFAULT '',
    meta_required boolean NOT NULL DEFAULT true,
    meta_kv TEXT[] NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE app_args OWNER TO tapis_app;
COMMENT ON COLUMN app_args.seq_id IS 'Arg sequence id';
COMMENT ON COLUMN app_args.app_seq_id IS 'Sequence id of application';

-- Scheduler options table
-- Scheduler options associated with an app
-- All columns are specified NOT NULL to make queries easier. <col> = null is not the same as <col> is null
CREATE TABLE scheduler_options
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id SERIAL REFERENCES apps(seq_id) ON DELETE CASCADE,
    arg_val VARCHAR(128) NOT NULL DEFAULT '',
    meta_name VARCHAR(128) NOT NULL DEFAULT '',
    meta_description VARCHAR(128) NOT NULL DEFAULT '',
    meta_required boolean NOT NULL DEFAULT true,
    meta_kv TEXT[] NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE scheduler_options OWNER TO tapis_app;
COMMENT ON COLUMN scheduler_options.seq_id IS 'Arg sequence id';
COMMENT ON COLUMN scheduler_options.app_seq_id IS 'Sequence id of application';

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
CREATE TRIGGER file_inputs_updated
    BEFORE UPDATE ON file_inputs
EXECUTE PROCEDURE trigger_set_updated();
CREATE TRIGGER container_args_updated
    BEFORE UPDATE ON container_args
EXECUTE PROCEDURE trigger_set_updated();
CREATE TRIGGER app_args_updated
    BEFORE UPDATE ON app_args
EXECUTE PROCEDURE trigger_set_updated();
CREATE TRIGGER scheduler_options_updated
    BEFORE UPDATE ON scheduler_options
EXECUTE PROCEDURE trigger_set_updated();