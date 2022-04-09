-- Initial DB schema creation for Tapis Apps Service
-- postgres commands to create all tables, indices and other database artifacts required.
-- Prerequisites:
-- Create DB named tapisappdb and user named tapis_app
--   CREATE DATABASE tapisappdb ENCODING='UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';
--   CREATE USER tapis_app WITH ENCRYPTED PASSWORD '<password>'
--   GRANT ALL PRIVILEGES ON DATABASE tapisappdb TO tapis_app;
-- Fast way to check for table:
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

-- Set permissions
-- GRANT CONNECT ON DATABASE tapisappdb TO tapis_app;
-- GRANT USAGE ON SCHEMA tapis_app TO tapis_app;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA tapis_app TO tapis_app;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA tapis_app TO tapis_app;

-- ----------------------------------------------------------------------------------------
--                                     APPS
-- ----------------------------------------------------------------------------------------
-- Apps table
-- Top level table containing version independent attributes
CREATE TABLE apps
(
  seq_id  SERIAL PRIMARY KEY,
  tenant  TEXT NOT NULL,
  id      TEXT NOT NULL,
  latest_version TEXT NOT NULL,
  owner    TEXT NOT NULL,
  enabled  BOOLEAN NOT NULL DEFAULT true,
  containerized BOOLEAN NOT NULL DEFAULT true,
  deleted    BOOLEAN NOT NULL DEFAULT false,
  created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  UNIQUE (tenant,id)
);
ALTER TABLE apps OWNER TO tapis_app;
CREATE INDEX app_tenant_id_idx ON apps (tenant, id);
COMMENT ON COLUMN apps.seq_id IS 'Application sequence id';
COMMENT ON COLUMN apps.tenant IS 'Tenant name associated with the application';
COMMENT ON COLUMN apps.id IS 'Unique name for the application';
COMMENT ON COLUMN apps.owner IS 'User name of application owner';
COMMENT ON COLUMN apps.enabled IS 'Indicates if application is currently active and available for use';
COMMENT ON COLUMN apps.deleted IS 'Indicates if application has been soft deleted';
COMMENT ON COLUMN apps.created IS 'UTC time for when record was created';
COMMENT ON COLUMN apps.updated IS 'UTC time for when record was last updated';

-- App versions table
-- Basic app attributes that can vary by version
CREATE TABLE apps_versions
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id INTEGER REFERENCES apps(seq_id) ON DELETE CASCADE,
    tenant  TEXT NOT NULL,
    id      TEXT NOT NULL,
    version TEXT NOT NULL,
    description TEXT,
    runtime TEXT NOT NULL,
    runtime_version TEXT,
    runtime_options TEXT[],
    container_image TEXT,
    job_type TEXT,
    max_jobs INTEGER NOT NULL DEFAULT -1,
    max_jobs_per_user INTEGER NOT NULL DEFAULT -1,
    strict_file_inputs BOOLEAN NOT NULL DEFAULT false,
-- ==== Start jobAttributes ======================================
    job_description TEXT,
    dynamic_exec_system BOOLEAN NOT NULL DEFAULT false,
    exec_system_constraints TEXT[],
    exec_system_id TEXT,
    exec_system_exec_dir TEXT,
    exec_system_input_dir TEXT,
    exec_system_output_dir TEXT,
    exec_system_logical_queue TEXT,
    archive_system_id TEXT,
    archive_system_dir TEXT,
    archive_on_app_error BOOLEAN NOT NULL DEFAULT true,
    is_mpi BOOLEAN NOT NULL DEFAULT false,
    mpi_cmd TEXT,
    cmd_prefix TEXT,
    parameter_set JSONB NOT NULL,
    file_inputs JSONB NOT NULL,
    file_input_arrays JSONB NOT NULL,
    node_count INTEGER NOT NULL DEFAULT 1,
    cores_per_node INTEGER NOT NULL DEFAULT 1,
    memory_mb INTEGER NOT NULL DEFAULT 100,
    max_minutes INTEGER NOT NULL DEFAULT 10,
    subscriptions JSONB NOT NULL,
    job_tags TEXT[] NOT NULL,
-- ==== End jobAttributes ======================================
    tags       TEXT[] NOT NULL,
    notes      JSONB NOT NULL,
    uuid uuid NOT NULL,
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    UNIQUE (app_seq_id,version)
);
ALTER TABLE apps_versions OWNER TO tapis_app;
CREATE INDEX app_version_seqid_idx ON apps_versions (version, app_seq_id);
COMMENT ON COLUMN apps_versions.seq_id IS 'Sequence id for specific version of application';
COMMENT ON COLUMN apps_versions.app_seq_id IS 'Sequence id of application';
COMMENT ON COLUMN apps_versions.version IS 'Application version';
COMMENT ON COLUMN apps_versions.description IS 'Application description';
COMMENT ON COLUMN apps_versions.tags IS 'Tags for user supplied key:value pairs';
COMMENT ON COLUMN apps_versions.notes IS 'Notes for general information stored as JSON';
COMMENT ON COLUMN apps_versions.created IS 'UTC time for when record was created';
COMMENT ON COLUMN apps_versions.updated IS 'UTC time for when record was last updated';

-- App updates table
-- Track update requests for apps
CREATE TABLE app_updates
(
    seq_id SERIAL PRIMARY KEY,
    app_seq_id INTEGER REFERENCES apps(seq_id) ON DELETE CASCADE,
    app_ver_seq_id INTEGER,
    obo_tenant TEXT NOT NULL,
    obo_user TEXT NOT NULL,
    jwt_tenant TEXT NOT NULL,
    jwt_user TEXT NOT NULL,
    app_id TEXT NOT NULL,
    app_version TEXT,
    operation TEXT NOT NULL,
    description JSONB NOT NULL,
    raw_data TEXT,
    uuid uuid NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
ALTER TABLE app_updates OWNER TO tapis_app;
COMMENT ON COLUMN app_updates.seq_id IS 'Application update request id';
COMMENT ON COLUMN app_updates.app_seq_id IS 'Sequence id of application being updated';
COMMENT ON COLUMN app_updates.app_ver_seq_id IS 'Sequence id of application version being updated';
COMMENT ON COLUMN app_updates.obo_tenant IS 'OBO Tenant associated with the change request';
COMMENT ON COLUMN app_updates.obo_user IS 'OBO User associated with the change request';
COMMENT ON COLUMN app_updates.jwt_tenant IS 'Tenant of user who requested the update';
COMMENT ON COLUMN app_updates.jwt_user IS 'Name of user who requested the update';
COMMENT ON COLUMN app_updates.app_id IS 'Id of application being updated';
COMMENT ON COLUMN app_updates.app_version IS 'Version of application being updated';
COMMENT ON COLUMN app_updates.operation IS 'Type of update operation';
COMMENT ON COLUMN app_updates.description IS 'JSON describing the change. Secrets scrubbed as needed.';
COMMENT ON COLUMN app_updates.raw_data IS 'Raw data associated with the request, if available. Secrets scrubbed as needed.';
COMMENT ON COLUMN app_updates.uuid IS 'UUID of app being updated';
COMMENT ON COLUMN app_updates.created IS 'UTC time for when record was created';
