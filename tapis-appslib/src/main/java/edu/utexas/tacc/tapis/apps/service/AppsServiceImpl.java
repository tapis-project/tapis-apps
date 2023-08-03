package edu.utexas.tacc.tapis.apps.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import edu.utexas.tacc.tapis.apps.model.*;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.utexas.tacc.tapis.security.client.model.SKShareHasPrivilegeParms;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.systems.client.gen.model.LogicalQueue;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.dao.AppsDaoImpl;
import edu.utexas.tacc.tapis.apps.model.App.JobType;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.search.parser.ASTParser;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.security.client.gen.model.ReqShareResource;
import edu.utexas.tacc.tapis.security.client.gen.model.SkShare;
import edu.utexas.tacc.tapis.security.client.gen.model.SkShareList;
import edu.utexas.tacc.tapis.security.client.model.SKShareDeleteShareParms;
import edu.utexas.tacc.tapis.security.client.model.SKShareGetSharesParms;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
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
  private static final String FILES_SERVICE = TapisConstants.SERVICE_NAME_FILES;
  private static final String JOBS_SERVICE = TapisConstants.SERVICE_NAME_JOBS;
  private static final Set<String> SVCLIST_IMPERSONATE = new HashSet<>(Set.of(JOBS_SERVICE));
  private static final Set<String> SVCLIST_RESOURCETENANT = new HashSet<>(Set.of(JOBS_SERVICE));

  // Message keys
  private static final String ERROR_ROLLBACK = "APPLIB_ERROR_ROLLBACK";
  private static final String NOT_FOUND = "APPLIB_NOT_FOUND";

  // NotAuthorizedException requires a Challenge, although it serves no purpose here.
  private static final String NO_CHALLENGE = "NoChallenge";

  // Compiled regex for splitting around ":"
  private static final Pattern COLON_SPLIT = Pattern.compile(":");

  // Named and typed null values to make it clear what is being passed in to a method
  private static final String nullOwner = null;
  private static final String nullImpersonationId = null;
  private static final String nullTargetUser = null;
  private static final Set<Permission> nullPermSet = null;
  private static final AppShare nullAppsShare = null;

  // Sharing constants
  private static final String OP_SHARE = "share";
  private static final String OP_UNSHARE = "unShare";
  private static final Set<String> publicUserSet = Collections.singleton(SKClient.PUBLIC_GRANTEE); // "~public"
  private static final String APPS_SHR_TYPE = "apps";

  // Connection timeouts for SKClient
  private static final int SK_READ_TIMEOUT_MS = 20000;
  private static final int SK_CONN_TIMEOUT_MS = 20000;

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  public enum AuthListType {OWNED, SHARED_DIRECT, SHARED_PUBLIC, MINE, READ_PERM, ALL}
  public static final AuthListType DEFAULT_LIST_TYPE = AuthListType.OWNED;

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

  /**
   * Initialize the service:
   *   init service context
   *   migrate DB
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

  // -----------------------------------------------------------------------
  // ------------------------- Apps -------------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a new app object given an App and the text used to create the App.
   * Secrets in the text should be masked.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param app - Pre-populated App object
   * @param rawData - Text used to create the App object - secrets should be scrubbed. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - app exists OR App in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public void createApp(ResourceRequestUser rUser, App app, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException
  {
    AppOperation op = AppOperation.create;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (app == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    _log.trace(LibUtils.getMsgAuth("APPLIB_CREATE_TRACE", rUser, rawData));
    String tenant = app.getTenant();
    String appId = app.getId();
    String appVersion = app.getVersion();

    // ---------------------------- Check inputs ------------------------------------
    // Required app attributes: id, version
    if (StringUtils.isBlank(tenant) || StringUtils.isBlank(appId) ||
        StringUtils.isBlank(appVersion))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", rUser, appId));
    }

    // Check if app with id+version already exists
    if (dao.checkForApp(tenant, appId, appVersion, true))
    {
      throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_APP_EXISTS", rUser, appId, appVersion));
    }

    // Make sure owner, notes and tags are all set
    app.setDefaults();

    // ----------------- Resolve variables for any attributes that might contain them --------------------
    app.resolveVariables(rUser.getOboUserId());

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerKnown(rUser, op, appId, app.getOwner());

    // ---------------- Check for reserved names ------------------------
    checkReservedIds(rUser, appId);

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(rUser, app);

    // Construct Json string representing the App about to be created
    // This will be used as the description for the change history record
    App scrubbedApp = new App(app);
    String changeDescription = TapisGsonUtils.getGson().toJson(scrubbedApp);

    // ----------------- Create all artifacts --------------------
    // Creation of app and perms not in single DB transaction.
    // Use try/catch to rollback any writes in case of failure.
    boolean appCreated = false;
    String appsPermSpecALL = getPermSpecAllStr(tenant, appId);

    try {
      // ------------------- Make Dao call to persist the app -----------------------------------
      appCreated = dao.createApp(rUser, app, changeDescription, rawData);

    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      // Log error
      String msg = LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ROLLBACK", rUser, appId, e0.getMessage());
      _log.error(msg);

      // Rollback
      // Remove app from DB
      if (appCreated) try {dao.hardDeleteApp(tenant, appId); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, appId, "hardDelete", e.getMessage()));}
      throw e0;
    }
  }

  /**
   * Update existing version of an app given a PatchApp and the text used to create the PatchApp.
   * Secrets in the text should be masked.
   * Attributes that can be updated:
   *   description, runtime, runtimeVersion, runtimeOptions, containerImage, maxJobs, maxJobsPerUser, strictFileInputs,
   *   all of jobAttributes (including all of parameterSet), tags, notes.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param patchApp - Pre-populated PatchApp object
   * @param rawData - Text used to create the PatchApp object - secrets should be scrubbed. Saved in update record.
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public void patchApp(ResourceRequestUser rUser, String appId, String appVersion, PatchApp patchApp, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (patchApp == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId) || StringUtils.isBlank(appId) ||
        StringUtils.isBlank(appVersion) || StringUtils.isBlank(rawData))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", rUser, appId));
    }

    // App with given version must already exist and not be deleted
    if (!dao.checkForAppVersion(resourceTenantId, appId, appVersion, false))
    {
      throw new NotFoundException(LibUtils.getMsgAuth("APPLIB_VER_NOT_FOUND", rUser, appId));
    }

    // If needed process request to create list of env variables with proper defaults.
    // Note that because this is a patch DO NOT fill in with non-null unless it is in the request.
    // On service side we rely on null to indicate it was not in the patch request.
    if (patchApp.getJobAttributes() != null &&
        patchApp.getJobAttributes().getParameterSet() != null &&
        patchApp.getJobAttributes().getParameterSet().getEnvVariables() != null)
    {
      List<KeyValuePair> envVars = App.processEnvVariables(patchApp.getJobAttributes().getParameterSet().getEnvVariables());
      patchApp.getJobAttributes().getParameterSet().setEnvVariables(envVars);
    }

    // Retrieve the app being patched and create fully populated App with changes merged in
    App origApp = dao.getApp(resourceTenantId, appId, appVersion);
    App patchedApp = createPatchedApp(origApp, patchApp);

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerKnown(rUser, op, appId, origApp.getOwner());

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(rUser, patchedApp);

    // Construct Json string representing the PatchApp about to be used to update the app
    String updateJsonStr = TapisGsonUtils.getGson().toJson(patchApp);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the app -----------------------------------
    dao.patchApp(rUser, appId, appVersion, patchedApp, updateJsonStr, rawData);
  }

  /**
   * Update all updatable attributes of an app given an App and the text used to create the App.
   * Incoming App must contain the tenantId, appId and appVersion.
   * Secrets in the text should be masked.
   * Attributes that cannot be updated and so will be looked up and filled in:
   *   tenant, id, version, owner, enabled
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param putApp - Pre-populated App object (including tenantId, appId, appVersion)
   * @param rawData - Text used to create the App object - secrets should be scrubbed. Saved in update record.
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public void putApp(ResourceRequestUser rUser, App putApp, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (putApp == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    // Extract various names for convenience
    String tenant = putApp.getTenant();
    String appId = putApp.getId();
    String appVersion = putApp.getVersion();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenant) || StringUtils.isBlank(appId) ||
        StringUtils.isBlank(appVersion) || StringUtils.isBlank(rawData))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", rUser, appId));
    }

    // App version must already exist and not be deleted
    if (!dao.checkForAppVersion(tenant, appId, appVersion, false))
    {
      throw new NotFoundException(LibUtils.getMsgAuth("APPLIB_VER_NOT_FOUND", rUser, appId, appVersion));
    }

    // Retrieve the app being updated and create fully populated App with changes merged in
    App origApp = dao.getApp(tenant, appId, appVersion);
    App updatedApp = createUpdatedApp(origApp, putApp);

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerKnown(rUser, op, appId, origApp.getOwner());

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(rUser, updatedApp);

    // Construct Json string representing the App about to be used to update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(putApp);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the app -----------------------------------
    dao.putApp(rUser, updatedApp, updateJsonStr, rawData);
  }

  /**
   * Update enabled to true for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public int enableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    return updateEnabled(rUser, appId, AppOperation.enable);
  }

  /**
   * Update enabled to false for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public int disableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    return updateEnabled(rUser, appId, AppOperation.disable);
  }

  /**
   * Update deleted to true for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public int deleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    return updateDeleted(rUser, appId, AppOperation.delete);
  }

  /**
   * Update deleted to false for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public int undeleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    return updateDeleted(rUser, appId, AppOperation.undelete);
  }

  /**
   * Change owner of an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param newOwnerName - Username of new owner
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public int changeAppOwner(ResourceRequestUser rUser, String appId, String newOwnerName)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    AppOperation op = AppOperation.changeOwner;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(newOwnerName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
 
    String resourceTenantId = rUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", rUser, appId));

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
         throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // Retrieve the old owner
    String oldOwnerName = dao.getAppOwner(resourceTenantId, appId);

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerKnown(rUser, op, appId, oldOwnerName);

    // If new owner same as old owner then this is a no-op
    if (newOwnerName.equals(oldOwnerName)) return 0;

    // ----------------- Make all updates --------------------
    // Changes not in single DB transaction.
    // Use try/catch to rollback any changes in case of failure.
    String appsPermSpec = getPermSpecAllStr(resourceTenantId, appId);
    try {
      // ------------------- Make Dao call to update the app owner -----------------------------------
      dao.updateAppOwner(rUser, resourceTenantId, appId, newOwnerName);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      try { dao.updateAppOwner(rUser, resourceTenantId, appId, oldOwnerName); } catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, appId, "updateOwner", e.getMessage()));}
      throw e0;
    }
    return 1;
  }

  /**
   * Hard delete an app record given the app name.
   * Also remove artifacts from the Security Kernel.
   * NOTE: This is package-private. Only test code should ever use it.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   */
  int hardDeleteApp(ResourceRequestUser rUser, String resourceTenantId, String appId)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.hardDelete;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // If app does not exist then 0 changes
    if (!dao.checkForApp(resourceTenantId, appId, true)) return 0;

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // Remove SK artifacts
    removeSKArtifacts(resourceTenantId, appId);

    // Delete the app
    return dao.hardDeleteApp(resourceTenantId, appId);
  }

  /**
   * Hard delete all resources in the "test" tenant.
   * Also remove artifacts from the Security Kernel.
   * NOTE: This is package-private. Only test code should ever use it.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   */
  int hardDeleteAllTestTenantResources(ResourceRequestUser rUser)
          throws TapisException, TapisClientException
  {
    // For safety hard code the tenant name
    String resourceTenantId = "test";
    // Fetch all resource Ids including deleted items
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    var resourceIdSet = dao.getAppIDs(resourceTenantId, true);
    for (String id : resourceIdSet)
    {
      hardDeleteApp(rUser, resourceTenantId, id);
    }
    return resourceIdSet.size();
  }

  /**
   * checkForApp
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - Name of the app
   * @return true if app exists and has not been deleted, false otherwise
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public boolean checkForApp(ResourceRequestUser rUser, String appId) throws TapisException, TapisClientException
  {
    return checkForApp(rUser, appId, false);
  }

  /**
   * checkForApp
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - Name of the app
   * @param includeDeleted - indicates if check should include resources marked as deleted
   * @return true if app exists and has not been deleted, false otherwise
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public boolean checkForApp(ResourceRequestUser rUser, String appId, boolean includeDeleted)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    String resourceTenantId = rUser.getOboTenantId();
 
    // We need owner to check auth and if app not there cannot find owner, so cannot do auth check if no app
    if (dao.checkForApp(resourceTenantId, appId, includeDeleted)) {
      // ------------------------- Check authorization -------------------------
      checkAuthOwnerUnknown(rUser, op, appId);
      return true;
    }
    return false;
  }

  /**
   * isEnabled
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - Name of the app
   * @return true if app is enabled, false otherwise
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public boolean isEnabled(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    String resourceTenantId = rUser.getOboTenantId();

    // Resource must exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);
    return dao.isEnabled(resourceTenantId, appId);
  }

  /**
   * getApp
   * Retrieve specified or most recently created version of an application.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - Name of the app
   * @param appVersion - Version of the app, null or blank for latest version
   * @param requireExecPerm - check for EXECUTE permission as well as READ permission
   * @param impersonationId - use provided Tapis username instead of oboUser when checking auth
   * @param resourceTenant - use provided tenant instead of oboTenant which fetching resource
   * @return populated instance of an App or null if not found or user not authorized.
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public App getApp(ResourceRequestUser rUser, String appId, String appVersion, boolean requireExecPerm,
                    String impersonationId, String resourceTenant)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    // Extract various names for convenience
    String oboOrImpersonatedUser = StringUtils.isBlank(impersonationId) ? rUser.getOboUserId() : impersonationId;
    String oboOrResourceTenant = StringUtils.isBlank(resourceTenant) ? rUser.getOboTenantId() : resourceTenant;

    // If impersonationId set confirm that it is allowed and determine final obo user.
    if (!StringUtils.isBlank(impersonationId)) checkImpersonationAllowed(rUser, op, appId, impersonationId);
    // If resourceTenant set confirm it is allowed
    if (!StringUtils.isBlank(resourceTenant)) checkResourceTenantAllowed(rUser, op, appId, resourceTenant);

    // Fetch the app. We need to make sure it exists and is not deleted.
    // Also, knowing app owner is useful here. We can skip auth checking.
    App app = dao.getApp(oboOrResourceTenant, appId, appVersion);
    if (app == null) return null;

    // ------------------------- Check authorization -------------------------
    // If owner is making the request then always allowed, and we will not set sharedAppCtx
    boolean isPermitted = true;
    String sharedAppCtx = null;

    // If not owner we need to do some authorization checking and determine if sharedAppCtx must be set
    String owner = app.getOwner();
    if (!oboOrImpersonatedUser.equals(owner))
    {
      // Access might be allowed due to permission or due to sharing, so we will need to check for both.
      // Also, need to record if it was allowed due to sharing. Jobs needs to know.
      // First check if allowed by permissions or ownership and record the result.
      try
      {
        checkAuth(rUser, op, appId, owner, nullTargetUser, nullPermSet, impersonationId);
      }
      catch (ForbiddenException e) {isPermitted = false;}

      // Check shared app context
      // Even if allowed by permission we still need to check for sharing and set sharedAppCtx in the returned app.
      boolean shared = isAppSharedWithUser(rUser, appId, oboOrImpersonatedUser, Permission.READ);
      // NOTE: Grantor is always app owner
      if (shared) sharedAppCtx = owner;

      // If not permitted or shared then deny
      if (!isPermitted && !shared)
      {
        throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, appId, op.name()));
      }

      // If flag is set to also require EXECUTE perm then make explicit auth call to make sure user has exec perm
      if (!shared && requireExecPerm)
      {
        checkAuth(rUser, AppOperation.execute, appId, owner, nullTargetUser, nullPermSet, impersonationId);
      }
    }

    // Update dynamically computed info.
    app.setSharedAppCtx(sharedAppCtx);
    AppShare appShare = getAppShare(rUser, appId);
    app.setIsPublic(appShare.isPublic());
    app.setSharedWithUsers(appShare.getUserList());
    return app;
  }

  /**
   * Get count of all apps matching certain criteria.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @param listType - allows for filtering results based on authorization: OWNED, SHARED_PUBLIC, ALL
   * @return Count of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public int getAppsTotalCount(ResourceRequestUser rUser, List<String> searchList, List<OrderBy> orderByList,
                               String startAfter, boolean includeDeleted, String listType)
          throws TapisException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));

    // Process listType. Figure out how we will filter based on authorization. OWNED, ALL, etc.
    // If no listType provided use the default
    if (StringUtils.isBlank(listType)) listType = DEFAULT_LIST_TYPE.name();
    // Validate the listType enum (case insensitive).
    listType = listType.toUpperCase();
    if (!EnumUtils.isValidEnum(AuthListType.class, listType))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_LISTTYPE_ERROR", rUser, listType);
      _log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    AuthListType listTypeEnum = AuthListType.valueOf(listType);

    // Set some flags for convenience and clarity
    boolean allItems = AuthListType.ALL.equals(listTypeEnum);             // Include everything
    boolean publicOnly = AuthListType.SHARED_PUBLIC.equals(listTypeEnum); // Include only publicly shared
    boolean sharedOnly = AuthListType.SHARED_DIRECT.equals(listTypeEnum); // Include only shared directly with user
    boolean mine = AuthListType.MINE.equals(listTypeEnum);                // Include owned and directly shared with user
    boolean readPermOnly = AuthListType.READ_PERM.equals(listTypeEnum);       // Include only directly granted READ/MODIFY

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
        String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", rUser, e.getMessage());
        _log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // If needed, get IDs for items for which requester has READ or MODIFY permission
    Set<String> viewableIDs = new HashSet<>();
    if (allItems || readPermOnly) viewableIDs = getViewableAppIDs(rUser);

    // If needed, get IDs for items shared with the requester or only shared publicly.
    Set<String> sharedIDs = new HashSet<>();
    if (allItems) sharedIDs = getSharedAppIDs(rUser, false, false);
    else if (publicOnly) sharedIDs = getSharedAppIDs(rUser, true, false);
    else if (sharedOnly || mine) sharedIDs = getSharedAppIDs(rUser, false, true);

    // Count all allowed systems matching the search conditions
    return dao.getAppsCount(rUser, verifiedSearchList, null, orderByList, startAfter, versionSpecified, includeDeleted,
                            listTypeEnum, viewableIDs, sharedIDs);
  }

  /**
   * Get apps
   * Retrieve apps accessible by requester and matching any search conditions provided.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @param listType - allows for filtering results based on authorization: OWNED, SHARED_PUBLIC, ALL
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getApps(ResourceRequestUser rUser, List<String> searchList, int limit, List<OrderBy> orderByList,
                           int skip, String startAfter, boolean includeDeleted, String listType)
          throws TapisException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));

    // Process listType. Figure out how we will filter based on authorization. OWNED, ALL, etc.
    // If no listType provided use the default
    if (StringUtils.isBlank(listType)) listType = DEFAULT_LIST_TYPE.name();
    // Validate the listType enum (case insensitive).
    listType = listType.toUpperCase();
    if (!EnumUtils.isValidEnum(AuthListType.class, listType))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_LISTTYPE_ERROR", rUser, listType);
      _log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    AuthListType listTypeEnum = AuthListType.valueOf(listType);

    // Set some flags for convenience and clarity
    boolean allItems = AuthListType.ALL.equals(listTypeEnum);             // Include everything
    boolean publicOnly = AuthListType.SHARED_PUBLIC.equals(listTypeEnum); // Include only publicly shared
    boolean sharedOnly = AuthListType.SHARED_DIRECT.equals(listTypeEnum); // Include only shared directly with user
    boolean mine = AuthListType.MINE.equals(listTypeEnum);                // Include owned and directly shared with user
    boolean readPermOnly = AuthListType.READ_PERM.equals(listTypeEnum);       // Include only directly granted READ/MODIFY

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
        String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", rUser, e.getMessage());
        _log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // If needed, get IDs for items for which requester has READ or MODIFY permission
    Set<String> viewableIDs = new HashSet<>();
    if (allItems || readPermOnly) viewableIDs = getViewableAppIDs(rUser);

    // If needed, get IDs for items shared with the requester or only shared publicly.
    Set<String> sharedIDs = new HashSet<>();
    if (allItems) sharedIDs = getSharedAppIDs(rUser, false, false);
    else if (publicOnly) sharedIDs = getSharedAppIDs(rUser, true, false);
    else if (sharedOnly || mine) sharedIDs = getSharedAppIDs(rUser, false, true);

    List<App> apps = dao.getApps(rUser, verifiedSearchList, null, limit, orderByList, skip, startAfter,
                                 versionSpecified, includeDeleted, listTypeEnum, viewableIDs, sharedIDs);
    // Update dynamically computed info.
    for (App app : apps)
    {
      AppShare appShare = getAppShare(rUser, app.getId());
      app.setIsPublic(appShare.isPublic());
      app.setSharedWithUsers(appShare.getUserList());
    }
    return apps;
  }

  /**
   * Get apps.
   * Use provided string containing a valid SQL where clause for the search.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param sqlSearchStr - string containing a valid SQL where clause
   * @param includeDeleted - whether to included resources that have been marked as deleted.
   * @param listType - allows for filtering results based on authorization: OWNED, SHARED_PUBLIC, ALL
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getAppsUsingSqlSearchStr(ResourceRequestUser rUser, String sqlSearchStr, int limit,
                                            List<OrderBy> orderByList, int skip, String startAfter,
                                            boolean includeDeleted, String listType)
          throws TapisException, TapisClientException
  {
    // If search string is empty delegate to getApps()
    if (StringUtils.isBlank(sqlSearchStr)) return getApps(rUser, null, limit, orderByList, skip, startAfter,
                                                          includeDeleted, listType);

    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));

    // Process listType. Figure out how we will filter based on authorization. OWNED, ALL, etc.
    // If no listType provided use the default
    if (StringUtils.isBlank(listType)) listType = DEFAULT_LIST_TYPE.name();
    // Validate the listType enum (case insensitive).
    listType = listType.toUpperCase();
    if (!EnumUtils.isValidEnum(AuthListType.class, listType))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_LISTTYPE_ERROR", rUser, listType);
      _log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    AuthListType listTypeEnum = AuthListType.valueOf(listType);

    // Set some flags for convenience and clarity
    boolean allItems = AuthListType.ALL.equals(listTypeEnum);             // Include everything
    boolean publicOnly = AuthListType.SHARED_PUBLIC.equals(listTypeEnum); // Include only publicly shared
    boolean sharedOnly = AuthListType.SHARED_DIRECT.equals(listTypeEnum); // Include only shared directly with user
    boolean mine = AuthListType.MINE.equals(listTypeEnum);                // Include owned and directly shared with user
    boolean readPermOnly = AuthListType.READ_PERM.equals(listTypeEnum);       // Include only directly granted READ/MODIFY

    // Validate and parse the sql string into an abstract syntax tree (AST)
    // The activemq parser validates and parses the string into an AST but there does not appear to be a way
    //          to use the resulting BooleanExpression to walk the tree. How to now create a usable AST?
    //   I believe we don't want to simply try to run the where clause for various reasons:
    //      - SQL injection
    //      - we want to verify the validity of each <attr>.<op>.<value>
    //        looks like activemq parser will ensure the leaf nodes all represent <attr>.<op>.<value> and in principle
    //        we should be able to check each one and generate of list of errors for reporting.
    //  Looks like jOOQ can parse an SQL string into a jooq Condition. Do this in the Dao? But still seems like no way
    //    to walk the AST and check each condition, so we can report on errors.
//    BooleanExpression searchAST;
    ASTNode searchAST;
    try { searchAST = ASTParser.parse(sqlSearchStr); }
    catch (Exception e)
    {
      String msg = LibUtils.getMsgAuth("APPLIB_SEARCH_ERROR", rUser, e.getMessage());
      _log.error(msg, e);
      throw new IllegalArgumentException(msg);
    }

    // If needed, get IDs for items for which requester has READ or MODIFY permission
    Set<String> viewableIDs = new HashSet<>();
    if (allItems || readPermOnly) viewableIDs = getViewableAppIDs(rUser);

    // If needed, get IDs for items shared with the requester or only shared publicly.
    Set<String> sharedIDs = new HashSet<>();
    if (allItems) sharedIDs = getSharedAppIDs(rUser, false, false);
    else if (publicOnly) sharedIDs = getSharedAppIDs(rUser, true, false);
    else if (sharedOnly || mine) sharedIDs = getSharedAppIDs(rUser, false, true);

    // Pass in null for versionSpecified since the Dao makes the same call we would make, so no time saved doing it here.
    Boolean versionSpecified = null;

    // Get all allowed apps matching the search conditions
    List<App> apps = dao.getApps(rUser, null, searchAST, limit, orderByList, skip, startAfter, versionSpecified,
                                 includeDeleted, listTypeEnum, viewableIDs, sharedIDs);
    // Update dynamically computed flags.
    for (App app : apps)
    {
      AppShare appShare = getAppShare(rUser, app.getId());
      app.setIsPublic(appShare.isPublic());
      app.setSharedWithUsers(appShare.getUserList());
    }
    return apps;
  }

  /**
   * Get app owner
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - Name of the app
   * @return - Owner or null if app not found or user not authorized
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public String getAppOwner(ResourceRequestUser rUser, String appId) throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(rUser.getOboTenantId(), appId, false)) return null;

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    return dao.getAppOwner(rUser.getOboTenantId(), appId);
  }

  // -----------------------------------------------------------------------
  // --------------------------- Permissions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Grant permissions to a user for an app.
   * Grant of MODIFY implies grant of READ
   * NOTE: Permissions only impact the default user role
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param userName - Target user for operation
   * @param permissions - list of permissions to be granted
   * @param rawData - Client provided text used to create the permissions list. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public void grantUserPermissions(ResourceRequestUser rUser, String appId, String userName,
                                   Set<Permission> permissions, String rawData)
          throws NotFoundException, TapisException, TapisClientException
  {
    AppOperation op = AppOperation.grantPerms;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(userName))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_SYSTEM", rUser));

    String oboTenant = rUser.getOboTenantId();

    // If system does not exist or has been deleted then throw an exception
    if (!dao.checkForApp(oboTenant, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // Check to see if owner is trying to update permissions for themselves.
    // If so throw an exception because this would be confusing since owner always has full permissions.
    // For an owner permissions are never checked directly.
    String owner = checkForOwnerPermUpdate(rUser, appId, userName, op.name());

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerKnown(rUser, op, appId, owner);

    // Check inputs. If anything null or empty throw an exception
    if (permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    // Grant of MODIFY implies grant of READ
    if (permissions.contains(Permission.MODIFY)) permissions.add(Permission.READ);

    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(oboTenant, appId, permissions);

    // Assign perms to user.
    // Start of updates. Will need to rollback on failure.
    try
    {
      // Assign perms to user. SK creates a default role for the user
      for (String permSpec : permSpecSet)
      {
        getSKClient().grantUserPermission(oboTenant, userName, permSpec);
      }
    }
    catch (TapisClientException tce)
    {
      // Rollback
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      String msg = LibUtils.getMsgAuth("APPLIB_PERM_ERROR_ROLLBACK", rUser, appId, tce.getMessage());
      _log.error(msg);

      // Revoke permissions that may have been granted.
      for (String permSpec : permSpecSet)
      {
        try { getSKClient().revokeUserPermission(oboTenant, userName, permSpec); }
        catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, appId, "revokePerm", e.getMessage()));}
      }
      // Convert to TapisException and re-throw
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", rUser, appId, op.name()), tce);
    }

    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(rUser, oboTenant, appId, NO_APP_VERSION, op, updateJsonStr, rawData);
  }

  /**
   * Revoke permissions from a user for an app
   * Revoke of READ implies revoke of MODIFY
   * NOTE: Permissions only impact the default user role
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param targetUser - Target user for operation
   * @param permissions - list of permissions to be revoked
   * @param rawData - Client provided text used to create the permissions list. Saved in update record.
   * @return Number of items revoked
   *
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public int revokeUserPermissions(ResourceRequestUser rUser, String appId, String targetUser,
                                   Set<Permission> permissions, String rawData)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.revokePerms;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(targetUser))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_SYSTEM", rUser));

    String oboTenant = rUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist or has been deleted then return 0 changes
    if (!dao.checkForApp(oboTenant, appId, false)) return 0;

    // Check to see if owner is trying to update permissions for themselves.
    // If so throw an exception because this would be confusing since owner always has full permissions.
    // For an owner permissions are never checked directly.
    String owner = checkForOwnerPermUpdate(rUser, appId, targetUser, op.name());

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, op, appId, owner, targetUser, permissions);

    // Check inputs. If anything null or empty throw an exception
    if (permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    // Revoke of READ implies revoke of MODIFY
    if (permissions.contains(Permission.READ)) permissions.add(Permission.MODIFY);

    int changeCount;
    // Determine current set of user permissions
    var userPermSet = getUserPermSet(targetUser, oboTenant, appId);

    try
    {
      // Revoke perms
      changeCount = revokePermissions(oboTenant, appId, targetUser, permissions);
    }
    catch (TapisClientException tce)
    {
      // Rollback
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      String msg = LibUtils.getMsgAuth("APPLIB_PERM_ERROR_ROLLBACK", rUser, appId, tce.getMessage());
      _log.error(msg);

      // Grant permissions that may have been revoked and that the user previously held.
      for (Permission perm : permissions)
      {
        if (userPermSet.contains(perm))
        {
          String permSpec = getPermSpecStr(oboTenant, appId, perm);
          try { getSKClient().grantUserPermission(oboTenant, targetUser, permSpec); }
          catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, appId, "grantPerm", e.getMessage()));}
        }
      }

      // Convert to TapisException and re-throw
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", rUser, appId, op.name()), tce);
    }

    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(rUser, oboTenant, appId, NO_APP_VERSION, op, updateJsonStr, rawData);
    return changeCount;
  }

  /**
   * Get list of app permissions for a user
   * NOTE: This retrieves permissions from all roles.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param targetUser - Target user for operation
   * @return List of permissions
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public Set<Permission> getUserPermissions(ResourceRequestUser rUser, String appId, String targetUser)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.getPerms;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId) || StringUtils.isBlank(targetUser))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    String resourceTenantId = rUser.getOboTenantId();

    // If app does not exist or has been deleted then throw an exception
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, op, appId, nullOwner, targetUser, nullPermSet);

    // Use Security Kernel client to check for each permission in the enum list
    return getUserPermSet(targetUser, resourceTenantId, appId);
  }

  /**
   * Retrieves App History list from given user and app name.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return - App history list as the result
   * @throws TapisException - for Tapis related exceptions
   * @throws TapisClientException - for Tapis Client related exceptions
   */
  @Override
  public List<AppHistoryItem> getAppHistory(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException
  {
    // ---------------------------- Check inputs ------------------------------------
    // Required app attributes: rUser, id
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    // Extract various names for convenience
    String oboTenantId = rUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(oboTenantId, appId, true)) return null;

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ------------------- Make Dao call to retrieve the app history -----------------------
    List<AppHistoryItem> result = dao.getAppHistory(oboTenantId, appId);

    return result;
  }
  
  
  /**
   * Retrieves app share information from given user and app name.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @return - app share information as the result
   * @throws TapisException - for Tapis related exceptions
   * @throws TapisClientException - for Tapis Client related exceptions
   */
  @Override
  public AppShare getAppShare(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException
  {
    // ---------------------------- Check inputs ------------------------------------
    // Required app attributes: rUser, id
    AppOperation op = AppOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    // Extract various names for convenience
    String oboTenantId = rUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(oboTenantId, appId, true)) return null;

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ------------------- Make a call to retrieve the app sharing -----------------------
    // Create SKShareGetSharesParms needed for SK calls.
    var skParms = new SKShareGetSharesParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(oboTenantId);
    skParms.setResourceId1(appId);

    var userSet = new HashSet<String>();
    
    // First determine if app is publicly shared. Search for share to grantee ~public
    skParms.setGrantee(SKClient.PUBLIC_GRANTEE);
    SkShareList skShares = getSKClient().getShares(skParms);
    // Set isPublic based on result.
    boolean isPublic = (skShares != null && skShares.getShares() != null && !skShares.getShares().isEmpty());
    // Now get all the users with whom the system has been shared
    skParms.setGrantee(null);
    skParms.setIncludePublicGrantees(false);
    skShares = getSKClient().getShares(skParms);
    if (skShares != null && skShares.getShares() != null)
    {
      for (SkShare skShare : skShares.getShares())
      {
        userSet.add(skShare.getGrantee());
      }
    }

    var shareInfo = new AppShare(isPublic, userSet);
    return shareInfo;
  }
  
  @Override
  public void shareApp(ResourceRequestUser rUser, String appId, AppShare postShare)
      throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ----------------- Make update --------------------
    updateUserShares(rUser, OP_SHARE, appId, postShare, false);
    
  }
  @Override
  public void unshareApp(ResourceRequestUser rUser, String appId, AppShare postShare)
      throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ----------------- Make update --------------------
    updateUserShares(rUser, OP_UNSHARE, appId, postShare, false);
    
  }
  @Override
  public void shareAppPublicly(ResourceRequestUser rUser, String appId) throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ----------------- Make update --------------------
    updateUserShares(rUser, OP_SHARE, appId, nullAppsShare, true);
    
  }
  
  @Override
  public void unshareAppPublicly(ResourceRequestUser rUser, String appId) throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);

    // ----------------- Make update --------------------
    updateUserShares(rUser, OP_UNSHARE, appId, nullAppsShare, true);
    
  }
  

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   * Update enabled attribute for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param appOp - operation, enable or disable
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  private int updateEnabled(ResourceRequestUser rUser, String appId, AppOperation appOp)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    String resourceTenantId = rUser.getOboTenantId();

    // App must already exist and not be deleted
    if (!dao.checkForApp(resourceTenantId, appId, false))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, appOp, appId);

    // ----------------- Make update --------------------
    if (appOp == AppOperation.enable)
      dao.updateEnabled(rUser, resourceTenantId, appId, true);
    else
      dao.updateEnabled(rUser, resourceTenantId, appId, false);
    return 1;
  }

  /**
   * Update deleted attribute for an app
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - name of app
   * @param appOp - operation, delete or undelete
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  private int updateDeleted(ResourceRequestUser rUser, String appId, AppOperation appOp)
          throws TapisException, IllegalArgumentException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));

    String resourceTenantId = rUser.getOboTenantId();

    // App must exist
    if (!dao.checkForApp(resourceTenantId, appId, true))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, appOp, appId);

    // ----------------- Make update --------------------
    if (appOp == AppOperation.delete)
      dao.updateDeleted(rUser, resourceTenantId, appId, true);
    else
      dao.updateDeleted(rUser, resourceTenantId, appId, false);
    return 1;
  }
  
  /**
   * Get Security Kernel client.
   * Need to use serviceClients.getClient() every time because it checks for expired service jwt token and
   *   refreshes it as needed.
   * Apps service always calls SK as itself, i.e. oboUser=apps, oboTenant=*site_admin_tenant*
   * @return SK client
   * @throws TapisException - for Tapis related exceptions
   */
  private SKClient getSKClient() throws TapisException
  {
    String oboTenant = getServiceTenantId();
    String oboUser = getServiceUserId();
    try
    {
      SKClient skClient = serviceClients.getClient(oboUser, oboTenant, SKClient.class);
      skClient.setReadTimeout(SK_READ_TIMEOUT_MS);
      skClient.setConnectTimeout(SK_CONN_TIMEOUT_MS);
      return skClient;
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SECURITY, oboTenant, oboUser);
      throw new TapisException(msg, e);
    }
  }

  /**
   * Get Systems client associated with specified tenant
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @return Systems client
   * @throws TapisException - for Tapis related exceptions
   */
  private SystemsClient getSystemsClient(ResourceRequestUser rUser) throws TapisException
  {
    SystemsClient sysClient;
    String tenantName;
    String userName;
    // If service request then use oboTenant and oboUser in OBO headers
    // else for user request use authenticated username and tenant in OBO headers
    if (rUser.isServiceRequest())
    {
      tenantName = rUser.getOboTenantId();
      userName = rUser.getOboUserId();
    }
    else
    {
      tenantName = rUser.getJwtTenantId();
      userName = rUser.getJwtUserId();
    }
    try
    {
      sysClient = serviceClients.getClient(userName, tenantName, SystemsClient.class);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SYSTEMS, tenantName, userName);
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
  private void checkReservedIds(ResourceRequestUser rUser, String id) throws IllegalStateException
  {
    if (App.RESERVED_ID_SET.contains(id.toUpperCase()))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_CREATE_RESERVED", rUser, id);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * Check constraints on App attributes.
   * If execSystemId set
   *   - Check that it exists with canExec = true.
   *   - Check authorization
   * If archiveSystemId set
   *   - Check that it exists.
   *   - Check authorization
   * Check LogicalQueue max/min constraints.
   * Collect and report as many errors as possible, so they can all be fixed before next attempt
   * @param app - the App to check
   * @throws IllegalStateException - if any constraints are violated
   */
  private void validateApp(ResourceRequestUser rUser, App app) throws TapisException, IllegalStateException
  {
    // Make api level checks, i.e. checks that do not involve a dao or service call.
    List<String> errMessages = app.checkAttributeRestrictions();
    var systemsClient = getSystemsClient(rUser);

    // Now make checks that do require a dao or service call.
    // If execSystemId is set verify it
    if (!StringUtils.isBlank(app.getExecSystemId())) checkExecSystem(systemsClient, app, errMessages);

    // If archiveSystemId is set verify it
    if (!StringUtils.isBlank(app.getArchiveSystemId())) checkArchiveSystem(systemsClient, app, errMessages);

    // If validation failed throw an exception
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(rUser, app.getId(), errMessages);
      _log.error(allErrors);
      throw new IllegalStateException(allErrors);
    }
  }

  /**
   * Retrieve set of user permissions given sk client, user, tenant, id
   * @param userName - name of user
   * @param tenantName - name of tenant
   * @param resourceId - Id of resource
   * @return - Set of Permissions for the user
   */
  private Set<Permission> getUserPermSet(String userName, String tenantName, String resourceId)
          throws TapisClientException, TapisException
  {
    var userPerms = new HashSet<Permission>();
    for (Permission perm : Permission.values())
    {
      String permSpec = String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), resourceId);
      if (getSKClient().isPermitted(tenantName, userName, permSpec)) userPerms.add(perm);
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
  private static String getListOfErrors(ResourceRequestUser rUser, String appId, List<String> msgList) {
    var sb = new StringBuilder(LibUtils.getMsgAuth("APPLIB_CREATE_INVALID_ERRORLIST", rUser, appId));
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
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param id App id
   * @param targetOboUser user for whom perms are being updated
   * @param opStr Operation in progress, for logging
   * @return name of owner
   */
  private String checkForOwnerPermUpdate(ResourceRequestUser rUser, String id, String targetOboUser, String opStr)
          throws TapisException
  {
    // Look up owner. If not found then consider not authorized. Very unlikely at this point.
    String owner = dao.getAppOwner(rUser.getOboTenantId(), id);
    if (StringUtils.isBlank(owner))
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_OP_NO_OWNER", rUser, id, opStr));
    // If owner making the request and owner is the target user for the perm update then reject.
    if (owner.equals(rUser.getOboUserId()) && owner.equals(targetOboUser))
    {
      // If it is a svc making request reject with not authorized, if user making request reject with special message.
      // Need this check since svc not allowed to update perms but checkAuth happens after checkForOwnerPermUpdate.
      // Without this the op would be denied with a misleading message.
      // Unfortunately this means auth check for svc in 2 places but not clear how to avoid it.
      //   On the bright side it means at worst operation will be denied when maybe it should be allowed which is better
      //   than the other way around.
      if (rUser.isServiceRequest()) throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, id, opStr));
      else throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_OWNER_UPDATE", rUser, id, opStr));
    }
    return owner;
  }

  /**
   * Determine all apps for which the user has READ or MODIFY permission.
   */
  private Set<String> getViewableAppIDs(ResourceRequestUser rUser) throws TapisException, TapisClientException
  {
    var appIDs = new HashSet<String>();
    // Use implies to filter permissions returned. Without implies all permissions for apps, etc. are returned.
    String impliedBy = null;
    String implies = String.format("%s:%s:*:*", PERM_SPEC_PREFIX, rUser.getOboTenantId());
    var userPerms = getSKClient().getUserPerms(rUser.getOboTenantId(), rUser.getOboUserId(), implies, impliedBy);
    // Check each perm to see if it allows user READ access.
    for (String userPerm : userPerms)
    {
      if (StringUtils.isBlank(userPerm)) continue;
      // Split based on :, permSpec has the format system:<tenant>:<perms>:<system_name>
      // NOTE: This assumes value in last field is always an id and never a wildcard.
      String[] permFields = COLON_SPLIT.split(userPerm);
      if (permFields.length < 4) continue;
      if (permFields[0].equals(PERM_SPEC_PREFIX) &&
              (permFields[2].contains(Permission.READ.name()) ||
                      permFields[2].contains(Permission.MODIFY.name()) ||
                      permFields[2].contains(App.PERMISSION_WILDCARD)))
      {
        // If system exists add ID to the list
        // else resource no longer exists or has been deleted so remove orphaned permissions
        if (dao.checkForApp(rUser.getOboTenantId(), permFields[3], false))
        {
          appIDs.add(permFields[3]);
        }
        else
        {
          // Log a warning and remove the permission
          String msg = LibUtils.getMsgAuth("APPLIB_PERM_ORPHAN", rUser, permFields[3]);
          _log.warn(msg);
          removeOrphanedSKPerms(permFields[3], rUser.getOboTenantId());
        }
      }
    }
    return appIDs;
  }

  /**
   * Determine apps that are shared with a user.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param publicOnly - Include only items shared public
   * @param directOnly - Include only items shared directly with user. Exclude publicly shared items
   */
  private Set<String> getSharedAppIDs(ResourceRequestUser rUser, boolean publicOnly, boolean directOnly)
          throws TapisException, TapisClientException
  {
    var appIDs = new HashSet<String>();
    // Extract various names for convenience
    String oboTenantId = rUser.getOboTenantId();
    String oboUserId = rUser.getOboUserId();

    // ------------------- Make a call to retrieve the app sharing -----------------------
    // Create SKShareGetSharesParms needed for SK calls.
    var skParms = new SKShareGetSharesParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(oboTenantId);
    // Set grantee based on whether we want just public or not.
    if (publicOnly) skParms.setGrantee(SKClient.PUBLIC_GRANTEE);
    else skParms.setGrantee(oboUserId);

    // Determine if we should include public or not.
    if (directOnly) skParms.setIncludePublicGrantees(false);
    else skParms.setIncludePublicGrantees(true);

    // Call SK to get all shared with oboUser and add them to the set
    var skShares = getSKClient().getShares(skParms);
    if (skShares != null && skShares.getShares() != null)
    {
      for (SkShare skShare : skShares.getShares())
      {
        appIDs.add(skShare.getResourceId1());
      }
    }
    return appIDs;
  }

  /**
   * Check to see if a user has the specified permission
   * By default use JWT tenant and user from rUser, allow for optional tenant or user.
   */
  private boolean isPermitted(ResourceRequestUser rUser, String tenantToCheck, String userToCheck,
                              String appId, Permission perm)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
    String permSpecStr = getPermSpecStr(tenantName, appId, perm);
    return getSKClient().isPermitted(tenantName, userName, permSpecStr);
  }

  /**
   * Check to see if a user has any of the set of permissions
   * By default use JWT tenant and user from rUser, allow for optional tenant or user.
   */
  private boolean isPermittedAny(ResourceRequestUser rUser, String tenantToCheck, String userToCheck,
                                 String appId, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
    var permSpecs = new ArrayList<String>();
    for (Permission perm : perms) {
      permSpecs.add(getPermSpecStr(tenantName, appId, perm));
    }
    return getSKClient().isPermittedAny(tenantName, userName, permSpecs.toArray(App.EMPTY_STR_ARRAY));
  }

  /**
   * Remove all SK artifacts associated with an App: user permissions, App role
   * No checks are done for incoming arguments and the app must exist
   */
  private void removeSKArtifacts(String resourceTenantId, String appId)
          throws TapisException, TapisClientException
  {
    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = String.format(PERM_SPEC_TEMPLATE, resourceTenantId, "%", appId);
    var userNames = getSKClient().getUsersWithPermission(resourceTenantId, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames)
    {
      revokePermissions(resourceTenantId, appId, userName, ALL_PERMS);
      // Remove wildcard perm
      getSKClient().revokeUserPermission(resourceTenantId, userName, getPermSpecAllStr(resourceTenantId, appId));
    }
  }

  /**
   * Remove all SK permissions associated with given app ID, tenant. App does not need to exist.
   * Used to clean up orphaned permissions.
   */
  private void removeOrphanedSKPerms(String appId, String tenant)
          throws TapisException, TapisClientException
  {
    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = String.format(PERM_SPEC_TEMPLATE, tenant, "%", appId);
    var userNames = getSKClient().getUsersWithPermission(tenant, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames)
    {
      revokePermissions(tenant, appId, userName, ALL_PERMS);
      // Remove wildcard perm
      getSKClient().revokeUserPermission(tenant, userName, getPermSpecAllStr(tenant, appId));
    }
  }

  /**
   * Revoke permissions
   * No checks are done for incoming arguments and the app must exist
   */
  private int revokePermissions(String resourceTenantId, String appId, String userName, Set<Permission> permissions)
          throws TapisClientException, TapisException
  {
    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(resourceTenantId, appId, permissions);
    // Remove perms from default user role
    for (String permSpec : permSpecSet)
    {
      getSKClient().revokeUserPermission(resourceTenantId, userName, permSpec);
    }
    return permSpecSet.size();
  }

  /**
   * Create an updated App based on the app created from a PUT request.
   * Attributes that cannot be updated and must be filled in from the original system:
   *   tenant, id, owner, enabled
   */
  private App createUpdatedApp(App origApp, App putApp)
  {
    // Rather than exposing otherwise unnecessary setters we use a special constructor.
    App updatedApp = new App(putApp, origApp.getTenant(), origApp.getId(), origApp.getVersion());
    updatedApp.setOwner(origApp.getOwner());
    updatedApp.setEnabled(origApp.isEnabled());
    return updatedApp;
  }

  /**
   * Merge a patch into an existing App
   * Attributes that can be updated:
   *   description, runtime, runtimeVersion, runtimeOptions, containerImage, maxJobs, maxJobsPerUser, strictFileInputs,
   *   jobAttributes, tags, notes.
   */
  private App createPatchedApp(App o, PatchApp p)
  {
    // Start off with the current up
    App app1 = new App(o);
    // Now update fields that are being patched
    if (p.getDescription() != null) app1.setDescription(p.getDescription());
    if (p.getRuntime() != null) app1.setRuntime(p.getRuntime());
    if (p.getRuntimeVersion() != null) app1.setRuntimeVersion(p.getRuntimeVersion());
    if (p.getRuntimeOptions() != null) app1.setRuntimeOptions(p.getRuntimeOptions());
    if (p.getContainerImage() != null) app1.setContainerImage(p.getContainerImage());
    // In PatchApp jobType is set to a special value to indicate it was not part of the patch
    if (p.getJobType() != JobType.UNSET) app1.setJobType(p.getJobType());
    if (p.getMaxJobs() != null) app1.setMaxJobs(p.getMaxJobs());
    if (p.getMaxJobsPerUser() != null) app1.setMaxJobsPerUser(p.getMaxJobsPerUser());
    if (p.isStrictFileInputs() != null) app1.setStrictFileInputs(p.isStrictFileInputs());
    // Start JobAttributes
    if (p.getJobAttributes() != null)
    {
      JobAttributes jobAttrs = p.getJobAttributes();
      if (jobAttrs.getDescription() != null) app1.setJobDescription(jobAttrs.getDescription());
      if (jobAttrs.isDynamicExecSystem() != null) app1.setDynamicExecSystem(jobAttrs.isDynamicExecSystem());
      if (jobAttrs.getExecSystemConstraints() != null) app1.setExecSystemConstraints(jobAttrs.getExecSystemConstraints());
      if (jobAttrs.getExecSystemId() != null) app1.setExecSystemId(jobAttrs.getExecSystemId());
      if (jobAttrs.getExecSystemExecDir() != null) app1.setExecSystemExecDir(jobAttrs.getExecSystemExecDir());
      if (jobAttrs.getExecSystemInputDir() != null) app1.setExecSystemInputDir(jobAttrs.getExecSystemInputDir());
      if (jobAttrs.getExecSystemOutputDir() != null) app1.setExecSystemOutputDir(jobAttrs.getExecSystemOutputDir());
      if (jobAttrs.getExecSystemLogicalQueue() != null) app1.setExecSystemLogicalQueue(jobAttrs.getExecSystemLogicalQueue());
      if (jobAttrs.getArchiveSystemId() != null) app1.setArchiveSystemId(jobAttrs.getArchiveSystemId());
      if (jobAttrs.getArchiveSystemDir() != null) app1.setArchiveSystemDir(jobAttrs.getArchiveSystemDir());
      if (jobAttrs.getArchiveOnAppError() != null) app1.setArchiveOnAppError(jobAttrs.getArchiveOnAppError());
      if (jobAttrs.getIsMpi() != null) app1.setIsMpi(jobAttrs.getIsMpi());
      if (jobAttrs.getMpiCmd() != null) app1.setMpiCmd(jobAttrs.getMpiCmd());
      if (jobAttrs.getCmdPrefix() != null) app1.setCmdPrefix(jobAttrs.getCmdPrefix());
      // If parameterSet is being updated then we need to include it
      if (jobAttrs.getParameterSet() != null)
      {
        ParameterSet pParmSet = jobAttrs.getParameterSet();
        if (pParmSet.getAppArgs() != null) app1.getParameterSet().setAppArgs(pParmSet.getAppArgs());
        if (pParmSet.getContainerArgs() != null) app1.getParameterSet().setContainerArgs(pParmSet.getContainerArgs());
        if (pParmSet.getSchedulerOptions() != null) app1.getParameterSet().setSchedulerOptions(pParmSet.getSchedulerOptions());
        if (pParmSet.getEnvVariables() != null) app1.getParameterSet().setEnvVariables(pParmSet.getEnvVariables());
        // If ArchiveFilter in ParameterSet is being updated then process it
        if (pParmSet.getArchiveFilter() != null)
        {
          ArchiveFilter af = pParmSet.getArchiveFilter();
          if (af.getIncludes() != null) app1.getParameterSet().getArchiveFilter().setIncludes(af.getIncludes());
          if (af.getExcludes() != null) app1.getParameterSet().getArchiveFilter().setExcludes(af.getExcludes());
          if (af.isIncludeLaunchFiles() != null)
            app1.getParameterSet().getArchiveFilter().setIncludeLaunchFiles(af.isIncludeLaunchFiles());
        }
        // If LogConfig in ParameterSet is being updated then process it
        if (pParmSet.getLogConfig() != null)
        {
          LogConfig lc = pParmSet.getLogConfig();
          if (!StringUtils.isBlank(lc.getStdoutFilename())) app1.getParameterSet().getLogConfig().setStdoutFilename(lc.getStdoutFilename());
          if (!StringUtils.isBlank(lc.getStderrFilename())) app1.getParameterSet().getLogConfig().setStderrFilename(lc.getStderrFilename());
        }
      }
      if (jobAttrs.getFileInputs() != null) app1.setFileInputs(jobAttrs.getFileInputs());
      if (jobAttrs.getFileInputArrays() != null) app1.setFileInputArrays(jobAttrs.getFileInputArrays());
      if (jobAttrs.getNodeCount() != null) app1.setNodeCount(jobAttrs.getNodeCount());
      if (jobAttrs.getCoresPerNode() != null) app1.setCoresPerNode(jobAttrs.getCoresPerNode());
      if (jobAttrs.getMemoryMB() != null) app1.setMemoryMB(jobAttrs.getMemoryMB());
      if (jobAttrs.getMaxMinutes() != null) app1.setMaxMinutes(jobAttrs.getMaxMinutes());
      if (jobAttrs.getSubscriptions() != null) app1.setSubscriptions(jobAttrs.getSubscriptions());
      if (jobAttrs.getTags() != null) app1.setJobTags(jobAttrs.getTags());
      // End JobAttributes
    }
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

  /**
   * Check attributes related to execSystemId
   *   - verify that execSystemId exists
   *   - verify that execSystem.canExec == true
   *   - verify that if jobType is BATCH then execSystem.canRunBatch == true
   *   - if app type is BATCH and app specifies a LogicalQueue
   *     - verify that logical queue is defined for the execSystem
   *     - verify that constraints for the queue are not violated.
   */
  private void checkExecSystem(SystemsClient systemsClient, App app, List<String> errMessages)
  {
    if (app == null || StringUtils.isBlank(app.getExecSystemId())) return;
    String execSystemId = app.getExecSystemId();
    String msg;
    TapisSystem execSystem = null;
    // Verify that execSystem exists with canExec == true
    try
    {
      // Get system, requireExecPerm=true
      // authnMethod=null, requireExec=true, select=null, returnCred=false, impersonationId=null, sharedAppCtx=null
      execSystem = systemsClient.getSystem(execSystemId, null, true, null, false, null, null, null);
    }
    catch (TapisClientException e)
    {
      msg = LibUtils.getMsg("APPLIB_EXECSYS_CHECK_ERROR", execSystemId, e.getMessage());
      _log.error(msg, e);
      errMessages.add(msg);
    }

    // Note that if execSystem is null we add the message and return.
    // We do not continue since remaining checks rely on having an execSystem.
    if (execSystem == null)
    {
      msg = LibUtils.getMsg("APPLIB_EXECSYS_NO_SYSTEM", execSystemId);
      errMessages.add(msg);
      return;
    }

    // Verify that execSystem.canExec == true
    if (execSystem.getCanExec() == null || !execSystem.getCanExec())
    {
      msg = LibUtils.getMsg("APPLIB_EXECSYS_NOT_EXEC", execSystemId);
      errMessages.add(msg);
    }

    // If app type is not BATCH then we are done. Remaining checks are for BATCH apps
    if (!JobType.BATCH.equals(app.getJobType())) return;

    // Verify that for a BATCH app then execSystem.canRunBatch == true
    // NOTE: Constraints for Systems requires that this means there is at least one logical queue for the system
    //       so if app does not specify an execQ then should still be OK.
    if (app.getJobType() != null && (execSystem.getCanRunBatch() == null || !execSystem.getCanRunBatch()))
    {
      msg = LibUtils.getMsg("APPLIB_EXECSYS_NOT_BATCH", execSystemId);
      errMessages.add(msg);
    }

    // If app specifies a LogicalQueue make sure the constraints for the queue are not violated.
    //     Max/Min constraints to check: nodeCount, coresPerNode, memoryMB, runMinutes
    // If no execQ then we are done. Remaining checks all related to execQ
    String execQName = app.getExecSystemLogicalQueue();
    if (StringUtils.isBlank(execQName)) return;

    List<LogicalQueue> batchQueues = execSystem.getBatchLogicalQueues();
    // Make sure the queue is defined for the system
    // NOTE: If no queue found then add message and return since remaining checks all involve the execQ constraints
    LogicalQueue execQ = getLogicalQ(batchQueues, execQName);
    if (batchQueues == null || batchQueues.isEmpty() || execQ == null)
    {
      msg = LibUtils.getMsg("APPLIB_EXECQ_NOT_FOUND", execQName, execSystemId);
      errMessages.add(msg);
      return;
    }

    // Check that app execQ settings do not violate the system execQ constraints
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
    int appMemoryMb = app.getMemoryMB();
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

  /**
   * Check attributes related to archiveSystemId
   *   - verify that archiveSystemId exists
   */
  private void checkArchiveSystem(SystemsClient systemsClient, App app, List<String> errMessages)
  {
    if (app == null || StringUtils.isBlank(app.getArchiveSystemId())) return;
    String msg;
    String archiveSystemId = app.getArchiveSystemId();
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

  // ************************************************************************
  // **************************  Auth checking ******************************
  // ************************************************************************

  /*
   * Check for case when owner is not known and no need for impersonationId, targetUser or perms
   */
  private void checkAuthOwnerUnknown(ResourceRequestUser rUser, AppOperation op, String appId)
          throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, appId, nullOwner, nullTargetUser, nullPermSet, nullImpersonationId);
  }

  /*
   * Check for case when owner is known and no need for impersonationId, targetUser or perms
   */
  private void checkAuthOwnerKnown(ResourceRequestUser rUser, AppOperation op, String appId, String owner)
          throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, appId, owner, nullTargetUser, nullPermSet, nullImpersonationId);
  }

  /**
   * Overloaded method for callers that do not support impersonation
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param systemId - name of the system
   * @param owner - app owner
   * @param targetUser - Target user for operation
   * @param perms - List of permissions for the revokePerm case
   */
  private void checkAuth(ResourceRequestUser rUser, AppOperation op, String systemId, String owner,
                         String targetUser, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, systemId, owner, targetUser, perms, nullImpersonationId);
  }

  /**
   * Standard authorization check using all arguments.
   * Check is different for service and user requests.
   *
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and an exception thrown.
   *
   * Auth check:
   *  - always allow read, execute, getPerms for a service calling as itself.
   *  - if svc not calling as itself do the normal checks using oboUserOrImpersonationId.
   *  - Note that if svc request and no special cases apply then final standard user request type check is done.
   *
   * Many callers do not support impersonation, so make impersonationId the final argument and provide an overloaded
   *   method for simplicity.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   * @param owner - app owner
   * @param targetUser - Target user for operation
   * @param perms - List of permissions for the revokePerm case
   * @param impersonationId - for auth check use this user in place of oboUser
   */
  private void checkAuth(ResourceRequestUser rUser, AppOperation op, String appId, String owner,
                         String targetUser, Set<Permission> perms, String impersonationId)
          throws TapisException, TapisClientException
  {
    // Check service and user requests separately to avoid confusing a service name with a username
    if (rUser.isServiceRequest())
    {
      // NOTE: This call will do a final checkAuthOboUser() if no special cases apply.
      checkAuthSvc(rUser, op, appId, owner, targetUser, perms, impersonationId);
    }
    else
    {
      // This is an OboUser check
      checkAuthOboUser(rUser, op, appId, owner, targetUser, perms, impersonationId);
    }
  }

  /**
   * Service authorization check. Special auth exceptions and checks are made for service requests:
   *  - Always allow read, execute, getPerms for a service calling as itself.
   *
   * If no special cases apply then final standard user request type auth check is made.
   *
   * ONLY CALL this method when it is a service request
   *
   * A check should be made for app existence before calling this method.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   */
  private void checkAuthSvc(ResourceRequestUser rUser, AppOperation op, String appId, String owner, String targetUser,
                            Set<Permission> perms, String impersonationId)
          throws TapisException, TapisClientException
  {
    // If ever called and not a svc request then fall back to denied
    if (!rUser.isServiceRequest())
      throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, appId, op.name()));

    // This is a service request. The username will be the service name. E.g. files, jobs, streams, etc
    String svcName = rUser.getJwtUserId();
    String svcTenant = rUser.getJwtTenantId();

    // Always allow read, execute, getPerms for a service calling as itself.
    if ((op == AppOperation.read || op == AppOperation.execute || op == AppOperation.getPerms) &&
            (svcName.equals(rUser.getOboUserId()) && svcTenant.equals(rUser.getOboTenantId()))) return;

    // No more special cases. Do the standard auth check
    // Some services, such as Jobs, count on Apps to check auth for OboUserOrImpersonationId
    checkAuthOboUser(rUser, op, appId, owner, targetUser, perms, impersonationId);
  }

  /**
   * OboUser based authorization check.
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and authorization is denied.
   * Operations:
   *  Create -      must be owner or have admin role
   *  Delete -      must be owner or have admin role
   *  ChangeOwner - must be owner or have admin role
   *  GrantPerm -   must be owner or have admin role
   *  Read -     must be owner or have admin role or have READ or MODIFY permission or have share
   *  getPerms - must be owner or have admin role or have READ or MODIFY permission
   *  Modify - must be owner or have admin role or have MODIFY permission
   *  Execute - must be owner or have admin role or have EXECUTE permission or have share
   *  RevokePerm -  must be owner or have admin role or apiUserId=targetUser and meet certain criteria (allowUserRevokePerm)
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the system
   * @param owner - system owner
   * @param impersonationId - for auth check use this Id in place of oboUser
   * @param targetUser - Target user for operation
   * @param perms - List of permissions for the revokePerm case
   */
  private void checkAuthOboUser(ResourceRequestUser rUser, AppOperation op, String appId, String owner,
                                String targetUser, Set<Permission> perms, String impersonationId)
          throws TapisException, TapisClientException
  {
    String oboTenant = rUser.getOboTenantId();
    String oboOrImpersonatedUser = StringUtils.isBlank(impersonationId) ? rUser.getOboUserId() : impersonationId;

    // Some checks do not require owner
    // Only an admin can hard delete
    if (op == AppOperation.hardDelete)
    {
      if (hasAdminRole(rUser)) return;
    }

    // Remaining checks require owner. If no owner specified and owner cannot be determined then it is an error.
    if (StringUtils.isBlank(owner)) owner = dao.getAppOwner(oboTenant, appId);
    if (StringUtils.isBlank(owner))
    {
      String msg = LibUtils.getMsgAuth("APPLIB_OP_NO_OWNER", rUser, appId, op.name());
      _log.error(msg);
      throw new TapisException(msg);
    }
    switch(op) {
      case create:
      case enable:
      case disable:
      case delete:
      case undelete:
      case changeOwner:
      case grantPerms:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser))
          return;
        break;
      case read:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
                isPermittedAny(rUser, oboTenant, oboOrImpersonatedUser, appId, READMODIFY_PERMS) ||
                isAppSharedWithUser(rUser, appId, oboOrImpersonatedUser, Permission.READ))
          return;
        break;
      case getPerms:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
                isPermittedAny(rUser, oboTenant, oboOrImpersonatedUser, appId, READMODIFY_PERMS))
          return;
        break;
      case modify:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
                isPermitted(rUser, oboTenant, oboOrImpersonatedUser, appId, Permission.MODIFY))
          return;
        break;
      case execute:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
                isPermitted(rUser, oboTenant, oboOrImpersonatedUser, appId, Permission.EXECUTE) ||
                isAppSharedWithUser(rUser, appId, oboOrImpersonatedUser, Permission.EXECUTE))
          return;
        break;
      case revokePerms:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
                (oboOrImpersonatedUser.equals(targetUser) && allowUserRevokePerm(rUser, appId, perms)))
          return;
        break;
    }
    // Not authorized, throw an exception
    throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, appId, op.name()));
  }

  /**
   * Check if an app is shared with a user.
   * SK call hasPrivilege includes check for public sharing.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - system to check
   * @param targetUser - user to check
   * @param privilege - privilege to check
   * @return - Boolean value that indicates if app is shared
   * @throws TapisClientException SKClient error
   * @throws TapisException other error
   */
  private boolean isAppSharedWithUser(ResourceRequestUser rUser, String appId, String targetUser, Permission privilege)
          throws TapisClientException, TapisException
  {
    String oboTenant = rUser.getOboTenantId();
    // Create SKShareGetSharesParms needed for SK calls.
    SKShareHasPrivilegeParms skParms = new SKShareHasPrivilegeParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(oboTenant);
    skParms.setResourceId1(appId);
    skParms.setGrantee(targetUser);
    skParms.setPrivilege(privilege.name());
    return getSKClient().hasPrivilege(skParms);
  }

  /**
   * Confirm that caller is allowed to impersonate a Tapis user.
   * Must be a service request from a service allowed to impersonate
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   */
  private void checkImpersonationAllowed(ResourceRequestUser rUser, AppOperation op, String appId, String impersonationId)
  {
    // If a service request the username will be the service name. E.g. files, jobs, streams, etc
    String svcName = rUser.getJwtUserId();
    if (!rUser.isServiceRequest() || !SVCLIST_IMPERSONATE.contains(svcName))
    {
      throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH_IMPERSONATE", rUser, appId, op.name(), impersonationId));
    }
    // An allowed service is impersonating, log it
    _log.info(LibUtils.getMsgAuth("APPLIB_AUTH_IMPERSONATE", rUser, appId, op.name(), impersonationId));
  }

  /**
   * Confirm that caller is allowed to set resourceTenant
   * Must be a service request from a service in the allowed list.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   */
  private void checkResourceTenantAllowed(ResourceRequestUser rUser, AppOperation op, String appId, String resourceTenant)
  {
    // If a service request the username will be the service name. E.g. files, jobs, streams, etc
    String svcName = rUser.getJwtUserId();
    if (!rUser.isServiceRequest() || !SVCLIST_RESOURCETENANT.contains(svcName))
    {
      throw new ForbiddenException(LibUtils.getMsgAuth("APPLIB_UNAUTH_RESOURCETENANT", rUser, appId, op.name(), resourceTenant));
    }
    // An allowed service is impersonating, log it
    _log.trace(LibUtils.getMsgAuth("APPLIB_AUTH_RESOURCETENANT", rUser, appId, op.name(), resourceTenant));
  }

  /**
   * Check to see if the oboUser has the admin role in the obo tenant
   */
  private boolean hasAdminRole(ResourceRequestUser rUser) throws TapisException, TapisClientException
  {
    return getSKClient().isAdmin(rUser.getOboTenantId(), rUser.getOboUserId());
  }

  /**
   * Check to see if a user who is not owner or admin is authorized to revoke permissions
   * If oboUser is revoking only READ then only need READ, otherwise also need MODIFY
   */
  private boolean allowUserRevokePerm(ResourceRequestUser rUser, String appId, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Perms should never be null. Fall back to deny as best security practice.
    if (perms == null) return false;
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();
    if (perms.contains(Permission.MODIFY)) return isPermitted(rUser, oboTenant, oboUser, appId, Permission.MODIFY);
    if (perms.contains(Permission.READ)) return isPermittedAny(rUser, oboTenant, oboUser, appId, READMODIFY_PERMS);
    return false;
  }

  /*
   * Determine if an app is shared publicly
   */
  private boolean isAppSharedPublic(ResourceRequestUser rUser, String tenant, String appId)
          throws TapisException, TapisClientException
  {
    // Create SKShareGetSharesParms needed for SK calls.
    var skParms = new SKShareGetSharesParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(tenant);
    skParms.setResourceId1(appId);
    skParms.setGrantee(SKClient.PUBLIC_GRANTEE);
    var skShares = getSKClient().getShares(skParms);
    return (skShares != null && skShares.getShares() != null && !skShares.getShares().isEmpty());
  }

  /*
   * Common routine to update share/unshare for a list of users.
   * Can be used to mark a system publicly shared with all users in tenant including "~public" in the set of users.
   * 
   * @param rUser - Resource request user
   * @param shareOpName - Operation type: share/unshare
   * @param appId - App ID
   * @param  appShare - App share object
   * @param isPublic - Indicates if the sharing operation is public
   * @throws TapisClientException - for Tapis client exception
   * @throws TapisException - for Tapis exception
   */
  private void updateUserShares(ResourceRequestUser rUser, String shareOpName, String appId, AppShare appShare, boolean isPublic) 
      throws TapisClientException, TapisException
  {
    AppOperation op = AppOperation.modify;
    // ---------------------------- Check inputs ------------------------------------
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", rUser));
    
    Set<String> userList;
    if (!isPublic) {
      // if is not public update userList must have items
      if (appShare == null || appShare.getUserList() ==null || appShare.getUserList().isEmpty())
          throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_USER_LIST", rUser));
      userList = appShare.getUserList();
    } else {
      userList = publicUserSet; // "~public"
    }

    String oboTenantId = rUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForApp(oboTenantId, appId, true))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, appId));

    // ------------------------- Check authorization -------------------------
    checkAuthOwnerUnknown(rUser, op, appId);
    
    switch (shareOpName)
    {
      case OP_SHARE ->
      {
        // Create request object needed for SK calls.
        var reqShareResource = new ReqShareResource();
        reqShareResource.setResourceType(APPS_SHR_TYPE);
        reqShareResource.setTenant(oboTenantId);
        reqShareResource.setResourceId1(appId);
        reqShareResource.setGrantor(rUser.getOboUserId());

        for (String userName : userList)
        {
          reqShareResource.setGrantee(userName);
          reqShareResource.setPrivilege(Permission.READ.name());
          getSKClient().shareResource(reqShareResource);
          reqShareResource.setPrivilege(Permission.EXECUTE.name());
          getSKClient().shareResource(reqShareResource);
        }
      }
      case OP_UNSHARE ->
      {
        // Create object needed for SK calls.
        SKShareDeleteShareParms deleteShareParms = new SKShareDeleteShareParms();
        deleteShareParms.setResourceType(APPS_SHR_TYPE);
        deleteShareParms.setTenant(oboTenantId);
        deleteShareParms.setResourceId1(appId);
        deleteShareParms.setGrantor(rUser.getOboUserId());

        for (String userName : userList)
        {
          deleteShareParms.setGrantee(userName);
          deleteShareParms.setPrivilege(Permission.READ.name());
          getSKClient().deleteShare(deleteShareParms);
          deleteShareParms.setPrivilege(Permission.EXECUTE.name());
          getSKClient().deleteShare(deleteShareParms);
        }
      }
    }
  }
}
