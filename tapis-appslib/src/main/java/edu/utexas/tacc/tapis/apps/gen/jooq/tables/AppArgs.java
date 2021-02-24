/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppArgsRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row7;
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
public class AppArgs extends TableImpl<AppArgsRecord> {

    private static final long serialVersionUID = 332927403;

    /**
     * The reference instance of <code>tapis_app.app_args</code>
     */
    public static final AppArgs APP_ARGS = new AppArgs();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AppArgsRecord> getRecordType() {
        return AppArgsRecord.class;
    }

    /**
     * The column <code>tapis_app.app_args.seq_id</code>. Arg sequence id
     */
    public final TableField<AppArgsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('app_args_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "Arg sequence id");

    /**
     * The column <code>tapis_app.app_args.app_ver_seq_id</code>. Sequence id of application
     */
    public final TableField<AppArgsRecord, Integer> APP_VER_SEQ_ID = createField(DSL.name("app_ver_seq_id"), org.jooq.impl.SQLDataType.INTEGER, this, "Sequence id of application");

    /**
     * The column <code>tapis_app.app_args.arg_val</code>.
     */
    public final TableField<AppArgsRecord, String> ARG_VAL = createField(DSL.name("arg_val"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.field("''::character varying", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>tapis_app.app_args.meta_name</code>.
     */
    public final TableField<AppArgsRecord, String> META_NAME = createField(DSL.name("meta_name"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.field("''::character varying", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>tapis_app.app_args.meta_description</code>.
     */
    public final TableField<AppArgsRecord, String> META_DESCRIPTION = createField(DSL.name("meta_description"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.field("''::character varying", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>tapis_app.app_args.meta_required</code>.
     */
    public final TableField<AppArgsRecord, Boolean> META_REQUIRED = createField(DSL.name("meta_required"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("true", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.app_args.meta_key_value_pairs</code>.
     */
    public final TableField<AppArgsRecord, String[]> META_KEY_VALUE_PAIRS = createField(DSL.name("meta_key_value_pairs"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * Create a <code>tapis_app.app_args</code> table reference
     */
    public AppArgs() {
        this(DSL.name("app_args"), null);
    }

    /**
     * Create an aliased <code>tapis_app.app_args</code> table reference
     */
    public AppArgs(String alias) {
        this(DSL.name(alias), APP_ARGS);
    }

    /**
     * Create an aliased <code>tapis_app.app_args</code> table reference
     */
    public AppArgs(Name alias) {
        this(alias, APP_ARGS);
    }

    private AppArgs(Name alias, Table<AppArgsRecord> aliased) {
        this(alias, aliased, null);
    }

    private AppArgs(Name alias, Table<AppArgsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> AppArgs(Table<O> child, ForeignKey<O, AppArgsRecord> key) {
        super(child, key, APP_ARGS);
    }

    @Override
    public Schema getSchema() {
        return TapisApp.TAPIS_APP;
    }

    @Override
    public Identity<AppArgsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_APP_ARGS;
    }

    @Override
    public UniqueKey<AppArgsRecord> getPrimaryKey() {
        return Keys.APP_ARGS_PKEY;
    }

    @Override
    public List<UniqueKey<AppArgsRecord>> getKeys() {
        return Arrays.<UniqueKey<AppArgsRecord>>asList(Keys.APP_ARGS_PKEY);
    }

    @Override
    public List<ForeignKey<AppArgsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<AppArgsRecord, ?>>asList(Keys.APP_ARGS__APP_ARGS_APP_VER_SEQ_ID_FKEY);
    }

    public AppsVersions appsVersions() {
        return new AppsVersions(this, Keys.APP_ARGS__APP_ARGS_APP_VER_SEQ_ID_FKEY);
    }

    @Override
    public AppArgs as(String alias) {
        return new AppArgs(DSL.name(alias), this);
    }

    @Override
    public AppArgs as(Name alias) {
        return new AppArgs(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AppArgs rename(String name) {
        return new AppArgs(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AppArgs rename(Name name) {
        return new AppArgs(name, null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<Integer, Integer, String, String, String, Boolean, String[]> fieldsRow() {
        return (Row7) super.fieldsRow();
    }
}
