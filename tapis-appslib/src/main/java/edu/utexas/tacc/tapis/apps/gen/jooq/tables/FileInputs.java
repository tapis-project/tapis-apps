/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.FileInputsRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
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
public class FileInputs extends TableImpl<FileInputsRecord> {

    private static final long serialVersionUID = -1294078454;

    /**
     * The reference instance of <code>tapis_app.file_inputs</code>
     */
    public static final FileInputs FILE_INPUTS = new FileInputs();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FileInputsRecord> getRecordType() {
        return FileInputsRecord.class;
    }

    /**
     * The column <code>tapis_app.file_inputs.seq_id</code>. File input sequence id
     */
    public final TableField<FileInputsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('file_inputs_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "File input sequence id");

    /**
     * The column <code>tapis_app.file_inputs.app_ver_seq_id</code>. Sequence id of application requiring the file input
     */
    public final TableField<FileInputsRecord, Integer> APP_VER_SEQ_ID = createField(DSL.name("app_ver_seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('file_inputs_app_ver_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "Sequence id of application requiring the file input");

    /**
     * The column <code>tapis_app.file_inputs.source_url</code>.
     */
    public final TableField<FileInputsRecord, String> SOURCE_URL = createField(DSL.name("source_url"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.file_inputs.target_path</code>.
     */
    public final TableField<FileInputsRecord, String> TARGET_PATH = createField(DSL.name("target_path"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.file_inputs.in_place</code>.
     */
    public final TableField<FileInputsRecord, Boolean> IN_PLACE = createField(DSL.name("in_place"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.file_inputs.meta_name</code>.
     */
    public final TableField<FileInputsRecord, String> META_NAME = createField(DSL.name("meta_name"), org.jooq.impl.SQLDataType.CLOB.nullable(false).defaultValue(org.jooq.impl.DSL.field("''::text", org.jooq.impl.SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_app.file_inputs.meta_description</code>.
     */
    public final TableField<FileInputsRecord, String> META_DESCRIPTION = createField(DSL.name("meta_description"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.file_inputs.meta_required</code>.
     */
    public final TableField<FileInputsRecord, Boolean> META_REQUIRED = createField(DSL.name("meta_required"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>tapis_app.file_inputs.meta_key_value_pairs</code>.
     */
    public final TableField<FileInputsRecord, String[]> META_KEY_VALUE_PAIRS = createField(DSL.name("meta_key_value_pairs"), org.jooq.impl.SQLDataType.CLOB.getArrayDataType(), this, "");

    /**
     * Create a <code>tapis_app.file_inputs</code> table reference
     */
    public FileInputs() {
        this(DSL.name("file_inputs"), null);
    }

    /**
     * Create an aliased <code>tapis_app.file_inputs</code> table reference
     */
    public FileInputs(String alias) {
        this(DSL.name(alias), FILE_INPUTS);
    }

    /**
     * Create an aliased <code>tapis_app.file_inputs</code> table reference
     */
    public FileInputs(Name alias) {
        this(alias, FILE_INPUTS);
    }

    private FileInputs(Name alias, Table<FileInputsRecord> aliased) {
        this(alias, aliased, null);
    }

    private FileInputs(Name alias, Table<FileInputsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> FileInputs(Table<O> child, ForeignKey<O, FileInputsRecord> key) {
        super(child, key, FILE_INPUTS);
    }

    @Override
    public Schema getSchema() {
        return TapisApp.TAPIS_APP;
    }

    @Override
    public Identity<FileInputsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_FILE_INPUTS;
    }

    @Override
    public UniqueKey<FileInputsRecord> getPrimaryKey() {
        return Keys.FILE_INPUTS_PKEY;
    }

    @Override
    public List<UniqueKey<FileInputsRecord>> getKeys() {
        return Arrays.<UniqueKey<FileInputsRecord>>asList(Keys.FILE_INPUTS_PKEY, Keys.FILE_INPUTS_APP_VER_SEQ_ID_SOURCE_URL_TARGET_PATH_KEY);
    }

    @Override
    public List<ForeignKey<FileInputsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<FileInputsRecord, ?>>asList(Keys.FILE_INPUTS__FILE_INPUTS_APP_VER_SEQ_ID_FKEY);
    }

    public AppsVersions appsVersions() {
        return new AppsVersions(this, Keys.FILE_INPUTS__FILE_INPUTS_APP_VER_SEQ_ID_FKEY);
    }

    @Override
    public FileInputs as(String alias) {
        return new FileInputs(DSL.name(alias), this);
    }

    @Override
    public FileInputs as(Name alias) {
        return new FileInputs(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FileInputs rename(String name) {
        return new FileInputs(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FileInputs rename(Name name) {
        return new FileInputs(name, null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, Integer, String, String, Boolean, String, String, Boolean, String[]> fieldsRow() {
        return (Row9) super.fieldsRow();
    }
}
