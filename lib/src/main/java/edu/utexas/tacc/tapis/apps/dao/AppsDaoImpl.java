package edu.utexas.tacc.tapis.apps.dao;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.search.parser.ASTBinaryExpression;
import edu.utexas.tacc.tapis.search.parser.ASTLeaf;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.parser.ASTUnaryExpression;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.activemq.filter.UnaryExpression;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.apps.gen.jooq.tables.records.AppsRecord;
import static edu.utexas.tacc.tapis.apps.gen.jooq.Tables.*;
import static edu.utexas.tacc.tapis.apps.gen.jooq.Tables.APPS;

import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.search.SearchUtils.SearchOperator;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.Capability;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

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

  private static final String EMPTY_JSON = "{}";

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */
  /**
   * Create a new app.
   *
   * @return Sequence id of object created
   * @throws TapisException - on error
   * @throws IllegalStateException - if app already exists
   */
  @Override
  public int createApp(AuthenticatedUser authenticatedUser, App app, String createJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException {
    String opName = "createApp";
    // Generated sequence id
    int appId = -1;
    // ------------------------- Check Input -------------------------
    if (app == null) LibUtils.logAndThrowNullParmException(opName, "app");
    if (authenticatedUser == null) LibUtils.logAndThrowNullParmException(opName, "authenticatedUser");
    if (StringUtils.isBlank(createJsonStr)) LibUtils.logAndThrowNullParmException(opName, "createJson");
    if (StringUtils.isBlank(app.getTenant())) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(app.getName())) LibUtils.logAndThrowNullParmException(opName, "appName");
    if (app.getAppType() == null) LibUtils.logAndThrowNullParmException(opName, "appType");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if app exists or has been soft deleted. If yes then throw IllegalStateException
      boolean doesExist = checkForApp(db, app.getTenant(), app.getName(), true);
      if (doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_SYS_EXISTS", authenticatedUser, app.getName()));

      // Make sure owner, notes and tags are all set
      String owner = App.DEFAULT_OWNER;
      if (StringUtils.isNotBlank(app.getOwner())) owner = app.getOwner();
      String[] tagsStrArray = App.DEFAULT_TAGS;
      if (app.getTags() != null) tagsStrArray = app.getTags();
      JsonObject notesObj = App.DEFAULT_NOTES;
      if (app.getNotes() != null) notesObj = (JsonObject) app.getNotes();

      Record record = db.insertInto(APPS)
              .set(APPS.TENANT, app.getTenant())
              .set(APPS.NAME, app.getName())
              .set(APPS.DESCRIPTION, app.getDescription())
              .set(APPS.APP_TYPE, app.getAppType())
              .set(APPS.OWNER, owner)
              .set(APPS.ENABLED, app.isEnabled())
              .set(APPS.TAGS, tagsStrArray)
              .set(APPS.NOTES, notesObj)
              .returningResult(APPS.ID)
              .fetchOne();
      appId = record.getValue(APPS.ID);

      // Persist job capabilities
      persistJobCapabilities(db, app, appId);

      // Persist update record
      addUpdate(db, authenticatedUser, appId, AppOperation.create, createJsonStr, scrubbedText);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "apps");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return appId;
  }

  /**
   * Update an existing app.
   * Following columns will be updated:
   *  description, enabled, jobCapabilities, tags, notes
   * @return Sequence id of object created
   * @throws TapisException - on error
   * @throws IllegalStateException - if app already exists
   */
  @Override
  public int updateApp(AuthenticatedUser authenticatedUser, App patchedApp, PatchApp patchApp,
                           String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException {
    String opName = "updateApp";
    // ------------------------- Check Input -------------------------
    if (patchedApp == null) LibUtils.logAndThrowNullParmException(opName, "patchedApp");
    if (patchApp == null) LibUtils.logAndThrowNullParmException(opName, "patchApp");
    if (authenticatedUser == null) LibUtils.logAndThrowNullParmException(opName, "authenticatedUser");
    if (StringUtils.isBlank(updateJsonStr)) LibUtils.logAndThrowNullParmException(opName, "updateJson");
    if (StringUtils.isBlank(patchedApp.getTenant())) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(patchedApp.getName())) LibUtils.logAndThrowNullParmException(opName, "appName");
    if (patchedApp.getAppType() == null) LibUtils.logAndThrowNullParmException(opName, "appType");
    if (patchedApp.getId() < 1) LibUtils.logAndThrowNullParmException(opName, "appId");
    // Pull out some values for convenience
    String tenant = patchedApp.getTenant();
    String name = patchedApp.getName();
    int appId = patchedApp.getId();

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if app exists and has not been soft deleted. If no then throw IllegalStateException
      boolean doesExist = checkForApp(db, tenant, name, false);
      if (!doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", authenticatedUser, name));

      // Make sure notes and tags are all set
      String[] tagsStrArray = App.DEFAULT_TAGS;
      if (patchedApp.getTags() != null) tagsStrArray = patchedApp.getTags();
      JsonObject notesObj =  App.DEFAULT_NOTES;
      if (patchedApp.getNotes() != null) notesObj = (JsonObject) patchedApp.getNotes();

      db.update(APPS)
              .set(APPS.DESCRIPTION, patchedApp.getDescription())
              .set(APPS.ENABLED, patchedApp.isEnabled())
              .set(APPS.TAGS, tagsStrArray)
              .set(APPS.NOTES, notesObj)
              .where(APPS.ID.eq(appId))
              .execute();

      // If jobCapabilities updated then replace them
      if (patchApp.getJobCapabilities() != null) {
        db.deleteFrom(CAPABILITIES).where(CAPABILITIES.APP_ID.eq(appId)).execute();
        persistJobCapabilities(db, patchedApp, appId);
      }

      // Persist update record
      addUpdate(db, authenticatedUser, appId, AppOperation.modify, updateJsonStr, scrubbedText);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "apps");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return appId;
  }

  /**
   * Update owner of an app given app Id and new owner name
   *
   */
  @Override
  public void updateAppOwner(AuthenticatedUser authenticatedUser, int appId, String newOwnerName) throws TapisException
  {
    String opName = "changeOwner";
    // ------------------------- Check Input -------------------------
    if (appId < 1) LibUtils.logAndThrowNullParmException(opName, "appId");
    if (StringUtils.isBlank(newOwnerName)) LibUtils.logAndThrowNullParmException(opName, "newOwnerName");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(APPS).set(APPS.OWNER, newOwnerName).where(APPS.ID.eq(appId)).execute();
      // Persist update record
      String updateJsonStr = TapisGsonUtils.getGson().toJson(newOwnerName);
      addUpdate(db, authenticatedUser, appId, AppOperation.changeOwner, updateJsonStr , null);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "apps");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Soft delete an app record given the app name.
   *
   */
  @Override
  public int softDeleteApp(AuthenticatedUser authenticatedUser, int appId) throws TapisException
  {
    String opName = "softDeleteApp";
    int rows = -1;
    // ------------------------- Check Input -------------------------
    if (appId < 1) LibUtils.logAndThrowNullParmException(opName, "appId");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // If app does not exist or has been soft deleted return 0
      if (!db.fetchExists(APPS, APPS.ID.eq(appId), APPS.DELETED.eq(false)))
      {
        return 0;
      }
      rows = db.update(APPS).set(APPS.DELETED, true).where(APPS.ID.eq(appId)).execute();
      // Persist update record
      addUpdate(db, authenticatedUser, appId, AppOperation.softDelete, EMPTY_JSON, null);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "apps");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return rows;
  }

  /**
   * Hard delete an app record given the app name.
   */
  @Override
  public int hardDeleteApp(String tenant, String name) throws TapisException
  {
    String opName = "hardDeleteApp";
    int rows = -1;
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(name)) LibUtils.logAndThrowNullParmException(opName, "name");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.deleteFrom(APPS).where(APPS.TENANT.eq(tenant),APPS.NAME.eq(name)).execute();
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
      // execute SELECT to_regclass('tapis_sys.apps');
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
      // Rollback always logs msg and throws exception.
      // In this case of a simple check we ignore the exception, we just want the log msg
      try { LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "apps"); }
      catch (Exception e1) { }
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
    // Workaround to avoid checksum error during develop/deploy of SNAPSHOT versions when it is not a true migration.
//    flyway.repair();
    flyway.migrate();
  }

  /**
   * checkForAppByName
   * @param name - app name
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForAppByName(String tenant, String name, boolean includeDeleted) throws TapisException {
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
      result = checkForApp(db, tenant, name, includeDeleted);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, name, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getAppByName
   * @param name - app name
   * @return App object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public App getAppByName(String tenant, String name) throws TapisException {
    // Initialize result.
    App result = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      AppsRecord r = db.selectFrom(APPS)
              .where(APPS.TENANT.eq(tenant),APPS.NAME.eq(name),APPS.DELETED.eq(false))
              .fetchOne();
      if (r == null) return null;
      else result = r.into(App.class);

      // Retrieve and set job capabilities
      result.setJobCapabilities(retrieveJobCaps(db, result.getId()));

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "App", tenant, name, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getApps
   * Conditions in searchList must be processed by SearchUtils.validateAndExtractSearchCondition(cond)
   *   prior to this call for proper validation and treatment of special characters.
   * @param tenant - tenant name
   * @param searchList - optional list of conditions used for searching
   * @param IDs - list of app IDs to consider. null indicates no restriction.
   * @return - list of App objects
   * @throws TapisException - on error
   */
  @Override
  public List<App> getApps(String tenant, List<String> searchList, List<Integer> IDs) throws TapisException
  {
    // The result list should always be non-null.
    var retList = new ArrayList<App>();

    // If no IDs in list then we are done.
    if (IDs != null && IDs.isEmpty()) return retList;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Begin where condition for this query
      Condition whereCondition = (APPS.TENANT.eq(tenant)).and(APPS.DELETED.eq(false));

//      // DEBUG
//      // Iterate over all columns and show the type
//      Field<?>[] cols = APPS.fields();
//      for (Field<?> col : cols)
//      {
//        var dataType = col.getDataType();
//        int sqlType = dataType.getSQLType();
//        String sqlTypeName = dataType.getTypeName();
//        _log.error("Column name: " + col.getName() + " type: " + sqlTypeName);
//      }
//      // DEBUG

      // Add searchList to where condition
      whereCondition = addSearchListToWhere(whereCondition, searchList);

      // Add IN condition for list of IDs
      if (IDs != null && !IDs.isEmpty()) whereCondition = whereCondition.and(APPS.ID.in(IDs));

      // Execute the select
      Result<AppsRecord> results = db.selectFrom(APPS).where(whereCondition).fetch();
      if (results == null || results.isEmpty()) return retList;

      // Fill in job capabilities list from aux table
      for (AppsRecord r : results)
      {
        App s = r.into(App.class);
        s.setJobCapabilities(retrieveJobCaps(db, s.getId()));
        retList.add(s);
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
   * getAppsUsingSearchAST
   * TODO This method and getApps almost identical. Might be a way to combine. Construct AST from searchStr?
   * Search for apps using an abstract syntax tree (AST).
   * @param tenant - tenant name
   * @param searchAST - AST containing search conditions
   * @param IDs - list of app IDs to consider. null indicates no restriction.
   * @return - list of App objects
   * @throws TapisException - on error
   */
  @Override
  public List<App> getAppsUsingSearchAST(String tenant, ASTNode searchAST, List<Integer> IDs) throws TapisException
  {
    // If searchAST null or empty delegate to getApps
    if (searchAST == null) return getApps(tenant, null, IDs);
    // The result list should always be non-null.
    var retList = new ArrayList<App>();

    // If no IDs in list then we are done.
    if (IDs != null && IDs.isEmpty()) return retList;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Begin where condition for this query
      Condition whereCondition = (APPS.TENANT.eq(tenant)).and(APPS.DELETED.eq(false));

      // Add searchAST to where condition
      Condition astCondition = createConditionFromAst(searchAST);
      if (astCondition != null) whereCondition = whereCondition.and(astCondition);

      // Add IN condition for list of IDs
      if (IDs != null && !IDs.isEmpty()) whereCondition = whereCondition.and(APPS.ID.in(IDs));

      // Execute the select
      Result<AppsRecord> results = db.selectFrom(APPS).where(whereCondition).fetch();
      if (results == null || results.isEmpty()) return retList;

      // Fill in job capabilities list from aux table
      for (AppsRecord r : results)
      {
        App s = r.into(App.class);
        s.setJobCapabilities(retrieveJobCaps(db, s.getId()));
        retList.add(s);
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
   * getAppNames
   * @param tenant - tenant name
   * @return - List of app names
   * @throws TapisException - on error
   */
  @Override
  public List<String> getAppNames(String tenant) throws TapisException
  {
    // The result list is always non-null.
    var list = new ArrayList<String>();

    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      // ------------------------- Call SQL ----------------------------
      // Use jOOQ to build query string
      DSLContext db = DSL.using(conn);
      Result<?> result = db.select(APPS.NAME).from(APPS).where(APPS.TENANT.eq(tenant)).fetch();
      // Iterate over result
      for (Record r : result) { list.add(r.get(APPS.NAME)); }
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
    return list;
  }

  /**
   * getAppOwner
   * @param tenant - name of tenant
   * @param name - name of app
   * @return Owner or null if no app found
   * @throws TapisException - on error
   */
  @Override
  public String getAppOwner(String tenant, String name) throws TapisException
  {
    String owner = null;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      owner = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant), APPS.NAME.eq(name)).fetchOne(APPS.OWNER);

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
   * getAppId
   * @param tenant - name of tenant
   * @param name - name of app
   * @return appId or -1 if no app found
   * @throws TapisException - on error
   */
  @Override
  public int getAppId(String tenant, String name) throws TapisException
  {
    int appId = -1;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      appId = db.selectFrom(APPS).where(APPS.TENANT.eq(tenant), APPS.NAME.eq(name)).fetchOne(APPS.ID);

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
    return appId;
  }

  /**
   * Add an update record given the app Id and operation type
   *
   */
  @Override
  public void addUpdateRecord(AuthenticatedUser authenticatedUser, int appId, AppOperation op, String upd_json,
                              String upd_text) throws TapisException
  {
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      addUpdate(db, authenticatedUser, appId, op, upd_json, upd_text);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "apps");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /**
   * Given an sql connection and basic info add an update record
   *
   */
  private void addUpdate(DSLContext db, AuthenticatedUser authenticatedUser, int appId,
                         AppOperation op, String upd_json, String upd_text)
  {
    String updJsonStr = (StringUtils.isBlank(upd_json)) ? EMPTY_JSON : upd_json;
    // Persist update record
    db.insertInto(APP_UPDATES)
            .set(APP_UPDATES.APP_ID, appId)
            .set(APP_UPDATES.USER_NAME, authenticatedUser.getOboUser())
            .set(APP_UPDATES.USER_TENANT, authenticatedUser.getOboTenantId())
            .set(APP_UPDATES.OPERATION, op)
            .set(APP_UPDATES.UPD_JSON, TapisGsonUtils.getGson().fromJson(updJsonStr, JsonElement.class))
            .set(APP_UPDATES.UPD_TEXT, upd_text)
            .execute();
  }

  /**
   * Given an sql connection check to see if specified app exists and has/has not been soft deleted
   * @param db - jooq context
   * @param tenant - name of tenant
   * @param name - name of app
   * @param includeDeleted -if soft deleted apps should be included
   * @return - true if app exists, else false
   */
  private static boolean checkForApp(DSLContext db, String tenant, String name, boolean includeDeleted)
  {
    if (includeDeleted) return db.fetchExists(APPS, APPS.NAME.eq(name),APPS.TENANT.eq(tenant));
    else return db.fetchExists(APPS, APPS.NAME.eq(name),APPS.TENANT.eq(tenant),APPS.DELETED.eq(false));
  }

  /**
   * Persist job capabilities given an sql connection and an app
   */
  private static void persistJobCapabilities(DSLContext db, App app, int appId)
  {
    var jobCapabilities = app.getJobCapabilities();
    if (jobCapabilities == null || jobCapabilities.isEmpty()) return;

    for (Capability cap : jobCapabilities) {
      String valStr = "";
      if (cap.getValue() != null ) valStr = cap.getValue();
      db.insertInto(CAPABILITIES).set(CAPABILITIES.APP_ID, appId)
              .set(CAPABILITIES.CATEGORY, cap.getCategory())
              .set(CAPABILITIES.NAME, cap.getName())
              .set(CAPABILITIES.VALUE, valStr)
              .execute();
    }
  }

  /**
   * Get capabilities for an app from an auxiliary table
   * @param db - DB connection
   * @param appId - app
   * @return list of capabilities
   */
  private static List<Capability> retrieveJobCaps(DSLContext db, int appId)
  {
    List<Capability> capRecords = db.selectFrom(CAPABILITIES).where(CAPABILITIES.APP_ID.eq(appId)).fetchInto(Capability.class);
    return capRecords;
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
      whereCondition = addSearchCondStrToWhere(whereCondition, condStr, "AND");
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
    else if (op.equalsIgnoreCase("AND"))
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
    else if (op.equalsIgnoreCase("OR"))
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
      String condStr = lValue + "." + binaryNode.getOp() + "." + rValue;
      // Validate and create a condition from the string
      return addSearchCondStrToWhere(null, condStr, null);
    }
  }

  /**
   * Take a string containing a single condition and create a new condition or join it to an existing condition.
   * Validate column name, search comparison operator and compatibility of column type + search operator + column value
   * @param whereCondition existing condition. If null a new condition is returned.
   * @param searchStr Single search condition in the form column_name.op.value
   * @param joinOp If whereCondition is not null use AND or OR to join the condition with the whereCondition
   * @return resulting where condition
   * @throws TapisException on error
   */
  private static Condition addSearchCondStrToWhere(Condition whereCondition, String searchStr, String joinOp)
          throws TapisException
  {
    // If we have no search string then return what we were given
    if (StringUtils.isBlank(searchStr)) return whereCondition;
    // If we are given a condition but no indication of how to join new condition to it then return what we were given
    if (whereCondition != null && StringUtils.isBlank(joinOp)) return whereCondition;
    if (whereCondition != null && joinOp != null && !joinOp.equalsIgnoreCase("AND") && !joinOp.equalsIgnoreCase("OR"))
    {
      return whereCondition;
    }

    // Parse search value into column name, operator and value
    // Format must be column_name.op.value
    String[] parsedStrArray = searchStr.split("\\.", 3);
    // Validate column name
    String column = parsedStrArray[0];
    Field<?> col = APPS.field(DSL.name(column));
    // Check for column name passed in as camelcase
    if (col == null)
    {
      col = APPS.field(DSL.name(SearchUtils.camelCaseToSnakeCase(column)));
    }
    // If column not found then it is an error
    if (col == null)
    {
      String msg = LibUtils.getMsg("APPLIB_DB_NO_COLUMN", APPS.getName(), DSL.name(column));
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
    if (StringUtils.isBlank(joinOp)) return newCondition;
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
    List<String> valList = Collections.emptyList();
    if (SearchUtils.listOpSet.contains(op)) valList = SearchUtils.getValueList(val);
    switch (op) {
      case EQ:
        return col.eq(val);
      case NEQ:
        return col.ne(val);
      case LT:
        return col.lt(val);
      case LTE:
        return col.le(val);
      case GT:
        return col.gt(val);
      case GTE:
        return col.ge(val);
      case LIKE:
        return col.like(val);
      case NLIKE:
        return col.notLike(val);
      case IN:
        return col.in(valList);
      case NIN:
        return col.notIn(valList);
      case BETWEEN:
        return col.between(valList.get(0), valList.get(1));
      case NBETWEEN:
        return col.notBetween(valList.get(0), valList.get(1));
    }
    return null;
  }
}
