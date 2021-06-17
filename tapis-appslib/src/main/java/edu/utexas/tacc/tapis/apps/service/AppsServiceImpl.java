package edu.utexas.tacc.tapis.apps.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import edu.utexas.tacc.tapis.apps.dao.AppsDaoImpl;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.systems.client.gen.model.LogicalQueue;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.search.parser.ASTParser;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;

import static edu.utexas.tacc.tapis.shared.TapisConstants.APPS_SERVICE;
import static edu.utexas.tacc.tapis.apps.model.App.NO_APP_VERSION;

/*
 * Service level methods for Apps.
 *   Uses Dao layer and other service library classes to perform all top level service operations.
 * Annotate as an hk2 Service so that default scope for Dependency Injection is singleton
 */
@Service
public class AppsServiceImpl implements AppsService
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(AppsServiceImpl.class);

  private static final Set<Permission> ALL_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY, Permission.EXECUTE));
  private static final Set<Permission> READMODIFY_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  // Permspec format for systems is "system:<tenant>:<perm_list>:<system_id>"
  private static final String PERM_SPEC_PREFIX = "app";
  private static final String PERM_SPEC_TEMPLATE = "app:%s:%s:%s";

  private static final String SERVICE_NAME = TapisConstants.SERVICE_NAME_APPS;
  private static final String FILES_SERVICE = "files";
  private static final String JOBS_SERVICE = "jobs";
  private static final Set<String> SVCLIST_READ = new HashSet<>(Set.of(FILES_SERVICE, JOBS_SERVICE));

  // Message keys
  private static final String ERROR_ROLLBACK = "APPLIB_ERROR_ROLLBACK";
  private static final String NOT_FOUND = "APPLIB_NOT_FOUND";

  // NotAuthorizedException requires a Challenge, although it serves no purpose here.
  private static final String NO_CHALLENGE = "NoChallenge";

  // Compiled regex for splitting around ":"
  private static final Pattern COLON_SPLIT = Pattern.compile(":");

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
  @Inject
  private AppsDao dao;

  @Inject
  private ServiceClients serviceClients;

  @Inject
  private ServiceContext serviceContext;

  // We must be running on a specific site and this will never change
  // These are initialized in method initService()
  private static String siteId;
  private static String siteAdminTenantId;
  public static String getSiteId() {return siteId;}
  public static String getServiceTenantId() {return siteAdminTenantId;}
  public static String getServiceUserId() {return SERVICE_NAME;}
  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  // -----------------------------------------------------------------------
  // ------------------------- Apps -------------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a new app object given an App and the text used to create the App.
   * Secrets in the text should be masked.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param app - Pre-populated App object
   * @param scrubbedText - Text used to create the App object - secrets should be scrubbed. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - app exists OR App in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public void createApp(AuthenticatedUser authenticatedUser, App app, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotAuthorizedException
  {
    AppOperation op = AppOperation.create;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (app == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    _log.trace(LibUtils.getMsgAuth("APPLIB_CREATE_TRACE", authenticatedUser, scrubbedText));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appId = app.getId();
    String appVersion = app.getVersion();
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    // Required app attributes: id, version
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId) || StringUtils.isBlank(appId) ||
        StringUtils.isBlank(appVersion))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appId));
    }

    // Check if app with id+version already exists
    if (dao.checkForApp(appTenantName, appId, appVersion, true))
    {
      throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_APP_EXISTS", authenticatedUser, appId, appVersion));
    }

    // Make sure owner, notes and tags are all set
    // Note that this is done before auth so owner can get resolved and used during auth check.
    app.setTenant(appTenantName);
    app.setDefaults();

    // ----------------- Resolve variables for any attributes that might contain them --------------------
    app.resolveVariables(authenticatedUser.getOboUser());

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, app.getOwner(), null, null);

    // ---------------- Check for reserved names ------------------------
    checkReservedIds(authenticatedUser, appId);

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(authenticatedUser, app);

    // Construct Json string representing the App about to be created
    App scrubbedApp = new App(app);
    String createJsonStr = TapisGsonUtils.getGson().toJson(scrubbedApp);

    // ----------------- Create all artifacts --------------------
    // Creation of app and perms not in single DB transaction.
    // Use try/catch to rollback any writes in case of failure.
    boolean appCreated = false;
    String appsPermSpecALL = getPermSpecAllStr(appTenantName, appId);

    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient();
    try {
      // ------------------- Make Dao call to persist the app -----------------------------------
      appCreated = dao.createApp(authenticatedUser, app, createJsonStr, scrubbedText);

      // ------------------- Add permissions -----------------------------
      // Give owner full access to the app
      skClient.grantUserPermission(appTenantName, app.getOwner(), appsPermSpecALL);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      // Log error
      String msg = LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ROLLBACK", authenticatedUser, appId, e0.getMessage());
      _log.error(msg);

      // Rollback
      // Remove app from DB
      if (appCreated) try {dao.hardDeleteApp(appTenantName, appId); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "hardDelete", e.getMessage()));}
      // Remove perms
      try { skClient.revokeUserPermission(appTenantName, app.getOwner(), appsPermSpecALL); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "revokePermOwner", e.getMessage()));}
      throw e0;
    }
  }

  /**
   * Update existing version of an app given a PatchApp and the text used to create the PatchApp.
   * Secrets in the text should be masked.
   * Attributes that can be updated:
   *   description, runtime, runtimeVersion, runtimeOptions, containerImage, maxJobs, maxJobsPerUser, strictFileInputs,
   *   all of jobAttributes (including all of parameterSet), tags, notes.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param patchApp - Pre-populated PatchApp object
   * @param scrubbedText - Text used to create the PatchApp object - secrets should be scrubbed. Saved in update record.
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public void updateApp(AuthenticatedUser authenticatedUser, PatchApp patchApp, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException,
                 NotAuthorizedException, NotFoundException
  {
    AppOperation op = AppOperation.modify;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (patchApp == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = patchApp.getTenant();
    String appId = patchApp.getId();
    String appVersion = patchApp.getVersion();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId) || StringUtils.isBlank(appId) || StringUtils.isBlank(scrubbedText))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appId));
    }

    // App must already exist and not be deleted
    if (!dao.checkForApp(appTenantName, appId, false))
    {
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));
    }

    // Retrieve the app being patched and create fully populated App with changes merged in
    App origApp = dao.getApp(appTenantName, appId, appVersion);
    App patchedApp = createPatchedApp(origApp, patchApp);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, origApp.getOwner(), null, null);

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(authenticatedUser, patchedApp);

    // Construct Json string representing the PatchApp about to be used to update the app
    String updateJsonStr = TapisGsonUtils.getGson().toJson(patchApp);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the app -----------------------------------
    dao.updateApp(authenticatedUser, patchedApp, patchApp, updateJsonStr, scrubbedText);
  }

  /**
   * Update enabled to true for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int enableApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    return updateEnabled(authenticatedUser, appId, AppOperation.enable);
  }

  /**
   * Update enabled to false for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int disableApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    return updateEnabled(authenticatedUser, appId, AppOperation.disable);
  }

  /**
   * Update deleted to true for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int deleteApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    return updateDeleted(authenticatedUser, appId, AppOperation.delete);
  }

  /**
   * Update deleted to false for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int undeleteApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    return updateDeleted(authenticatedUser, appId, AppOperation.undelete);
  }

  /**
   * Change owner of an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param newOwnerName - User name of new owner
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int changeAppOwner(AuthenticatedUser authenticatedUser, String appId, String newOwnerName)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    AppOperation op = AppOperation.changeOwner;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(newOwnerName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = tenantName;
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appId));

    // App must already exist and not be deleted
    if (!dao.checkForApp(appTenantName, appId, false))
         throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));

    // Retrieve the old owner
    String oldOwnerName = dao.getAppOwner(appTenantName, appId);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, oldOwnerName, null, null);

    // If new owner same as old owner then this is a no-op
    if (newOwnerName.equals(oldOwnerName)) return 0;

    // ----------------- Make all updates --------------------
    // Changes not in single DB transaction.
    // Use try/catch to rollback any changes in case of failure.
    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient();
    String appsPermSpec = getPermSpecAllStr(appTenantName, appId);
    try {
      // ------------------- Make Dao call to update the app owner -----------------------------------
      dao.updateAppOwner(authenticatedUser, appId, newOwnerName);
      // Add permissions for new owner
      skClient.grantUserPermission(appTenantName, newOwnerName, appsPermSpec);
      // Remove permissions from old owner
      skClient.revokeUserPermission(appTenantName, oldOwnerName, appsPermSpec);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      try { dao.updateAppOwner(authenticatedUser, appId, oldOwnerName); } catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "updateOwner", e.getMessage()));}
      try { skClient.revokeUserPermission(appTenantName, newOwnerName, appsPermSpec); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "revokePermNewOwner", e.getMessage()));}
      try { skClient.grantUserPermission(appTenantName, oldOwnerName, appsPermSpec); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "grantPermOldOwner", e.getMessage()));}
      throw e0;
    }
    return 1;
  }

  /**
   * Hard delete an app record given the app name.
   * Also remove artifacts from the Security Kernel.
   * NOTE: This is package-private. Only test code should ever use it.
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  int hardDeleteApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    AppOperation op = AppOperation.hardDelete;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist then 0 changes
    if (!dao.checkForApp(appTenantName, appId, true)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, null, null, null);

    // Remove SK artifacts
    removeSKArtifacts(authenticatedUser, appId, op);

    // Delete the app
    return dao.hardDeleteApp(appTenantName, appId);
  }

  /**
   * Initialize the service:
   *   Check for Apps admin role. If not found create it
   */
  public void initService(String siteId1, String siteAdminTenantId1, String svcPassword) throws TapisException, TapisClientException
  {
    // Initialize service context and site info
    siteId = siteId1;
    siteAdminTenantId = siteAdminTenantId1;
    serviceContext.initServiceJWT(siteId, APPS_SERVICE, svcPassword);
    // Make sure DB is present and updated to latest version using flyway
    dao.migrateDB();
  }

  /**
   * Check that we can connect with DB and that the main table of the service exists.
   * @return null if all OK else return an Exception
   */
  public Exception checkDB()
  {
    return dao.checkDB();
  }

  /**
   * checkForApp
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - Name of the app
   * @return true if app exists and has not been deleted, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public boolean checkForApp(AuthenticatedUser authenticatedUser, String appId) throws TapisException, NotAuthorizedException, TapisClientException
  {
    return checkForApp(authenticatedUser, appId, false);
  }

  /**
   * checkForApp
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - Name of the app
   * @param includeDeleted - indicates if check should include resources marked as deleted
   * @return true if app exists and has not been deleted, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public boolean checkForApp(AuthenticatedUser authenticatedUser, String appId, boolean includeDeleted)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so cannot do auth check if no app
    if (dao.checkForApp(appTenantName, appId, includeDeleted)) {
      // ------------------------- Check service level authorization -------------------------
      checkAuth(authenticatedUser, op, appId, null, null, null);
      return true;
    }
    return false;
  }

  /**
   * isEnabled
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - Name of the app
   * @return true if app is enabled, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public boolean isEnabled(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotFoundException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // Resource must exist and not be deleted
    if (!dao.checkForApp(appTenantName, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, null, null, null);
    return dao.isEnabled(appTenantName, appId);
  }

  /**
   * getApp
   * Retrieve specified or most recently created version of an application.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - Name of the app
   * @param appVersion - Version of the app, null or blank for latest version
   * @param requireExecPerm - check for EXECUTE permission as well as READ permission
   * @return populated instance of an App or null if not found or user not authorized.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public App getApp(AuthenticatedUser authenticatedUser, String appId, String appVersion, boolean requireExecPerm)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app and oboUser as apiUserId
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
    {
      appTenantName = authenticatedUser.getOboTenantId();
    }

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(appTenantName, appId, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, null, null, null);
    // If flag is set to also require EXECUTE perm then make a special auth call
    if (requireExecPerm)
    {
      checkAuthUser(authenticatedUser, AppOperation.execute, appTenantName, authenticatedUser.getOboUser(),
                    appId, null, null, null);
    }

    App result = dao.getApp(appTenantName, appId, appVersion);
    return result;
  }

  /**
   * Get count of all apps matching certain criteria and for which user has READ permission
   * @param authenticatedUser - principal user containing tenant and user info
   * @param searchList - optional list of conditions used for searching
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return Count of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public int getAppsTotalCount(AuthenticatedUser authenticatedUser, List<String> searchList,
                               List<OrderBy> orderByList, String startAfter, boolean showDeleted)
          throws TapisException, TapisClientException
  {
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Determine tenant scope for user
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the user
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
      appTenantName = authenticatedUser.getOboTenantId();

    // Build verified list of search conditions and check if any search conditions involve the version attribute
    boolean versionSpecified = false;
    var verifiedSearchList = new ArrayList<String>();
    if (searchList != null && !searchList.isEmpty())
    {
      try
      {
        for (String cond : searchList)
        {
          if (AppsDaoImpl.checkCondForVersion(cond)) versionSpecified = true;
          // Use SearchUtils to validate condition
          String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(cond);
          verifiedSearchList.add(verifiedCondStr);
        }
      }
      catch (Exception e)
      {
        String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", authenticatedUser, e.getMessage());
        _log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // Get list of IDs for which requester has view permission.
    // This is either all (null) or a list of IDs.
    Set<String> allowedAppIDs = getAllowedAppIDs(authenticatedUser, appTenantName);

    // If none are allowed we know count is 0
    if (allowedAppIDs != null && allowedAppIDs.isEmpty()) return 0;

    // Count all allowed systems matching the search conditions
    return dao.getAppsCount(appTenantName, verifiedSearchList, null, allowedAppIDs, orderByList, startAfter,
                            versionSpecified, showDeleted);
  }

  /**
   * Get all apps for which user has READ permission
   * @param authenticatedUser - principal user containing tenant and user info
   * @param searchList - optional list of conditions used for searching
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getApps(AuthenticatedUser authenticatedUser, List<String> searchList, int limit,
                           List<OrderBy> orderByList, int skip, String startAfter, boolean showDeleted)
          throws TapisException, TapisClientException
  {
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Determine tenant scope for user
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the user
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
      appTenantName = authenticatedUser.getOboTenantId();

    // Build verified list of search conditions and check if any search conditions involve the version attribute
    boolean versionSpecified = false;
    var verifiedSearchList = new ArrayList<String>();
    if (searchList != null && !searchList.isEmpty())
    {
      try
      {
        for (String cond : searchList)
        {
          if (AppsDaoImpl.checkCondForVersion(cond)) versionSpecified = true;
          // Use SearchUtils to validate condition
          String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(cond);
          verifiedSearchList.add(verifiedCondStr);
        }
      }
      catch (Exception e)
      {
        String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", authenticatedUser, e.getMessage());
        _log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // Get list of IDs of apps for which requester has view permission.
    // This is either all apps (null) or a list of IDs.
    Set<String> allowedAppIDs = getAllowedAppIDs(authenticatedUser, appTenantName);

    // Get all allowed apps matching the search conditions
    List<App> apps = dao.getApps(appTenantName, verifiedSearchList, null, allowedAppIDs, limit, orderByList, skip,
                                 startAfter, versionSpecified, showDeleted);
    return apps;
  }

  /**
   * Get all apps for which user has view permission.
   * Use provided string containing a valid SQL where clause for the search.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param sqlSearchStr - string containing a valid SQL where clause
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getAppsUsingSqlSearchStr(AuthenticatedUser authenticatedUser, String sqlSearchStr, int limit,
                                            List<OrderBy> orderByList, int skip, String startAfter, boolean showDeleted)
          throws TapisException, TapisClientException
  {
    // If search string is empty delegate to getApps()
    if (StringUtils.isBlank(sqlSearchStr)) return getApps(authenticatedUser, null, limit, orderByList, skip,
                                                          startAfter, showDeleted);

    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Determine tenant scope for user
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the user
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
      appTenantName = authenticatedUser.getOboTenantId();

    // Validate and parse the sql string into an abstract syntax tree (AST)
    // The activemq parser validates and parses the string into an AST but there does not appear to be a way
    //          to use the resulting BooleanExpression to walk the tree. How to now create a usable AST?
    //   I believe we don't want to simply try to run the where clause for various reasons:
    //      - SQL injection
    //      - we want to verify the validity of each <attr>.<op>.<value>
    //        looks like activemq parser will ensure the leaf nodes all represent <attr>.<op>.<value> and in principle
    //        we should be able to check each one and generate of list of errors for reporting.
    //  Looks like jOOQ can parse an SQL string into a jooq Condition. Do this in the Dao? But still seems like no way
    //    to walk the AST and check each condition so we can report on errors.
//    BooleanExpression searchAST;
    ASTNode searchAST;
    try { searchAST = ASTParser.parse(sqlSearchStr); }
    catch (Exception e)
    {
      String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      throw new IllegalArgumentException(msg);
    }

    // Get list of IDs of apps for which requester has READ permission.
    // This is either all apps (null) or a list of IDs.
    Set<String> allowedAppIDs = getAllowedAppIDs(authenticatedUser, appTenantName);

    // Pass in null for versionSpecified since the Dao makes the same call we would make so no time saved doing it here.
    Boolean versionSpecified = null;

    // Get all allowed apps matching the search conditions
    List<App> apps = dao.getApps(appTenantName, null, searchAST, allowedAppIDs, limit, orderByList, skip, startAfter,
                                 versionSpecified, showDeleted);
    return apps;
  }

  /**
   * Get list of app IDs
   * @param authenticatedUser - principal user containing tenant and user info
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return - list of apps
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public Set<String> getAppIDs(AuthenticatedUser authenticatedUser, boolean showDeleted) throws TapisException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Get all app names
    Set<String> appIds = dao.getAppIDs(authenticatedUser.getTenantId(), showDeleted);
    var allowedNames = new HashSet<String>();
    // Filter based on user authorization
    for (String name: appIds)
    {
      try {
        checkAuth(authenticatedUser, op, name, null, null, null);
        allowedNames.add(name);
      }
      catch (NotAuthorizedException | TapisClientException e) { }
    }
    return allowedNames;
  }

  /**
   * Get app owner
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - Name of the app
   * @return - Owner or null if app not found or user not authorized
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public String getAppOwner(AuthenticatedUser authenticatedUser, String appId) throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));

    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(appTenantName, appId, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, null, null, null);

    return dao.getAppOwner(appTenantName, appId);
  }

  // -----------------------------------------------------------------------
  // --------------------------- Permissions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Grant permissions to a user for an app.
   * Grant of MODIFY implies grant of READ
   * NOTE: Permissions only impact the default user role
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param userName - Target user for operation
   * @param permissions - list of permissions to be granted
   * @param updateText - Client provided text used to create the permissions list. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public void grantUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName,
                                   Set<Permission> permissions, String updateText)
          throws NotFoundException, NotAuthorizedException, TapisException, TapisClientException
  {
    AppOperation op = AppOperation.grantPerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(userName))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_SYSTEM", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
    {
      appTenantName = authenticatedUser.getOboTenantId();
    }

    // If system does not exist or has been deleted then throw an exception
    if (!dao.checkForApp(appTenantName, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));

    // Check to see if owner is trying to update permissions for themselves.
    // If so throw an exception because this would be confusing since owner always has full permissions.
    // For an owner permissions are never checked directly.
    String owner = checkForOwnerPermUpdate(authenticatedUser, appTenantName, appId, userName, op.name());

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, owner, null, null);

    // Check inputs. If anything null or empty throw an exception
    if (permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    // Grant of MODIFY implies grant of READ
    if (permissions.contains(Permission.MODIFY)) permissions.add(Permission.READ);

    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(appTenantName, appId, permissions);

    // Get the Security Kernel client
    var skClient = getSKClient();

    // Assign perms to user.
    // Start of updates. Will need to rollback on failure.
    try
    {
      // Assign perms to user. SK creates a default role for the user
      for (String permSpec : permSpecSet)
      {
        skClient.grantUserPermission(appTenantName, userName, permSpec);
      }
    }
    catch (TapisClientException tce)
    {
      // Rollback
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      String msg = LibUtils.getMsgAuth("APPLIB_PERM_ERROR_ROLLBACK", authenticatedUser, appId, tce.getMessage());
      _log.error(msg);

      // Revoke permissions that may have been granted.
      for (String permSpec : permSpecSet)
      {
        try { skClient.revokeUserPermission(appTenantName, userName, permSpec); }
        catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "revokePerm", e.getMessage()));}
      }
      // Convert to TapisException and re-throw
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", authenticatedUser, appId, op.name()), tce);
    }

    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(authenticatedUser, appTenantName, appId, NO_APP_VERSION, op, updateJsonStr, updateText);
  }

  /**
   * Revoke permissions from a user for an app
   * Revoke of READ implies revoke of MODIFY
   * NOTE: Permissions only impact the default user role
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param userName - Target user for operation
   * @param permissions - list of permissions to be revoked
   * @param updateText - Client provided text used to create the permissions list. Saved in update record.
   * @return Number of items revoked
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int revokeUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName,
                                   Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.revokePerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(userName))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_SYSTEM", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist or has been deleted then return 0 changes
    if (!dao.checkForApp(appTenantName, appId, false)) return 0;

    // Check to see if owner is trying to update permissions for themselves.
    // If so throw an exception because this would be confusing since owner always has full permissions.
    // For an owner permissions are never checked directly.
    String owner = checkForOwnerPermUpdate(authenticatedUser, appTenantName, appId, userName, op.name());

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, owner, userName, permissions);

    // Check inputs. If anything null or empty throw an exception
    if (permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    // Revoke of READ implies revoke of MODIFY
    if (permissions.contains(Permission.READ)) permissions.add(Permission.MODIFY);

    var skClient = getSKClient();
    int changeCount;
    // Determine current set of user permissions
    var userPermSet = getUserPermSet(skClient, userName, appTenantName, appId);

    try
    {
      // Revoke perms
      changeCount = revokePermissions(skClient, appTenantName, appId, userName, permissions);
    }
    catch (TapisClientException tce)
    {
      // Rollback
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      String msg = LibUtils.getMsgAuth("APPLIB_PERM_ERROR_ROLLBACK", authenticatedUser, appId, tce.getMessage());
      _log.error(msg);

      // Grant permissions that may have been revoked and that the user previously held.
      for (Permission perm : permissions)
      {
        if (userPermSet.contains(perm))
        {
          String permSpec = getPermSpecStr(appTenantName, appId, perm);
          try { skClient.grantUserPermission(appTenantName, userName, permSpec); }
          catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, authenticatedUser, appId, "grantPerm", e.getMessage()));}
        }
      }

      // Convert to TapisException and re-throw
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", authenticatedUser, appId, op.name()), tce);
    }

    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(authenticatedUser, appTenantName, appId, NO_APP_VERSION, op, updateJsonStr, updateText);
    return changeCount;
  }

  /**
   * Get list of app permissions for a user
   * NOTE: This retrieves permissions from all roles.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param userName - Target user for operation
   * @return List of permissions
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public Set<Permission> getUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.getPerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(userName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist or has been deleted then return null
    if (!dao.checkForApp(appTenantName, appId, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appId, null, userName, null);

    // Use Security Kernel client to check for each permission in the enum list
    var skClient = getSKClient();
    return getUserPermSet(skClient, userName, appTenantName, appId);
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   * Update enabled attribute for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param appOp - operation, enable or disable
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  private int updateEnabled(AuthenticatedUser authenticatedUser, String appId, AppOperation appOp)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = tenantName;
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appId));

    // App must already exist and not be deleted
    if (!dao.checkForApp(appTenantName, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, appOp, appId, null, null, null);

    // ----------------- Make update --------------------
    if (appOp == AppOperation.enable)
      dao.updateEnabled(authenticatedUser, appId, true);
    else
      dao.updateEnabled(authenticatedUser, appId, false);
    return 1;
  }

  /**
   * Update deleted attribute for an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appId - name of app
   * @param appOp - operation, delete or undelete
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  private int updateDeleted(AuthenticatedUser authenticatedUser, String appId, AppOperation appOp)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = tenantName;
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appId));

    // App must exist
    if (!dao.checkForApp(appTenantName, appId, true))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, authenticatedUser, appId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, appOp, appId, null, null, null);

    // ----------------- Make update --------------------
    if (appOp == AppOperation.delete)
      dao.updateDeleted(authenticatedUser, appId, true);
    else
      dao.updateDeleted(authenticatedUser, appId, false);
    return 1;
  }

  /**
   * Get Security Kernel client
   * Note: The service always calls SK as itself.
   * @return SK client
   * @throws TapisException - for Tapis related exceptions
   */
  private SKClient getSKClient() throws TapisException
  {
    SKClient skClient;
    String tenantName = siteAdminTenantId;
    String userName = SERVICE_NAME;
    try
    {
      skClient = serviceClients.getClient(userName, tenantName, SKClient.class);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SECURITY, tenantName, userName);
      throw new TapisException(msg, e);
    }

    return skClient;
  }

  /**
   * Get Systems client associated with specified tenant
   * @param authenticatedUser - name of tenant
   * @return Systems client
   * @throws TapisException - for Tapis related exceptions
   */
  private SystemsClient getSystemsClient(AuthenticatedUser authenticatedUser) throws TapisException
  {
    SystemsClient sysClient;
    String tenantName;
    String userName;
    // If service request use oboTenant and oboUser in OBO headers
    // else for user request use authenticated user name and tenant in OBO headers
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
    {
      tenantName = authenticatedUser.getOboTenantId();
      userName = authenticatedUser.getOboUser();
    }
    else
    {
      tenantName = authenticatedUser.getTenantId();
      userName = authenticatedUser.getName();
    }
    try
    {
      sysClient = serviceClients.getClient(userName, tenantName, SystemsClient.class);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SYSTEMS,
              authenticatedUser.getTenantId(), authenticatedUser.getName());
      throw new TapisException(msg, e);
    }
    return sysClient;
  }

  /**
   * Check for reserved names.
   * Endpoints defined lead to certain names that are not valid.
   * Invalid names: healthcheck, readycheck, search
   * @param id - the id to check
   * @throws IllegalStateException - if attempt to create a resource with a reserved name
   */
  private void checkReservedIds(AuthenticatedUser authenticatedUser, String id) throws IllegalStateException
  {
    if (App.RESERVED_ID_SET.contains(id.toUpperCase()))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_CREATE_RESERVED", authenticatedUser, id);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * Check constraints on App attributes.
   * Check that system referenced by execSystemId exists with canExec = true.
   * Check that system referenced by archiveSystemId exists.
   * Check LogicalQueue max/min constraints.
   * Collect and report as many errors as possible so they can all be fixed before next attempt
   * @param app - the App to check
   * @throws IllegalStateException - if any constraints are violated
   */
  private void validateApp(AuthenticatedUser authenticatedUser, App app)
          throws TapisException, IllegalStateException
  {
    String msg;
    // Make api level checks, i.e. checks that do not involve a dao or service call.
    List<String> errMessages = app.checkAttributeRestrictions();
    var systemsClient = getSystemsClient(authenticatedUser);

    // If execSystemId is set verify that it exists with canExec = true
    // and if app specifies a LogicalQueue make sure the constraints for the queue are not violated.
    String execSystemId = app.getExecSystemId();
    if (!StringUtils.isBlank(execSystemId))
    {
      TapisSystem execSystem = null;
      try
      {
        execSystem = systemsClient.getSystem(execSystemId);
      }
      catch (TapisClientException e)
      {
        msg = LibUtils.getMsg("APPLIB_EXECSYS_CHECK_ERROR", execSystemId, e.getMessage());
        _log.error(msg, e);
        errMessages.add(msg);
      }
      if (execSystem == null)
      {
        msg = LibUtils.getMsg("APPLIB_EXECSYS_NO_SYSTEM", execSystemId);
        errMessages.add(msg);
      }
      else if (execSystem.getCanExec() == null || !execSystem.getCanExec())
      {
        msg = LibUtils.getMsg("APPLIB_EXECSYS_NOT_EXEC", execSystemId);
        errMessages.add(msg);
      }

      // If app specifies a LogicalQueue make sure the constraints for the queue are not violated.
      //     Max/Min constraints to check: nodeCount, coresPerNode, memoryMB, runMinutes
      String execQName = app.getExecSystemLogicalQueue();
      if (!StringUtils.isBlank(execQName))
      {
        List<LogicalQueue> batchQueues = (execSystem == null ? null : execSystem.getBatchLogicalQueues());
        // Make sure the queue is defined for the system
        LogicalQueue execQ = getLogicalQ(batchQueues, execQName);
        if (batchQueues == null || batchQueues.isEmpty() || execQ == null)
        {
          msg = LibUtils.getMsg("APPLIB_EXECQ_NOT_FOUND", execQName, execSystemId);
          errMessages.add(msg);
        }
        // Check constraints
        if (execQ != null)
        {
          Integer maxNodeCount = execQ.getMaxNodeCount();
          Integer minNodeCount = execQ.getMinNodeCount();
          Integer maxCoresPerNode = execQ.getMaxCoresPerNode();
          Integer minCoresPerNode = execQ.getMinCoresPerNode();
          Integer maxMemoryMB = execQ.getMaxMemoryMB();
          Integer minMemoryMB = execQ.getMinMemoryMB();
          Integer maxMinutes = execQ.getMaxMinutes();
          Integer minMinutes = execQ.getMinMinutes();
          int appNodeCount = app.getNodeCount();
          int appCoresPerNode = app.getCoresPerNode();
          int appMemoryMb = app.getMemoryMb();
          int appMaxMinutes = app.getMaxMinutes();

          // If queue defines limit and app specifies limit and app limit out of range then add error
          // NodeCount
          if (maxNodeCount != null && maxNodeCount > 0 && appNodeCount > 0 && appNodeCount > maxNodeCount)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_HIGH", "NodeCount", appNodeCount, maxNodeCount, execQName, execSystemId);
            errMessages.add(msg);
          }
          if (minNodeCount != null && minNodeCount > 0 && appNodeCount > 0 && appNodeCount < minNodeCount)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_LOW", "NodeCount", appNodeCount, minNodeCount, execQName, execSystemId);
            errMessages.add(msg);
          }
          // CoresPerNode
          if (maxCoresPerNode != null && maxCoresPerNode > 0 && appCoresPerNode > 0 && appCoresPerNode > maxCoresPerNode)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_HIGH", "CoresPerNode", appCoresPerNode, maxCoresPerNode, execQName, execSystemId);
            errMessages.add(msg);
          }
          if (minCoresPerNode != null && minCoresPerNode > 0 && appCoresPerNode > 0 && appCoresPerNode < minCoresPerNode)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_LOW", "CoresPerNode", appCoresPerNode, minCoresPerNode, execQName, execSystemId);
            errMessages.add(msg);
          }
          // MemoryMB
          if (maxMemoryMB != null && maxMemoryMB > 0 && appMemoryMb > 0 && appMemoryMb > maxMemoryMB)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_HIGH", "MemoryMB", appMemoryMb, maxMemoryMB, execQName, execSystemId);
            errMessages.add(msg);
          }
          if (minMemoryMB != null && minMemoryMB > 0 && appMemoryMb > 0 && appMemoryMb < minMemoryMB)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_LOW", "MemoryMB", appMemoryMb, minMemoryMB, execQName, execSystemId);
            errMessages.add(msg);
          }
          // Minutes
          if (maxMinutes != null && maxMinutes > 0 && appMaxMinutes > 0 && appMaxMinutes > maxMinutes)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_HIGH", "MaxMinutes", appMaxMinutes, maxMinutes, execQName, execSystemId);
            errMessages.add(msg);
          }
          if (minMinutes != null && minMinutes > 0 && appMaxMinutes > 0 && appMaxMinutes < minMinutes)
          {
            msg = LibUtils.getMsg("APPLIB_EXECQ_LIMIT_LOW", "MaxMinutes", appMaxMinutes, minMinutes, execQName, execSystemId);
            errMessages.add(msg);
          }
        }
      }
    }

    // If archiveSystemId is set verify that it exists
    String archiveSystemId = app.getArchiveSystemId();
    if (!StringUtils.isBlank(archiveSystemId))
    {
      TapisSystem archiveSystem = null;
      try
      {
        archiveSystem = systemsClient.getSystem(archiveSystemId);
      }
      catch (TapisClientException e)
      {
        msg = LibUtils.getMsg("APPLIB_ARCHSYS_CHECK_ERROR", archiveSystemId, e.getMessage());
        _log.error(msg, e);
        errMessages.add(msg);
      }
      if (archiveSystem == null)
      {
        msg = LibUtils.getMsg("APPLIB_ARCHSYS_NO_SYSTEM", archiveSystemId);
        errMessages.add(msg);
      }
    }

    // If validation failed throw an exception
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(authenticatedUser, app.getId(), errMessages);
      _log.error(allErrors);
      throw new IllegalStateException(allErrors);
    }
  }

  /**
   * Retrieve set of user permissions given sk client, user, tenant, id
   * @param skClient - SK client
   * @param userName - name of user
   * @param tenantName - name of tenant
   * @param resourceId - Id of resource
   * @return - Set of Permissions for the user
   */
  private static Set<Permission> getUserPermSet(SKClient skClient, String userName, String tenantName,
                                                String resourceId)
          throws TapisClientException
  {
    var userPerms = new HashSet<Permission>();
    for (Permission perm : Permission.values())
    {
      String permSpec = String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), resourceId);
      if (skClient.isPermitted(tenantName, userName, permSpec)) userPerms.add(perm);
    }
    return userPerms;
  }

  /**
   * Create a set of individual permSpec entries based on the list passed in
   * @param permList - list of individual permissions
   * @return - Set of permSpec entries based on permissions
   */
  private static Set<String> getPermSpecSet(String tenantName, String appId, Set<Permission> permList)
  {
    var permSet = new HashSet<String>();
    for (Permission perm : permList) { permSet.add(getPermSpecStr(tenantName, appId, perm)); }
    return permSet;
  }

  /**
   * Create a permSpec given a permission
   * @param perm - permission
   * @return - permSpec entry based on permission
   */
  private static String getPermSpecStr(String tenantName, String appId, Permission perm)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), appId);
  }

  /**
   * Create a permSpec for all permissions
   * @return - permSpec entry for all permissions
   */
  private static String getPermSpecAllStr(String tenantName, String appId)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, "*", appId);
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(AuthenticatedUser authenticatedUser, String appId, List<String> msgList) {
    var sb = new StringBuilder(LibUtils.getMsgAuth("APPLIB_CREATE_INVALID_ERRORLIST", authenticatedUser, appId));
    sb.append(System.lineSeparator());
    if (msgList == null || msgList.isEmpty()) return sb.toString();
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   * Check to see if owner is trying to update permissions for themselves.
   * If so throw an exception because this would be confusing since owner always has full permissions.
   * For an owner permissions are never checked directly.
   *
   * @param authenticatedUser User making the request
   * @param tenant App tenant
   * @param id App id
   * @param userName user for whom perms are being updated
   * @param opStr Operation in progress, for logging
   * @return name of owner
   */
  private String checkForOwnerPermUpdate(AuthenticatedUser authenticatedUser, String tenant, String id,
                                         String userName, String opStr)
          throws TapisException, NotAuthorizedException
  {
    // Look up owner. If not found then consider not authorized. Very unlikely at this point.
    String owner = dao.getAppOwner(tenant, id);
    if (StringUtils.isBlank(owner))
      throw new NotAuthorizedException(LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, id, opStr), NO_CHALLENGE);
    // If owner making the request and owner is the target user for the perm update then reject.
    if (owner.equals(authenticatedUser.getOboUser()) && owner.equals(userName))
    {
      // If it is a svc making request reject with no auth, if user making request reject with special message.
      // Need this check since svc not allowed to update perms but checkAuth happens after checkForOwnerPermUpdate.
      // Without this the op would be denied with a misleading message.
      // Unfortunately this means auth check for svc in 2 places but not clear how to avoid it.
      //   On the bright side it means at worst operation will be denied when maybe it should be allowed which is better
      //   than the other way around.
      if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
        throw new NotAuthorizedException(LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, id, opStr), NO_CHALLENGE);
      else
        throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_OWNER_UPDATE", authenticatedUser, id, opStr));
    }
    return owner;
  }

  /**
   * Standard service level authorization check. Check is different for service and user requests.
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and authorization is denied.
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param op - operation name
   * @param appId - name of the app
   * @param owner - app owner
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuth(AuthenticatedUser authenticatedUser, AppOperation op, String appId,
                         String owner, String targetUser, Set<Permission> perms)
      throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException
  {
    // Check service and user requests separately to avoid confusing a service name with a user name
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) {
      // This is a service request. The user name will be the service name. E.g. files, jobs, streams, etc
      if (op == AppOperation.read && SVCLIST_READ.contains(authenticatedUser.getName())) return;
    }
    else
    {
      // User check
      checkAuthUser(authenticatedUser, op, null, null, appId, owner, targetUser, perms);
      return;
    }
    // Not authorized, throw an exception
    throw new NotAuthorizedException(LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, appId, op.name()), NO_CHALLENGE);
  }

  /**
   * User based authorization check.
   * Can be used for OBOUser type checks.
   * By default use JWT tenant and user from authenticatedUser, allow for optional tenant or user.
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and
   *   authorization is denied.
   * Operations:
   *  Create -      must be owner or have admin role
   *  Delete -      must be owner or have admin role
   *  ChangeOwner - must be owner or have admin role
   *  GrantPerm -   must be owner or have admin role
   *  Read -     must be owner or have admin role or have READ or MODIFY permission or be in list of allowed services
   *  getPerms - must be owner or have admin role or have READ or MODIFY permission or be in list of allowed services
   *  Modify - must be owner or have admin role or have MODIFY permission
   *  Execute - must be owner or have admin role or have EXECUTE permission
   *  RevokePerm -  must be owner or have admin role or apiUserId=targetUser and meet certain criteria (allowUserRevokePerm)
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param op - operation name
   * @param tenantToCheck - optional name of the tenant to use. Default is to use authenticatedUser.
   * @param userToCheck - optional name of the user to check. Default is to use authenticatedUser.
   * @param appId - name of the system
   * @param owner - system owner
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuthUser(AuthenticatedUser authenticatedUser, AppOperation op,
                             String tenantToCheck, String userToCheck,
                             String appId, String owner, String targetUser, Set<Permission> perms)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    // Requires owner. If no owner specified and owner cannot be determined then log an error and deny.
    if (StringUtils.isBlank(owner)) owner = dao.getAppOwner(tenantName, appId);
    if (StringUtils.isBlank(owner)) {
      String msg = LibUtils.getMsgAuth("APPLIB_AUTH_NO_OWNER", authenticatedUser, appId, op.name());
      _log.error(msg);
      throw new NotAuthorizedException(msg, NO_CHALLENGE);
    }
    switch(op) {
      case create:
      case enable:
      case disable:
      case delete:
      case undelete:
      case changeOwner:
      case grantPerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName))
          return;
        break;
      case hardDelete:
        if (hasAdminRole(authenticatedUser, tenantName, userName)) return;
        break;
      case read:
      case getPerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
              isPermittedAny(authenticatedUser, tenantName, userName, appId, READMODIFY_PERMS))
          return;
        break;
      case modify:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                isPermitted(authenticatedUser, tenantName, userName, appId, Permission.MODIFY))
          return;
        break;
      case execute:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                isPermitted(authenticatedUser, tenantName, userName, appId, Permission.EXECUTE))
          return;
        break;
      case revokePerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                (userName.equals(targetUser) &&
                        allowUserRevokePerm(authenticatedUser, tenantName, userName, appId, perms)))
          return;
        break;
    }
    // Not authorized, throw an exception
    throw new NotAuthorizedException(LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, appId, op.name()), NO_CHALLENGE);
  }

  /**
   * Determine all apps that a user is allowed to see.
   * If all apps return null else return list of app IDs
   * An empty list indicates no apps allowed.
   */
  private Set<String> getAllowedAppIDs(AuthenticatedUser authenticatedUser, String appTenantName)
          throws TapisException, TapisClientException
  {
    // If requester is a service or an admin then all apps allowed
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()) ||
        hasAdminRole(authenticatedUser, null, null)) return null;
    var appIDs = new HashSet<String>();
    var userPerms = getSKClient().getUserPerms(appTenantName, authenticatedUser.getOboUser());
    // Check each perm to see if it allows user READ access.
    for (String userPerm : userPerms)
    {
      if (StringUtils.isBlank(userPerm)) continue;
      // Split based on :, permSpec has the format app:<tenant>:<perms>:<system_name>
      // NOTE: This assumes value in last field is always an id and never a wildcard.
      String[] permFields = COLON_SPLIT.split(userPerm);
      if (permFields.length < 4) continue;
      if (permFields[0].equals(PERM_SPEC_PREFIX) &&
           (permFields[2].contains(Permission.READ.name()) ||
            permFields[2].contains(Permission.MODIFY.name()) ||
            permFields[2].contains(App.PERMISSION_WILDCARD)))
      {
        appIDs.add(permFields[3]);
      }
    }
    return appIDs;
  }

  /**
   * Check to see if a user has the service admin role
   * By default use authenticatedUser, allow for optional tenant or user.
   */
  private boolean hasAdminRole(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    var skClient = getSKClient();
    return skClient.isAdmin(tenantName, userName);
  }

  /**
   * Check to see if a user has the specified permission
   * By default use JWT tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean isPermitted(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                              String appId, Permission perm)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    var skClient = getSKClient();
    String permSpecStr = getPermSpecStr(tenantName, appId, perm);
    return skClient.isPermitted(tenantName, userName, permSpecStr);
  }

  /**
   * Check to see if a user has any of the set of permissions
   * By default use JWT tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean isPermittedAny(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                                 String appId, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    var skClient = getSKClient();
    var permSpecs = new ArrayList<String>();
    for (Permission perm : perms) {
      permSpecs.add(getPermSpecStr(tenantName, appId, perm));
    }
    return skClient.isPermittedAny(tenantName, userName, permSpecs.toArray(App.EMPTY_STR_ARRAY));
  }

  /**
   * Check to see if a user who is not owner or admin is authorized to revoke permissions
   * By default use JWT tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean allowUserRevokePerm(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                                      String appId, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Perms should never be null. Fall back to deny as best security practice.
    if (perms == null) return false;
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    if (perms.contains(Permission.MODIFY)) return isPermitted(authenticatedUser, tenantName, userName, appId, Permission.MODIFY);
    if (perms.contains(Permission.READ)) return isPermittedAny(authenticatedUser, tenantName, userName, appId, READMODIFY_PERMS);
    return false;
  }

  /**
   * Remove all SK artifacts associated with an App: user permissions, App role
   * No checks are done for incoming arguments and the app must exist
   */
  private void removeSKArtifacts(AuthenticatedUser authenticatedUser, String appId, AppOperation op)
          throws TapisException, TapisClientException
  {
    // For service request use oboTenant for tenant associated with the app
    String appTenantName = authenticatedUser.getTenantId();
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    var skClient = getSKClient();

    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = String.format(PERM_SPEC_TEMPLATE, appTenantName, "%", appId);
    var userNames = skClient.getUsersWithPermission(appTenantName, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames)
    {
      revokePermissions(skClient, appTenantName, appId, userName, ALL_PERMS);
      // Remove wildcard perm
      skClient.revokeUserPermission(appTenantName, userName, getPermSpecAllStr(appTenantName, appId));
    }
  }

  /**
   * Revoke permissions
   * No checks are done for incoming arguments and the app must exist
   */
  private static int revokePermissions(SKClient skClient, String appTenantName, String appId, String userName, Set<Permission> permissions)
          throws TapisClientException
  {
    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(appTenantName, appId, permissions);
    // Remove perms from default user role
    for (String permSpec : permSpecSet)
    {
      skClient.revokeUserPermission(appTenantName, userName, permSpec);
    }
    return permSpecSet.size();
  }

  /**
   * Merge a patch into an existing App
   * Attributes that can be updated:
   *   description, runtime, runtimeVersion, runtimeOptions, containerImage, maxJobs, maxJobsPerUser, strictFileInputs,
   *   jobAttributes, tags, notes.
   */
  private App createPatchedApp(App o, PatchApp p)
  {
    App app1 = new App(o);
    if (p.getDescription() != null) app1.setDescription(p.getDescription());
    if (p.getRuntime() != null) app1.setRuntime(p.getRuntime());
    if (p.getRuntimeVersion() != null) app1.setRuntimeVersion(p.getRuntimeVersion());
    if (p.getRuntimeOptions() != null) app1.setRuntimeOptions(p.getRuntimeOptions());
    if (p.getContainerImage() != null) app1.setContainerImage(p.getContainerImage());
    if (p.getMaxJobs() != null) app1.setMaxJobs(p.getMaxJobs());
    if (p.getMaxJobsPerUser() != null) app1.setMaxJobsPerUser(p.getMaxJobsPerUser());
    if (p.isStrictFileInputs() != null) app1.setStrictFileInputs(p.isStrictFileInputs());
    // Start JobAttributes
    if (p.getJobDescription() != null) app1.setJobDescription(p.getJobDescription());
    if (p.isDynamicExecSystem() != null) app1.setDynamicExecSystem(p.isDynamicExecSystem());
    if (p.getExecSystemConstraints() != null) app1.setExecSystemConstraints(p.getExecSystemConstraints());
    if (p.getExecSystemId() != null) app1.setExecSystemId(p.getExecSystemId());
    if (p.getExecSystemExecDir() != null) app1.setExecSystemExecDir(p.getExecSystemExecDir());
    if (p.getExecSystemInputDir() != null) app1.setExecSystemInputDir(p.getExecSystemInputDir());
    if (p.getExecSystemOutputDir() != null) app1.setExecSystemOutputDir(p.getExecSystemOutputDir());
    if (p.getExecSystemLogicalQueue() != null) app1.setExecSystemLogicalQueue(p.getExecSystemLogicalQueue());
    if (p.getArchiveSystemId() != null) app1.setArchiveSystemId(p.getArchiveSystemId());
    if (p.getArchiveSystemDir() != null) app1.setArchiveSystemDir(p.getArchiveSystemDir());
    if (p.getArchiveOnAppError() != null) app1.setArchiveOnAppError(p.getArchiveOnAppError());
    // Start parameterSet
    if (p.getAppArgs() != null) app1.setAppArgs(p.getAppArgs());
    if (p.getContainerArgs() != null) app1.setContainerArgs(p.getContainerArgs());
    if (p.getSchedulerOptions() != null) app1.setSchedulerOptions(p.getSchedulerOptions());
    if (p.getEnvVariables() != null) app1.setEnvVariables(p.getEnvVariables());
    if (p.getArchiveIncludes() != null) app1.setArchiveIncludes(p.getArchiveIncludes());
    if (p.getArchiveExcludes() != null) app1.setArchiveExcludes(p.getArchiveExcludes());
    if (p.getArchiveIncludeLaunchFiles() != null) app1.setArchiveIncludeLaunchFiles(p.getArchiveIncludeLaunchFiles());
    // End parameterSet
    if (p.getFileInputs() != null) app1.setFileInputs(p.getFileInputs());
    if (p.getNodeCount() != null) app1.setNodeCount(p.getNodeCount());
    if (p.getCoresPerNode() != null) app1.setCoresPerNode(p.getCoresPerNode());
    if (p.getMemoryMb() != null) app1.setMemoryMb(p.getMemoryMb());
    if (p.getMaxMinutes() != null) app1.setMaxMinutes(p.getMaxMinutes());
    if (p.getNotifSubscriptions() != null) app1.setNotificationSubscriptions(p.getNotifSubscriptions());
    if (p.getJobTags() != null) app1.setJobTags(p.getJobTags());
    // End JobAttributes

    if (p.getTags() != null) app1.setTags(p.getTags());
    if (p.getNotes() != null) app1.setNotes(p.getNotes());
    return app1;
  }

  /**
   * Find and return a LogicalQueue given list of queues and a queue name
   * Return null if not found
   */
  private static LogicalQueue getLogicalQ(List<LogicalQueue> qList, String qName)
  {
    // If no list or no name then return null
    if (qList == null || qList.isEmpty() || StringUtils.isBlank(qName)) return null;
    // Search the list of the queue with the requested name.
    for (LogicalQueue q : qList) { if (qName.equals(q.getName())) return q; }
    return null;
  }
}
