package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
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
  int createApp(AuthenticatedUser authenticatedUser, App app, String scrubbedText)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, TapisClientException;

  int updateApp(AuthenticatedUser authenticatedUser, PatchApp patchApp, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int changeAppOwner(AuthenticatedUser authenticatedUser, String appName, String newOwnerName)
          throws TapisException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException, TapisClientException;

  int softDeleteAppByName(AuthenticatedUser authenticatedUser, String appName)
          throws TapisException, NotAuthorizedException, TapisClientException;

  boolean checkForAppByName(AuthenticatedUser authenticatedUser, String appName)
          throws TapisException, NotAuthorizedException, TapisClientException;

  App getApp(AuthenticatedUser authenticatedUser, String appName, boolean requireExecPerm)
          throws TapisException, NotAuthorizedException, TapisClientException;

  List<App> getApps(AuthenticatedUser authenticatedUser, List<String> searchList)
          throws TapisException, TapisClientException;

  List<App> getAppsUsingSqlSearchStr(AuthenticatedUser authenticatedUser, String searchStr)
          throws TapisException, TapisClientException;

  List<String> getAppNames(AuthenticatedUser authenticatedUser)
          throws TapisException;

  String getAppOwner(AuthenticatedUser authenticatedUser, String appName)
          throws TapisException, NotAuthorizedException, TapisClientException;

  void grantUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException;

  int revokeUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName, Set<Permission> permissions, String updateText)
          throws TapisException, NotAuthorizedException, TapisClientException;

  Set<Permission> getUserPermissions(AuthenticatedUser authenticatedUser, String appName, String userName)
          throws TapisException, NotAuthorizedException, TapisClientException;
}
