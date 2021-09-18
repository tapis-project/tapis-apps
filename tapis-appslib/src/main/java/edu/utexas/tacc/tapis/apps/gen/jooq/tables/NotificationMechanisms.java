/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables;


import edu.utexas.tacc.tapis.apps.gen.jooq.Keys;
import edu.utexas.tacc.tapis.apps.gen.jooq.TapisApp;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.NotificationMechanismsRecord;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;

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
import org.jooq.impl.EnumConverter;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationMechanisms extends TableImpl<NotificationMechanismsRecord> {

    private static final long serialVersionUID = 1L;

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
    public final TableField<NotificationMechanismsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.subscription_seq_id</code>.
     */
    public final TableField<NotificationMechanismsRecord, Integer> SUBSCRIPTION_SEQ_ID = createField(DSL.name("subscription_seq_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.mechanism</code>.
     */
    public final TableField<NotificationMechanismsRecord, NotifMechanismType> MECHANISM = createField(DSL.name("mechanism"), SQLDataType.CLOB.nullable(false), this, "", new EnumConverter<String, NotifMechanismType>(String.class, NotifMechanismType.class));

    /**
     * The column <code>tapis_app.notification_mechanisms.webhook_url</code>.
     */
    public final TableField<NotificationMechanismsRecord, String> WEBHOOK_URL = createField(DSL.name("webhook_url"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_app.notification_mechanisms.email_address</code>.
     */
    public final TableField<NotificationMechanismsRecord, String> EMAIL_ADDRESS = createField(DSL.name("email_address"), SQLDataType.CLOB, this, "");

    private NotificationMechanisms(Name alias, Table<NotificationMechanismsRecord> aliased) {
        this(alias, aliased, null);
    }

    private NotificationMechanisms(Name alias, Table<NotificationMechanismsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
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

    /**
     * Create a <code>tapis_app.notification_mechanisms</code> table reference
     */
    public NotificationMechanisms() {
        this(DSL.name("notification_mechanisms"), null);
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
        return (Identity<NotificationMechanismsRecord, Integer>) super.getIdentity();
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

    private transient NotificationSubscriptions _notificationSubscriptions;

    public NotificationSubscriptions notificationSubscriptions() {
        if (_notificationSubscriptions == null)
            _notificationSubscriptions = new NotificationSubscriptions(this, Keys.NOTIFICATION_MECHANISMS__NOTIFICATION_MECHANISMS_SUBSCRIPTION_SEQ_ID_FKEY);

        return _notificationSubscriptions;
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
    public Row5<Integer, Integer, NotifMechanismType, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
