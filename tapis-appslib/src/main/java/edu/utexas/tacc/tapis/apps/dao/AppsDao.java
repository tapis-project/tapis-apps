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

  void updateAppOwner(AuthenticatedUser authenticatedUser, int appSeqId, String newOwnerName) throws TapisException;

  int softDeleteApp(AuthenticatedUser authenticatedUser, int appSeqId) throws TapisException;

  void addUpdateRecord(AuthenticatedUser authenticatedUser, int appSeqId, AppOperation op, String upd_json, String upd_text) throws TapisException;

  int hardDeleteApp(String tenant, String appId) throws TapisException;

  Exception checkDB();

  void migrateDB() throws TapisException;

  boolean checkForApp(String tenant, String appId, boolean includeDeleted) throws TapisException;

  App getApp(String tenant, String appId) throws TapisException;

  List<App> getApps(String tenant, List<String> searchList, List<Integer> seqIDs) throws TapisException;

  List<App> getAppsUsingSearchAST(String tenant, ASTNode searchAST, List<Integer> seqIDs) throws TapisException;

  List<String> getAppNames(String tenant) throws TapisException;

  String getAppOwner(String tenant, String appId) throws TapisException;

  int getAppSeqId(String tenant, String appId) throws TapisException;
}
