package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;

import java.util.List;
import java.util.Set;

public interface AppsDao
{
  boolean createApp(AuthenticatedUser authenticatedUser, App app, String createJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateApp(AuthenticatedUser authenticatedUser, App patchedApp, PatchApp patchApp,
                    String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateEnabled(AuthenticatedUser authenticatedUser, String id, boolean enabled) throws TapisException;

  void updateAppOwner(AuthenticatedUser authenticatedUser, String id, String newOwnerName) throws TapisException;

  int softDeleteApp(AuthenticatedUser authenticatedUser, String id) throws TapisException;

  void addUpdateRecord(AuthenticatedUser authenticatedUser, String tenant, String id, String version,
                       AppOperation op, String upd_json, String upd_text) throws TapisException;

  int hardDeleteApp(String tenant, String id) throws TapisException;

  Exception checkDB();

  void migrateDB() throws TapisException;

  boolean checkForApp(String tenant, String id, boolean includeDeleted) throws TapisException;

  boolean checkForApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  boolean isEnabled(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id, String version) throws TapisException;

  App getApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  int getAppsCount(String tenant, List<String> searchList, ASTNode searchAST, Set<String> setOfIDs,
                   List<OrderBy> orderByList, String startAfter) throws TapisException;

  List<App> getApps(String tenant, List<String> searchList, ASTNode searchAST, Set<String> appIDs, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter) throws TapisException;

  Set<String> getAppIDs(String tenant) throws TapisException;

  String getAppOwner(String tenant, String id) throws TapisException;
}
