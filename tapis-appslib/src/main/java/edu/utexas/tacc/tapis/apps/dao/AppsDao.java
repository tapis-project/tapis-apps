package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;

import java.util.List;
import java.util.Set;

public interface AppsDao
{
  boolean createApp(ResourceRequestUser rUser, App app, String createJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void patchApp(ResourceRequestUser rUser, App patchedApp, PatchApp patchApp, String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void putApp(ResourceRequestUser rUser, App putApp, String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateEnabled(ResourceRequestUser rUser, String tenantId, String id, boolean enabled) throws TapisException;

  void updateDeleted(ResourceRequestUser rUser, String tenantId, String id, boolean deleted) throws TapisException;

  void updateAppOwner(ResourceRequestUser rUser, String tenantId, String id, String newOwnerName) throws TapisException;

  void addUpdateRecord(ResourceRequestUser rUser, String tenant, String id, String version,
                       AppOperation op, String upd_json, String upd_text) throws TapisException;

  int hardDeleteApp(String tenant, String id) throws TapisException;

  Exception checkDB();

  void migrateDB() throws TapisException;

  boolean checkForApp(String tenant, String id, boolean includeDeleted) throws TapisException;

  boolean checkForAppVersion(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  boolean checkForApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  boolean isEnabled(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id) throws TapisException;

  App getApp(String tenant, String id, String version) throws TapisException;

  App getApp(String tenant, String id, String version, boolean includeDeleted) throws TapisException;

  int getAppsCount(String tenant, List<String> searchList, ASTNode searchAST, Set<String> setOfIDs,
                   List<OrderBy> orderByList, String startAfter, Boolean versionSpecified,
                   boolean showDeleted) throws TapisException;

  List<App> getApps(String tenant, List<String> searchList, ASTNode searchAST, Set<String> appIDs, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter, Boolean versionSpecified,
                    boolean showDeleted) throws TapisException;

  Set<String> getAppIDs(String tenant, boolean showDeleted) throws TapisException;

  String getAppOwner(String tenant, String id) throws TapisException;
}
