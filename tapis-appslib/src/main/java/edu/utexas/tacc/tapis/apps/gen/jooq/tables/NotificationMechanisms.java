/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.NotificationMechanismsRecord;
import edu.utexas.tacc.tapis.apps.model.NotificationMechanism.NotificationMechanismType;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
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
public class NotificationMechanisms extends TableImpl<NotificationMechanismsRecord> {

    private static final long serialVersionUID = 2085902932;

    /**
     * The reference instance of <code>tapis_app.notification_mechanisms</code>
     */
    public static final NotificationMechanisms NOTIFICATION_MECHANISMS = new NotificationMechanisms();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<NotificationMechanismsRecord> getRecordType() {
        return NotificationMechanismsRecord.class;
    }

    /**
     * The column <code>tapis_app.notification_mechanisms.seq_id</code>.
     */
    public final TableField<NotificationMechanismsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('notification_mechanisms_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.subscription_seq_id</code>.
     */
    public final TableField<NotificationMechanismsRecord, Integer> SUBSCRIPTION_SEQ_ID = createField(DSL.name("subscription_seq_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('notification_mechanisms_subscription_seq_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.mechanism</code>.
     */
    public final TableField<NotificationMechanismsRecord, NotificationMechanismType> MECHANISM = createField(DSL.name("mechanism"), org.jooq.impl.SQLDataType.VARCHAR.asEnumDataType(edu.utexas.tacc.tapis.apps.gen.jooq.enums.NotificationMechanismType.class), this, "", new org.jooq.impl.EnumConverter<edu.utexas.tacc.tapis.apps.gen.jooq.enums.NotificationMechanismType, edu.utexas.tacc.tapis.apps.model.NotificationMechanism.NotificationMechanismType>(edu.utexas.tacc.tapis.apps.gen.jooq.enums.NotificationMechanismType.class, edu.utexas.tacc.tapis.apps.model.NotificationMechanism.NotificationMechanismType.class));

    /**
     * The column <code>tapis_app.notification_mechanisms.webhook_url</code>.
     */
    public final TableField<NotificationMechanismsRecord, String> WEBHOOK_URL = createField(DSL.name("webhook_url"), org.jooq.impl.SQLDataType.VARCHAR(128), this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.email_address</code>.
     */
    public final TableField<NotificationMechanismsRecord, String> EMAIL_ADDRESS = createField(DSL.name("email_address"), org.jooq.impl.SQLDataType.VARCHAR(128), this, "");

    /**
     * Create a <code>tapis_app.notification_mechanisms</code> table reference
     */
    public NotificationMechanisms() {
        this(DSL.name("notification_mechanisms"), null);
    }

    /**
     * Create an aliased <code>tapis_app.notification_mechanisms</code> table reference
     */
    public NotificationMechanisms(String alias) {
        this(DSL.name(alias), NOTIFICATION_MECHANISMS);
    }

    /**
     * Create an aliased <code>tapis_app.notification_mechanisms</code> table reference
     */
    public NotificationMechanisms(Name alias) {
        this(alias, NOTIFICATION_MECHANISMS);
    }

    private NotificationMechanisms(Name alias, Table<NotificationMechanismsRecord> aliased) {
        this(alias, aliased, null);
    }

    private NotificationMechanisms(Name alias, Table<NotificationMechanismsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> NotificationMechanisms(Table<O> child, ForeignKey<O, NotificationMechanismsRecord> key) {
        super(child, key, NOTIFICATION_MECHANISMS);
    }

    @Override
    public Schema getSchema() {
        return TapisApp.TAPIS_APP;
    }

    @Override
    public Identity<NotificationMechanismsRecord, Integer> getIdentity() {
        return Keys.IDENTITY_NOTIFICATION_MECHANISMS;
    }

    @Override
    public UniqueKey<NotificationMechanismsRecord> getPrimaryKey() {
        return Keys.NOTIFICATION_MECHANISMS_PKEY;
    }

    @Override
    public List<UniqueKey<NotificationMechanismsRecord>> getKeys() {
        return Arrays.<UniqueKey<NotificationMechanismsRecord>>asList(Keys.NOTIFICATION_MECHANISMS_PKEY);
    }

    @Override
    public List<ForeignKey<NotificationMechanismsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<NotificationMechanismsRecord, ?>>asList(Keys.NOTIFICATION_MECHANISMS__NOTIFICATION_MECHANISMS_SUBSCRIPTION_SEQ_ID_FKEY);
    }

    public NotificationSubscriptions notificationSubscriptions() {
        return new NotificationSubscriptions(this, Keys.NOTIFICATION_MECHANISMS__NOTIFICATION_MECHANISMS_SUBSCRIPTION_SEQ_ID_FKEY);
    }

    @Override
    public NotificationMechanisms as(String alias) {
        return new NotificationMechanisms(DSL.name(alias), this);
    }

    @Override
    public NotificationMechanisms as(Name alias) {
        return new NotificationMechanisms(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationMechanisms rename(String name) {
        return new NotificationMechanisms(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationMechanisms rename(Name name) {
        return new NotificationMechanisms(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Integer, NotificationMechanismType, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
