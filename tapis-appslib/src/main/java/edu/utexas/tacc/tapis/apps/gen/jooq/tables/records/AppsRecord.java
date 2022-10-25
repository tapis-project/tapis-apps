/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq.tables.records;


import edu.utexas.tacc.tapis.apps.gen.jooq.tables.Apps;

import java.time.LocalDateTime;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AppsRecord extends UpdatableRecordImpl<AppsRecord> implements Record10<Integer, String, String, String, String, Boolean, Boolean, Boolean, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>tapis_app.apps.seq_id</code>. Application sequence id
     */
    public void setSeqId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_app.apps.seq_id</code>. Application sequence id
     */
    public Integer getSeqId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>tapis_app.apps.tenant</code>. Tenant name associated
     * with the application
     */
    public void setTenant(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_app.apps.tenant</code>. Tenant name associated
     * with the application
     */
    public String getTenant() {
        return (String) get(1);
    }

    /**
     * Setter for <code>tapis_app.apps.id</code>. Unique name for the
     * application
     */
    public void setId(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_app.apps.id</code>. Unique name for the
     * application
     */
    public String getId() {
        return (String) get(2);
    }

    /**
     * Setter for <code>tapis_app.apps.latest_version</code>.
     */
    public void setLatestVersion(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_app.apps.latest_version</code>.
     */
    public String getLatestVersion() {
        return (String) get(3);
    }

    /**
     * Setter for <code>tapis_app.apps.owner</code>. User name of application
     * owner
     */
    public void setOwner(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_app.apps.owner</code>. User name of application
     * owner
     */
    public String getOwner() {
        return (String) get(4);
    }

    /**
     * Setter for <code>tapis_app.apps.enabled</code>. Indicates if application
     * is currently active and available for use
     */
    public void setEnabled(Boolean value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_app.apps.enabled</code>. Indicates if application
     * is currently active and available for use
     */
    public Boolean getEnabled() {
        return (Boolean) get(5);
    }

    /**
     * Setter for <code>tapis_app.apps.containerized</code>.
     */
    public void setContainerized(Boolean value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_app.apps.containerized</code>.
     */
    public Boolean getContainerized() {
        return (Boolean) get(6);
    }

    /**
     * Setter for <code>tapis_app.apps.deleted</code>. Indicates if application
     * has been soft deleted
     */
    public void setDeleted(Boolean value) {
        set(7, value);
    }

    /**
     * Getter for <code>tapis_app.apps.deleted</code>. Indicates if application
     * has been soft deleted
     */
    public Boolean getDeleted() {
        return (Boolean) get(7);
    }

    /**
     * Setter for <code>tapis_app.apps.created</code>. UTC time for when record
     * was created
     */
    public void setCreated(LocalDateTime value) {
        set(8, value);
    }

    /**
     * Getter for <code>tapis_app.apps.created</code>. UTC time for when record
     * was created
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(8);
    }

    /**
     * Setter for <code>tapis_app.apps.updated</code>. UTC time for when record
     * was last updated
     */
    public void setUpdated(LocalDateTime value) {
        set(9, value);
    }

    /**
     * Getter for <code>tapis_app.apps.updated</code>. UTC time for when record
     * was last updated
     */
    public LocalDateTime getUpdated() {
        return (LocalDateTime) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, String, String, String, String, Boolean, Boolean, Boolean, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, String, String, String, String, Boolean, Boolean, Boolean, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Apps.APPS.SEQ_ID;
    }

    @Override
    public Field<String> field2() {
        return Apps.APPS.TENANT;
    }

    @Override
    public Field<String> field3() {
        return Apps.APPS.ID;
    }

    @Override
    public Field<String> field4() {
        return Apps.APPS.LATEST_VERSION;
    }

    @Override
    public Field<String> field5() {
        return Apps.APPS.OWNER;
    }

    @Override
    public Field<Boolean> field6() {
        return Apps.APPS.ENABLED;
    }

    @Override
    public Field<Boolean> field7() {
        return Apps.APPS.CONTAINERIZED;
    }

    @Override
    public Field<Boolean> field8() {
        return Apps.APPS.DELETED;
    }

    @Override
    public Field<LocalDateTime> field9() {
        return Apps.APPS.CREATED;
    }

    @Override
    public Field<LocalDateTime> field10() {
        return Apps.APPS.UPDATED;
    }

    @Override
    public Integer component1() {
        return getSeqId();
    }

    @Override
    public String component2() {
        return getTenant();
    }

    @Override
    public String component3() {
        return getId();
    }

    @Override
    public String component4() {
        return getLatestVersion();
    }

    @Override
    public String component5() {
        return getOwner();
    }

    @Override
    public Boolean component6() {
        return getEnabled();
    }

    @Override
    public Boolean component7() {
        return getContainerized();
    }

    @Override
    public Boolean component8() {
        return getDeleted();
    }

    @Override
    public LocalDateTime component9() {
        return getCreated();
    }

    @Override
    public LocalDateTime component10() {
        return getUpdated();
    }

    @Override
    public Integer value1() {
        return getSeqId();
    }

    @Override
    public String value2() {
        return getTenant();
    }

    @Override
    public String value3() {
        return getId();
    }

    @Override
    public String value4() {
        return getLatestVersion();
    }

    @Override
    public String value5() {
        return getOwner();
    }

    @Override
    public Boolean value6() {
        return getEnabled();
    }

    @Override
    public Boolean value7() {
        return getContainerized();
    }

    @Override
    public Boolean value8() {
        return getDeleted();
    }

    @Override
    public LocalDateTime value9() {
        return getCreated();
    }

    @Override
    public LocalDateTime value10() {
        return getUpdated();
    }

    @Override
    public AppsRecord value1(Integer value) {
        setSeqId(value);
        return this;
    }

    @Override
    public AppsRecord value2(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public AppsRecord value3(String value) {
        setId(value);
        return this;
    }

    @Override
    public AppsRecord value4(String value) {
        setLatestVersion(value);
        return this;
    }

    @Override
    public AppsRecord value5(String value) {
        setOwner(value);
        return this;
    }

    @Override
    public AppsRecord value6(Boolean value) {
        setEnabled(value);
        return this;
    }

    @Override
    public AppsRecord value7(Boolean value) {
        setContainerized(value);
        return this;
    }

    @Override
    public AppsRecord value8(Boolean value) {
        setDeleted(value);
        return this;
    }

    @Override
    public AppsRecord value9(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public AppsRecord value10(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public AppsRecord values(Integer value1, String value2, String value3, String value4, String value5, Boolean value6, Boolean value7, Boolean value8, LocalDateTime value9, LocalDateTime value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AppsRecord
     */
    public AppsRecord() {
        super(Apps.APPS);
    }

    /**
     * Create a detached, initialised AppsRecord
     */
    public AppsRecord(Integer seqId, String tenant, String id, String latestVersion, String owner, Boolean enabled, Boolean containerized, Boolean deleted, LocalDateTime created, LocalDateTime updated) {
        super(Apps.APPS);

        setSeqId(seqId);
        setTenant(tenant);
        setId(id);
        setLatestVersion(latestVersion);
        setOwner(owner);
        setEnabled(enabled);
        setContainerized(containerized);
        setDeleted(deleted);
        setCreated(created);
        setUpdated(updated);
    }
}
