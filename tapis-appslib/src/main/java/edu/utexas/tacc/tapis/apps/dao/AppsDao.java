package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;

import java.util.List;
import java.util.Set;

public interface AppsDao
{
  int createApp(AuthenticatedUser authenticatedUser, App app, String createJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  int updateApp(AuthenticatedUser authenticatedUser, App patchedApp, PatchApp patchApp,
                    String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateAppOwner(AuthenticatedUser authenticatedUser, int seqId, String newOwnerName) throws TapisException;

  int softDeleteApp(AuthenticatedUser authenticatedUser, int seqId) throws TapisException;

  void addUpdateRecord(AuthenticatedUser authenticatedUser, int seqId, int verSeqId, AppOperation op,
                       String upd_json, String upd_text) throws TapisException;

  int hardDeleteApp(String tenant, String id) throws TapisException;

  Exception checkDB();

  void migrateDB() throws TapisException;

  boolean checkForApp(String tenant, String id, boolean includeDeleted) throws TapisException;

  boolean checkForApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  boolean isEnabled(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id, String version) throws TapisException;

  App getApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  List<App> getApps(String tenant, List<String> searchList, Set<String> seqIDs) throws TapisException;

  List<App> getAppsUsingSearchAST(String tenant, ASTNode searchAST, Set<String> seqIDs) throws TapisException;

  Set<String> getAppIDs(String tenant) throws TapisException;

  String getAppOwner(String tenant, String id) throws TapisException;

  int getAppSeqId(String tenant, String id) throws TapisException;
}
