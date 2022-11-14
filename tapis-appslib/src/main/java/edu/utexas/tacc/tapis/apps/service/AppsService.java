package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;
import edu.utexas.tacc.tapis.apps.model.AppShare;

import org.jvnet.hk2.annotations.Contract;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Set;

/*
 * Interface for Apps Service
 * Annotate as an hk2 Contract in case we have multiple implementations
 */
@Contract
public interface AppsService
{
  void createApp(ResourceRequestUser rUser, App app, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException;

  void patchApp(ResourceRequestUser rUser, String appId, String appVersion, PatchApp patchApp, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  void putApp(ResourceRequestUser rUser, App putApp, String rawData)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int enableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int disableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int deleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int undeleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int changeAppOwner(ResourceRequestUser rUser, String appId, String newOwnerName)
          throws TapisException, TapisClientException, IllegalArgumentException, NotFoundException;

  boolean checkForApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException;

  boolean checkForApp(ResourceRequestUser rUser, String appId, boolean includeDeleted)
          throws TapisException, TapisClientException;

  boolean isEnabled(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException;

  App getApp(ResourceRequestUser rUser, String appId, String appVersion, boolean requireExecPerm, String impersonationId)
          throws TapisException, TapisClientException;

  int getAppsTotalCount(ResourceRequestUser rUser, List<String> searchList, List<OrderBy> orderByList,
                        String startAfter, boolean includeDeleted, String listType)
          throws TapisException, TapisClientException;

  List<App> getApps(ResourceRequestUser rUser, List<String> searchList, int limit, List<OrderBy> orderByList,
                    int skip, String startAfter, boolean includeDeleted, String listType)
          throws TapisException, TapisClientException;

  List<App> getAppsUsingSqlSearchStr(ResourceRequestUser rUser, String searchStr, int limit, List<OrderBy> orderByList,
                                     int skip, String startAfter, boolean includeDeleted, String listType)
          throws TapisException, TapisClientException;

  String getAppOwner(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException;

  void grantUserPermissions(ResourceRequestUser rUser, String appId, String userName, Set<Permission> permissions, String rawData)
          throws TapisException, TapisClientException;

  int revokeUserPermissions(ResourceRequestUser rUser, String appId, String userName, Set<Permission> permissions, String rawData)
          throws TapisException, TapisClientException;

  Set<Permission> getUserPermissions(ResourceRequestUser rUser, String appId, String userName)
          throws TapisException, TapisClientException;

  List<AppHistoryItem> getAppHistory(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException;

  //------------------- Share ---------------------------------
  // -----------------------------------------------------------------------
  AppShare getAppShare(ResourceRequestUser rUser, String appId)
      throws TapisException, TapisClientException;
  
  void shareApp(ResourceRequestUser rUser, String appId, AppShare appsShare)
      throws TapisException, TapisClientException, IllegalStateException;
  
  void unshareApp(ResourceRequestUser rUser, String appId, AppShare appsShare) 
      throws TapisException, TapisClientException, IllegalStateException;

  void shareAppPublicly(ResourceRequestUser rUser, String appId) 
      throws TapisException, TapisClientException, IllegalStateException;
  
  void unshareAppPublicly(ResourceRequestUser rUser, String appId) 
      throws TapisException, TapisClientException, IllegalStateException;
}
