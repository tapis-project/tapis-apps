/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables.records;


import edu.utexas.tacc.tapis.apps.gen.jooq.tables.NotificationMechanisms;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationMechanismsRecord extends UpdatableRecordImpl<NotificationMechanismsRecord> implements Record5<Integer, Integer, NotifMechanismType, String, String> {

    private static final long serialVersionUID = -1271080551;

    /**
     * Setter for <code>tapis_app.notification_mechanisms.seq_id</code>.
     */
    public void setSeqId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_app.notification_mechanisms.seq_id</code>.
     */
    public Integer getSeqId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>tapis_app.notification_mechanisms.subscription_seq_id</code>.
     */
    public void setSubscriptionSeqId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_app.notification_mechanisms.subscription_seq_id</code>.
     */
    public Integer getSubscriptionSeqId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>tapis_app.notification_mechanisms.mechanism</code>.
     */
    public void setMechanism(NotifMechanismType value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_app.notification_mechanisms.mechanism</code>.
     */
    public NotifMechanismType getMechanism() {
        return (NotifMechanismType) get(2);
    }

    /**
     * Setter for <code>tapis_app.notification_mechanisms.webhook_url</code>.
     */
    public void setWebhookUrl(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_app.notification_mechanisms.webhook_url</code>.
     */
    public String getWebhookUrl() {
        return (String) get(3);
    }

    /**
     * Setter for <code>tapis_app.notification_mechanisms.email_address</code>.
     */
    public void setEmailAddress(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_app.notification_mechanisms.email_address</code>.
     */
    public String getEmailAddress() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Integer, NotifMechanismType, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, Integer, NotifMechanismType, String, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return NotificationMechanisms.NOTIFICATION_MECHANISMS.SEQ_ID;
    }

    @Override
    public Field<Integer> field2() {
        return NotificationMechanisms.NOTIFICATION_MECHANISMS.SUBSCRIPTION_SEQ_ID;
    }

    @Override
    public Field<NotifMechanismType> field3() {
        return NotificationMechanisms.NOTIFICATION_MECHANISMS.MECHANISM;
    }

    @Override
    public Field<String> field4() {
        return NotificationMechanisms.NOTIFICATION_MECHANISMS.WEBHOOK_URL;
    }

    @Override
    public Field<String> field5() {
        return NotificationMechanisms.NOTIFICATION_MECHANISMS.EMAIL_ADDRESS;
    }

    @Override
    public Integer component1() {
        return getSeqId();
    }

    @Override
    public Integer component2() {
        return getSubscriptionSeqId();
    }

    @Override
    public NotifMechanismType component3() {
        return getMechanism();
    }

    @Override
    public String component4() {
        return getWebhookUrl();
    }

    @Override
    public String component5() {
        return getEmailAddress();
    }

    @Override
    public Integer value1() {
        return getSeqId();
    }

    @Override
    public Integer value2() {
        return getSubscriptionSeqId();
    }

    @Override
    public NotifMechanismType value3() {
        return getMechanism();
    }

    @Override
    public String value4() {
        return getWebhookUrl();
    }

    @Override
    public String value5() {
        return getEmailAddress();
    }

    @Override
    public NotificationMechanismsRecord value1(Integer value) {
        setSeqId(value);
        return this;
    }

    @Override
    public NotificationMechanismsRecord value2(Integer value) {
        setSubscriptionSeqId(value);
        return this;
    }

    @Override
    public NotificationMechanismsRecord value3(NotifMechanismType value) {
        setMechanism(value);
        return this;
    }

    @Override
    public NotificationMechanismsRecord value4(String value) {
        setWebhookUrl(value);
        return this;
    }

    @Override
    public NotificationMechanismsRecord value5(String value) {
        setEmailAddress(value);
        return this;
    }

    @Override
    public NotificationMechanismsRecord values(Integer value1, Integer value2, NotifMechanismType value3, String value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached NotificationMechanismsRecord
     */
    public NotificationMechanismsRecord() {
        super(NotificationMechanisms.NOTIFICATION_MECHANISMS);
    }

    /**
     * Create a detached, initialised NotificationMechanismsRecord
     */
    public NotificationMechanismsRecord(Integer seqId, Integer subscriptionSeqId, NotifMechanismType mechanism, String webhookUrl, String emailAddress) {
        super(NotificationMechanisms.NOTIFICATION_MECHANISMS);

        set(0, seqId);
        set(1, subscriptionSeqId);
        set(2, mechanism);
        set(3, webhookUrl);
        set(4, emailAddress);
    }
}
