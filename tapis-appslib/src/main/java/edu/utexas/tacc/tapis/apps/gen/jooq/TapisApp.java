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

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TapisApp extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_app</code>
     */
    public static final TapisApp TAPIS_APP = new TapisApp();

    /**
     * The table <code>tapis_app.app_args</code>.
     */
    public final AppArgs APP_ARGS = AppArgs.APP_ARGS;

    /**
     * The table <code>tapis_app.app_updates</code>.
     */
    public final AppUpdates APP_UPDATES = AppUpdates.APP_UPDATES;

    /**
     * The table <code>tapis_app.apps</code>.
     */
    public final Apps APPS = Apps.APPS;

    /**
     * The table <code>tapis_app.apps_versions</code>.
     */
    public final AppsVersions APPS_VERSIONS = AppsVersions.APPS_VERSIONS;

    /**
     * The table <code>tapis_app.container_args</code>.
     */
    public final ContainerArgs CONTAINER_ARGS = ContainerArgs.CONTAINER_ARGS;

    /**
     * The table <code>tapis_app.file_inputs</code>.
     */
    public final FileInputs FILE_INPUTS = FileInputs.FILE_INPUTS;

    /**
     * The table <code>tapis_app.flyway_schema_history</code>.
     */
    public final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>tapis_app.notification_mechanisms</code>.
     */
    public final NotificationMechanisms NOTIFICATION_MECHANISMS = NotificationMechanisms.NOTIFICATION_MECHANISMS;

    /**
     * The table <code>tapis_app.notification_subscriptions</code>.
     */
    public final NotificationSubscriptions NOTIFICATION_SUBSCRIPTIONS = NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS;

    /**
     * The table <code>tapis_app.scheduler_options</code>.
     */
    public final SchedulerOptions SCHEDULER_OPTIONS = SchedulerOptions.SCHEDULER_OPTIONS;

    /**
     * No further instances allowed
     */
    private TapisApp() {
        super("tapis_app", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Sequence<?>> getSequences() {
        return Arrays.<Sequence<?>>asList(
            Sequences.APP_ARGS_SEQ_ID_SEQ,
            Sequences.APP_UPDATES_SEQ_ID_SEQ,
            Sequences.APPS_SEQ_ID_SEQ,
            Sequences.APPS_VERSIONS_SEQ_ID_SEQ,
            Sequences.CONTAINER_ARGS_SEQ_ID_SEQ,
            Sequences.FILE_INPUTS_SEQ_ID_SEQ,
            Sequences.NOTIFICATION_MECHANISMS_SEQ_ID_SEQ,
            Sequences.NOTIFICATION_SUBSCRIPTIONS_SEQ_ID_SEQ,
            Sequences.SCHEDULER_OPTIONS_SEQ_ID_SEQ);
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.<Table<?>>asList(
            AppArgs.APP_ARGS,
            AppUpdates.APP_UPDATES,
            Apps.APPS,
            AppsVersions.APPS_VERSIONS,
            ContainerArgs.CONTAINER_ARGS,
            FileInputs.FILE_INPUTS,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            NotificationMechanisms.NOTIFICATION_MECHANISMS,
            NotificationSubscriptions.NOTIFICATION_SUBSCRIPTIONS,
            SchedulerOptions.SCHEDULER_OPTIONS);
    }
}
