package edu.utexas.tacc.tapis.apps.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.security.client.gen.model.ReqShareResource;
import edu.utexas.tacc.tapis.security.client.gen.model.SkShare;
import edu.utexas.tacc.tapis.security.client.gen.model.SkShareList;
import edu.utexas.tacc.tapis.security.client.model.SKShareDeleteShareParms;
import edu.utexas.tacc.tapis.security.client.model.SKShareGetSharesParms;
import edu.utexas.tacc.tapis.security.client.model.SKShareHasPrivilegeParms;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.model.AppShare;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;


import static edu.utexas.tacc.tapis.apps.model.App.*;
import static edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.JOBS_SERVICE;

/*
   Utility class containing Tapis authentication (permissions and sharing)
   related methods needed by the service implementation.
 */
public class AuthUtils
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger log = LoggerFactory.getLogger(AuthUtils.class);

  // Permission constants
  // Permspec format for apps is "apps:<tenant>:<perm_list>:<app_id>"
  public static final String PERM_SPEC_TEMPLATE = "app:%s:%s:%s";
  public static final String PERM_SPEC_PREFIX = "app";
  // Sets of individual permissions, for convenience
  static final Set<Permission> ALL_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY, Permission.EXECUTE));
  private static final Set<Permission> READMODIFY_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));

  // Sharing constants
  static final String OP_SHARE = "share";
  static final String OP_UNSHARE = "unShare";
  static final Set<String> PUBLIC_USER_SET = Collections.singleton(SKClient.PUBLIC_GRANTEE); // "~public"
  static final String APPS_SHR_TYPE = "apps";

  // Lists of services allowed to perform certain restricted functionality:
  //     impersonate user, set shared context, impersonate tenant
  private static final Set<String> SVCLIST_IMPERSONATE = new HashSet<>(Set.of(JOBS_SERVICE));
  private static final Set<String> SVCLIST_RESOURCETENANT = new HashSet<>(Set.of(JOBS_SERVICE));


  // Named and typed null values to make it clear what is being passed in to a method
  private static final String nullOwner = null;
  private static final String nullImpersonationId = null;
  private static final String nullTargetUser = null;
  private static final Set<Permission> nullPermSet = null;

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
  @Inject
  private AppsDao dao;
  @Inject
  private ServiceClients serviceClients;
  @Inject
  private AppUtils appUtils;

  /* **************************************************************************** */
  /*                                Public Methods                                */
  /* **************************************************************************** */

  /* **************************************************************************** */
  /*                                Package-Private Methods                       */
  /* **************************************************************************** */

  /*
   * Check for case when owner is not known and no need for impersonationId, targetUser or perms
   */
  void checkAuthOwnerUnknown(ResourceRequestUser rUser, AppOperation op, String appId)
        throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, appId, nullOwner, nullTargetUser, nullPermSet, nullImpersonationId);
  }

  /*
   * Check for case when owner is known and no need for impersonationId, targetUser or perms
   */
  void checkAuthOwnerKnown(ResourceRequestUser rUser, AppOperation op, String appId, String owner)
        throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, appId, owner, nullTargetUser, nullPermSet, nullImpersonationId);
  }

  /**
   * Overloaded method for callers that do not support impersonation
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   * @param owner - app owner
   * @param targetUser - Target user for operation
   * @param perms - List of permissions for the revokePerm case
   */
  void checkAuth(ResourceRequestUser rUser, AppOperation op, String appId, String owner, String targetUser,
                 Set<Permission> perms)
        throws TapisException, TapisClientException
  {
    checkAuth(rUser, op, appId, owner, targetUser, perms, nullImpersonationId);
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
  void checkAuth(ResourceRequestUser rUser, AppOperation op, String appId, String owner,
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
   * Determine apps that are shared with a user.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param oboUser - Since tenant admin can impersonate, this might be different from rUser.getOboUser()
   * @param publicOnly - Include only items shared public
   * @param directOnly - Include only items shared directly with user. Exclude publicly shared items
   */
  Set<String> getSharedAppIDs(ResourceRequestUser rUser, String oboUser, boolean publicOnly, boolean directOnly)
        throws TapisException, TapisClientException
  {
    var appIDs = new HashSet<String>();
    // Extract various names for convenience
    String oboTenantId = rUser.getOboTenantId();

    // ------------------- Make a call to retrieve share info -----------------------
    // Create SKShareGetSharesParms needed for SK calls.
    var skParms = new SKShareGetSharesParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(oboTenantId);
    // Set grantee based on whether we want just public or not.
    if (publicOnly) skParms.setGrantee(SKClient.PUBLIC_GRANTEE);
    else skParms.setGrantee(oboUser);

    // Determine if we should include public or not.
    if (directOnly) skParms.setIncludePublicGrantees(false);
    else skParms.setIncludePublicGrantees(true);

    // Call SK to get all shared with oboUser and add them to the set
    var skShares = appUtils.getSKClient(rUser).getShares(skParms);
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
   * Check if an app is shared directly with a user.
   * SK call hasPrivilege includes check for public sharing.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param appId - app to check
   * @param targetUser - user to check
   * @param privilege - privilege to check
   * @return - Boolean value that indicates if app is shared
   * @throws TapisClientException SKClient error
   * @throws TapisException other error
   */
  boolean isAppSharedWithUser(ResourceRequestUser rUser, String appId, String targetUser, Permission privilege)
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
    return appUtils.getSKClient(rUser).hasPrivilege(skParms);
  }

  /**
   * Revoke permissions
   * No checks are done for incoming arguments and the app must exist
   */
  int revokePermissions(ResourceRequestUser rUser, String resourceTenantId, String appId, String userName, Set<Permission> permissions)
        throws TapisClientException, TapisException
  {
    // Create a set of individual permSpec entries based on the list passed in
    Set<String> permSpecSet = getPermSpecSet(resourceTenantId, appId, permissions);
    // Remove perms from default user role
    for (String permSpec : permSpecSet)
    {
      appUtils.getSKClient(rUser).revokeUserPermission(resourceTenantId, userName, permSpec);
    }
    return permSpecSet.size();
  }

  /**
   * Confirm that caller is allowed to impersonate a Tapis user.
   * Must be a service request from a service allowed to impersonate
   * impersonationId and resourceTenant used for logging only.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   */
  void checkImpersonateUserAllowed(ResourceRequestUser rUser, AppOperation op, String appId,
                                   String impersonationId, String resourceTenant)
        throws TapisException, TapisClientException
  {
    // If a user request and user is a tenant admin then log message and allow.
    if (!rUser.isServiceRequest() && hasAdminRole(rUser))
    {
      // A tenant admin is impersonating, log message and allow
      log.info(LibUtils.getMsgAuth("APPLIB_AUTH_USR_IMPERSONATE", rUser, appId, op.name(), impersonationId, resourceTenant));
      return;
    }
    // If a service request the username will be the service name. E.g. files, jobs, streams, etc
    String svcName = rUser.getJwtUserId();
    // If a service request and service is in the allowed list then log message and allow.
    if (rUser.isServiceRequest() && SVCLIST_IMPERSONATE.contains(svcName))
    {
      log.info(LibUtils.getMsgAuth("APPLIB_AUTH_SVC_IMPERSONATE", rUser, appId, op.name(), impersonationId, resourceTenant));
      return;
    }
    // Deny authorization
    String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH_IMPERSONATE", rUser, appId, op.name(), impersonationId, resourceTenant);
    log.warn(msg);
    throw new ForbiddenException(msg);
  }

  /**
   * Confirm that caller is allowed to set resourceTenant
   * Must be a service request from a service in the allowed list.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param appId - name of the app
   */
  void checkResourceTenantAllowed(ResourceRequestUser rUser, AppOperation op, String appId, String resourceTenant)
  {
    // If a service request the username will be the service name. E.g. files, jobs, streams, etc
    String svcName = rUser.getJwtUserId();
    // If a service request and service is in the allowed list then log message and allow.
    if (rUser.isServiceRequest() && SVCLIST_RESOURCETENANT.contains(svcName))
    {
      log.info(LibUtils.getMsgAuth("APPLIB_AUTH_RESOURCETENANT", rUser, appId, op.name(), resourceTenant));
      return;
    }
    // Deny authorization
    String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH_RESOURCETENANT", rUser, appId, op.name(), resourceTenant);
    log.warn(msg);
    throw new ForbiddenException(msg);
  }

  /**
   * Check to see if the oboUser has the admin role in the obo tenant
   */
  private boolean hasAdminRole(ResourceRequestUser rUser) throws TapisException, TapisClientException
  {
    return appUtils.getSKClient(rUser).isAdmin(rUser.getOboTenantId(), rUser.getOboUserId());
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
    var skShares = appUtils.getSKClient(rUser).getShares(skParms);
    return (skShares != null && skShares.getShares() != null && !skShares.getShares().isEmpty());
  }




// ??????????????????????????????????????????????????????????????????????????????????????????
// ??????????????????????????????????????????????????????????????????????????????????????????
// ??????????????????????????????????????????????????????????????????????????????????????????
  /*
   * Update all share info associated with an app to have a new grantor for the share records.
   * No checks are done for incoming arguments and the app must exist
   */
  void updateShareGrantorToNewOwner(ResourceRequestUser rUser, App app, String newOwner)
        throws TapisException, TapisClientException
  {
    String appId = app.getId();
    String appTenant = app.getTenant();
    AppShare appShare = getAppShareInfo(rUser, appTenant, appId);
    // If nothing to do then return
    if (appShare == null || appShare.getUserList() == null || appShare.getUserList().isEmpty()) return;
    // Save the current list of users. We need it when adding share records back in.
    Set<String> userList = appShare.getUserList();

    // First remove all existing skShare records
    // Create object needed for SK calls.
    SKShareDeleteShareParms deleteShareParms = new SKShareDeleteShareParms();
    deleteShareParms.setResourceType(APPS_SHR_TYPE);
    deleteShareParms.setTenant(app.getTenant());
    deleteShareParms.setResourceId1(appId);
    for (SkShare skShare : appShare.getSkShares())
    {
      deleteShareParms.setGrantor(skShare.getGrantor());
      deleteShareParms.setGrantee(skShare.getGrantee());
      deleteShareParms.setPrivilege(Permission.READ.name());
      appUtils.getSKClient(rUser).deleteShare(deleteShareParms);
      deleteShareParms.setPrivilege(Permission.EXECUTE.name());
      appUtils.getSKClient(rUser).deleteShare(deleteShareParms);
    }

    // Re-create shares using newOwner as grantor
    // Create request object needed for SK calls.
    var reqShareResource = new ReqShareResource();
    reqShareResource.setResourceType(APPS_SHR_TYPE);
    reqShareResource.setTenant(appTenant);
    reqShareResource.setResourceId1(appId);
    reqShareResource.setGrantor(newOwner);
    for (String userName : userList)
    {
      reqShareResource.setGrantee(userName);
      reqShareResource.setPrivilege(Permission.READ.name());
      appUtils.getSKClient(rUser).shareResource(reqShareResource);
      reqShareResource.setPrivilege(Permission.EXECUTE.name());
      appUtils.getSKClient(rUser).shareResource(reqShareResource);
    }
  }

  /*
   * Remove all share info associated with an app.
   * No checks are done for incoming arguments and the app must exist
   */
  void deleteAllShareInfo(ResourceRequestUser rUser, App app) // Wrapper for backward compatibility
        throws TapisException, TapisClientException
  {
    deleteAllShareInfo(rUser, app, true);
  }
  void deleteAllShareInfo(ResourceRequestUser rUser, App app, boolean unsharePublic)
        throws TapisException, TapisClientException
  {
    String appId = app.getId();
    if (unsharePublic) updateUserShares(rUser, OP_UNSHARE, appId, null, true);
    AppShare appShare = getAppShareInfo(rUser, app.getTenant(), appId);
    // If any shareInfo to remove do so now.
    if (appShare != null && appShare.getUserList() != null && !appShare.getUserList().isEmpty())
    {
      updateUserShares(rUser, OP_UNSHARE, appId, appShare, false);
    }
  }

  /**
   * Create a permSpec for all permissions
   * @return - permSpec entry for all permissions
   */
  static String getPermSpecAllStr(String tenantName, String appId)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, "*", appId);
  }

  /**
   * Create a permSpec given a permission
   * @param perm - permission
   * @return - permSpec entry based on permission
   */
  static String getPermSpecStr(String tenantName, String appId, Permission perm)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), appId);
  }

  /**
   * Remove all SK permissions associated with given app ID, tenant. App does not need to exist.
   * Used to clean up orphaned permissions.
   */
  void removeOrphanedSKPerms(ResourceRequestUser rUser, String appId, String tenant)
        throws TapisException, TapisClientException
  {
    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = String.format(PERM_SPEC_TEMPLATE, tenant, "%", appId);
    var userNames = appUtils.getSKClient(rUser).getUsersWithPermission(tenant, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames)
    {
      revokePermissions(rUser, tenant, appId, userName, ALL_PERMS);
      // Remove wildcard perm
      appUtils.getSKClient(rUser).revokeUserPermission(tenant, userName, getPermSpecAllStr(tenant, appId));
    }
  }

  /**
   * Remove all SK artifacts associated with an App: user permissions, App role
   * No checks are done for incoming arguments and the app must exist
   */
  void revokeAllSKPermissions(ResourceRequestUser rUser, String resourceTenantId, String appId)
        throws TapisException, TapisClientException
  {
    // Use Security Kernel client to find all users with perms associated with the app.
    String permSpec = String.format(PERM_SPEC_TEMPLATE, resourceTenantId, "%", appId);
    var userNames = appUtils.getSKClient(rUser).getUsersWithPermission(resourceTenantId, permSpec);
    // Revoke all perms for all users
    for (String userName : userNames)
    {
      revokePermissions(rUser, resourceTenantId, appId, userName, ALL_PERMS);
      // Remove wildcard perm
      appUtils.getSKClient(rUser).revokeUserPermission(resourceTenantId, userName, getPermSpecAllStr(resourceTenantId, appId));
    }
  }

  /*
   * Get app share info
   */
  AppShare getAppShareInfo(ResourceRequestUser rUser, String tenant, String appId)
        throws TapisException, TapisClientException
  {
    // Attributes needed to create an AppShare
    boolean isPublic = false;
    var userSet = new HashSet<String>();
    Set<String> publicGrantors = Collections.emptySet();
    SkShareList skShares;

    // Create SKShareGetSharesParms needed for SK calls.
    var skParms = new SKShareGetSharesParms();
    skParms.setResourceType(APPS_SHR_TYPE);
    skParms.setTenant(tenant);
    skParms.setResourceId1(appId);

    // First determine if app is publicly shared. Search for shares to grantee ~public
    skParms.setGrantee(SKClient.PUBLIC_GRANTEE);
    skShares = appUtils.getSKClient(rUser).getShares(skParms);
    // Set isPublic and publicGrantors based on result.
    if (skShares != null && skShares.getShares() != null && !skShares.getShares().isEmpty())
    {
      isPublic = true;
      publicGrantors = new HashSet<>();
      for (SkShare skShare : skShares.getShares()) { publicGrantors.add(skShare.getGrantor()); }
    }

    // Now get all the users with whom the app has been shared and all individual skShare records.
    // We use a Set for usernames because for some purposes we only care about which users have a share record.
    // We also include a List of share records because we could have multiple grantors per user for a share.
    // Plus, we will need the list when we got to remove the share records. We want to make sure we remove all
    //   records regardless of who created the share record.
    skParms.setGrantee(null);
    skParms.setIncludePublicGrantees(false);
    skShares = appUtils.getSKClient(rUser).getShares(skParms);
    List<SkShare> skShareList = new ArrayList<>();
    if (skShares != null && skShares.getShares() != null)
    {
      for (SkShare skShare : skShares.getShares())
      {
        skShareList.add(skShare);
        userSet.add(skShare.getGrantee());
      }
    }
    return new AppShare(isPublic, userSet, skShareList, publicGrantors);
  }

  /*
   * Common routine to update share/unshare for a list of users.
   * Can be used to mark an app publicly shared with all users in tenant including "~public" in the set of users.
   *
   * @param rUser - Resource request user
   * @param shareOpName - Operation type: share/unshare
   * @param appId - App ID
   * @param appShare - App share object
   * @param isPublic - Indicates if the sharing operation is public
   * @throws TapisClientException - for Tapis client exception
   * @throws TapisException - for Tapis exception
   */
  void updateUserShares(ResourceRequestUser rUser, String shareOpName, String appId, AppShare appShare, boolean isPublic)
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
      userList = PUBLIC_USER_SET; // "~public"
    }

    String oboTenantId = rUser.getOboTenantId();

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
          appUtils.getSKClient(rUser).shareResource(reqShareResource);
          reqShareResource.setPrivilege(Permission.EXECUTE.name());
          appUtils.getSKClient(rUser).shareResource(reqShareResource);
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
          appUtils.getSKClient(rUser).deleteShare(deleteShareParms);
          deleteShareParms.setPrivilege(Permission.EXECUTE.name());
          appUtils.getSKClient(rUser).deleteShare(deleteShareParms);
        }
      }
    }
  }

  /**
   * Create a set of individual permSpec entries based on the list passed in
   * @param permList - list of individual permissions
   * @return - Set of permSpec entries based on permissions
   */
  static Set<String> getPermSpecSet(String tenantName, String appId, Set<Permission> permList)
  {
    var permSet = new HashSet<String>();
    for (Permission perm : permList) { permSet.add(getPermSpecStr(tenantName, appId, perm)); }
    return permSet;
  }

  /**
   * Retrieve set of user permissions given sk client, user, tenant, id
   * @param userName - name of user
   * @param tenantName - name of tenant
   * @param resourceId - Id of resource
   * @return - Set of Permissions for the user
   */
  Set<Permission> getUserPermSet(ResourceRequestUser rUser, String userName, String tenantName, String resourceId)
        throws TapisClientException, TapisException
  {
    var userPerms = new HashSet<Permission>();
    for (Permission perm : Permission.values())
    {
      String permSpec = String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), resourceId);
      if (appUtils.getSKClient(rUser).isPermitted(tenantName, userName, permSpec)) userPerms.add(perm);
    }
    return userPerms;
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

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
    {
      String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, appId, op.name());
      log.warn(msg);
      throw new ForbiddenException(msg);
    }

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
   *  Create -      must be owner or have admin role or have MODIFY permission (to allow for new app versions)
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
   * @param appId - name of the app
   * @param owner - app owner
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
      log.error(msg);
      throw new TapisException(msg);
    }
    switch(op) {
      case enable:
      case disable:
      case lock:
      case unlock:
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
      case create:
        if (owner.equals(oboOrImpersonatedUser) || hasAdminRole(rUser) ||
              isPermitted(rUser, oboTenant, oboOrImpersonatedUser, appId, Permission.MODIFY))
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
    String msg = LibUtils.getMsgAuth("APPLIB_UNAUTH", rUser, appId, op.name());
    log.info(msg);
    throw new ForbiddenException(msg);
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
    return appUtils.getSKClient(rUser).isPermitted(tenantName, userName, permSpecStr);
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
    return appUtils.getSKClient(rUser).isPermittedAny(tenantName, userName, permSpecs.toArray(App.EMPTY_STR_ARRAY));
  }
}
