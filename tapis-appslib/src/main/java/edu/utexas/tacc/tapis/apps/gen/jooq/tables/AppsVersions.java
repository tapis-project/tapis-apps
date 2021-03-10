/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.apps.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.apps.gen.jooq.Indexes;
import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsVersionsRecord;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AppsVersions extends TableImpl<AppsVersionsRecord> {

    private static final long serialVersionUID = -1453030990;

    /**
     * The reference instance of <code>tapis_app.apps_versions</code>
     */
    public static final AppsVersions APPS_VERSIONS = new AppsVersions();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AppsVersionsRecord> getRecordType() {
        return AppsVersionsRecord.class;
    }

    /**
     * The column <code>tapis_app.apps_versions.seq_id</code>. Sequence id for specific version of application
     */
    public final TableField<AppsVersionsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('apps_versions_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "Sequence id for specific version of application");

    /**
     * The column <code>tapis_app.apps_versions.app_seq_id</code>. Sequence id of application
     */
    public final TableField<AppsVersionsRecord, Integer> APP_SEQ_ID = createField(DSL.name("app_seq_id"), org.jooq.impl.SQLDataType.INTEGER, this, "Sequence id of application");

    /**
     * The column <code>tapis_app.apps_versions.version</code>. Application version
     */
    public final TableField<AppsVersionsRecord, String> VERSION = createField(DSL.name("version"), org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "Application version");

    /**
     * The column <code>tapis_app.apps_versions.description</code>. Application description
     */
    public final TableField<AppsVersionsRecord, String> DESCRIPTION = createField(DSL.name("description"), org.jooq.impl.SQLDataType.CLOB, this, "Application description");

    /**
     * The column <code>tapis_app.apps_versions.runtime</code>.
     */
    public final TableField<AppsVersionsRecord, Runtime> RUNTIME = createField(DSL.name("runtime"), org.jooq.impl.SQLDataType.VARCHAR.nullable(false).asEnumDataType(edu.utexas.tacc.tapis.apps.gen.jooq.enums.RuntimeType.class), this, "", new org.jooq.impl.EnumConverter<edu.utexas.tacc.tapis.apps.gen.jooq.enums.RuntimeType, edu.utexas.tacc.tapis.apps.model.App.Runtime>(edu.utexas.tacc.tapis.apps.gen.jooq.enums.RuntimeType.class, edu.utexas.tacc.tapis.apps.model.App.Runtime.class));

    /**
     * The column <code>tapis_app.apps_versions.runtime_version</code>.
     */
    public final TableField<AppsVersionsRecord, String> RUNTIME_VERSION = createField(DSL.name("runtime_version"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.container_image</code>.
     */
    public final TableField<AppsVersionsRecord, String> CONTAINER_IMAGE = createField(DSL.name("container_image"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.max_jobs</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> MAX_JOBS = createField(DSL.name("max_jobs"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.max_jobs_per_user</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> MAX_JOBS_PER_USER = createField(DSL.name("max_jobs_per_user"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.strict_file_inputs</code>.
     */
    public final TableField<AppsVersionsRecord, Boolean> STRICT_FILE_INPUTS = createField(DSL.name("strict_file_inputs"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.job_description</code>.
     */
    public final TableField<AppsVersionsRecord, String> JOB_DESCRIPTION = createField(DSL.name("job_description"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.dynamic_exec_system</code>.
     */
    public final TableField<AppsVersionsRecord, Boolean> DYNAMIC_EXEC_SYSTEM = createField(DSL.name("dynamic_exec_system"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_constraints</code>.
     */
    public final TableField<AppsVersionsRecord, String[]> EXEC_SYSTEM_CONSTRAINTS = createField(DSL.name("exec_system_constraints"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_id</code>.
     */
    public final TableField<AppsVersionsRecord, String> EXEC_SYSTEM_ID = createField(DSL.name("exec_system_id"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_exec_dir</code>.
     */
    public final TableField<AppsVersionsRecord, String> EXEC_SYSTEM_EXEC_DIR = createField(DSL.name("exec_system_exec_dir"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_input_dir</code>.
     */
    public final TableField<AppsVersionsRecord, String> EXEC_SYSTEM_INPUT_DIR = createField(DSL.name("exec_system_input_dir"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_output_dir</code>.
     */
    public final TableField<AppsVersionsRecord, String> EXEC_SYSTEM_OUTPUT_DIR = createField(DSL.name("exec_system_output_dir"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.exec_system_logical_queue</code>.
     */
    public final TableField<AppsVersionsRecord, String> EXEC_SYSTEM_LOGICAL_QUEUE = createField(DSL.name("exec_system_logical_queue"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.archive_system_id</code>.
     */
    public final TableField<AppsVersionsRecord, String> ARCHIVE_SYSTEM_ID = createField(DSL.name("archive_system_id"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.archive_system_dir</code>.
     */
    public final TableField<AppsVersionsRecord, String> ARCHIVE_SYSTEM_DIR = createField(DSL.name("archive_system_dir"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.apps_versions.archive_on_app_error</code>.
     */
    public final TableField<AppsVersionsRecord, Boolean> ARCHIVE_ON_APP_ERROR = createField(DSL.name("archive_on_app_error"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("true", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.env_variables</code>.
     */
    public final TableField<AppsVersionsRecord, String[]> ENV_VARIABLES = createField(DSL.name("env_variables"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * The column <code>tapis_app.apps_versions.archive_includes</code>.
     */
    public final TableField<AppsVersionsRecord, String[]> ARCHIVE_INCLUDES = createField(DSL.name("archive_includes"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * The column <code>tapis_app.apps_versions.archive_excludes</code>.
     */
    public final TableField<AppsVersionsRecord, String[]> ARCHIVE_EXCLUDES = createField(DSL.name("archive_excludes"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * The column <code>tapis_app.apps_versions.node_count</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> NODE_COUNT = createField(DSL.name("node_count"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.cores_per_node</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> CORES_PER_NODE = createField(DSL.name("cores_per_node"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.memory_mb</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> MEMORY_MB = createField(DSL.name("memory_mb"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.max_minutes</code>.
     */
    public final TableField<AppsVersionsRecord, Integer> MAX_MINUTES = createField(DSL.name("max_minutes"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("'-1'::integer", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.apps_versions.job_tags</code>.
     */
    public final TableField<AppsVersionsRecord, String[]> JOB_TAGS = createField(DSL.name("job_tags"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * The column <code>tapis_app.apps_versions.tags</code>. Tags for user supplied key:value pairs
     */
    public final TableField<AppsVersionsRecord, String[]> TAGS = createField(DSL.name("tags"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "Tags for user supplied key:value pairs");

    /**
     * The column <code>tapis_app.apps_versions.notes</code>. Notes for general information stored as JSON
     */
    public final TableField<AppsVersionsRecord, JsonElement> NOTES = createField(DSL.name("notes"), org.jooq.impl.SQLDataType.JSONB.nullable(false), this, "Notes for general information stored as JSON", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_app.apps_versions.uuid</code>.
     */
    public final TableField<AppsVersionsRecord, UUID> UUID = createField(DSL.name("uuid"), org.jooq.impl.SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>tapis_app.apps_versions.created</code>. UTC time for when record was created
     */
    public final TableField<AppsVersionsRecord, LocalDateTime> CREATED = createField(DSL.name("created"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false).defaultValue(org.jooq.impl.DSL.field("timezone('utc'::text, now())", org.jooq.impl.SQLDataType.LOCALDATETIME)), this, "UTC time for when record was created");

    /**
     * The column <code>tapis_app.apps_versions.updated</code>. UTC time for when record was last updated
     */
    public final TableField<AppsVersionsRecord, LocalDateTime> UPDATED = createField(DSL.name("updated"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false).defaultValue(org.jooq.impl.DSL.field("timezone('utc'::text, now())", org.jooq.impl.SQLDataType.LOCALDATETIME)), this, "UTC time for when record was last updated");

    /**
     * Create a <code>tapis_app.apps_versions</code> table reference
     */
    public AppsVersions() {
        this(DSL.name("apps_versions"), null);
    }

    /**
     * Create an aliased <code>tapis_app.apps_versions</code> table reference
     */
    public AppsVersions(String alias) {
        this(DSL.name(alias), APPS_VERSIONS);
    }

    /**
     * Create an aliased <code>tapis_app.apps_versions</code> table reference
     */
    public AppsVersions(Name alias) {
        this(alias, APPS_VERSIONS);
    }

    private AppsVersions(Name alias, Table<AppsVersionsRecord> aliased) {
        this(alias, aliased, null);
    }

    private AppsVersions(Name alias, Table<AppsVersionsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> AppsVersions(Table<O> child, ForeignKey<O, AppsVersionsRecord> key) {
        super(child, key, APPS_VERSIONS);
    }

    @Override
    public Schema getSchema() {
        return TapisApp.TAPIS_APP;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.APP_VERSION_SEQID_IDX);
    }

    @Override
    public Identity<AppsVersionsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_APPS_VERSIONS;
    }

    @Override
    public UniqueKey<AppsVersionsRecord> getPrimaryKey() {
        return Keys.APPS_VERSIONS_PKEY;
    }

    @Override
    public List<UniqueKey<AppsVersionsRecord>> getKeys() {
        return Arrays.<UniqueKey<AppsVersionsRecord>>asList(Keys.APPS_VERSIONS_PKEY, Keys.APPS_VERSIONS_APP_SEQ_ID_VERSION_KEY);
    }

    @Override
    public List<ForeignKey<AppsVersionsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<AppsVersionsRecord, ?>>asList(Keys.APPS_VERSIONS__APPS_VERSIONS_APP_SEQ_ID_FKEY);
    }

    public Apps apps() {
        return new Apps(this, Keys.APPS_VERSIONS__APPS_VERSIONS_APP_SEQ_ID_FKEY);
    }

    @Override
    public AppsVersions as(String alias) {
        return new AppsVersions(DSL.name(alias), this);
    }

    @Override
    public AppsVersions as(Name alias) {
        return new AppsVersions(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AppsVersions rename(String name) {
        return new AppsVersions(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AppsVersions rename(Name name) {
        return new AppsVersions(name, null);
    }
}
