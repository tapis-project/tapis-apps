package edu.utexas.tacc.tapis.apps.dao;

import java.util.List;
import java.util.Set;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;

import edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.AuthListType;

public interface AppsDao
{
  boolean createApp(ResourceRequestUser rUser, App app, String changeDescription, String rawData)
          throws TapisException, IllegalStateException;

  void patchApp(ResourceRequestUser rUser, String appId, String appVersion, App patchedApp,
                String changeDescription, String rawData)
          throws TapisException, IllegalStateException;

  void putApp(ResourceRequestUser rUser, App putApp, String changeDescription, String rawData)
          throws TapisException, IllegalStateException;

  void updateEnabled(ResourceRequestUser rUser, String tenantId, String id, boolean enabled) throws TapisException;

  void updateLocked(ResourceRequestUser rUser, String tenantId, String id, String version, boolean locked) throws TapisException;

  void updateDeleted(ResourceRequestUser rUser, String tenantId, String id, boolean deleted) throws TapisException;

  void updateAppOwner(ResourceRequestUser rUser, String tenantId, String id, String newOwnerName) throws TapisException;

  void addUpdateRecord(ResourceRequestUser rUser, String tenant, String id, String version,
                       AppOperation op, String changeDescription, String rawData) throws TapisException;

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

  int getAppsCount(ResourceRequestUser rUser, List<String> searchList, ASTNode searchAST, List<OrderBy> orderByList,
                   String startAfter, Boolean versionSpecified, boolean showDeleted, AuthListType listType,
                   Set<String> viewableAppIDs, Set<String> sharedAppIDs)
          throws TapisException;

  List<App> getApps(ResourceRequestUser rUser, List<String> searchList, ASTNode searchAST, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter, Boolean versionSpecified,
                    boolean showDeleted, AuthListType listType, Set<String> viewableAppIDs, Set<String> sharedAppIDs)
          throws TapisException;

  Set<String> getAppIDs(String tenant, boolean showDeleted) throws TapisException;

  String getAppOwner(String tenant, String id) throws TapisException;

  List<AppHistoryItem> getAppHistory(String oboTenantId, String appId) throws TapisException;
}
