/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.apps.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppUpdatesRecord;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function14;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row14;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.EnumConverter;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AppUpdates extends TableImpl<AppUpdatesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_app.app_updates</code>
     */
    public static final AppUpdates APP_UPDATES = new AppUpdates();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AppUpdatesRecord> getRecordType() {
        return AppUpdatesRecord.class;
    }

    /**
     * The column <code>tapis_app.app_updates.seq_id</code>. Application update
     * request id
     */
    public final TableField<AppUpdatesRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "Application update request id");

    /**
     * The column <code>tapis_app.app_updates.app_seq_id</code>. Sequence id of
     * application being updated
     */
    public final TableField<AppUpdatesRecord, Integer> APP_SEQ_ID = createField(DSL.name("app_seq_id"), SQLDataType.INTEGER, this, "Sequence id of application being updated");

    /**
     * The column <code>tapis_app.app_updates.app_ver_seq_id</code>. Sequence id
     * of application version being updated
     */
    public final TableField<AppUpdatesRecord, Integer> APP_VER_SEQ_ID = createField(DSL.name("app_ver_seq_id"), SQLDataType.INTEGER, this, "Sequence id of application version being updated");

    /**
     * The column <code>tapis_app.app_updates.obo_tenant</code>. OBO Tenant
     * associated with the change request
     */
    public final TableField<AppUpdatesRecord, String> OBO_TENANT = createField(DSL.name("obo_tenant"), SQLDataType.CLOB.nullable(false), this, "OBO Tenant associated with the change request");

    /**
     * The column <code>tapis_app.app_updates.obo_user</code>. OBO User
     * associated with the change request
     */
    public final TableField<AppUpdatesRecord, String> OBO_USER = createField(DSL.name("obo_user"), SQLDataType.CLOB.nullable(false), this, "OBO User associated with the change request");

    /**
     * The column <code>tapis_app.app_updates.jwt_tenant</code>. Tenant of user
     * who requested the update
     */
    public final TableField<AppUpdatesRecord, String> JWT_TENANT = createField(DSL.name("jwt_tenant"), SQLDataType.CLOB.nullable(false), this, "Tenant of user who requested the update");

    /**
     * The column <code>tapis_app.app_updates.jwt_user</code>. Name of user who
     * requested the update
     */
    public final TableField<AppUpdatesRecord, String> JWT_USER = createField(DSL.name("jwt_user"), SQLDataType.CLOB.nullable(false), this, "Name of user who requested the update");

    /**
     * The column <code>tapis_app.app_updates.app_id</code>. Id of application
     * being updated
     */
    public final TableField<AppUpdatesRecord, String> APP_ID = createField(DSL.name("app_id"), SQLDataType.CLOB.nullable(false), this, "Id of application being updated");

    /**
     * The column <code>tapis_app.app_updates.app_version</code>. Version of
     * application being updated
     */
    public final TableField<AppUpdatesRecord, String> APP_VERSION = createField(DSL.name("app_version"), SQLDataType.CLOB, this, "Version of application being updated");

    /**
     * The column <code>tapis_app.app_updates.operation</code>. Type of update
     * operation
     */
    public final TableField<AppUpdatesRecord, AppOperation> OPERATION = createField(DSL.name("operation"), SQLDataType.CLOB.nullable(false), this, "Type of update operation", new EnumConverter<String, AppOperation>(String.class, AppOperation.class));

    /**
     * The column <code>tapis_app.app_updates.description</code>. JSON
     * describing the change. Secrets scrubbed as needed.
     */
    public final TableField<AppUpdatesRecord, JsonElement> DESCRIPTION = createField(DSL.name("description"), SQLDataType.JSONB.nullable(false), this, "JSON describing the change. Secrets scrubbed as needed.", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_app.app_updates.raw_data</code>. Raw data
     * associated with the request, if available. Secrets scrubbed as needed.
     */
    public final TableField<AppUpdatesRecord, String> RAW_DATA = createField(DSL.name("raw_data"), SQLDataType.CLOB, this, "Raw data associated with the request, if available. Secrets scrubbed as needed.");

    /**
     * The column <code>tapis_app.app_updates.uuid</code>. UUID of app being
     * updated
     */
    public final TableField<AppUpdatesRecord, java.util.UUID> UUID = createField(DSL.name("uuid"), SQLDataType.UUID.nullable(false), this, "UUID of app being updated");

    /**
     * The column <code>tapis_app.app_updates.created</code>. UTC time for when
     * record was created
     */
    public final TableField<AppUpdatesRecord, LocalDateTime> CREATED = createField(DSL.name("created"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "UTC time for when record was created");

    private AppUpdates(Name alias, Table<AppUpdatesRecord> aliased) {
        this(alias, aliased, null);
    }

    private AppUpdates(Name alias, Table<AppUpdatesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>tapis_app.app_updates</code> table reference
     */
    public AppUpdates(String alias) {
        this(DSL.name(alias), APP_UPDATES);
    }

    /**
     * Create an aliased <code>tapis_app.app_updates</code> table reference
     */
    public AppUpdates(Name alias) {
        this(alias, APP_UPDATES);
    }

    /**
     * Create a <code>tapis_app.app_updates</code> table reference
     */
    public AppUpdates() {
        this(DSL.name("app_updates"), null);
    }

    public <O extends Record> AppUpdates(Table<O> child, ForeignKey<O, AppUpdatesRecord> key) {
        super(child, key, APP_UPDATES);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : TapisApp.TAPIS_APP;
    }

    @Override
    public Identity<AppUpdatesRecord, Integer> getIdentity() {
        return (Identity<AppUpdatesRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<AppUpdatesRecord> getPrimaryKey() {
        return Keys.APP_UPDATES_PKEY;
    }

    @Override
    public List<ForeignKey<AppUpdatesRecord, ?>> getReferences() {
        return Arrays.asList(Keys.APP_UPDATES__APP_UPDATES_APP_SEQ_ID_FKEY);
    }

    private transient Apps _apps;

    /**
     * Get the implicit join path to the <code>tapis_app.apps</code> table.
     */
    public Apps apps() {
        if (_apps == null)
            _apps = new Apps(this, Keys.APP_UPDATES__APP_UPDATES_APP_SEQ_ID_FKEY);

        return _apps;
    }

    @Override
    public AppUpdates as(String alias) {
        return new AppUpdates(DSL.name(alias), this);
    }

    @Override
    public AppUpdates as(Name alias) {
        return new AppUpdates(alias, this);
    }

    @Override
    public AppUpdates as(Table<?> alias) {
        return new AppUpdates(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public AppUpdates rename(String name) {
        return new AppUpdates(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AppUpdates rename(Name name) {
        return new AppUpdates(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public AppUpdates rename(Table<?> name) {
        return new AppUpdates(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row14 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row14<Integer, Integer, Integer, String, String, String, String, String, String, AppOperation, JsonElement, String, java.util.UUID, LocalDateTime> fieldsRow() {
        return (Row14) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function14<? super Integer, ? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super AppOperation, ? super JsonElement, ? super String, ? super java.util.UUID, ? super LocalDateTime, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function14<? super Integer, ? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super AppOperation, ? super JsonElement, ? super String, ? super java.util.UUID, ? super LocalDateTime, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
