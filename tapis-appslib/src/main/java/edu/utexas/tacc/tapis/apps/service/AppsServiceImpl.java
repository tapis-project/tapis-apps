package edu.utexas.tacc.tapis.apps.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import edu.utexas.tacc.tapis.search.parser.ASTParser;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.security.client.gen.model.SkRole;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.config.RuntimeParameters;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import static edu.utexas.tacc.tapis.shared.TapisConstants.SERVICE_NAME_APPS;
import static edu.utexas.tacc.tapis.apps.model.App.APIUSERID_VAR;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_VAR;
import static edu.utexas.tacc.tapis.apps.model.App.TENANT_VAR;

/*
 * Service level methods for Apps.
 *   Uses Dao layer and other service library classes to perform all top level service operations.
 * Annotate as an hk2 Service so that default scope for DI is singleton
 */
@Service
public class AppsServiceImpl implements AppsService
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  public static final String APPS_ADMIN_ROLE = "AppsAdmin";
  public static final String APPS_ADMIN_DESCRIPTION = "Administrative role for Apps service";
  public static final String APPS_DEFAULT_ADMIN_TENANT = "admin";

  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(AppsServiceImpl.class);

  private static final String[] ALL_VARS = {APIUSERID_VAR, OWNER_VAR, TENANT_VAR};
  private static final Set<Permission> ALL_PERMS = new HashSet<>(Set.of(Permission.ALL));
  private static final Set<Permission> READMODIFY_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final String PERM_SPEC_PREFIX = "app:";

  private static final String HDR_TAPIS_TOKEN = "X-Tapis-Token";
  private static final String HDR_TAPIS_TENANT = "X-Tapis-Tenant";
  private static final String HDR_TAPIS_USER = "X-Tapis-User";

  // TODO determine if certain services need special permissions
  private static final String FILES_SERVICE = "files";
  private static final String JOBS_SERVICE = "jobs";
  private static final Set<String> SVCLIST_READ = new HashSet<>(Set.of(FILES_SERVICE, JOBS_SERVICE));

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
  private SKClient skClient;

  @Inject
  private ServiceContext serviceContext;

  // We must be running on a specific site and this will never change.
  private static String siteId;

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
   * @return Sequence id of object created
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - app exists OR App in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int createApp(AuthenticatedUser authenticatedUser, App app, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotAuthorizedException
  {
    AppOperation op = AppOperation.create;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (app == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appName = app.getId();
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    // Required app attributes: name, type
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId) || StringUtils.isBlank(appName) ||
        app.getAppType() == null)
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appName));
    }

    // Check if app already exists
    if (dao.checkForAppByName(appTenantName, appName, true))
    {
      throw new IllegalStateException(LibUtils.getMsgAuth("APPLIB_APP_EXISTS", authenticatedUser, appName));
    }

    // Make sure owner, notes and tags are all set
    // Note that this is done before auth so owner can get resolved and used during auth check.
    app.setTenant(appTenantName);
    App.checkAndSetDefaults(app);

    // ----------------- Resolve variables for any attributes that might contain them --------------------
    resolveVariables(app, authenticatedUser.getOboUser());

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, app.getId(), app.getOwner(), null, null);

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(authenticatedUser, app);

    // Construct Json string representing the App about to be created
    App scrubbedApp = new App(app);
    String createJsonStr = TapisGsonUtils.getGson().toJson(scrubbedApp);

    // ----------------- Create all artifacts --------------------
    // Creation of app and role/perms not in single DB transaction. Need to handle failure of role/perms operations
    // Use try/catch to rollback any writes in case of failure.
    int itemId = -1;
    String roleNameR = null;
    String appsPermSpecR = getPermSpecStr(appTenantName, appName, Permission.READ);
    String appsPermSpecALL = getPermSpecStr(appTenantName, appName, Permission.ALL);
    // TODO remove filesPermSpec related code
//    String filesPermSpec = "files:" + appTenantName + ":*:" + appName;

    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient(authenticatedUser);
    try {
      // ------------------- Make Dao call to persist the app -----------------------------------
      itemId = dao.createApp(authenticatedUser, app, createJsonStr, scrubbedText);

      // Add permission roles for the app. This is only used for filtering apps based on who is authz
      //   to READ, so no other roles needed.
      roleNameR = App.ROLE_READ_PREFIX + itemId;
      // TODO/TBD: Currently app owner owns the role. Plan is to have apps service own the role
      //           This will need coordinated changes with SK
      //   might need to munge app tenant into the role name (?)
      // TODO/TBD: Keep the delete? Also, currently it fails due to skauthz failure
      // Delete role, because role may already exist due to failure of rollback
//      _log.error("DELETE roleNameR="+ roleNameR);
//      skClient.deleteRoleByName(appTenantName, "apps", roleNameR);
//      skClient.deleteRoleByName(appTenantName, app.getOwner(), roleNameR);
      skClient.createRole(appTenantName, roleNameR, "Role allowing READ for app " + appName);
      skClient.addRolePermission(appTenantName, roleNameR, appsPermSpecR);

      // ------------------- Add permissions and role assignments -----------------------------
      // Give owner full access to the app
      skClient.grantUserPermission(appTenantName, app.getOwner(), appsPermSpecALL);
      skClient.grantUserRole(appTenantName, app.getOwner(), roleNameR);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      // Log error
      String msg = LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ROLLBACK", authenticatedUser, appName, e0.getMessage());
      _log.error(msg);

      // Rollback
      // Remove app from DB
      if (itemId != -1) try {dao.hardDeleteApp(appTenantName, appName); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "hardDelete", e.getMessage()));}
      // Remove perms
      try { skClient.revokeUserPermission(appTenantName, app.getOwner(), appsPermSpecALL); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "revokePermOwner", e.getMessage()));}
      // TODO remove filesPermSpec related code
//      try { skClient.revokeUserPermission(appTenantName, app.getOwner(), filesPermSpec);  }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "revokePermF1", e.getMessage()));}
      // Remove role assignments and roles
      if (!StringUtils.isBlank(roleNameR)) {
        try { skClient.revokeUserRole(appTenantName, app.getOwner(), roleNameR);  }
        catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "revokeRoleOwner", e.getMessage()));}
        try { skClient.deleteRoleByName(appTenantName, roleNameR);  }
        catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "deleteRole", e.getMessage()));}
      }
      throw e0;
    }
    return itemId;
  }

  /**
   * Update an app object given a PatchApp and the text used to create the PatchApp.
   * Secrets in the text should be masked.
   * Attributes that can be updated:
   *   description, enabled, jobCapabilities, tags, notes.
   * Attributes that cannot be updated:
   *   tenant, name, appType, owner
   * @param authenticatedUser - principal user containing tenant and user info
   * @param patchApp - Pre-populated PatchApp object
   * @param scrubbedText - Text used to create the PatchApp object - secrets should be scrubbed. Saved in update record.
   * @return Sequence id of object updated
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - App not found
   */
  @Override
  public int updateApp(AuthenticatedUser authenticatedUser, PatchApp patchApp, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException
  {
    AppOperation op = AppOperation.modify;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (patchApp == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = patchApp.getTenant();
    String appName = patchApp.getName();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId) || StringUtils.isBlank(appName) || StringUtils.isBlank(scrubbedText))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appName));
    }

    // App must already exist and not be soft deleted
    if (!dao.checkForAppByName(appTenantName, appName, false))
    {
      throw new NotFoundException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", authenticatedUser, appName));
    }

    // Retrieve the app being patched and create fully populated App with changes merged in
    App origApp = dao.getApp(appTenantName, appName);
    App patchedApp = createPatchedApp(origApp, patchApp);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, origApp.getOwner(), null, null);

    // ---------------- Check constraints on App attributes ------------------------
    validateApp(authenticatedUser, patchedApp);

    // Construct Json string representing the PatchApp about to be used to update the app
    String updateJsonStr = TapisGsonUtils.getGson().toJson(patchApp);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the app -----------------------------------
    dao.updateApp(authenticatedUser, patchedApp, patchApp, updateJsonStr, scrubbedText);
    return origApp.getSeqId();
  }

  /**
   * Change owner of an app
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @param newOwnerName - User name of new owner
   * @return Number of items updated
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting App would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - App not found
   */
  @Override
  public int changeAppOwner(AuthenticatedUser authenticatedUser, String appName, String newOwnerName)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    AppOperation op = AppOperation.changeOwner;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName) || StringUtils.isBlank(newOwnerName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = tenantName;
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(tenantName) || StringUtils.isBlank(apiUserId))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_CREATE_ERROR_ARG", authenticatedUser, appName));

    // App must already exist and not be soft deleted
    if (!dao.checkForAppByName(appTenantName, appName, false))
         throw new NotFoundException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", authenticatedUser, appName));

    // Retrieve the app being updated
    App tmpApp = dao.getApp(appTenantName, appName);
    int appId = tmpApp.getSeqId();
    String oldOwnerName = tmpApp.getOwner();

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, tmpApp.getOwner(), null, null);

    // If new owner same as old owner then this is a no-op
    if (newOwnerName.equals(oldOwnerName)) return 0;

    // ----------------- Make all updates --------------------
    // Changes not in single DB transaction. Need to handle failure of role/perms operations
    // Use try/catch to rollback any changes in case of failure.
    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient(authenticatedUser);
    try {
      // ------------------- Make Dao call to update the app owner -----------------------------------
      dao.updateAppOwner(authenticatedUser, appId, newOwnerName);
      // Add permissions for new owner
      String appsPermSpec = getPermSpecStr(appTenantName, appName, Permission.ALL);
      skClient.grantUserPermission(appTenantName, newOwnerName, appsPermSpec);
      // TODO remove addition of files related permSpec
      // Give owner files service related permission for root directory
//      String filesPermSpec = "files:" + appTenantName + ":*:" + appName;
//      skClient.grantUserPermission(appTenantName, newOwnerName, filesPermSpec);
      // Remove permissions from old owner
      skClient.revokeUserPermission(appTenantName, oldOwnerName, appsPermSpec);
      // TODO: Notify files service of the change
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      try { dao.updateAppOwner(authenticatedUser, appId, oldOwnerName); } catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "updateOwner", e.getMessage()));}
      String appsPermSpec = getPermSpecStr(appTenantName, appName, Permission.ALL);
      // TODO remove filesPermSpec related code
//      String filesPermSpec = "files:" + appName + ":*:" + appName;
      try { skClient.revokeUserPermission(appTenantName, newOwnerName, appsPermSpec); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "revokePermNewOwner", e.getMessage()));}
//      try { skClient.revokeUserPermission(appTenantName, newOwnerName, filesPermSpec); }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "revokePermF1", e.getMessage()));}
      try { skClient.grantUserPermission(appTenantName, oldOwnerName, appsPermSpec); }
      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "grantPermOldOwner", e.getMessage()));}
//      try { skClient.grantUserPermission(appTenantName, oldOwnerName, filesPermSpec); }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth("APPLIB_ERROR_ROLLBACK", authenticatedUser, appName, "grantPermF1", e.getMessage()));}
      throw e0;
    }
    return 1;
  }

  /**
   * Soft delete an app record given the app name.
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int softDeleteAppByName(AuthenticatedUser authenticatedUser, String appName) throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.softDelete;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String appTenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist or has been soft deleted then 0 changes
    if (!dao.checkForAppByName(appTenantName, appName, false)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);

    App app = dao.getApp(appTenantName, appName);
    String owner = app.getOwner();

    var skClient = getSKClient(authenticatedUser);
    // Delete the app
    return dao.softDeleteApp(authenticatedUser, app.getSeqId());
  }

  /**
   * Hard delete an app record given the app name.
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  public int hardDeleteAppByName(AuthenticatedUser authenticatedUser, String appName)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    AppOperation op = AppOperation.hardDelete;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();
    String apiUserId = authenticatedUser.getName();
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist then 0 changes
    if (!dao.checkForAppByName(appTenantName, appName, true)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);

    String owner = dao.getAppOwner(appTenantName, appName);
    int appId = dao.getAppSeqId(appTenantName, appName);

    var skClient = getSKClient(authenticatedUser);

    // TODO/TBD: How to make sure all perms for an app are removed?
    // TODO: See if it makes sense to have a SK method to do this in one operation
    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = PERM_SPEC_PREFIX + appTenantName + ":%:" + appName;
    var userNames = skClient.getUsersWithPermission(appTenantName, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames) {
      revokePermissions(skClient, appTenantName, appName, userName, ALL_PERMS);
    }
    // Remove role assignments and roles
    String roleNameR = App.ROLE_READ_PREFIX + appId;
    // Remove role assignments for owner
    skClient.revokeUserRole(appTenantName, owner, roleNameR);
    // Remove role assignments for other users
    userNames = skClient.getUsersWithRole(appTenantName, roleNameR);
    for (String userName : userNames) skClient.revokeUserRole(appTenantName, userName, roleNameR);
    // Remove the roles
    skClient.deleteRoleByName(appTenantName, roleNameR);

    // Delete the app
    return dao.hardDeleteApp(appTenantName, appName);
  }

  /**
   * Initialize the service:
   *   Check for Apps admin role. If not found create it
   */
  public void initService(String svcSiteId) throws TapisException, TapisClientException
  {
    siteId = svcSiteId;
    // Get service admin tenant
    String svcAdminTenant = TenantManager.getInstance().getSiteAdminTenantId(siteId);
    if (StringUtils.isBlank(svcAdminTenant)) svcAdminTenant = APPS_DEFAULT_ADMIN_TENANT;
    // Create user for SK client
    // NOTE: getSKClient() does not require the jwt to be set in AuthenticatedUser but we keep it here as a reminder
    //       that in general this may be the pattern to follow.
    String svcJwt = serviceContext.getAccessJWT(svcAdminTenant, SERVICE_NAME_APPS);
    AuthenticatedUser svcUser =
        new AuthenticatedUser(SERVICE_NAME_APPS, svcAdminTenant, TapisThreadContext.AccountType.service.name(),
                              null, SERVICE_NAME_APPS, svcAdminTenant, null, siteId, svcJwt);
    // Use SK client to check for admin role and create it if necessary
    var skClient = getSKClient(svcUser);
    // Check for admin role, continue if error getting role.
    // TODO: Move msgs to properties file
    // TODO/TBD: Do we still need the special service admin role "AppsAdmin" or should be use the tenant admin role?
    SkRole adminRole = null;
    try
    {
      adminRole = skClient.getRoleByName(svcAdminTenant, APPS_ADMIN_ROLE);
    }
    catch (TapisClientException e)
    {
      String msg = e.getTapisMessage();
      // If we have a special message then log it
      if (!StringUtils.isBlank(msg)) _log.error("Unable to get Admin Role. Caught TapisClientException: " + msg);
      // If there is no message or the message is something other than "role does not exist" then log the exception.
      // There may be a problem with SK but do not throw (i.e. fail) just because we cannot get the role at this point.
      if (msg == null || !msg.startsWith("TAPIS_NOT_FOUND")) _log.error("Unable to get Admin Role. Caught Exception: " + e);
    }
    if (adminRole == null)
    {
      _log.info("Apps administrative role not found. Role name: " + APPS_ADMIN_ROLE);
      skClient.createRole(svcAdminTenant, APPS_ADMIN_ROLE, APPS_ADMIN_DESCRIPTION);
      _log.info("Apps administrative created. Role name: " + APPS_ADMIN_ROLE);
    }
    else
    {
      _log.info("Apps administrative role found. Role name: " + APPS_ADMIN_ROLE);
    }
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
   * checkForAppByName
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - Name of the app
   * @return true if app exists and has not been soft deleted, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public boolean checkForAppByName(AuthenticatedUser authenticatedUser, String appName) throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so cannot do auth check if no app
    if (dao.checkForAppByName(appTenantName, appName, false)) {
      // ------------------------- Check service level authorization -------------------------
      checkAuth(authenticatedUser, op, appName, null, null, null);
      return true;
    }
    return false;
  }

  /**
   * getApp
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - Name of the app
   * @param requireExecPerm - check for EXECUTE permission as well as READ permission
   * @return populated instance of an App or null if not found or user not authorized.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public App getApp(AuthenticatedUser authenticatedUser, String appName, boolean requireExecPerm)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    // Extract various names for convenience
    String apiUserId = authenticatedUser.getName();
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app and oboUser as apiUserId
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
    {
      appTenantName = authenticatedUser.getOboTenantId();
      apiUserId = authenticatedUser.getOboUser();
    }

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForAppByName(appTenantName, appName, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);
    // If flag is set to also require EXECUTE perm then make a special auth call
    if (requireExecPerm)
    {
      checkAuthUser(authenticatedUser, AppOperation.execute, appTenantName, authenticatedUser.getOboUser(),
                    appName, null, null, null);
    }

    App result = dao.getApp(appTenantName, appName);
    return result;
  }

  /**
   * Get all apps for which user has READ permission
   * @param authenticatedUser - principal user containing tenant and user info
   * @param searchList - optional list of conditions used for searching
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getApps(AuthenticatedUser authenticatedUser, List<String> searchList)
          throws TapisException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Determine tenant scope for user
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the user
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
      appTenantName = authenticatedUser.getOboTenantId();

    // Build verified list of search conditions
    var verifiedSearchList = new ArrayList<String>();
    if (searchList != null && !searchList.isEmpty())
    {
      try
      {
        for (String cond : searchList)
        {
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

    // Get list of IDs of apps for which requester has READ permission.
    // This is either all apps (null) or a list of IDs based on roles.
    List<Integer> allowedAppIDs = getAllowedAppIDs(authenticatedUser, appTenantName);

    // Get all allowed apps matching the search conditions
    List<App> apps = dao.getApps(authenticatedUser.getTenantId(), verifiedSearchList, allowedAppIDs);

// This is a simple brute force way to only get allowed apps
//      try {
//        checkAuth(authenticatedUser, op, app.getName(), null, null, null);
//        allowedApps.add(app);
//      }
//      catch (NotAuthorizedException e) { }
    return apps;
  }

  /**
   * Get all apps for which user has READ permission.
   * Use provided string containing a valid SQL where clause for the search.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param sqlSearchStr - string containing a valid SQL where clause
   * @return List of App objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<App> getAppsUsingSqlSearchStr(AuthenticatedUser authenticatedUser, String sqlSearchStr)
          throws TapisException, TapisClientException
  {
    // If search string is empty delegate to getApps()
    if (StringUtils.isBlank(sqlSearchStr)) return getApps(authenticatedUser, null);

    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Determine tenant scope for user
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the user
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
      appTenantName = authenticatedUser.getOboTenantId();

    // Validate and parse the sql string into an abstract syntax tree (AST)
    // TODO/TBD: The activemq parser validates and parses the string into an AST but there does not appear to be a way
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
    // This is either all apps (null) or a list of IDs based on roles.
    List<Integer> allowedAppIDs = getAllowedAppIDs(authenticatedUser, appTenantName);

    // Get all allowed apps matching the search conditions
    List<App> apps = dao.getAppsUsingSearchAST(authenticatedUser.getTenantId(), searchAST, allowedAppIDs);

    return apps;
  }

  /**
   * Get list of app names
   * @param authenticatedUser - principal user containing tenant and user info
   * @return - list of apps
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<String> getAppNames(AuthenticatedUser authenticatedUser) throws TapisException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    // Get all app names
    List<String> appNames = dao.getAppNames(authenticatedUser.getTenantId());
    var allowedNames = new ArrayList<String>();
    // Filter based on user authorization
    for (String name: appNames)
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
   * @param appName - Name of the app
   * @return - Owner or null if app not found or user not authorized
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public String getAppOwner(AuthenticatedUser authenticatedUser, String appName) throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.read;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName)) throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));

    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist then return null
    if (!dao.checkForAppByName(appTenantName, appName, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);

    return dao.getAppOwner(authenticatedUser.getTenantId(), appName);
  }

  // -----------------------------------------------------------------------
  // --------------------------- Permissions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Grant permissions to a user for an app
   * NOTE: This only impacts the default user role
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @param userName - Target user for operation
   * @param permissions - list of permissions to be granted
   * @param updateText - Client provided text used to create the permissions list. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public void grantUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName,
                                   Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.grantPerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName) || StringUtils.isBlank(userName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist or has been soft deleted then throw an exception
    if (!dao.checkForAppByName(appTenantName, appName, false))
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_NOT_FOUND", authenticatedUser, appName));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);

    int appId = dao.getAppSeqId(appTenantName, appName);

    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();

    // Check inputs. If anything null or empty throw an exception
    if (StringUtils.isBlank(tenantName) || permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(appTenantName, appName, permissions);

    // Get the Security Kernel client
    var skClient = getSKClient(authenticatedUser);

    // TODO: Mutliple txns. Need to handle failure
    // TODO: Use try/catch to rollback in case of failure.

    // Assign perms and roles to user.
    try
    {
      // Grant permission roles as appropriate, RoleR
      String roleNameR = App.ROLE_READ_PREFIX + appId;
      for (Permission perm : permissions)
      {
        if (perm.equals(Permission.READ)) skClient.grantUserRole(appTenantName, userName, roleNameR);
        else if (perm.equals(Permission.ALL))
        {
          skClient.grantUserRole(appTenantName, userName, roleNameR);
        }
      }
      // Assign perms to user. SK creates a default role for the user
      for (String permSpec : permSpecSet)
      {
        skClient.grantUserPermission(appTenantName, userName, permSpec);
      }
    }
    // If tapis client exception then log error and convert to TapisException
    catch (TapisClientException tce)
    {
      _log.error(tce.toString());
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", authenticatedUser, appName, op.name()), tce);
    }
    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(authenticatedUser, appId, op, updateJsonStr, updateText);
  }

  /**
   * Revoke permissions from a user for an app
   * NOTE: This only impacts the default user role
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @param userName - Target user for operation
   * @param permissions - list of permissions to be revoked
   * @param updateText - Client provided text used to create the permissions list. Saved in update record.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int revokeUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName,
                                   Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.revokePerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName) || StringUtils.isBlank(userName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // We need owner to check auth and if app not there cannot find owner, so
    // if app does not exist or has been soft deleted then return 0 changes
    if (!dao.checkForAppByName(appTenantName, appName, false)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, null, null);

    // Retrieve the app Id. Used to add an update record.
    int appId = dao.getAppSeqId(appTenantName, appName);

    // Extract various names for convenience
    String tenantName = authenticatedUser.getTenantId();

    // Check inputs. If anything null or empty throw an exception
    if (StringUtils.isBlank(tenantName) || permissions == null || permissions.isEmpty())
    {
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    }

    var skClient = getSKClient(authenticatedUser);
    int changeCount;

    // TODO: Mutliple txns. Need to handle failure
    // TODO: Use try/catch to rollback in case of failure.

    try {
      // Revoke permission roles as appropriate, RoleR
      String roleNameR = App.ROLE_READ_PREFIX + appId;
      for (Permission perm : permissions) {
        if (perm.equals(Permission.READ)) skClient.revokeUserRole(appTenantName, userName, roleNameR);
        else if (perm.equals(Permission.ALL)) {
          skClient.revokeUserRole(appTenantName, userName, roleNameR);
        }
      }
      changeCount = revokePermissions(skClient, appTenantName, appName, userName, permissions);
    }
    catch (TapisClientException tce)
    {
      // If tapis client exception then log error and convert to TapisException
      _log.error(tce.toString());
      throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", authenticatedUser, appName, op.name()), tce);
    }
    // Construct Json string representing the update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(permissions);
    // Create a record of the update
    dao.addUpdateRecord(authenticatedUser, appId, op, updateJsonStr, updateText);
    return changeCount;
  }

  /**
   * Get list of app permissions for a user
   * NOTE: This retrieves permissions from all roles.
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of app
   * @param userName - Target user for operation
   * @return List of permissions
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public Set<Permission> getUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    AppOperation op = AppOperation.getPerms;
    if (authenticatedUser == null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(appName) || StringUtils.isBlank(userName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("APPLIB_NULL_INPUT_APP", authenticatedUser));
    String appTenantName = authenticatedUser.getTenantId();
    // For service request use oboTenant for tenant associated with the app
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) appTenantName = authenticatedUser.getOboTenantId();

    // If app does not exist or has been soft deleted then return null
    if (!dao.checkForAppByName(appTenantName, appName, false)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(authenticatedUser, op, appName, null, userName, null);

    // Use Security Kernel client to check for each permission in the enum list
    var userPerms = new HashSet<Permission>();
    var skClient = getSKClient(authenticatedUser);
    for (Permission perm : Permission.values())
    {
      String permSpec = PERM_SPEC_PREFIX + appTenantName + ":" + perm.name() + ":" + appName;
      try
      {
        Boolean isAuthorized = skClient.isPermitted(appTenantName, userName, permSpec);
        if (Boolean.TRUE.equals(isAuthorized)) userPerms.add(perm);
      }
      // If tapis client exception then log error and convert to TapisException
      catch (TapisClientException tce)
      {
        _log.error(tce.toString());
        throw new TapisException(LibUtils.getMsgAuth("APPLIB_PERM_SK_ERROR", authenticatedUser, appName, op.name()), tce);
      }
    }
    return userPerms;
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   *  TODO: revisit this. There is now ServiceContext which will probably help.
   * Get Security Kernel client associated with specified tenant
   * @param authenticatedUser - name of tenant
   * @return SK client
   * @throws TapisException - for Tapis related exceptions
   */
  private SKClient getSKClient(AuthenticatedUser authenticatedUser) throws TapisException
  {
    // Use TenantManager to get tenant info. Needed for tokens and SK base URLs.
    Tenant userTenant = TenantManager.getInstance().getTenant(authenticatedUser.getTenantId());

    // Update SKClient on the fly. If this becomes a bottleneck we can add a cache.
    // Get Security Kernel URL from the env or the tenants service. Env value has precedence.
    //    String skURL = "https://dev.develop.tapis.io/v3";
    String skURL = RuntimeParameters.getInstance().getSkSvcURL();
    if (StringUtils.isBlank(skURL)) skURL = userTenant.getSecurityKernel();
    if (StringUtils.isBlank(skURL)) throw new TapisException(LibUtils.getMsgAuth("APPLIB_CREATE_SK_URL_ERROR", authenticatedUser));
    // TODO remove strip-off of everything after /v3 once tenant is updated or we do something different for base URL in auto-generated clients
    // Strip off everything after the /v3 so we have a valid SK base URL
    skURL = skURL.substring(0, skURL.indexOf("/v3") + 3);

    skClient.setBasePath(skURL);
    skClient.addDefaultHeader(HDR_TAPIS_TOKEN, serviceContext.getServiceJWT().getAccessJWT(siteId));

    // For service jwt pass along oboTenant and oboUser in OBO headers
    // For user jwt use authenticated user name and tenant in OBO headers
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()))
    {
      skClient.addDefaultHeader(HDR_TAPIS_TENANT, authenticatedUser.getOboTenantId());
      skClient.addDefaultHeader(HDR_TAPIS_USER, authenticatedUser.getOboUser());
    }
    else
    {
      skClient.addDefaultHeader(HDR_TAPIS_TENANT, authenticatedUser.getTenantId());
      skClient.addDefaultHeader(HDR_TAPIS_USER, authenticatedUser.getName());
    }
    return skClient;
  }

  /**
   * Resolve variables for App attributes
   * @param app - the App to process
   */
  private static App resolveVariables(App app, String oboUser)
  {
    // Resolve owner if necessary. If empty or "${apiUserId}" then fill in oboUser.
    // Note that for a user request oboUser and apiUserId are the same and for a service request we want oboUser here.
    String owner = app.getOwner();
    if (StringUtils.isBlank(owner) || owner.equalsIgnoreCase(APIUSERID_VAR)) owner = oboUser;
    app.setOwner(owner);

//    // Perform variable substitutions that happen at create time: bucketName, rootDir, jobLocalWorkingDir, jobLocalArchiveDir
//    // NOTE: effectiveUserId is not processed. Var reference is retained and substitution done as needed when system is retrieved.
//    //    ALL_VARS = {APIUSERID_VAR, OWNER_VAR, TENANT_VAR};
//    String[] allVarSubstitutions = {oboUser, owner, system.getTenant()};
//    system.setBucketName(StringUtils.replaceEach(system.getBucketName(), ALL_VARS, allVarSubstitutions));
//    system.setRootDir(StringUtils.replaceEach(system.getRootDir(), ALL_VARS, allVarSubstitutions));
//    system.setJobLocalWorkingDir(StringUtils.replaceEach(system.getJobLocalWorkingDir(), ALL_VARS, allVarSubstitutions));
//    system.setJobLocalArchiveDir(StringUtils.replaceEach(system.getJobLocalArchiveDir(), ALL_VARS, allVarSubstitutions));
//    system.setJobRemoteArchiveDir(StringUtils.replaceEach(system.getJobRemoteArchiveDir(), ALL_VARS, allVarSubstitutions));
    return app;
  }

  /**
   * Check constraints on App attributes.
   * Notes must be json
   * @param app - the App to check
   * @throws IllegalStateException - if any constraints are violated
   */
  private static void validateApp(AuthenticatedUser authenticatedUser, App app) throws IllegalStateException
  {
    String msg;
    var errMessages = new ArrayList<String>();
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
   * Create a set of individual permSpec entries based on the list passed in
   * @param permList - list of individual permissions
   * @return - Set of permSpec entries based on permissions
   */
  private static Set<String> getPermSpecSet(String tenantName, String appName, Set<Permission> permList)
  {
    var permSet = new HashSet<String>();
    if (permList.contains(Permission.ALL)) permSet.add(getPermSpecStr(tenantName, appName, Permission.ALL));
    else {
      for (Permission perm : permList) {
        permSet.add(getPermSpecStr(tenantName, appName, perm));
      }
    }
    return permSet;
  }

  /**
   * Create a permSpec given a permission
   * @param perm - permission
   * @return - permSpec entry based on permission
   */
  private static String getPermSpecStr(String tenantName, String appName, Permission perm)
  {
    if (perm.equals(Permission.ALL)) return PERM_SPEC_PREFIX + tenantName + ":*:" + appName;
    else return PERM_SPEC_PREFIX + tenantName + ":" + perm.name().toUpperCase() + ":" + appName;
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(AuthenticatedUser authenticatedUser, String appName, List<String> msgList) {
    var sb = new StringBuilder(LibUtils.getMsgAuth("APPLIB_CREATE_INVALID_ERRORLIST", authenticatedUser, appName));
    sb.append(System.lineSeparator());
    if (msgList == null || msgList.isEmpty()) return sb.toString();
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   * Standard service level authorization check. Check is different for service and user requests.
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and authorization is denied.
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param operation - operation name
   * @param appName - name of the app
   * @param owner - app owner
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuth(AuthenticatedUser authenticatedUser, AppOperation operation, String appName,
                         String owner, String targetUser, Set<Permission> perms)
      throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException
  {
    // Check service and user requests separately to avoid confusing a service name with a user name
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType())) {
      // This is a service request. The user name will be the service name. E.g. files, jobs, streams, etc
      switch (operation) {
        case read:
          if (SVCLIST_READ.contains(authenticatedUser.getName())) return;
          break;
      }
    }
    else
    {
      // User check
      checkAuthUser(authenticatedUser, operation, null, null, appName, owner, targetUser, perms);
      return;
    }
    // Not authorized, throw an exception
    String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, appName, operation.name());
    throw new NotAuthorizedException(msg);
  }

  /**
   * User based authorization check.
   * Can be used for OBOUser type checks.
   * By default use tenant and user from authenticatedUser, allow for optional tenant or user.
   * A check should be made for app existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and
   *   authorization is denied.
   * Operations:
   *  Create - must be owner or have admin role
   *  Read - must be owner or have admin role or have READ or MODIFY permission or be in list of allowed services
   *  Delete - must be owner or have admin role
   *  Modify - must be owner or have admin role or have MODIFY permission
   *  Execute - must be owner or have admin role or have EXECUTE permission
   *  ChangeOwner - must be owner or have admin role
   *  GrantPerm -  must be owner or have admin role
   *  RevokePerm -  must be owner or have admin role or apiUserId=targetUser and meet certain criteria (allowUserRevokePerm)
   *
   * @param authenticatedUser - principal user containing tenant and user info
   * @param operation - operation name
   * @param tenantToCheck - optional name of the tenant to use. Default is to use authenticatedUser.
   * @param userToCheck - optional name of the user to check. Default is to use authenticatedUser.
   * @param appName - name of the system
   * @param owner - system owner
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuthUser(AuthenticatedUser authenticatedUser, AppOperation operation,
                             String tenantToCheck, String userToCheck,
                             String appName, String owner, String targetUser, Set<Permission> perms)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException
  {
    // Use tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    // Requires owner. If no owner specified and owner cannot be determined then log an error and deny.
    if (StringUtils.isBlank(owner)) owner = dao.getAppOwner(tenantName, appName);
    if (StringUtils.isBlank(owner)) {
      String msg = LibUtils.getMsgAuth("APPLIB_AUTH_NO_OWNER", authenticatedUser, appName, operation.name());
      _log.error(msg);
      throw new NotAuthorizedException(msg);
    }
    switch(operation) {
      case create:
      case softDelete:
      case changeOwner:
      case grantPerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName)) return;
        break;
      case hardDelete:
        if (hasAdminRole(authenticatedUser, tenantName, userName)) return;
        break;
      case read:
      case getPerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
              isPermittedAny(authenticatedUser, tenantName, userName, appName, READMODIFY_PERMS)) return;
        break;
      case modify:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                isPermitted(authenticatedUser, tenantName, userName, appName, Permission.MODIFY)) return;
        break;
      case execute:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                isPermitted(authenticatedUser, tenantName, userName, appName, Permission.EXECUTE)) return;
        break;
      case revokePerms:
        if (owner.equals(userName) || hasAdminRole(authenticatedUser, tenantName, userName) ||
                (userName.equals(targetUser) &&
                        allowUserRevokePerm(authenticatedUser, tenantName, userName, appName, perms))) return;
        break;
    }
    // Not authorized, throw an exception
    String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH", authenticatedUser, appName, operation.name());
    throw new NotAuthorizedException(msg);
  }

  /**
   * Determine all apps that a user is allowed to see.
   * If all apps return null else return list of IDs
   * An empty list indicates no apps allowed.
   */
  private List<Integer> getAllowedAppIDs(AuthenticatedUser authenticatedUser, String appTenantName)
          throws TapisException, TapisClientException
  {
    // If requester is a service or an admin then all apps allowed
    // TODO: for all services or just some, such as files and jobs?
    if (TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType()) ||
        hasAdminRole(authenticatedUser, null, null)) return null;
    var appIDs = new ArrayList<Integer>();
    // Get roles for user and extract app IDs
    // TODO: Need a way to make sure roles that a user has created and assigned to themselves are not included
    //       Maybe a special role name? Or a search that only returns roles owned by "apps"
    // TODO: Is it possible for a user to already have roles in this format that are assigned to them but not owned by "apps"?
    //       If yes then it is a problem.
    List<String> userRoles = getSKClient(authenticatedUser).getUserRoles(appTenantName, authenticatedUser.getName());
    // Find roles of the form Apps_R_<id> and generate a list of IDs
    // TODO Create a function and turn this into a stream/lambda
    for (String role: userRoles)
    {
      if (role.startsWith(App.ROLE_READ_PREFIX))
      {
        String idStr = role.substring(role.indexOf(App.ROLE_READ_PREFIX) + App.ROLE_READ_PREFIX.length());
        // If id part of string is not integer then ignore this role.
        try {
          Integer id = Integer.parseInt(idStr);
          appIDs.add(id);
        } catch (NumberFormatException e) {};
      }
    }
    return appIDs;
  }

  /**
   * Check to see if a user has the service admin role
   * By default use tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean hasAdminRole(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck) throws TapisException
  {
    // TODO NOTE that tenantName is not yet used but will be once we make the SK call.
    // Use tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    // TODO Temporarily just require that user has SystemsAdmin in the name.
    // TODO: Use sk isAdmin method ot require that user have the tenant admin role
//    var skClient = getSKClient(authenticatedUser);
//    return skClient.hasRole(tenantName, userName, APPS_ADMIN_ROLE);
    if (userName.contains("AppsAdmin") ||
        userName.contains("admin") ||
        userName.equalsIgnoreCase("testuser9")) return true;
    else return false;
  }

  /**
   * Check to see if a user has the specified permission
   * By default use tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean isPermitted(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                              String appName, Permission perm)
          throws TapisException, TapisClientException
  {
    // Use tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    var skClient = getSKClient(authenticatedUser);
    String permSpecStr = getPermSpecStr(tenantName, appName, perm);
    return skClient.isPermitted(tenantName, userName, permSpecStr);
  }

  /**
   * Check to see if a user has any of the set of permissions
   * By default use tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean isPermittedAny(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                                 String appName, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Use tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    var skClient = getSKClient(authenticatedUser);
    var permSpecs = new ArrayList<String>();
    for (Permission perm : perms) {
      permSpecs.add(getPermSpecStr(tenantName, appName, perm));
    }
    return skClient.isPermittedAny(tenantName, userName, permSpecs.toArray(new String[0]));
  }

  /**
   * Check to see if a user who is not owner or admin is authorized to revoke permissions
   * By default use tenant and user from authenticatedUser, allow for optional tenant or user.
   */
  private boolean allowUserRevokePerm(AuthenticatedUser authenticatedUser, String tenantToCheck, String userToCheck,
                                      String appName, Set<Permission> perms)
          throws TapisException, TapisClientException
  {
    // Use tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? authenticatedUser.getTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? authenticatedUser.getName() : userToCheck);
    if (perms.contains(Permission.MODIFY)) return isPermitted(authenticatedUser, tenantName, userName, appName, Permission.MODIFY);
    if (perms.contains(Permission.READ)) return isPermittedAny(authenticatedUser, tenantName, userName, appName, READMODIFY_PERMS);
    // TODO what if perms contains ALL?
    return false;
  }

  /**
   * Revoke permissions
   * No checks are done for incoming arguments and the app must exist
   */
  private static int revokePermissions(SKClient skClient, String appTenantName, String appName, String userName, Set<Permission> permissions)
          throws TapisClientException
  {
    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(appTenantName, appName, permissions);
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
   *   description, enabled, tags, notes.
   */
  private App createPatchedApp(App o, PatchApp p)
  {
    App p1 = new App(o);
    if (p.getVersion() != null) p1.setVersion(p.getVersion());
    if (p.getDescription() != null) p1.setDescription(p.getDescription());
    if (p.isEnabled() != null) p1.setEnabled(p.isEnabled());
//    if (p.getJobCapabilities() != null) p1.setJobCapabilities(p.getJobCapabilities());
    if (p.getTags() != null) p1.setTags(p.getTags());
    if (p.getNotes() != null) p1.setNotes(p.getNotes());
    return p1;
  }
}
