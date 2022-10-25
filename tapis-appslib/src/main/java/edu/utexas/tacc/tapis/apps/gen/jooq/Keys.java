/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq;


import edu.utexas.tacc.tapis.apps.gen.jooq.tables.AppUpdates;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.Apps;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.AppsVersions;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.FlywaySchemaHistory;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppUpdatesRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsVersionsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.FlywaySchemaHistoryRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * tapis_app.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AppUpdatesRecord> APP_UPDATES_PKEY = Internal.createUniqueKey(AppUpdates.APP_UPDATES, DSL.name("app_updates_pkey"), new TableField[] { AppUpdates.APP_UPDATES.SEQ_ID }, true);
    public static final UniqueKey<AppsRecord> APPS_PKEY = Internal.createUniqueKey(Apps.APPS, DSL.name("apps_pkey"), new TableField[] { Apps.APPS.SEQ_ID }, true);
    public static final UniqueKey<AppsRecord> APPS_TENANT_ID_KEY = Internal.createUniqueKey(Apps.APPS, DSL.name("apps_tenant_id_key"), new TableField[] { Apps.APPS.TENANT, Apps.APPS.ID }, true);
    public static final UniqueKey<AppsVersionsRecord> APPS_VERSIONS_APP_SEQ_ID_VERSION_KEY = Internal.createUniqueKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_app_seq_id_version_key"), new TableField[] { AppsVersions.APPS_VERSIONS.APP_SEQ_ID, AppsVersions.APPS_VERSIONS.VERSION }, true);
    public static final UniqueKey<AppsVersionsRecord> APPS_VERSIONS_PKEY = Internal.createUniqueKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_pkey"), new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, DSL.name("flyway_schema_history_pk"), new TableField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<AppUpdatesRecord, AppsRecord> APP_UPDATES__APP_UPDATES_APP_SEQ_ID_FKEY = Internal.createForeignKey(AppUpdates.APP_UPDATES, DSL.name("app_updates_app_seq_id_fkey"), new TableField[] { AppUpdates.APP_UPDATES.APP_SEQ_ID }, Keys.APPS_PKEY, new TableField[] { Apps.APPS.SEQ_ID }, true);
    public static final ForeignKey<AppsVersionsRecord, AppsRecord> APPS_VERSIONS__APPS_VERSIONS_APP_SEQ_ID_FKEY = Internal.createForeignKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_app_seq_id_fkey"), new TableField[] { AppsVersions.APPS_VERSIONS.APP_SEQ_ID }, Keys.APPS_PKEY, new TableField[] { Apps.APPS.SEQ_ID }, true);
}
