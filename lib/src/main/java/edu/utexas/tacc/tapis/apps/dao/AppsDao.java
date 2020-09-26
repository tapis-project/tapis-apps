package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;

import java.util.List;

public interface AppsDao
{
  int createApp(AuthenticatedUser authenticatedUser, App app, String createJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  int updateApp(AuthenticatedUser authenticatedUser, App patchedApp, PatchApp patchApp,
                    String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateAppOwner(AuthenticatedUser authenticatedUser, int appId, String newOwnerName) throws TapisException;

  int softDeleteApp(AuthenticatedUser authenticatedUser, int appId) throws TapisException;

  void addUpdateRecord(AuthenticatedUser authenticatedUser, int appId, AppOperation op, String upd_json, String upd_text) throws TapisException;

  int hardDeleteApp(String tenant, String name) throws TapisException;

  Exception checkDB();

  void migrateDB() throws TapisException;

  boolean checkForAppByName(String tenant, String name, boolean includeDeleted) throws TapisException;

  App getAppByName(String tenant, String name) throws TapisException;

  List<App> getApps(String tenant, List<String> searchList, List<Integer> IDs) throws TapisException;

  List<App> getAppsUsingSearchAST(String tenant, ASTNode searchAST, List<Integer> IDs) throws TapisException;

  List<String> getAppNames(String tenant) throws TapisException;

  String getAppOwner(String tenant, String name) throws TapisException;

  int getAppId(String tenant, String name) throws TapisException;
}
