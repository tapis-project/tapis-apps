/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.apps.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.apps.gen.jooq.Indexes;
import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsRecord;
import edu.utexas.tacc.tapis.apps.model.App.AppType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row13;
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
public class Apps extends TableImpl<AppsRecord> {

    private static final long serialVersionUID = 1315346002;

    /**
     * The reference instance of <code>tapis_app.apps</code>
     */
    public static final Apps APPS = new Apps();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AppsRecord> getRecordType() {
        return AppsRecord.class;
    }

    /**
     * The column <code>tapis_app.apps.id</code>. App id
     */
    public final TableField<AppsRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('apps_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "App id");

    /**
     * The column <code>tapis_app.apps.tenant</code>. Tenant name associated with app
     */
    public final TableField<AppsRecord, String> TENANT = createField(DSL.name("tenant"), org.jooq.impl.SQLDataType.VARCHAR(24).nullable(false), this, "Tenant name associated with app");

    /**
     * The column <code>tapis_app.apps.name</code>. Unique name for the app
     */
    public final TableField<AppsRecord, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(256).nullable(false), this, "Unique name for the app");

    /**
     * The column <code>tapis_app.apps.description</code>. App description
     */
    public final TableField<AppsRecord, String> DESCRIPTION = createField(DSL.name("description"), org.jooq.impl.SQLDataType.VARCHAR(2048), this, "App description");

    /**
     * The column <code>tapis_app.apps.app_type</code>. Type of app
     */
    public final TableField<AppsRecord, AppType> APP_TYPE = createField(DSL.name("app_type"), org.jooq.impl.SQLDataType.VARCHAR.nullable(false).asEnumDataType(edu.utexas.tacc.tapis.apps.gen.jooq.enums.AppTypeType.class), this, "Type of app", new org.jooq.impl.EnumConverter<edu.utexas.tacc.tapis.apps.gen.jooq.enums.AppTypeType, edu.utexas.tacc.tapis.apps.model.App.AppType>(edu.utexas.tacc.tapis.apps.gen.jooq.enums.AppTypeType.class, edu.utexas.tacc.tapis.apps.model.App.AppType.class));

    /**
     * The column <code>tapis_app.apps.owner</code>. User name of app owner
     */
    public final TableField<AppsRecord, String> OWNER = createField(DSL.name("owner"), org.jooq.impl.SQLDataType.VARCHAR(60).nullable(false), this, "User name of app owner");

    /**
     * The column <code>tapis_app.apps.enabled</code>. Indicates if app is currently active and available for use
     */
    public final TableField<AppsRecord, Boolean> ENABLED = createField(DSL.name("enabled"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("true", org.jooq.impl.SQLDataType.BOOLEAN)), this, "Indicates if app is currently active and available for use");

    /**
     * The column <code>tapis_app.apps.tags</code>. Tags for user supplied key:value pairs
     */
    public final TableField<AppsRecord, String[]> TAGS = createField(DSL.name("tags"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "Tags for user supplied key:value pairs");

    /**
     * The column <code>tapis_app.apps.notes</code>. Notes for general information stored as JSON
     */
    public final TableField<AppsRecord, JsonElement> NOTES = createField(DSL.name("notes"), org.jooq.impl.SQLDataType.JSONB.nullable(false), this, "Notes for general information stored as JSON", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_app.apps.import_ref_id</code>. Optional reference ID for apps created via import
     */
    public final TableField<AppsRecord, String> IMPORT_REF_ID = createField(DSL.name("import_ref_id"), org.jooq.impl.SQLDataType.VARCHAR(256), this, "Optional reference ID for apps created via import");

    /**
     * The column <code>tapis_app.apps.deleted</code>. Indicates if app has been soft deleted
     */
    public final TableField<AppsRecord, Boolean> DELETED = createField(DSL.name("deleted"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "Indicates if app has been soft deleted");

    /**
     * The column <code>tapis_app.apps.created</code>. UTC time for when record was created
     */
    public final TableField<AppsRecord, LocalDateTime> CREATED = createField(DSL.name("created"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false).defaultValue(org.jooq.impl.DSL.field("timezone('utc'::text, now())", org.jooq.impl.SQLDataType.LOCALDATETIME)), this, "UTC time for when record was created");

    /**
     * The column <code>tapis_app.apps.updated</code>. UTC time for when record was last updated
     */
    public final TableField<AppsRecord, LocalDateTime> UPDATED = createField(DSL.name("updated"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false).defaultValue(org.jooq.impl.DSL.field("timezone('utc'::text, now())", org.jooq.impl.SQLDataType.LOCALDATETIME)), this, "UTC time for when record was last updated");

    /**
     * Create a <code>tapis_app.apps</code> table reference
     */
    public Apps() {
        this(DSL.name("apps"), null);
    }

    /**
     * Create an aliased <code>tapis_app.apps</code> table reference
     */
    public Apps(String alias) {
        this(DSL.name(alias), APPS);
    }

    /**
     * Create an aliased <code>tapis_app.apps</code> table reference
     */
    public Apps(Name alias) {
        this(alias, APPS);
    }

    private Apps(Name alias, Table<AppsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Apps(Name alias, Table<AppsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> Apps(Table<O> child, ForeignKey<O, AppsRecord> key) {
        super(child, key, APPS);
    }

    @Override
    public Schema getSchema() {
        return TapisApp.TAPIS_APP;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.APP_TENANT_NAME_IDX);
    }

    @Override
    public Identity<AppsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_APPS;
    }

    @Override
    public UniqueKey<AppsRecord> getPrimaryKey() {
        return Keys.APPS_PKEY;
    }

    @Override
    public List<UniqueKey<AppsRecord>> getKeys() {
        return Arrays.<UniqueKey<AppsRecord>>asList(Keys.APPS_PKEY, Keys.APPS_TENANT_NAME_KEY);
    }

    @Override
    public Apps as(String alias) {
        return new Apps(DSL.name(alias), this);
    }

    @Override
    public Apps as(Name alias) {
        return new Apps(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Apps rename(String name) {
        return new Apps(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Apps rename(Name name) {
        return new Apps(name, null);
    }

    // -------------------------------------------------------------------------
    // Row13 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row13<Integer, String, String, String, AppType, String, Boolean, String[], JsonElement, String, Boolean, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row13) super.fieldsRow();
    }
}