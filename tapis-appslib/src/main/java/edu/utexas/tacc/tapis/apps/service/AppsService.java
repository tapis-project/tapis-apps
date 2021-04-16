package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
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
  void createApp(AuthenticatedUser authenticatedUser, App app, String scrubbedText)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, TapisClientException;

  void updateApp(AuthenticatedUser authenticatedUser, PatchApp patchApp, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int enableApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException, TapisClientException;

  int disableApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException, TapisClientException;

  int changeAppOwner(AuthenticatedUser authenticatedUser, String appId, String newOwnerName)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException, TapisClientException;

  int softDeleteApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, TapisClientException;

  boolean checkForApp(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, TapisClientException;

  boolean isEnabled(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, TapisClientException;

  App getApp(AuthenticatedUser authenticatedUser, String appId, String appVersion, boolean requireExecPerm)
          throws TapisException, NotAuthorizedException, TapisClientException;

  int getAppsTotalCount(AuthenticatedUser authenticatedUser, List<String> searchList, List<OrderBy> orderByList,
                        String startAfter) throws TapisException, TapisClientException;

  List<App> getApps(AuthenticatedUser authenticatedUser, List<String> searchList, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException;

  List<App> getAppsUsingSqlSearchStr(AuthenticatedUser authenticatedUser, String searchStr, int limit,
                                     List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException;

  Set<String> getAppIDs(AuthenticatedUser authenticatedUser)
          throws TapisException;

  String getAppOwner(AuthenticatedUser authenticatedUser, String appId)
          throws TapisException, NotAuthorizedException, TapisClientException;

  void grantUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException;

  int revokeUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException;

  Set<Permission> getUserPermissions(AuthenticatedUser authenticatedUser, String appId, String userName)
          throws TapisException, NotAuthorizedException, TapisClientException;
}
