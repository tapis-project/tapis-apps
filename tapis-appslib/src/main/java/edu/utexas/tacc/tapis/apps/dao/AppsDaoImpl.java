package edu.utexas.tacc.tapis.apps.dao;

import java.sql.Connection;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;
import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.FileInputArray;
import edu.utexas.tacc.tapis.apps.model.ReqSubscribe;
import edu.utexas.tacc.tapis.apps.model.ParameterSet;
import edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.AuthListType;
import edu.utexas.tacc.tapis.search.parser.ASTBinaryExpression;
import edu.utexas.tacc.tapis.search.parser.ASTLeaf;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.parser.ASTUnaryExpression;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.OrderByDir;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import static edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.DEFAULT_LIST_TYPE;
import static edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.DEFAULT_ORDERBY_DIRECTION;

import static edu.utexas.tacc.tapis.apps.gen.jooq.Tables.*;
import static edu.utexas.tacc.tapis.apps.gen.jooq.Tables.APPS;

import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.search.SearchUtils.SearchOperator;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

import static edu.utexas.tacc.tapis.search.SearchUtils.SearchOperator.CONTAINS;
import static edu.utexas.tacc.tapis.search.SearchUtils.SearchOperator.NCONTAINS;
import static edu.utexas.tacc.tapis.apps.model.App.INVALID_SEQ_ID;
import static edu.utexas.tacc.tapis.apps.model.App.INVALID_UUID;

/*
 * Class to handle persistence and queries for Tapis App objects.
 */
public class AppsDaoImpl extends AbstractDao implements AppsDao
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(AppsDaoImpl.class);

  private static final String VERS_ANY = "%";
  private static final String EMPTY_JSON_OBJ_STR = "{}";
  private static final String EMPTY_JSON_ARRAY = "[]";
  private static final String[] EMPTY_STR_ARRAY = {};

  // Create a static Set of column names for tables APPS and APPS_VERSIONS
  private static final Set<String> APPS_FIELDS = new HashSet<>();
  private static final Set<String> APPS_VERSIONS_FIELDS = new HashSet<>();
  static
  {
    for (Field<?> field : APPS.fields()) { APPS_FIELDS.add(field.getName()); }
    for (Field<?> field : APPS_VERSIONS.fields()) { APPS_VERSIONS_FIELDS.add(field.getName()); }
  }

  // Compiled regex for splitting around "\."
  private static final Pattern DOT_SPLIT = Pattern.compile("\\.");

  // AND and OR operators
  private static final String AND = "AND";
  private static final String OR = "OR";

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /**
   * Create a new app with id+version
   *
   * @return true if created
   * @throws TapisException - on error
   * @throws IllegalStateException - if app id+version already exists or app has been marked deleted
   */
  @Override
  public boolean createApp(ResourceRequestUser rUser, App app, String changeDescription, String rawData)
          throws TapisException, IllegalStateException
  {
    String opName = "createApp";
    // ------------------------- Check Input -------------------------
    if (app == null) LibUtils.logAndThrowNullParmException(opName, "app");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "changeDescription");
    if (StringUtils.isBlank(app.getTenant())) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(app.getId())) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(app.getVersion())) LibUtils.logAndThrowNullParmException(opName, "appVersion");

    // Make sure owner, runtime, notes, tags etc. are set
    String owner = App.DEFAULT_OWNER;
    Runtime runtime = App.DEFAULT_RUNTIME;
    String[] runtimeOptionsStrArray = null;
    String[] execSystemConstraintsStrArray = null;
    JsonElement parameterSetJson = App.DEFAULT_PARAMETER_SET;
    JsonElement fileInputsJson = App.DEFAULT_FILE_INPUTS;
    JsonElement fileInputArraysJson = App.DEFAULT_FILE_INPUT_ARRAYS;
    JsonElement subscriptionsJson = App.DEFAULT_SUBSCRIPTIONS;
    String[] jobTagsStrArray = App.EMPTY_STR_ARRAY;
    String[] tagsStrArray = App.EMPTY_STR_ARRAY;
    JsonObject notesObj = App.DEFAULT_NOTES;

    if (StringUtils.isNotBlank(app.getOwner())) owner = app.getOwner();
    if (app.getRuntime() != null) runtime = app.getRuntime();
    // Convert runtimeOptions array from enum to string
    if (app.getRuntimeOptions() != null)
    {
      runtimeOptionsStrArray = app.getRuntimeOptions().stream().map(RuntimeOption::name).toArray(String[]::new);
    }
    if (app.getExecSystemConstraints() != null) execSystemConstraintsStrArray = app.getExecSystemConstraints();
    if (app.getParameterSet() != null) parameterSetJson = TapisGsonUtils.getGson().toJsonTree(app.getParameterSet());
    if (app.getFileInputs() != null) fileInputsJson = TapisGsonUtils.getGson().toJsonTree(app.getFileInputs());
    if (app.getFileInputArrays() != null) fileInputArraysJson = TapisGsonUtils.getGson().toJsonTree(app.getFileInputArrays());
    if (app.getSubscriptions() != null) subscriptionsJson = TapisGsonUtils.getGson().toJsonTree(app.getSubscriptions());
    if (app.getJobTags() != null) jobTagsStrArray = app.getJobTags();
    if (app.getTags() != null) tagsStrArray = app.getTags();
    if (app.getNotes() != null) notesObj = app.getNotes();

    // Generated sequence IDs
    int appSeqId = -1;
    int appVerSeqId = -1;
    // Generate uuid for the new app version
    app.setUuid(UUID.randomUUID());

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if app exists (even if deleted). If yes then throw IllegalStateException
      if (isDeleted(db, app.getTenant(), app.getId()))
        throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_APP_DELETED", rUser, app.getId()));

      // If app (id+version) exists then throw IllegalStateException
      if (checkIfAppExists(db, app.getTenant(), app.getId(), app.getVersion(), false))
        throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_APP_EXISTS", rUser, app.getId(),
                                                            app.getVersion()));
      // If no top level app entry this is the first version. Create the initial top level record
      if (!checkIfAppExists(db, app.getTenant(), app.getId(), null, false))
      {
        Record record = db.insertInto(APPS)
                .set(APPS.TENANT, app.getTenant())
                .set(APPS.ID, app.getId())
                .set(APPS.LATEST_VERSION, app.getVersion())
                .set(APPS.OWNER, owner)
                .set(APPS.ENABLED, app.isEnabled())
                .set(APPS.CONTAINERIZED, app.isContainerized())
                .returningResult(APPS.SEQ_ID)
                .fetchOne();
        if (record != null) appSeqId = record.getValue(APPS.SEQ_ID);
      }
      else
      {
        // Top level record exists. Get the sequence Id.
        appSeqId = getAppSeqIdUsingDb(db, app.getTenant(), app.getId());
      }

      // Insert new record into APPS_VERSIONS
      Record record = db.insertInto(APPS_VERSIONS)
              .set(APPS_VERSIONS.APP_SEQ_ID, appSeqId)
              .set(APPS_VERSIONS.TENANT, app.getTenant())
              .set(APPS_VERSIONS.ID, app.getId())
              .set(APPS_VERSIONS.VERSION, app.getVersion())
              .set(APPS_VERSIONS.DESCRIPTION, app.getDescription())
              .set(APPS_VERSIONS.LOCKED, app.isLocked())
              .set(APPS_VERSIONS.RUNTIME, runtime)
              .set(APPS_VERSIONS.RUNTIME_VERSION, app.getRuntimeVersion())
              .set(APPS_VERSIONS.RUNTIME_OPTIONS, runtimeOptionsStrArray)
              .set(APPS_VERSIONS.CONTAINER_IMAGE, app.getContainerImage())
              .set(APPS_VERSIONS.JOB_TYPE, app.getJobType())
              .set(APPS_VERSIONS.MAX_JOBS, app.getMaxJobs())
              .set(APPS_VERSIONS.MAX_JOBS_PER_USER, app.getMaxJobsPerUser())
              .set(APPS_VERSIONS.JOB_DESCRIPTION, app.getJobDescription())
              .set(APPS_VERSIONS.DYNAMIC_EXEC_SYSTEM, app.isDynamicExecSystem())
              .set(APPS_VERSIONS.EXEC_SYSTEM_CONSTRAINTS, execSystemConstraintsStrArray)
              .set(APPS_VERSIONS.EXEC_SYSTEM_ID, app.getExecSystemId())
              .set(APPS_VERSIONS.EXEC_SYSTEM_EXEC_DIR, app.getExecSystemExecDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_INPUT_DIR, app.getExecSystemInputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_OUTPUT_DIR, app.getExecSystemOutputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_INPUT_DIR, app.getDtnSystemInputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_OUTPUT_DIR, app.getDtnSystemOutputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_LOGICAL_QUEUE, app.getExecSystemLogicalQueue())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_ID, app.getArchiveSystemId())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_DIR, app.getArchiveSystemDir())
              .set(APPS_VERSIONS.ARCHIVE_ON_APP_ERROR, app.isArchiveOnAppError())
              .set(APPS_VERSIONS.IS_MPI, app.getIsMpi())
              .set(APPS_VERSIONS.MPI_CMD, app.getMpiCmd())
              .set(APPS_VERSIONS.CMD_PREFIX, app.getCmdPrefix())
              .set(APPS_VERSIONS.PARAMETER_SET, parameterSetJson)
              .set(APPS_VERSIONS.FILE_INPUTS, fileInputsJson)
              .set(APPS_VERSIONS.FILE_INPUT_ARRAYS, fileInputArraysJson)
              .set(APPS_VERSIONS.NODE_COUNT, app.getNodeCount())
              .set(APPS_VERSIONS.CORES_PER_NODE, app.getCoresPerNode())
              .set(APPS_VERSIONS.MEMORY_MB, app.getMemoryMB())
              .set(APPS_VERSIONS.MAX_MINUTES, app.getMaxMinutes())
              .set(APPS_VERSIONS.SUBSCRIPTIONS, subscriptionsJson)
              .set(APPS_VERSIONS.JOB_TAGS, jobTagsStrArray)
              .set(APPS_VERSIONS.TAGS, tagsStrArray)
              .set(APPS_VERSIONS.NOTES, notesObj)
              .set(APPS_VERSIONS.UUID, app.getUuid())
              .returningResult(APPS_VERSIONS.SEQ_ID)
              .fetchOne();

      // If record is null it is an error
      if (record == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("APPLIB_DB_NULL_RESULT", rUser, app.getId(), opName));
      }

      appVerSeqId = record.getValue(APPS_VERSIONS.SEQ_ID);

      // Update top level table APPS
      db.update(APPS).set(APPS.LATEST_VERSION, app.getVersion()).where(APPS.ID.eq(app.getId())).execute();

      // Persist change history record
      addUpdate(db, rUser, app.getTenant(), app.getId(), app.getVersion(), appSeqId, appVerSeqId, AppOperation.create,
                changeDescription, rawData, app.getUuid());

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", app.getId());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return true;
  }

  /**
   * Update all updatable attributes of an existing specific version of an application.
   * @throws TapisException - on error
   * @throws IllegalStateException - if app already exists
   */
  @Override
  public void putApp(ResourceRequestUser rUser, App putApp, String changeDescription, String rawData)
          throws TapisException, IllegalStateException {
    String opName = "putApp";
    // ------------------------- Check Input -------------------------
    if (putApp == null) LibUtils.logAndThrowNullParmException(opName, "putApp");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    // Pull out some values for convenience
    String tenantId = putApp.getTenant();
    String appId = putApp.getId();
    String appVersion = putApp.getVersion();
    // Check required attributes have been provided
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "changeDescription");
    if (StringUtils.isBlank(tenantId)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(appVersion)) LibUtils.logAndThrowNullParmException(opName, "appVersion");

    // Make sure runtime, notes, tags, etc are all set
    Runtime runtime = App.DEFAULT_RUNTIME;
    String[] execSystemConstraintsStrArray = null;
    JsonElement parameterSetJson = App.DEFAULT_PARAMETER_SET;
    JsonElement fileInputsJson = App.DEFAULT_FILE_INPUTS;
    JsonElement fileInputArraysJson = App.DEFAULT_FILE_INPUT_ARRAYS;
    JsonElement subscriptionsJson = App.DEFAULT_SUBSCRIPTIONS;
    String[] jobTagsStrArray = App.EMPTY_STR_ARRAY;
    String[] tagsStrArray = App.EMPTY_STR_ARRAY;
    JsonObject notesObj =  App.DEFAULT_NOTES;

    if (putApp.getRuntime() != null) runtime = putApp.getRuntime();
    String[] runtimeOptionsStrArray = null;
    // Convert runtimeOptions array from enum to string
    if (putApp.getRuntimeOptions() != null)
    {
      runtimeOptionsStrArray = putApp.getRuntimeOptions().stream().map(RuntimeOption::name).toArray(String[]::new);
    }

    if (putApp.getExecSystemConstraints() != null) execSystemConstraintsStrArray = putApp.getExecSystemConstraints();
    if (putApp.getParameterSet() != null) parameterSetJson = TapisGsonUtils.getGson().toJsonTree(putApp.getParameterSet());
    if (putApp.getFileInputs() != null) fileInputsJson = TapisGsonUtils.getGson().toJsonTree(putApp.getFileInputs());
    if (putApp.getFileInputArrays() != null) fileInputArraysJson = TapisGsonUtils.getGson().toJsonTree(putApp.getFileInputArrays());
    if (putApp.getSubscriptions() != null) subscriptionsJson = TapisGsonUtils.getGson().toJsonTree(putApp.getSubscriptions());
    if (putApp.getJobTags() != null) jobTagsStrArray = putApp.getJobTags();
    if (putApp.getTags() != null) tagsStrArray = putApp.getTags();
    if (putApp.getNotes() != null) notesObj = putApp.getNotes();

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if app exists and has not been marked as deleted. If no then throw IllegalStateException
      boolean doesExist = checkIfAppExists(db, tenantId, appId, appVersion, false);
      if (!doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", rUser, appId));

      // Make sure UUID filled in, needed for update record. Pre-populated putApp may not have it.
      UUID uuid = putApp.getUuid();
      if (uuid == null) uuid = getUUIDUsingDb(db, tenantId, appId, appVersion);

      int appSeqId = getAppSeqIdUsingDb(db, tenantId, appId);
      int appVerSeqId = -1;
      var result = db.update(APPS_VERSIONS)
              .set(APPS_VERSIONS.DESCRIPTION, putApp.getDescription())
              .set(APPS_VERSIONS.LOCKED, putApp.isLocked())
              .set(APPS_VERSIONS.RUNTIME, runtime)
              .set(APPS_VERSIONS.RUNTIME_VERSION, putApp.getRuntimeVersion())
              .set(APPS_VERSIONS.RUNTIME_OPTIONS, runtimeOptionsStrArray)
              .set(APPS_VERSIONS.CONTAINER_IMAGE, putApp.getContainerImage())
              .set(APPS_VERSIONS.JOB_TYPE, putApp.getJobType())
              .set(APPS_VERSIONS.MAX_JOBS, putApp.getMaxJobs())
              .set(APPS_VERSIONS.MAX_JOBS_PER_USER, putApp.getMaxJobsPerUser())
              .set(APPS_VERSIONS.STRICT_FILE_INPUTS, putApp.isStrictFileInputs())
              .set(APPS_VERSIONS.JOB_DESCRIPTION, putApp.getJobDescription())
              .set(APPS_VERSIONS.DYNAMIC_EXEC_SYSTEM, putApp.isDynamicExecSystem())
              .set(APPS_VERSIONS.EXEC_SYSTEM_CONSTRAINTS, execSystemConstraintsStrArray)
              .set(APPS_VERSIONS.EXEC_SYSTEM_ID, putApp.getExecSystemId())
              .set(APPS_VERSIONS.EXEC_SYSTEM_EXEC_DIR, putApp.getExecSystemExecDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_INPUT_DIR, putApp.getExecSystemInputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_OUTPUT_DIR, putApp.getExecSystemOutputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_INPUT_DIR, putApp.getDtnSystemInputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_OUTPUT_DIR, putApp.getDtnSystemOutputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_LOGICAL_QUEUE, putApp.getExecSystemLogicalQueue())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_ID, putApp.getArchiveSystemId())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_DIR, putApp.getArchiveSystemDir())
              .set(APPS_VERSIONS.ARCHIVE_ON_APP_ERROR, putApp.isArchiveOnAppError())
              .set(APPS_VERSIONS.IS_MPI, putApp.getIsMpi())
              .set(APPS_VERSIONS.MPI_CMD, putApp.getMpiCmd())
              .set(APPS_VERSIONS.CMD_PREFIX, putApp.getCmdPrefix())
              .set(APPS_VERSIONS.PARAMETER_SET, parameterSetJson)
              .set(APPS_VERSIONS.FILE_INPUTS, fileInputsJson)
              .set(APPS_VERSIONS.FILE_INPUT_ARRAYS, fileInputArraysJson)
              .set(APPS_VERSIONS.NODE_COUNT, putApp.getNodeCount())
              .set(APPS_VERSIONS.CORES_PER_NODE, putApp.getCoresPerNode())
              .set(APPS_VERSIONS.MEMORY_MB, putApp.getMemoryMB())
              .set(APPS_VERSIONS.MAX_MINUTES, putApp.getMaxMinutes())
              .set(APPS_VERSIONS.SUBSCRIPTIONS, subscriptionsJson)
              .set(APPS_VERSIONS.JOB_TAGS, jobTagsStrArray)
              .set(APPS_VERSIONS.TAGS, tagsStrArray)
              .set(APPS_VERSIONS.NOTES, notesObj)
              .set(APPS_VERSIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(APPS_VERSIONS.APP_SEQ_ID.eq(appSeqId),APPS_VERSIONS.VERSION.eq(appVersion))
              .returningResult(APPS_VERSIONS.SEQ_ID)
              .fetchOne();

      // If result is null it is an error
      if (result == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("APPLIB_DB_NULL_RESULT", rUser, appId, opName));
      }

      appVerSeqId = result.getValue(APPS_VERSIONS.SEQ_ID);

      // Persist update record
      addUpdate(db, rUser, tenantId, appId, appVersion, appSeqId, appVerSeqId, AppOperation.modify,
              changeDescription, rawData, uuid);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps_versions", appVersion);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update selected attributes of an existing specific version of an application.
   * @throws TapisException - on error
   * @throws IllegalStateException - if app already exists
   */
  @Override
  public void patchApp(ResourceRequestUser rUser, String appId, String appVersion, App patchedApp,
                       String changeDescription, String rawData)
          throws TapisException, IllegalStateException {
    String opName = "patchApp";
    // ------------------------- Check Input -------------------------
    if (patchedApp == null) LibUtils.logAndThrowNullParmException(opName, "patchedApp");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");

    String tenant = rUser.getOboTenantId();
    // Check required attributes have been provided
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "updateJson");
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(appVersion)) LibUtils.logAndThrowNullParmException(opName, "appVersion");

    // Make sure runtime, notes, tags, etc are all set
    Runtime runtime = App.DEFAULT_RUNTIME;
    String[] execSystemConstraintsStrArray = null;
    JsonElement parameterSetJson = App.DEFAULT_PARAMETER_SET;
    JsonElement fileInputsJson = App.DEFAULT_FILE_INPUTS;
    JsonElement fileInputArraysJson = App.DEFAULT_FILE_INPUT_ARRAYS;
    JsonElement subscriptionsJson = App.DEFAULT_SUBSCRIPTIONS;
    String[] jobTagsStrArray = App.EMPTY_STR_ARRAY;
    String[] tagsStrArray = App.EMPTY_STR_ARRAY;
    JsonObject notesObj =  App.DEFAULT_NOTES;

    if (patchedApp.getRuntime() != null) runtime = patchedApp.getRuntime();
    String[] runtimeOptionsStrArray = null;
    // Convert runtimeOptions array from enum to string
    if (patchedApp.getRuntimeOptions() != null)
    {
      runtimeOptionsStrArray = patchedApp.getRuntimeOptions().stream().map(RuntimeOption::name).toArray(String[]::new);
    }

    if (patchedApp.getExecSystemConstraints() != null) execSystemConstraintsStrArray = patchedApp.getExecSystemConstraints();
    if (patchedApp.getParameterSet() != null) parameterSetJson = TapisGsonUtils.getGson().toJsonTree(patchedApp.getParameterSet());
    if (patchedApp.getFileInputs() != null) fileInputsJson = TapisGsonUtils.getGson().toJsonTree(patchedApp.getFileInputs());
    if (patchedApp.getFileInputArrays() != null) fileInputArraysJson = TapisGsonUtils.getGson().toJsonTree(patchedApp.getFileInputArrays());
    if (patchedApp.getSubscriptions() != null) subscriptionsJson = TapisGsonUtils.getGson().toJsonTree(patchedApp.getSubscriptions());
    if (patchedApp.getJobTags() != null) jobTagsStrArray = patchedApp.getJobTags();
    if (patchedApp.getTags() != null) tagsStrArray = patchedApp.getTags();
    if (patchedApp.getNotes() != null) notesObj = patchedApp.getNotes();

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if app exists and has not been marked as deleted. If no then throw IllegalStateException
      boolean doesExist = checkIfAppExists(db, tenant, appId, appVersion, false);
      if (!doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", rUser, appId));

      int appSeqId = getAppSeqIdUsingDb(db, tenant, appId);
      int appVerSeqId = -1;
      var result = db.update(APPS_VERSIONS)
              .set(APPS_VERSIONS.DESCRIPTION, patchedApp.getDescription())
              .set(APPS_VERSIONS.RUNTIME, runtime)
              .set(APPS_VERSIONS.RUNTIME_VERSION, patchedApp.getRuntimeVersion())
              .set(APPS_VERSIONS.RUNTIME_OPTIONS, runtimeOptionsStrArray)
              .set(APPS_VERSIONS.CONTAINER_IMAGE, patchedApp.getContainerImage())
              .set(APPS_VERSIONS.JOB_TYPE, patchedApp.getJobType())
              .set(APPS_VERSIONS.MAX_JOBS, patchedApp.getMaxJobs())
              .set(APPS_VERSIONS.MAX_JOBS_PER_USER, patchedApp.getMaxJobsPerUser())
              .set(APPS_VERSIONS.STRICT_FILE_INPUTS, patchedApp.isStrictFileInputs())
              .set(APPS_VERSIONS.JOB_DESCRIPTION, patchedApp.getJobDescription())
              .set(APPS_VERSIONS.DYNAMIC_EXEC_SYSTEM, patchedApp.isDynamicExecSystem())
              .set(APPS_VERSIONS.EXEC_SYSTEM_CONSTRAINTS, execSystemConstraintsStrArray)
              .set(APPS_VERSIONS.EXEC_SYSTEM_ID, patchedApp.getExecSystemId())
              .set(APPS_VERSIONS.EXEC_SYSTEM_EXEC_DIR, patchedApp.getExecSystemExecDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_INPUT_DIR, patchedApp.getExecSystemInputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_OUTPUT_DIR, patchedApp.getExecSystemOutputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_INPUT_DIR, patchedApp.getDtnSystemInputDir())
              .set(APPS_VERSIONS.DTN_SYSTEM_OUTPUT_DIR, patchedApp.getDtnSystemOutputDir())
              .set(APPS_VERSIONS.EXEC_SYSTEM_LOGICAL_QUEUE, patchedApp.getExecSystemLogicalQueue())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_ID, patchedApp.getArchiveSystemId())
              .set(APPS_VERSIONS.ARCHIVE_SYSTEM_DIR, patchedApp.getArchiveSystemDir())
              .set(APPS_VERSIONS.ARCHIVE_ON_APP_ERROR, patchedApp.isArchiveOnAppError())
              .set(APPS_VERSIONS.IS_MPI, patchedApp.getIsMpi())
              .set(APPS_VERSIONS.MPI_CMD, patchedApp.getMpiCmd())
              .set(APPS_VERSIONS.CMD_PREFIX, patchedApp.getCmdPrefix())
              .set(APPS_VERSIONS.PARAMETER_SET, parameterSetJson)
              .set(APPS_VERSIONS.FILE_INPUTS, fileInputsJson)
              .set(APPS_VERSIONS.FILE_INPUT_ARRAYS, fileInputArraysJson)
              .set(APPS_VERSIONS.NODE_COUNT, patchedApp.getNodeCount())
              .set(APPS_VERSIONS.CORES_PER_NODE, patchedApp.getCoresPerNode())
              .set(APPS_VERSIONS.MEMORY_MB, patchedApp.getMemoryMB())
              .set(APPS_VERSIONS.MAX_MINUTES, patchedApp.getMaxMinutes())
              .set(APPS_VERSIONS.SUBSCRIPTIONS, subscriptionsJson)
              .set(APPS_VERSIONS.JOB_TAGS, jobTagsStrArray)
              .set(APPS_VERSIONS.TAGS, tagsStrArray)
              .set(APPS_VERSIONS.NOTES, notesObj)
              .set(APPS_VERSIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(APPS_VERSIONS.APP_SEQ_ID.eq(appSeqId),APPS_VERSIONS.VERSION.eq(appVersion))
              .returningResult(APPS_VERSIONS.SEQ_ID)
              .fetchOne();

      // If result is null it is an error
      if (result == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("APPLIB_DB_NULL_RESULT", rUser, appId, opName));
      }

      appVerSeqId = result.getValue(APPS_VERSIONS.SEQ_ID);

      // Persist update record
      addUpdate(db, rUser, tenant, appId, appVersion, appSeqId, appVerSeqId, AppOperation.modify,
              changeDescription, rawData, patchedApp.getUuid());

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps_versions", appVersion);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update attribute enabled for an app given app Id and value
   */
  @Override
  public void updateEnabled(ResourceRequestUser rUser, String tenantId, String appId, String appVersion, boolean enabled)
          throws TapisException
  {
    String opName = "updateEnabled";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");

    // AppOperation needed for recording the update
    AppOperation appOp = enabled ? AppOperation.enable : AppOperation.disable;
    String versionStr = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // If no version given update APPS table, else update APPS_VERSIONS table
      if (StringUtils.isBlank(appVersion))
      {
        db.update(APPS)
                .set(APPS.ENABLED, enabled)
                .set(APPS.UPDATED, TapisUtils.getUTCTimeNow())
                .where(APPS.TENANT.eq(tenantId), APPS.ID.eq(appId)).execute();
      }
      else
      {
        versionStr = appVersion;
        db.update(APPS_VERSIONS)
                .set(APPS_VERSIONS.VERSION_ENABLED, enabled)
                .set(APPS_VERSIONS.UPDATED, TapisUtils.getUTCTimeNow())
                .where(APPS_VERSIONS.TENANT.eq(tenantId), APPS_VERSIONS.ID.eq(appId), APPS_VERSIONS.VERSION.eq(appVersion)).execute();
      }
      // Persist update record
      String changeDescription = "{\"enabled\":" +  enabled + "}";
      addUpdate(db, rUser, tenantId, appId, versionStr, INVALID_SEQ_ID, INVALID_SEQ_ID,
                appOp, changeDescription , null, INVALID_UUID);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", appId);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update attribute locked for an app given app Id and value
   */
  @Override
  public void updateLocked(ResourceRequestUser rUser, String tenantId, String appId, String appVersion,
                           boolean locked) throws TapisException
  {
    String opName = "updateLocked";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(appVersion)) LibUtils.logAndThrowNullParmException(opName, "appVersion");

    // AppOperation needed for recording the update
    AppOperation appOp = locked ? AppOperation.lock : AppOperation.unlock;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(APPS_VERSIONS)
              .set(APPS_VERSIONS.LOCKED, locked)
              .where(APPS_VERSIONS.TENANT.eq(tenantId),APPS_VERSIONS.ID.eq(appId),APPS_VERSIONS.VERSION.eq(appVersion)).execute();
      // Persist update record
      String changeDescription = "{\"locked\":" +  locked + "}";
      // Persist update record. Using -1 for appVerSeqId results in it being fetched.
      int appSeqId = getAppSeqIdUsingDb(db, tenantId, appId);
      int appVerSeqId = -1;
      addUpdate(db, rUser, tenantId, appId, appVersion, appSeqId, appVerSeqId, appOp,
                changeDescription, null, INVALID_UUID);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", appId);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update attribute deleted for an app given app Id and value
   */
  @Override
  public void updateDeleted(ResourceRequestUser rUser, String tenantId, String appId, boolean deleted) throws TapisException
  {
    String opName = "updateDeleted";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");

    // AppOperation needed for recording the update
    AppOperation appOp = deleted ? AppOperation.delete : AppOperation.undelete;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(APPS)
              .set(APPS.DELETED, deleted)
              .set(APPS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(APPS.TENANT.eq(tenantId),APPS.ID.eq(appId)).execute();
      // Persist update record
      String changeDescription = "{\"deleted\":" +  deleted + "}";
      addUpdate(db, rUser, tenantId, appId, null, INVALID_SEQ_ID, INVALID_SEQ_ID,
              appOp, changeDescription , null, INVALID_UUID);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", appId);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update owner of an app given app Id and new owner name
   *
   */
  @Override
  public void updateAppOwner(ResourceRequestUser rUser, String tenantId, String appId, String newOwnerName) throws TapisException
  {
    String opName = "changeOwner";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(newOwnerName)) LibUtils.logAndThrowNullParmException(opName, "newOwnerName");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(APPS)
              .set(APPS.OWNER, newOwnerName)
              .set(APPS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(APPS.TENANT.eq(tenantId),APPS.ID.eq(appId)).execute();
      // Persist update record
      String changeDescription = "{\"owner\":\"" +  newOwnerName + "\"}";
      addUpdate(db, rUser, tenantId, appId, null, INVALID_SEQ_ID, INVALID_SEQ_ID,
                AppOperation.changeOwner, changeDescription , null, INVALID_UUID);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", appId);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Hard delete an app record given the app name.
   */
  @Override
  public int hardDeleteApp(String tenant, String appId) throws TapisException
  {
    String opName = "hardDeleteApp";
    _log.warn(LibUtils.getMsg("APPLIB_DB_HARD_DELETE", tenant, appId));
    int rows = -1;
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(appId)) LibUtils.logAndThrowNullParmException(opName, "name");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.deleteFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId)).execute();
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "apps");
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
    return rows;
  }

  /**
   * checkDB
   * Check that we can connect with DB and that the main table of the service exists.
   * @return null if all OK else return an exception
   */
  @Override
  public Exception checkDB()
  {
    Exception result = null;
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // execute SELECT to_regclass('tapis_app.apps');
      // Build and execute a simple postgresql statement to check for the table
      String sql = "SELECT to_regclass('" + APPS.getName() + "')";
      Result<Record> ret = db.resultQuery(sql).fetch();
      if (ret == null || ret.isEmpty() || ret.getValue(0,0) == null)
      {
        result = new TapisException(LibUtils.getMsg("APPLIB_CHECKDB_NO_TABLE", APPS.getName()));
      }
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      result = e;
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * migrateDB
   * Use Flyway to make sure DB schema is at the latest version
   */
  @Override
  public void migrateDB() throws TapisException
  {
    Flyway flyway = Flyway.configure().dataSource(getDataSource()).load();
    // Use repair() as workaround to avoid checksum error during develop/deploy of SNAPSHOT versions when it is not a true migration.
//    flyway.repair();
    flyway.migrate();
  }

  /**
   * checkForApp - check that app with specified Id (any version) exists
   * @param appId - app name
   * @param includeDeleted - whether to include deleted items
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForApp(String tenant, String appId, boolean includeDeleted) throws TapisException {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = checkIfAppExists(db, tenant, appId, null, includeDeleted);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * checkForAppVersion - check that app with specified Id and version exists
   * @param appId - app name
   * @param appVersion - app version
   * @param includeDeleted - whether to include deleted items
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForAppVersion(String tenant, String appId, String appVersion, boolean includeDeleted)
          throws TapisException {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = checkIfAppExists(db, tenant, appId, appVersion, includeDeleted);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * checkForApp - check that the App with specified Id and version exists
   * @param appId - app name
   * @param appVersion - app version
   * @param includeDeleted - whether to include deleted items
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForApp(String tenant, String appId, String appVersion, boolean includeDeleted) throws TapisException {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = checkIfAppExists(db, tenant, appId, appVersion, includeDeleted);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * isEnabled - check if app with specified Id is enabled
   * @param appId - app name
   * @param appVersion - (optional) version of the app
   * @return true if enabled else false
   * @throws TapisException - on error
   */
  @Override
  public boolean isEnabled(String tenant, String appId, String appVersion) throws TapisException {
    // Initialize result.
    boolean result = false;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      Boolean b;
      // If not getting specific version, check top level table, else check apps_versions table.
      if (StringUtils.isBlank(appVersion))
      {
        b = db.selectFrom(APPS)
                .where(APPS.TENANT.eq(tenant), APPS.ID.eq(appId), APPS.DELETED.eq(false))
                .fetchOne(APPS.ENABLED);
      }
      else
      {
        b = db.selectFrom(APPS_VERSIONS)
                .where(APPS_VERSIONS.TENANT.eq(tenant), APPS_VERSIONS.ID.eq(appId), APPS_VERSIONS.VERSION.eq(appVersion))
                .fetchOne(APPS_VERSIONS.VERSION_ENABLED);
      }
      if (b != null) result = b;
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getApp - retrieve the most recently created version of the app
   * @param appId - app name
   * @return App object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public App getApp(String tenant, String appId) throws TapisException
  {
    return getApp(tenant, appId, null, false);
  }

  /**
   * getApp
   * Retrieve specified or most recently created version of an application.
   * @param appId - app name
   * @param appVersion - app version, null for most recently created version
   * @return App object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public App getApp(String tenant, String appId, String appVersion) throws TapisException
  {
    return getApp(tenant, appId, appVersion, false);
  }

  /**
   * getApp
   * Retrieve specified or most recently created version of an application.
   * @param appId - app name
   * @param appVersion - app version, null for most recently created version
   * @param includeDeleted - whether to include deleted items
   * @return App object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public App getApp(String tenant, String appId, String appVersion, boolean includeDeleted) throws TapisException
  {
    // Initialize result.
    App app = null;
    String fetchVersion = appVersion;

    // Begin where condition for the query
    Condition whereCondition = APPS.TENANT.eq(tenant).and(APPS.ID.eq(appId));
    if (!includeDeleted) whereCondition = whereCondition.and(APPS.DELETED.eq(false));

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Use either provided version or latest version
      if (!StringUtils.isBlank(appVersion))
      {
        whereCondition = whereCondition.and(APPS_VERSIONS.VERSION.eq(fetchVersion));
      }
      else
      {
        fetchVersion = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId)).fetchOne(APPS.LATEST_VERSION);
        whereCondition = whereCondition.and(APPS_VERSIONS.VERSION.eq(fetchVersion));
      }

      // Fetch all attributes by joining APPS and APPS_VERSIONS tables
      Record appRecord;
      appRecord = db.selectFrom(APPS.join(APPS_VERSIONS).on(APPS_VERSIONS.APP_SEQ_ID.eq(APPS.SEQ_ID)))
                         .where(whereCondition).fetchOne();
      if (appRecord == null) return null;

      // Create an App object using the appRecord
      app = getAppFromJoinRecord(appRecord);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return app;
  }

  /**
   * getAppsCount
   * Count all Apps matching various search and sort criteria.
   *     Search conditions given as a list of strings or an abstract syntax tree (AST).
   * Conditions in searchList must be processed by SearchUtils.validateAndExtractSearchCondition(cond)
   *   prior to this call for proper validation and treatment of special characters.
   * WARNING: If both searchList and searchAST provided only searchList is used.
   * NOTE: Use versionSpecified = null to indicate this method should determine if a search condition specifies
   *       which versions to retrieve.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param searchAST - AST containing search conditions
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param versionSpecified - indicates (if known) if we are to get just latest version or all versions specified
   *                     by a search condition. Use null to indicate not known and this method should determine.
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @param listType - allows for filtering results based on authorization: OWNED, SHARED_PUBLIC, ALL
   * @param viewableIDs - list of IDs to include due to permission READ or MODIFY
   * @param sharedIDs - list of IDs shared with the requester or only shared publicly.
   * @return - count of items
   * @throws TapisException - on error
   */
  @Override
  public int getAppsCount(ResourceRequestUser rUser, List<String> searchList, ASTNode searchAST,
                          List<OrderBy> orderByList, String startAfter, Boolean versionSpecified, boolean includeDeleted,
                          AuthListType listType, Set<String> viewableIDs, Set<String> sharedIDs)
          throws TapisException
  {
    // Ensure we have a valid listType
    if (listType == null) listType = DEFAULT_LIST_TYPE;
    // For convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();
    boolean allItems = AuthListType.ALL.equals(listType);
    boolean publicOnly = AuthListType.SHARED_PUBLIC.equals(listType);
    boolean ownedOnly = AuthListType.OWNED.equals(listType);
    boolean sharedOnly = AuthListType.SHARED_DIRECT.equals(listType);
    boolean mine = AuthListType.MINE.equals(listType);
    boolean readPermOnly = AuthListType.READ_PERM.equals(listType);

    // If only looking for public items or only looking for directly shared items
    //   and there are none in the list we are done.
    if ((publicOnly || sharedOnly) && (sharedIDs == null || sharedIDs.isEmpty())) return 0;
    if (readPermOnly && (viewableIDs == null || viewableIDs.isEmpty())) return 0;

    // Ensure we have valid viewable and shared ID sets.
    if (viewableIDs == null) viewableIDs = Collections.emptySet();
    if (sharedIDs == null) sharedIDs = Collections.emptySet();

    // Ensure we have a non-null orderByList
    List<OrderBy> tmpOrderByList = new ArrayList<>();
    if (orderByList != null) tmpOrderByList = orderByList;

    // Determine the primary orderBy column (i.e. first in list). Used for startAfter
    String majorOrderByStr = null;
    OrderByDir majorSortDirection = DEFAULT_ORDERBY_DIRECTION;
    if (!tmpOrderByList.isEmpty())
    {
      majorOrderByStr = tmpOrderByList.get(0).getOrderByAttr();
      majorSortDirection = tmpOrderByList.get(0).getOrderByDir();
    }

    // Determine if we are doing an asc sort, important for startAfter
    boolean sortAsc = majorSortDirection != OrderByDir.DESC;

    // If startAfter is given then orderBy is required
    if (!StringUtils.isBlank(startAfter) && StringUtils.isBlank(majorOrderByStr))
    {
      throw new TapisException(LibUtils.getMsg("APPLIB_DB_INVALID_SORT_START", APPS.getName()));
    }

    // Validate orderBy columns
    // If orderBy column not found then it is an error
    // For count we do not need the actual column, so we just check that the column exists.
    // Down below in getApps() we need the actual column
    for (OrderBy orderBy : tmpOrderByList)
    {
      String orderByStr = orderBy.getOrderByAttr();
      if ((StringUtils.isBlank(orderByStr)) ||
           (!APPS_FIELDS.contains(SearchUtils.camelCaseToSnakeCase(orderByStr))) &&
           (!APPS_VERSIONS_FIELDS.contains(SearchUtils.camelCaseToSnakeCase(orderByStr))))
      {
        String msg = LibUtils.getMsg("APPLIB_DB_NO_COLUMN_SORT", DSL.name(orderByStr));
        throw new TapisException(msg);
      }
    }

    // Boolean used to determine if we are to get just latest version or all versions specified by a search condition
    // If searchList and searchAST are both provided then only searchList is checked.
    if (versionSpecified == null) versionSpecified = checkForVersion(searchList, searchAST);

    // Begin where condition for this query
    // Start with either tenant = <tenant> or
    //                   tenant = <tenant> and deleted = false
    Condition whereCondition;
    if (includeDeleted) whereCondition = APPS.TENANT.eq(oboTenant);
    else whereCondition = (APPS.TENANT.eq(oboTenant)).and(APPS.DELETED.eq(false));

    // If version was not specified then add condition to select only latest version of each app
    if (!versionSpecified) whereCondition = whereCondition.and(APPS_VERSIONS.VERSION.eq(APPS.LATEST_VERSION));

    // Add searchList or searchAST to where condition
    if (searchList != null)
    {
      whereCondition = addSearchListToWhere(whereCondition, searchList);
    }
    else if (searchAST != null)
    {
      Condition astCondition = createConditionFromAst(searchAST);
      if (astCondition != null) whereCondition = whereCondition.and(astCondition);
    }

    // Add startAfter.
    if (!StringUtils.isBlank(startAfter))
    {
      // Build search string, so we can re-use code for checking and adding a condition
      String searchStr;
      if (sortAsc) searchStr = majorOrderByStr + ".gt." + startAfter;
      else searchStr = majorOrderByStr + ".lt." + startAfter;
      whereCondition = addSearchCondStrToWhere(whereCondition, searchStr, AND);
    }

    // Build and add the listType condition:
    Condition listTypeCondition = null;
    if (ownedOnly)
    {
      // Only those owned by user
      listTypeCondition = APPS.OWNER.eq(oboUser);
    }
    else if (publicOnly || sharedOnly)
    {
      // Only those in sharedIDs list
      // NOTE: We check above for sharedIDs == null or is empty so no need to do it here
      listTypeCondition = APPS.ID.in(sharedIDs);
    }
    else if (mine)
    {
      // Owned by user or in sharedIDs list
      listTypeCondition = APPS.OWNER.eq(oboUser);
      if (!sharedIDs.isEmpty()) listTypeCondition = listTypeCondition.or(APPS.ID.in(sharedIDs));
    }
    else if (readPermOnly)
    {
      // Only those where user was granted READ or MODIFY permission
      listTypeCondition = APPS.ID.in(viewableIDs);
    }
    else if (allItems)
    {
      // Everything: owned, shared direct, shared public, READ/MODIFY perm
      listTypeCondition = APPS.OWNER.eq(oboUser);
      var setOfIDs = new HashSet<String>();
      if (!sharedIDs.isEmpty()) setOfIDs.addAll(sharedIDs);
      if (!viewableIDs.isEmpty()) setOfIDs.addAll(viewableIDs);
      if (!setOfIDs.isEmpty()) listTypeCondition = listTypeCondition.or(APPS.ID.in(setOfIDs));
    }

    whereCondition = whereCondition.and(listTypeCondition);

    // ------------------------- Build and execute SQL ----------------------------
    int count = 0;
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Execute the select including startAfter
      // NOTE: This is much simpler than the same section in getApps() because we are not ordering since
      //       we only want the count, and we are not limiting (we want a count of all records).
      Integer c = db.selectCount().from(APPS.join(APPS_VERSIONS).on(APPS_VERSIONS.APP_SEQ_ID.eq(APPS.SEQ_ID)))
                                  .where(whereCondition).fetchOne(0,int.class);
      if (c != null) count = c;

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "apps", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return count;
  }

  /**
   * getApps
   * Retrieve all Apps matching various search and sort criteria.
   *     Search conditions given as a list of strings or an abstract syntax tree (AST).
   * Conditions in searchList must be processed by SearchUtils.validateAndExtractSearchCondition(cond)
   *   prior to this call for proper validation and treatment of special characters.
   * NOTE: Use versionSpecified = null to indicate this method should determine if a search condition specifies
   *       which versions to retrieve.
   * WARNING: If both searchList and searchAST provided only searchList is used.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param searchAST - AST containing search conditions
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param versionSpecified - indicates (if known) if we are to get just latest version or all versions specified
   *                     by a search condition. Use null to indicate not known and this method should determine.
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @param listType - allows for filtering results based on authorization: OWNED, SHARED_PUBLIC, SHARED_DIRECT, MINE, READ_PERM, ALL
   * @param viewableIDs - list of IDs to include due to permission READ or MODIFY
   * @param sharedIDs - list of IDs shared with the requester or only shared publicly.
   * @return - list of TSystem objects
   * @throws TapisException - on error
   */
  @Override
  public List<App> getApps(ResourceRequestUser rUser, List<String> searchList, ASTNode searchAST, int limit,
                           List<OrderBy> orderByList, int skip, String startAfter, Boolean versionSpecified,
                           boolean includeDeleted, AuthListType listType, Set<String> viewableIDs, Set<String> sharedIDs)
          throws TapisException
  {
    // The result list should always be non-null.
    var retList = new ArrayList<App>();

    // Ensure we have a valid listType
    if (listType == null) listType = DEFAULT_LIST_TYPE;

    // For convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();
    boolean allItems = AuthListType.ALL.equals(listType);
    boolean publicOnly = AuthListType.SHARED_PUBLIC.equals(listType);
    boolean ownedOnly = AuthListType.OWNED.equals(listType);
    boolean sharedOnly = AuthListType.SHARED_DIRECT.equals(listType);
    boolean mine = AuthListType.MINE.equals(listType);
    boolean readPermOnly = AuthListType.READ_PERM.equals(listType);

    // If only looking for public items or only looking for directly shared items
    //   and there are none in the list we are done.
    if ((publicOnly || sharedOnly) && (sharedIDs == null || sharedIDs.isEmpty())) return retList;
    if (readPermOnly && (viewableIDs == null || viewableIDs.isEmpty())) return retList;

    // Ensure we have valid viewable and shared ID sets.
    if (viewableIDs == null) viewableIDs = Collections.emptySet();
    if (sharedIDs == null) sharedIDs = Collections.emptySet();

    // Ensure we have a non-null orderByList
    List<OrderBy> tmpOrderByList = new ArrayList<>();
    if (orderByList != null) tmpOrderByList = orderByList;

    // Determine the primary orderBy column (i.e. first in list). Used for startAfter
    String majorOrderByStr = null;
    OrderByDir majorSortDirection = DEFAULT_ORDERBY_DIRECTION;
    if (!tmpOrderByList.isEmpty())
    {
      majorOrderByStr = tmpOrderByList.get(0).getOrderByAttr();
      majorSortDirection = tmpOrderByList.get(0).getOrderByDir();
    }

    // Negative skip indicates no skip
    if (skip < 0) skip = 0;

    // Determine if we are doing an asc sort, important for startAfter
    boolean sortAsc = majorSortDirection != OrderByDir.DESC;

    // If startAfter is given then orderBy is required
    if (!StringUtils.isBlank(startAfter) && StringUtils.isBlank(majorOrderByStr))
    {
      throw new TapisException(LibUtils.getMsg("APPLIB_DB_INVALID_SORT_START", APPS.getName()));
    }

    // Determine and check orderBy columns, build orderFieldList
    // Each OrderField contains the column and direction
    List<OrderField> orderFieldList = new ArrayList<>();
    Field<?> colOrderBy;
    for (OrderBy orderBy : tmpOrderByList)
    {
      String orderByStr = orderBy.getOrderByAttr();
      String orderByStrSC = SearchUtils.camelCaseToSnakeCase(orderByStr);
      if (APPS_FIELDS.contains(orderByStrSC))
      {
        colOrderBy = APPS.field(DSL.name(orderByStrSC));
      }
      else if (APPS_VERSIONS_FIELDS.contains(orderByStrSC))
      {
        colOrderBy = APPS_VERSIONS.field(DSL.name(orderByStrSC));
      }
      else
      {
        String msg = LibUtils.getMsg("APPLIB_DB_NO_COLUMN_SORT", DSL.name(orderByStr));
        throw new TapisException(msg);
      }
      if (orderBy.getOrderByDir() == OrderBy.OrderByDir.ASC) orderFieldList.add(colOrderBy.asc());
      else orderFieldList.add(colOrderBy.desc());
    }

    // Boolean used to determine if we are to get just latest version or all versions specified by a search condition
    // If searchList and searchAST are both provided then only searchList is checked.
    if (versionSpecified == null) versionSpecified = checkForVersion(searchList, searchAST);

    // Begin where condition for the query
    // Start with either tenant = <tenant> or
    //                   tenant = <tenant> and deleted = false
    Condition whereCondition;
    if (includeDeleted) whereCondition = APPS.TENANT.eq(oboTenant);
    else whereCondition = (APPS.TENANT.eq(oboTenant)).and(APPS.DELETED.eq(false));

    // If version was not specified then add condition to select only latest version of each app
    if (!versionSpecified) whereCondition = whereCondition.and(APPS_VERSIONS.VERSION.eq(APPS.LATEST_VERSION));


    // Add searchList or searchAST to where condition
    if (searchList != null)
    {
      whereCondition = addSearchListToWhere(whereCondition, searchList);
    }
    else if (searchAST != null)
    {
      Condition astCondition = createConditionFromAst(searchAST);
      if (astCondition != null) whereCondition = whereCondition.and(astCondition);
    }

    // Add startAfter
    if (!StringUtils.isBlank(startAfter))
    {
      // Build search string, so we can re-use code for checking and adding a condition
      String searchStr;
      if (sortAsc) searchStr = majorOrderByStr + ".gt." + startAfter;
      else searchStr = majorOrderByStr + ".lt." + startAfter;
      whereCondition = addSearchCondStrToWhere(whereCondition, searchStr, AND);
    }

    // Build and add the listType condition:
    //  OWNED = single condition where owner = oboUser
    //  PUBLIC = single condition where id in setOfIDs
    //  ALL = where (owner = oboUser) OR (id in setOfIDs)
    Condition listTypeCondition = null;
    if (ownedOnly)
    {
      // Only those owned by user
      listTypeCondition = APPS.OWNER.eq(oboUser);
    }
    else if (publicOnly || sharedOnly)
    {
      // Only those in sharedIDs list
      // NOTE: We check above for sharedIDs == null or is empty so no need to do it here
      listTypeCondition = APPS.ID.in(sharedIDs);
    }
    else if (mine)
    {
      // Owned by user or in sharedIDs list
      listTypeCondition = APPS.OWNER.eq(oboUser);
      if (!sharedIDs.isEmpty()) listTypeCondition = listTypeCondition.or(APPS.ID.in(sharedIDs));
    }
    else if (readPermOnly)
    {
      // Only those where user was granted READ or MODIFY permission
      listTypeCondition = APPS.ID.in(viewableIDs);
    }
    else if (allItems)
    {
      // Everything: owned, shared direct, shared public, READ/MODIFY perm
      listTypeCondition = APPS.OWNER.eq(oboUser);
      var setOfIDs = new HashSet<String>();
      if (!sharedIDs.isEmpty()) setOfIDs.addAll(sharedIDs);
      if (!viewableIDs.isEmpty()) setOfIDs.addAll(viewableIDs);
      if (!setOfIDs.isEmpty()) listTypeCondition = listTypeCondition.or(APPS.ID.in(setOfIDs));
    }

    whereCondition = whereCondition.and(listTypeCondition);

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Execute the select including limit, orderByAttrList, skip and startAfter
      // NOTE: LIMIT + OFFSET is not standard among DBs and often very difficult to get right.
      //       Jooq claims to handle it well.
      // Join tables APPS and APPS_VERSIONS to get all fields
      Result<Record> results;
      org.jooq.SelectConditionStep condStep =
              db.selectFrom(APPS.join(APPS_VERSIONS).on(APPS_VERSIONS.APP_SEQ_ID.eq(APPS.SEQ_ID))).where(whereCondition);
      if (!StringUtils.isBlank(majorOrderByStr) && limit >= 0)
      {
        // We are ordering and limiting
        results = condStep.orderBy(orderFieldList).limit(limit).offset(skip).fetch();
      }
      else if (!StringUtils.isBlank(majorOrderByStr))
      {
        // We are ordering but not limiting
        results = condStep.orderBy(orderFieldList).fetch();
      }
      else if (limit >= 0)
      {
        // We are limiting but not ordering
        results = condStep.limit(limit).offset(skip).fetch();
      }
      else
      {
        // We are not limiting and not ordering
        results = condStep.fetch();
      }

      if (results == null || results.isEmpty()) return retList;

      // For each record found create an App object.
      for (Record appRecord : results)
      {
        // Create App from appRecord using appVersion=null to use the latest app version
        App a = getAppFromJoinRecord(appRecord);
        retList.add(a);
      }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "apps", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * getAppIDs
   * Fetch all resource IDs in a tenant
   * @param tenant - tenant name
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @return - List of app names
   * @throws TapisException - on error
   */
  @Override
  public Set<String> getAppIDs(String tenant, boolean includeDeleted) throws TapisException
  {
    // The result list is always non-null.
    var idList = new HashSet<String>();

    Condition whereCondition;
    if (includeDeleted) whereCondition = APPS.TENANT.eq(tenant);
    else whereCondition = (APPS.TENANT.eq(tenant)).and(APPS.DELETED.eq(false));

    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      // ------------------------- Call SQL ----------------------------
      // Use jOOQ to build query string
      DSLContext db = DSL.using(conn);
      Result<?> result = db.select(APPS.ID).from(APPS).where(whereCondition).fetch();
      // Iterate over result
      for (Record r : result) { idList.add(r.get(APPS.ID)); }
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "apps", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return idList;
  }

  /**
   * getAppOwner
   * @param tenant - name of tenant
   * @param appId - name of app
   * @return Owner or null if no app found
   * @throws TapisException - on error
   */
  @Override
  public String getAppOwner(String tenant, String appId) throws TapisException
  {
    String owner = null;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      owner = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId)).fetchOne(APPS.OWNER);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "apps", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return owner;
  }

  /**
   * Add an update record given the app Id, app version and operation type
   *
   */
  @Override
  public void addUpdateRecord(ResourceRequestUser rUser, String tenant, String appId, String appVer,
                              AppOperation op, String changeDescription, String rawData)
          throws TapisException
  {
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      addUpdate(db, rUser, tenant, appId, appVer, INVALID_SEQ_ID, INVALID_SEQ_ID, op,
              changeDescription, rawData, INVALID_UUID);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "apps", appId);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Retrieves App History list from given tenant and app name.
   *
   * @param oboTenantId - The tenant ID
   * @param appId - App name
   * @return - App history list
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<AppHistoryItem> getAppHistory(String oboTenantId, String appId) throws TapisException
  {
    // Initialize result.
    List<AppHistoryItem> appHistoryList =  new ArrayList<AppHistoryItem>();;

    // Begin where condition for the query
    Condition whereCondition = APP_UPDATES.OBO_TENANT.eq(oboTenantId).and(APP_UPDATES.APP_ID.eq(appId));

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);


      // Fetch all attributes by joining APPS and APPS_VERSIONS tables
      @org.jetbrains.annotations.NotNull SelectConditionStep<Record> results;
      results = db.selectFrom(APP_UPDATES.join(APPS_VERSIONS).on(APPS_VERSIONS.APP_SEQ_ID.eq(APP_UPDATES.APP_SEQ_ID)))
              .where(whereCondition);
      if (results == null) return null;

      // Create an App object using the appRecord
      for (Record r : results) { AppHistoryItem s = getAppHistoryFromRecord(r); appHistoryList.add(s); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", oboTenantId, appId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return appHistoryList;
  }

  /**
   * Check to see if a search condition references the column APPS_VERSIONS.VERSION
   * @param condStr Single search condition in the form column_name.op.value
   * @return true if condition references version else false
   */
  public static boolean checkCondForVersion(String condStr)
  {
    if (StringUtils.isBlank(condStr)) return false;
    // Parse search value into column name, operator and value
    // Format must be column_name.op.value
    String[] parsedStrArray = DOT_SPLIT.split(condStr, 3);
    if (parsedStrArray.length < 1) return false;
    else return parsedStrArray[0].equalsIgnoreCase(APPS_VERSIONS.VERSION.getName());
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /**
   * Given an sql connection and basic info add a change history record
   * If appSeqId < 1 then appSeqId is fetched.
   * If appVerSeqId < 1 and version is provided then appVerSeqId is fetched.
   * NOTE: Both app tenant and user tenant are recorded. If a service makes an update on behalf of itself
   *       the tenants may differ.
   *
   * @param db - Database connection
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param tenant - Tenant of the app being updated
   * @param id - Id of the app being updated
   * @param version - Version of the app being updated, may be null
   * @param appSeqId - Sequence Id of app being updated, if < 1 will be fetched
   * @param appVerSeqId - Sequence Id of version being updated, if < 1 and version given will be fetched
   * @param op - Operation, such as create, modify, etc.
   * @param changeDescription - JSON representing the update - with secrets scrubbed
   * @param rawData - Text data supplied by client - secrets should be scrubbed
   */
  private static void addUpdate(DSLContext db, ResourceRequestUser rUser, String tenant, String id,
                                String version, int appSeqId, int appVerSeqId, AppOperation op,
                                String changeDescription, String rawData, UUID uuid)
  {
    String updJsonStr = (StringUtils.isBlank(changeDescription)) ? EMPTY_JSON_OBJ_STR : changeDescription;
    if (appSeqId < 1)
    {
      appSeqId = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(id)).fetchOne(APPS.SEQ_ID);
    }
    if (appVerSeqId < 1 && !StringUtils.isBlank(version))
    {
      appVerSeqId = db.selectFrom(APPS_VERSIONS)
        .where(APPS_VERSIONS.APP_SEQ_ID.eq(appSeqId),APPS_VERSIONS.VERSION.eq(version))
        .fetchOne(APPS_VERSIONS.SEQ_ID);
    }
    // Persist update record
    db.insertInto(APP_UPDATES)
            .set(APP_UPDATES.APP_SEQ_ID, appSeqId)
            .set(APP_UPDATES.APP_VER_SEQ_ID, appVerSeqId)
            .set(APP_UPDATES.JWT_TENANT, rUser.getJwtTenantId())
            .set(APP_UPDATES.JWT_USER, rUser.getJwtUserId())
            .set(APP_UPDATES.OBO_TENANT, rUser.getOboTenantId())
            .set(APP_UPDATES.OBO_USER, rUser.getOboUserId())
            .set(APP_UPDATES.APP_ID, id)
            .set(APP_UPDATES.APP_VERSION, version)
            .set(APP_UPDATES.OPERATION, op)
            .set(APP_UPDATES.DESCRIPTION, TapisGsonUtils.getGson().fromJson(updJsonStr, JsonElement.class))
            .set(APP_UPDATES.RAW_DATA, rawData)
            .set(APP_UPDATES.UUID, uuid)
            .execute();
  }

  /**
   * Given an sql connection check to see if specified app exists. Inclusion of deleted items determined by flag.
   * @param db - jooq context
   * @param tenant - name of tenant
   * @param appId - Id of app
   * @param appVersion - version of app, null if check is for any version
   * @param includeDeleted - whether to include deleted items
   * @return - true if app exists according to given conditions, else false
   */
  private static boolean checkIfAppExists(DSLContext db, String tenant, String appId, String appVersion,
                                          boolean includeDeleted)
  {
    Integer appSeqId;
    // First check if app with given ID is present.
    if (includeDeleted)
    {
      appSeqId = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId))
                   .fetchOne(APPS.SEQ_ID);
    }
    else
    {
      appSeqId = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId),APPS.DELETED.eq(false))
                   .fetchOne(APPS.SEQ_ID);
    }

    if (appSeqId == null) return false;

    // So app does exist and appSeqId has been set.
    // If we do not care about version then we are done
    if (StringUtils.isBlank(appVersion)) return true;

    // We do care about version so check for specific version.
    return db.fetchExists(APPS_VERSIONS, APPS_VERSIONS.APP_SEQ_ID.eq(appSeqId), APPS_VERSIONS.VERSION.eq(appVersion));
  }

  /**
   * Given an sql connection check to see if specified app has been marked as deleted.
   * @param db - jooq context
   * @param tenant - name of tenant
   * @param appId - Id of app
   * @return - true if app has been deleted else false
   */
  private static boolean isDeleted(DSLContext db, String tenant, String appId)
  {
    return db.fetchExists(APPS, APPS.TENANT.eq(tenant), APPS.ID.eq(appId), APPS.DELETED.eq(true));
  }

  /**
   * Given an sql connection retrieve the app sequence id.
   * @param db - jooq context
   * @param tenant - name of tenant
   * @param appId - Id of app
   * @return - app sequence id
   */
  private static int getAppSeqIdUsingDb(DSLContext db, String tenant, String appId)
  {
    Integer sid = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.ID.eq(appId),APPS.DELETED.eq(false))
                      .fetchOne(APPS.SEQ_ID);
    if (sid == null) return 0;
    else return sid;
  }

  /**
   * Given an appRecord from a JOIN, create an App object
   *
   */
  private static App getAppFromJoinRecord(Record r)
  {
    App app;
    int appSeqId = r.get(APPS.SEQ_ID);
    int appVerSeqId = r.get(APPS_VERSIONS.SEQ_ID);

    // Put together full App model object
    // Convert LocalDateTime to Instant. Note that although "Local" is in the type, timestamps from the DB are in UTC.
    Instant created = r.get(APPS_VERSIONS.CREATED).toInstant(ZoneOffset.UTC);
    Instant updated = r.get(APPS_VERSIONS.UPDATED).toInstant(ZoneOffset.UTC);

    // Convert runtimeOption strings to enums
    String[] runtimeOptionsStrArray = r.get(APPS_VERSIONS.RUNTIME_OPTIONS);
    List<RuntimeOption> runtimeOptions = null;
    if (runtimeOptionsStrArray != null && runtimeOptionsStrArray.length != 0)
    {
      runtimeOptions = Arrays.stream(runtimeOptionsStrArray).map(RuntimeOption::valueOf).collect(Collectors.toList());
    }

    // Build lists for ParameterSet arguments, FileInputs, FileInputArrays.
    JsonElement parmSetJsonElement = r.get(APPS_VERSIONS.PARAMETER_SET);

    ParameterSet parmSet = TapisGsonUtils.getGson().fromJson(parmSetJsonElement, ParameterSet.class);
    // NOTE: At this point all ArgSpec notes inside parmSet are of type com.google.gson.internal.LinkedTreeMap
    //       gson has done the conversion because the embedded field is of type object.
    JsonElement fiJsonElement = r.get(APPS_VERSIONS.FILE_INPUTS);
    List<FileInput> fileInputs = Arrays.asList(TapisGsonUtils.getGson().fromJson(fiJsonElement, FileInput[].class));
    JsonElement fiaJsonElement = r.get(APPS_VERSIONS.FILE_INPUT_ARRAYS);
    List<FileInputArray> fileInputArrays = Arrays.asList(TapisGsonUtils.getGson().fromJson(fiaJsonElement, FileInputArray[].class));
    JsonElement subscriptionsJsonElement = r.get(APPS_VERSIONS.SUBSCRIPTIONS);
    List<ReqSubscribe> subscriptions =
            Arrays.asList(TapisGsonUtils.getGson().fromJson(subscriptionsJsonElement, ReqSubscribe[].class));
    app = new App(appSeqId, appVerSeqId, r.get(APPS.TENANT), r.get(APPS.ID), r.get(APPS_VERSIONS.VERSION),
            r.get(APPS_VERSIONS.DESCRIPTION), r.get(APPS_VERSIONS.JOB_TYPE), r.get(APPS.OWNER),
            r.get(APPS.ENABLED), r.get(APPS_VERSIONS.VERSION_ENABLED), r.get(APPS_VERSIONS.LOCKED), r.get(APPS.CONTAINERIZED),
            r.get(APPS_VERSIONS.RUNTIME), r.get(APPS_VERSIONS.RUNTIME_VERSION), runtimeOptions,
            r.get(APPS_VERSIONS.CONTAINER_IMAGE), r.get(APPS_VERSIONS.MAX_JOBS), r.get(APPS_VERSIONS.MAX_JOBS_PER_USER),
            r.get(APPS_VERSIONS.STRICT_FILE_INPUTS), r.get(APPS_VERSIONS.JOB_DESCRIPTION),
            r.get(APPS_VERSIONS.DYNAMIC_EXEC_SYSTEM), r.get(APPS_VERSIONS.EXEC_SYSTEM_CONSTRAINTS),
            r.get(APPS_VERSIONS.EXEC_SYSTEM_ID), r.get(APPS_VERSIONS.EXEC_SYSTEM_EXEC_DIR),
            r.get(APPS_VERSIONS.EXEC_SYSTEM_INPUT_DIR), r.get(APPS_VERSIONS.EXEC_SYSTEM_OUTPUT_DIR),
            r.get(APPS_VERSIONS.DTN_SYSTEM_INPUT_DIR), r.get(APPS_VERSIONS.DTN_SYSTEM_OUTPUT_DIR),
            r.get(APPS_VERSIONS.EXEC_SYSTEM_LOGICAL_QUEUE), r.get(APPS_VERSIONS.ARCHIVE_SYSTEM_ID),
            r.get(APPS_VERSIONS.ARCHIVE_SYSTEM_DIR), r.get(APPS_VERSIONS.ARCHIVE_ON_APP_ERROR),
            r.get(APPS_VERSIONS.IS_MPI), r.get(APPS_VERSIONS.MPI_CMD), r.get(APPS_VERSIONS.CMD_PREFIX),
            parmSet, fileInputs, fileInputArrays, r.get(APPS_VERSIONS.NODE_COUNT), r.get(APPS_VERSIONS.CORES_PER_NODE),
            r.get(APPS_VERSIONS.MEMORY_MB), r.get(APPS_VERSIONS.MAX_MINUTES), subscriptions,
            r.get(APPS_VERSIONS.JOB_TAGS), r.get(APPS_VERSIONS.TAGS), (JsonObject) r.get(APPS_VERSIONS.NOTES),
            r.get(APPS_VERSIONS.UUID), r.get(APPS.DELETED), created, updated);
    return app;
  }

  /**
   * Given an sql connection retrieve the app_ver uuid.
   * @param db - jooq context
   * @param id - Id of app
   * @param version - Version of app
   * @return - uuid
   */
  private static UUID getUUIDUsingDb(DSLContext db, String tenantId, String id, String version)
  {
    int appSeqId = getAppSeqIdUsingDb(db, tenantId, id);
    return db.selectFrom(APPS_VERSIONS).where(APPS_VERSIONS.APP_SEQ_ID.eq(appSeqId),APPS_VERSIONS.VERSION.eq(version))
            .fetchOne(APPS_VERSIONS.UUID);
  }

  /**
   * Determine if a searchList or searchAST contains a condition on the version column of the APPS_VERSIONS table.
   * If searchList and searchAST are both provided then only searchList is checked.
   * @param searchList List of conditions to add to the base condition
   * @return true if one of the search conditions references version else false
   * @throws TapisException on error
   */
  private static boolean checkForVersion(List<String> searchList, ASTNode searchAST) throws TapisException
  {
    boolean versionSpecified = false;
    if (searchList != null)
    {
      for (String condStr : searchList)
      {
        if (checkCondForVersion(condStr)) return true;
      }
    }
    else if (searchAST != null)
    {
      return checkASTNodeForVersion(searchAST);
    }
    return versionSpecified;
  }

  /**
   * Check to see if any conditions in an ASTNode reference the column APPS_VERSIONS.VERSION
   * @param astNode Node to check
   * @return true if condition references version else false
   * @throws  TapisException on error
   */
  private static boolean checkASTNodeForVersion(ASTNode astNode) throws TapisException
  {
    if (astNode == null || astNode instanceof ASTLeaf)
    {
      // A leaf node is a column name or value. Nothing to process since we only process a complete condition
      //   having the form column_name.op.value. We should never make it to here
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST1", (astNode == null ? "null" : astNode.toString()));
      throw new TapisException(msg);
    }
    else if (astNode instanceof ASTUnaryExpression)
    {
      // A unary node should have no operator and contain a binary node with two leaf nodes.
      // NOTE: Currently unary operators not supported. If support is provided for unary operators (such as NOT) then
      //   changes will be needed here.
      ASTUnaryExpression unaryNode = (ASTUnaryExpression) astNode;
      if (!StringUtils.isBlank(unaryNode.getOp()))
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_UNARY_OP", unaryNode.getOp(), unaryNode.toString());
        throw new TapisException(msg);
      }
      // Recursive call
      return checkASTNodeForVersion(unaryNode.getNode());
    }
    else if (astNode instanceof ASTBinaryExpression)
    {
      // It is a binary node
      ASTBinaryExpression binaryNode = (ASTBinaryExpression) astNode;
      // Recursive call
      return checkForVersionInBinaryExpression(binaryNode);
    }
    return false;
  }

  /**
   * Check to see if any conditions in a binary ASTNode reference the column APPS_VERSIONS.VERSION
   * @param binaryNode node to check
   * @return true if condition references version else false
   * @throws  TapisException on error
   */
  private static boolean checkForVersionInBinaryExpression(ASTBinaryExpression binaryNode) throws TapisException
  {
    // If we are given a null then something went very wrong.
    if (binaryNode == null)
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST2");
      throw new TapisException(msg);
    }
    // If operator is AND or OR then two sides to check. Treat AND/OR the same since all we care about
    //   is if condition contains version.
    // For other operators build the condition left.op.right and check it
    String op = binaryNode.getOp();
    ASTNode leftNode = binaryNode.getLeft();
    ASTNode rightNode = binaryNode.getRight();
    if (StringUtils.isBlank(op))
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST3", binaryNode.toString());
      throw new TapisException(msg);
    }
    else if (op.equalsIgnoreCase(AND) || op.equalsIgnoreCase(OR))
    {
      // Recursive calls
      return checkASTNodeForVersion(leftNode) || checkASTNodeForVersion(rightNode);
    }
    else
    {
      // End of recursion. Check the single condition
      // Since operator is not an AND or an OR we should have 2 unary nodes or a unary and leaf node
      String lValue;
      String rValue;
      if (leftNode instanceof ASTLeaf) lValue = ((ASTLeaf) leftNode).getValue();
      else if (leftNode instanceof ASTUnaryExpression) lValue =  ((ASTLeaf) ((ASTUnaryExpression) leftNode).getNode()).getValue();
      else
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST5", binaryNode.toString());
        throw new TapisException(msg);
      }
      if (rightNode instanceof ASTLeaf) rValue = ((ASTLeaf) rightNode).getValue();
      else if (rightNode instanceof ASTUnaryExpression) rValue =  ((ASTLeaf) ((ASTUnaryExpression) rightNode).getNode()).getValue();
      else
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST6", binaryNode.toString());
        throw new TapisException(msg);
      }
      // Build the string for the search condition, left.op.right
      String condStr = String.format("%s.%s.%s", lValue, binaryNode.getOp(), rValue);
      return checkCondForVersion(condStr);
    }
  }

  /**
   * Add searchList to where condition. All conditions are joined using AND
   * Validate column name, search comparison operator
   *   and compatibility of column type + search operator + column value
   * @param whereCondition base where condition
   * @param searchList List of conditions to add to the base condition
   * @return resulting where condition
   * @throws TapisException on error
   */
  private static Condition addSearchListToWhere(Condition whereCondition, List<String> searchList)
          throws TapisException
  {
    if (searchList == null || searchList.isEmpty()) return whereCondition;
    // Parse searchList and add conditions to the WHERE clause
    for (String condStr : searchList)
    {
      whereCondition = addSearchCondStrToWhere(whereCondition, condStr, AND);
    }
    return whereCondition;
  }

  /**
   * Create a condition for abstract syntax tree nodes by recursively walking the tree
   * @param astNode Abstract syntax tree node to add to the base condition
   * @return resulting condition
   * @throws TapisException on error
   */
  private static Condition createConditionFromAst(ASTNode astNode) throws TapisException
  {
    if (astNode == null || astNode instanceof ASTLeaf)
    {
      // A leaf node is a column name or value. Nothing to process since we only process a complete condition
      //   having the form column_name.op.value. We should never make it to here
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST1", (astNode == null ? "null" : astNode.toString()));
      throw new TapisException(msg);
    }
    else if (astNode instanceof ASTUnaryExpression)
    {
      // A unary node should have no operator and contain a binary node with two leaf nodes.
      // NOTE: Currently unary operators not supported. If support is provided for unary operators (such as NOT) then
      //   changes will be needed here.
      ASTUnaryExpression unaryNode = (ASTUnaryExpression) astNode;
      if (!StringUtils.isBlank(unaryNode.getOp()))
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_UNARY_OP", unaryNode.getOp(), unaryNode.toString());
        throw new TapisException(msg);
      }
      // Recursive call
      return createConditionFromAst(unaryNode.getNode());
    }
    else if (astNode instanceof ASTBinaryExpression)
    {
      // It is a binary node
      ASTBinaryExpression binaryNode = (ASTBinaryExpression) astNode;
      // Recursive call
      return createConditionFromBinaryExpression(binaryNode);
    }
    return null;
  }

  /**
   * Create a condition from an abstract syntax tree binary node
   * @param binaryNode Abstract syntax tree binary node to add to the base condition
   * @return resulting condition
   * @throws TapisException on error
   */
  private static Condition createConditionFromBinaryExpression(ASTBinaryExpression binaryNode) throws TapisException
  {
    // If we are given a null then something went very wrong.
    if (binaryNode == null)
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST2");
      throw new TapisException(msg);
    }
    // If operator is AND or OR then make recursive call for each side and join together
    // For other operators build the condition left.op.right and add it
    String op = binaryNode.getOp();
    ASTNode leftNode = binaryNode.getLeft();
    ASTNode rightNode = binaryNode.getRight();
    if (StringUtils.isBlank(op))
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST3", binaryNode.toString());
      throw new TapisException(msg);
    }
    else if (op.equalsIgnoreCase(AND))
    {
      // Recursive calls
      Condition cond1 = createConditionFromAst(leftNode);
      Condition cond2 = createConditionFromAst(rightNode);
      if (cond1 == null || cond2 == null)
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST4", binaryNode.toString());
        throw new TapisException(msg);
      }
      return cond1.and(cond2);

    }
    else if (op.equalsIgnoreCase(OR))
    {
      // Recursive calls
      Condition cond1 = createConditionFromAst(leftNode);
      Condition cond2 = createConditionFromAst(rightNode);
      if (cond1 == null || cond2 == null)
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST4", binaryNode.toString());
        throw new TapisException(msg);
      }
      return cond1.or(cond2);

    }
    else
    {
      // End of recursion. Create a single condition.
      // Since operator is not an AND or an OR we should have 2 unary nodes or a unary and leaf node
      String lValue;
      String rValue;
      if (leftNode instanceof ASTLeaf) lValue = ((ASTLeaf) leftNode).getValue();
      else if (leftNode instanceof ASTUnaryExpression) lValue =  ((ASTLeaf) ((ASTUnaryExpression) leftNode).getNode()).getValue();
      else
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST5", binaryNode.toString());
        throw new TapisException(msg);
      }
      if (rightNode instanceof ASTLeaf) rValue = ((ASTLeaf) rightNode).getValue();
      else if (rightNode instanceof ASTUnaryExpression) rValue =  ((ASTLeaf) ((ASTUnaryExpression) rightNode).getNode()).getValue();
      else
      {
        String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_AST6", binaryNode.toString());
        throw new TapisException(msg);
      }
      // Build the string for the search condition, left.op.right
      String condStr = String.format("%s.%s.%s", lValue, binaryNode.getOp(), rValue);
      // Validate and create a condition from the string
      return addSearchCondStrToWhere(null, condStr, null);
    }
  }

  /**
   * Take a string containing a single condition and create a new condition or join it to an existing condition.
   * Validate column name, search comparison operator and compatibility of column type + search operator + column value
   * @param whereCondition existing condition. If null a new condition is returned.
   * @param condStr Single search condition in the form column_name.op.value
   * @param joinOp If whereCondition is not null use AND or OR to join the condition with the whereCondition
   * @return resulting where condition
   * @throws TapisException on error
   */
  private static Condition addSearchCondStrToWhere(Condition whereCondition, String condStr, String joinOp)
          throws TapisException
  {
    // If we have no search string then return what we were given
    if (StringUtils.isBlank(condStr)) return whereCondition;
    // If we are given a condition but no indication of how to join new condition to it then return what we were given
    if (whereCondition != null && StringUtils.isBlank(joinOp)) return whereCondition;
    // NOTE: The "joinOp != null" appears to be necessary even though the IDE might mark it as redundant.
    if (whereCondition != null && joinOp != null && !joinOp.equalsIgnoreCase(AND) && !joinOp.equalsIgnoreCase(OR))
    {
      return whereCondition;
    }

    // Parse search value into column name, operator and value
    // Format must be column_name.op.value
    String[] parsedStrArray = DOT_SPLIT.split(condStr, 3);
    // Validate column name
    String column = parsedStrArray[0];
    // First, check to see if column is on list of unsupported attributes.
    if (App.SEARCH_ATTRS_UNSUPPORTED.contains(DSL.name(column).toString()))
    {
      throw new TapisException(LibUtils.getMsg("APPLIB_DB_SRCH_ATTR_UNSUPPORTED", APPS.getName(), DSL.name(column)));
    }


    // Column must be in either APPS table or APPS_VERSIONS table
    Field<?> col = null; //APPS.field(DSL.name(column));
    // Convert column name to camel case.
    String colNameSC = SearchUtils.camelCaseToSnakeCase(column);
    // Determine and check orderBy column
    if (APPS_FIELDS.contains(colNameSC))
    {
      col = APPS.field(DSL.name(colNameSC));
    }
    else if (APPS_VERSIONS_FIELDS.contains(colNameSC))
    {
      col = APPS_VERSIONS.field(DSL.name(colNameSC));
    }

    if (col == null)
    {
      String msg = LibUtils.getMsg("APPLIB_DB_NO_COLUMN", DSL.name(column));
      throw new TapisException(msg);
    }

    // Validate and convert operator string
    String opStr = parsedStrArray[1].toUpperCase();
    SearchOperator op = SearchUtils.getSearchOperator(opStr);
    if (op == null)
    {
      String msg = MsgUtils.getMsg("APPLIB_DB_INVALID_SEARCH_OP", opStr, APPS.getName(), DSL.name(column));
      throw new TapisException(msg);
    }

    // Check that column value is compatible for column type and search operator
    String val = parsedStrArray[2];
    checkConditionValidity(col, op, val);

     // If val is a timestamp then convert the string(s) to a form suitable for SQL
    // Use a utility method since val may be a single item or a list of items, e.g. for the BETWEEN operator
    if (col.getDataType().getSQLType() == Types.TIMESTAMP)
    {
      val = SearchUtils.convertValuesToTimestamps(op, val);
    }

    // Create the condition
    Condition newCondition = createCondition(col, op, val);
    // If specified add the condition to the WHERE clause
    if (StringUtils.isBlank(joinOp) || whereCondition == null) return newCondition;
    else if (joinOp.equalsIgnoreCase("AND")) return whereCondition.and(newCondition);
    else if (joinOp.equalsIgnoreCase("OR")) return whereCondition.or(newCondition);
    return newCondition;
  }

  /**
   * Validate condition expression based on column type, search operator and column string value.
   * Use java.sql.Types for validation.
   * @param col jOOQ column
   * @param op Operator
   * @param valStr Column value as string
   * @throws TapisException on error
   */
  private static void checkConditionValidity(Field<?> col, SearchOperator op, String valStr) throws TapisException
  {
    var dataType = col.getDataType();
    int sqlType = dataType.getSQLType();
    String sqlTypeName = dataType.getTypeName();
//    var t2 = dataType.getSQLDataType();
//    var t3 = dataType.getCastTypeName();
//    var t4 = dataType.getSQLType();
//    var t5 = dataType.getType();

    // Make sure we support the sqlType
    if (SearchUtils.ALLOWED_OPS_BY_TYPE.get(sqlType) == null)
    {
      String msg = LibUtils.getMsg("APPLIB_DB_UNSUPPORTED_SQLTYPE", APPS.getName(), col.getName(), op.name(), sqlTypeName);
      throw new TapisException(msg);
    }
    // Check that operation is allowed for column data type
    if (!SearchUtils.ALLOWED_OPS_BY_TYPE.get(sqlType).contains(op))
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_TYPE", APPS.getName(), col.getName(), op.name(), sqlTypeName);
      throw new TapisException(msg);
    }

    // Check that value (or values for op that takes a list) are compatible with sqlType
    if (!SearchUtils.validateTypeAndValueList(sqlType, op, valStr, sqlTypeName, APPS.getName(), col.getName()))
    {
      String msg = LibUtils.getMsg("APPLIB_DB_INVALID_SEARCH_VALUE", op.name(), sqlTypeName, valStr, APPS.getName(), col.getName());
      throw new TapisException(msg);
    }
  }

  /**
   * Add condition to SQL where clause given column, operator, value info
   * @param col jOOQ column
   * @param op Operator
   * @param val Column value
   * @return Resulting where clause
   */
  private static Condition createCondition(Field col, SearchOperator op, String val)
  {
    SearchOperator op1 = op;
    List<String> valList = Collections.emptyList();
    if (SearchUtils.listOpSet.contains(op)) valList = SearchUtils.getValueList(val);
    // If operator is IN or NIN and column type is array then handle it as CONTAINS or NCONTAINS
    if ((col.getDataType().getSQLType() == Types.ARRAY) && SearchOperator.IN.equals(op)) op1 = CONTAINS;
    if ((col.getDataType().getSQLType() == Types.ARRAY) && SearchOperator.NIN.equals(op)) op1 = NCONTAINS;
    Condition c = null;
    switch (op1) {
      case EQ -> c = col.eq(val);
      case NEQ -> c = col.ne(val);
      case LT -> c =  col.lt(val);
      case LTE -> c = col.le(val);
      case GT -> c =  col.gt(val);
      case GTE -> c = col.ge(val);
      case LIKE -> c = col.like(val);
      case NLIKE -> c = col.notLike(val);
      case IN -> c = col.in(valList);
      case NIN -> c = col.notIn(valList);
      case CONTAINS -> c = textArrayOverlaps(col, valList.toArray(), false);
      case NCONTAINS -> c = textArrayOverlaps(col, valList.toArray(), true);
      case BETWEEN -> c = col.between(valList.get(0), valList.get(1));
      case NBETWEEN -> c = col.notBetween(valList.get(0), valList.get(1));
    }
    return c;
  }

  /**
   * Converts App Updates record into an App History object.
   * 
   * @param r - App Updates record
   * @return - App History object
   */
  private AppHistoryItem getAppHistoryFromRecord(Record r)
  {
    return new AppHistoryItem(r.get(APP_UPDATES.JWT_TENANT), r.get(APP_UPDATES.JWT_USER),
                              r.get(APP_UPDATES.OBO_TENANT), r.get(APP_UPDATES.OBO_USER),
                              r.get(APP_UPDATES.APP_VERSION), r.get(APP_UPDATES.OPERATION),
                              r.get(APP_UPDATES.DESCRIPTION), r.get(APP_UPDATES.CREATED).toInstant(ZoneOffset.UTC));
  }

  /*
   * Implement the array overlap construct in jooq.
   * Given a column as a Field<T[]> and a java array create a jooq condition that
   * returns true if column contains any of the values in the array.
   */
  private static <T> Condition textArrayOverlaps(Field<T[]> col, T[] array, boolean negate)
  {
    Condition cond = DSL.condition("{0} && {1}::text[]", col, DSL.array(array));
    if (negate) return cond.not();
    else return cond;
  }
}
