package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;

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
  void createApp(ResourceRequestUser rUser, App app, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException;

  void patchApp(ResourceRequestUser rUser, String appId, String appVersion, PatchApp patchApp, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  void putApp(ResourceRequestUser rUser, App putApp, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int enableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int disableApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int deleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int undeleteApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int changeAppOwner(ResourceRequestUser rUser, String appId, String newOwnerName)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException, NotFoundException;

  boolean checkForApp(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  boolean checkForApp(ResourceRequestUser rUser, String appId, boolean includeDeleted)
          throws TapisException, TapisClientException, NotAuthorizedException;

  boolean isEnabled(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  App getApp(ResourceRequestUser rUser, String appId, String appVersion, boolean requireExecPerm)
          throws TapisException, TapisClientException, NotAuthorizedException;

  int getAppsTotalCount(ResourceRequestUser rUser, List<String> searchList, List<OrderBy> orderByList,
                        String startAfter, boolean showDeleted) throws TapisException, TapisClientException;

  List<App> getApps(ResourceRequestUser rUser, List<String> searchList, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter, boolean showDeleted)
          throws TapisException, TapisClientException;

  List<App> getAppsUsingSqlSearchStr(ResourceRequestUser rUser, String searchStr, int limit,
                                     List<OrderBy> orderByList, int skip, String startAfter, boolean showDeleted)
          throws TapisException, TapisClientException;

  Set<String> getAllowedAppIDs(ResourceRequestUser rUser, boolean showDeleted)
          throws TapisException;

  String getAppOwner(ResourceRequestUser rUser, String appId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  void grantUserPermissions(ResourceRequestUser rUser, String appId, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, TapisClientException, NotAuthorizedException;

  int revokeUserPermissions(ResourceRequestUser rUser, String appId, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, TapisClientException, NotAuthorizedException;

  Set<Permission> getUserPermissions(ResourceRequestUser rUser, String appId, String userName)
          throws TapisException, TapisClientException, NotAuthorizedException;

  List<AppHistoryItem> getAppHistory(ResourceRequestUser rUser, String appId) 
      throws TapisException, TapisClientException, NotAuthorizedException;
}
