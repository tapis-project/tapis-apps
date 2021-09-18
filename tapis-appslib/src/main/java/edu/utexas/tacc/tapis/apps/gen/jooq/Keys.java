/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.apps.gen.jooq;


import edu.utexas.tacc.tapis.apps.gen.jooq.tables.AppArgs;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.AppUpdates;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.Apps;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.AppsVersions;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.ContainerArgs;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.FileInputs;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.FlywaySchemaHistory;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.NotificationMechanisms;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.NotificationSubscriptions;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.SchedulerOptions;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppArgsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppUpdatesRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsVersionsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.ContainerArgsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.FileInputsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.FlywaySchemaHistoryRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.NotificationMechanismsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.NotificationSubscriptionsRecord;
import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.SchedulerOptionsRecord;

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

    public static final UniqueKey<AppArgsRecord> APP_ARGS_PKEY = Internal.createUniqueKey(AppArgs.APP_ARGS, DSL.name("app_args_pkey"), new TableField[] { AppArgs.APP_ARGS.SEQ_ID }, true);
    public static final UniqueKey<AppUpdatesRecord> APP_UPDATES_PKEY = Internal.createUniqueKey(AppUpdates.APP_UPDATES, DSL.name("app_updates_pkey"), new TableField[] { AppUpdates.APP_UPDATES.SEQ_ID }, true);
    public static final UniqueKey<AppsRecord> APPS_PKEY = Internal.createUniqueKey(Apps.APPS, DSL.name("apps_pkey"), new TableField[] { Apps.APPS.SEQ_ID }, true);
    public static final UniqueKey<AppsRecord> APPS_TENANT_ID_KEY = Internal.createUniqueKey(Apps.APPS, DSL.name("apps_tenant_id_key"), new TableField[] { Apps.APPS.TENANT, Apps.APPS.ID }, true);
    public static final UniqueKey<AppsVersionsRecord> APPS_VERSIONS_APP_SEQ_ID_VERSION_KEY = Internal.createUniqueKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_app_seq_id_version_key"), new TableField[] { AppsVersions.APPS_VERSIONS.APP_SEQ_ID, AppsVersions.APPS_VERSIONS.VERSION }, true);
    public static final UniqueKey<AppsVersionsRecord> APPS_VERSIONS_PKEY = Internal.createUniqueKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_pkey"), new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final UniqueKey<ContainerArgsRecord> CONTAINER_ARGS_PKEY = Internal.createUniqueKey(ContainerArgs.CONTAINER_ARGS, DSL.name("container_args_pkey"), new TableField[] { ContainerArgs.CONTAINER_ARGS.SEQ_ID }, true);
    public static final UniqueKey<FileInputsRecord> FILE_INPUTS_APP_VER_SEQ_ID_SOURCE_URL_TARGET_PATH_KEY = Internal.createUniqueKey(FileInputs.FILE_INPUTS, DSL.name("file_inputs_app_ver_seq_id_source_url_target_path_key"), new TableField[] { FileInputs.FILE_INPUTS.APP_VER_SEQ_ID, FileInputs.FILE_INPUTS.SOURCE_URL, FileInputs.FILE_INPUTS.TARGET_PATH }, true);
    public static final UniqueKey<FileInputsRecord> FILE_INPUTS_PKEY = Internal.createUniqueKey(FileInputs.FILE_INPUTS, DSL.name("file_inputs_pkey"), new TableField[] { FileInputs.FILE_INPUTS.SEQ_ID }, true);
    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, DSL.name("flyway_schema_history_pk"), new TableField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK }, true);
    public static final UniqueKey<NotificationMechanismsRecord> NOTIFICATION_MECHANISMS_PKEY = Internal.createUniqueKey(NotificationMechanisms.NOTIFICATION_MECHANISMS, DSL.name("notification_mechanisms_pkey"), new TableField[] { NotificationMechanisms.NOTIFICATION_MECHANISMS.SEQ_ID }, true);
    public static final UniqueKey<NotificationSubscriptionsRecord> NOTIFICATION_SUBSCRIPTIONS_PKEY = Internal.createUniqueKey(NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS, DSL.name("notification_subscriptions_pkey"), new TableField[] { NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS.SEQ_ID }, true);
    public static final UniqueKey<SchedulerOptionsRecord> SCHEDULER_OPTIONS_PKEY = Internal.createUniqueKey(SchedulerOptions.SCHEDULER_OPTIONS, DSL.name("scheduler_options_pkey"), new TableField[] { SchedulerOptions.SCHEDULER_OPTIONS.SEQ_ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<AppArgsRecord, AppsVersionsRecord> APP_ARGS__APP_ARGS_APP_VER_SEQ_ID_FKEY = Internal.createForeignKey(AppArgs.APP_ARGS, DSL.name("app_args_app_ver_seq_id_fkey"), new TableField[] { AppArgs.APP_ARGS.APP_VER_SEQ_ID }, Keys.APPS_VERSIONS_PKEY, new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final ForeignKey<AppUpdatesRecord, AppsRecord> APP_UPDATES__APP_UPDATES_APP_SEQ_ID_FKEY = Internal.createForeignKey(AppUpdates.APP_UPDATES, DSL.name("app_updates_app_seq_id_fkey"), new TableField[] { AppUpdates.APP_UPDATES.APP_SEQ_ID }, Keys.APPS_PKEY, new TableField[] { Apps.APPS.SEQ_ID }, true);
    public static final ForeignKey<AppsVersionsRecord, AppsRecord> APPS_VERSIONS__APPS_VERSIONS_APP_SEQ_ID_FKEY = Internal.createForeignKey(AppsVersions.APPS_VERSIONS, DSL.name("apps_versions_app_seq_id_fkey"), new TableField[] { AppsVersions.APPS_VERSIONS.APP_SEQ_ID }, Keys.APPS_PKEY, new TableField[] { Apps.APPS.SEQ_ID }, true);
    public static final ForeignKey<ContainerArgsRecord, AppsVersionsRecord> CONTAINER_ARGS__CONTAINER_ARGS_APP_VER_SEQ_ID_FKEY = Internal.createForeignKey(ContainerArgs.CONTAINER_ARGS, DSL.name("container_args_app_ver_seq_id_fkey"), new TableField[] { ContainerArgs.CONTAINER_ARGS.APP_VER_SEQ_ID }, Keys.APPS_VERSIONS_PKEY, new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final ForeignKey<FileInputsRecord, AppsVersionsRecord> FILE_INPUTS__FILE_INPUTS_APP_VER_SEQ_ID_FKEY = Internal.createForeignKey(FileInputs.FILE_INPUTS, DSL.name("file_inputs_app_ver_seq_id_fkey"), new TableField[] { FileInputs.FILE_INPUTS.APP_VER_SEQ_ID }, Keys.APPS_VERSIONS_PKEY, new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final ForeignKey<NotificationMechanismsRecord, NotificationSubscriptionsRecord> NOTIFICATION_MECHANISMS__NOTIFICATION_MECHANISMS_SUBSCRIPTION_SEQ_ID_FKEY = Internal.createForeignKey(NotificationMechanisms.NOTIFICATION_MECHANISMS, DSL.name("notification_mechanisms_subscription_seq_id_fkey"), new TableField[] { NotificationMechanisms.NOTIFICATION_MECHANISMS.SUBSCRIPTION_SEQ_ID }, Keys.NOTIFICATION_SUBSCRIPTIONS_PKEY, new TableField[] { NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS.SEQ_ID }, true);
    public static final ForeignKey<NotificationSubscriptionsRecord, AppsVersionsRecord> NOTIFICATION_SUBSCRIPTIONS__NOTIFICATION_SUBSCRIPTIONS_APP_VER_SEQ_ID_FKEY = Internal.createForeignKey(NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS, DSL.name("notification_subscriptions_app_ver_seq_id_fkey"), new TableField[] { NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS.APP_VER_SEQ_ID }, Keys.APPS_VERSIONS_PKEY, new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
    public static final ForeignKey<SchedulerOptionsRecord, AppsVersionsRecord> SCHEDULER_OPTIONS__SCHEDULER_OPTIONS_APP_VER_SEQ_ID_FKEY = Internal.createForeignKey(SchedulerOptions.SCHEDULER_OPTIONS, DSL.name("scheduler_options_app_ver_seq_id_fkey"), new TableField[] { SchedulerOptions.SCHEDULER_OPTIONS.APP_VER_SEQ_ID }, Keys.APPS_VERSIONS_PKEY, new TableField[] { AppsVersions.APPS_VERSIONS.SEQ_ID }, true);
}
