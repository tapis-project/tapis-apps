package edu.utexas.tacc.tapis.apps.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.ArchiveFilter;
import edu.utexas.tacc.tapis.apps.model.NotificationSubscription;
import edu.utexas.tacc.tapis.apps.model.ParameterSet;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_LIMIT;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_SKIP;

/**
 * Test the AppsDao class against a DB running locally
 */
@Test(groups={"integration"})
public class AppsDaoTest
{
  private AppsDaoImpl dao;
  private ResourceRequestUser rUser;

  // Test data
  int numApps = 13;
  App[] apps = IntegrationUtils.makeApps(numApps, "Dao");

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + AppsDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    rUser = new ResourceRequestUser(new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                          null, apiUser, tenantName, null, null, null));
    // Cleanup anything leftover from previous failed run
    teardown();
  }

  @AfterSuite
  public void teardown() throws Exception {
    System.out.println("Executing AfterSuite teardown for " + AppsDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (int i = 0; i < numApps; i++)
    {
      dao.hardDeleteApp(tenantName, apps[i].getId());
    }

    App tmpApp = dao.getApp(tenantName, apps[0].getId(), apps[0].getVersion(), true);
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  // Test create for a single item
  @Test
  public void testCreate() throws Exception
  {
    App app0 = apps[0];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
  }

  // Test retrieving a single item
  @Test
  public void testGet() throws Exception {
    App app0 = apps[1];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertNotNull(tmpApp, "Failed to get item, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Found item, id: " + app0.getId() + " version: " + app0.getVersion());

    // Verify data in main tables
    // ===========================
    Assert.assertEquals(tmpApp.getId(), app0.getId());
    Assert.assertEquals(tmpApp.getVersion(), app0.getVersion());
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), app0.getOwner());
    Assert.assertEquals(tmpApp.isEnabled(), app0.isEnabled());
    Assert.assertEquals(tmpApp.getRuntime().name(), app0.getRuntime().name());
    Assert.assertEquals(tmpApp.getRuntimeVersion(), app0.getRuntimeVersion());
    // Verify runtimeOptions
    List<RuntimeOption> rtOps = tmpApp.getRuntimeOptions();
    Assert.assertNotNull(rtOps);
    List<RuntimeOption> sys0RTOptions = app0.getRuntimeOptions();
    Assert.assertNotNull(sys0RTOptions);
    for (RuntimeOption rtOption : sys0RTOptions)
    {
      Assert.assertTrue(rtOps.contains(rtOption), "List of runtime options did not contain: " + rtOption.name());
    }
    Assert.assertEquals(tmpApp.getContainerImage(), app0.getContainerImage());
    Assert.assertEquals(tmpApp.getMaxJobs(), app0.getMaxJobs());
    Assert.assertEquals(tmpApp.getMaxJobsPerUser(), app0.getMaxJobsPerUser());
    Assert.assertEquals(tmpApp.isStrictFileInputs(), app0.isStrictFileInputs());
    Assert.assertEquals(tmpApp.getJobDescription(), app0.getJobDescription());
    Assert.assertEquals(tmpApp.isDynamicExecSystem(), app0.isDynamicExecSystem());
    // Verify execSystemConstraints
    String[] tmpExecSystemConstraints = tmpApp.getExecSystemConstraints();
    Assert.assertNotNull(tmpExecSystemConstraints, "execSystemConstraints value was null");
    var execSystemConstraintsList = Arrays.asList(tmpExecSystemConstraints);
    Assert.assertEquals(tmpExecSystemConstraints.length, execSystemConstraints1.length, "Wrong number of constraints");
    for (String execSystemConstraintStr : execSystemConstraints1)
    {
      Assert.assertTrue(execSystemConstraintsList.contains(execSystemConstraintStr));
      System.out.println("Found execSystemConstraint: " + execSystemConstraintStr);
    }
    Assert.assertEquals(tmpApp.getExecSystemId(), app0.getExecSystemId());
    Assert.assertEquals(tmpApp.getExecSystemExecDir(), app0.getExecSystemExecDir());
    Assert.assertEquals(tmpApp.getExecSystemInputDir(), app0.getExecSystemInputDir());
    Assert.assertEquals(tmpApp.getExecSystemOutputDir(), app0.getExecSystemOutputDir());
    Assert.assertEquals(tmpApp.getExecSystemLogicalQueue(), app0.getExecSystemLogicalQueue());
    Assert.assertEquals(tmpApp.getArchiveSystemId(), app0.getArchiveSystemId());
    Assert.assertEquals(tmpApp.getArchiveSystemDir(), app0.getArchiveSystemDir());
    Assert.assertEquals(tmpApp.isArchiveOnAppError(), app0.isArchiveOnAppError());

   // Verify parameterSet
    ParameterSet parmSet = tmpApp.getParameterSet();
    ParameterSet parmSet0 = app0.getParameterSet();
    Assert.assertNotNull(parmSet, "parameterSet was null");
    verifyAppArgs("App Arg", parmSet0.getAppArgs(), parmSet.getAppArgs());
    verifyAppArgs("Container Arg", parmSet0.getContainerArgs(), parmSet.getContainerArgs());
    verifyAppArgs("Scheduler Option Arg", parmSet0.getSchedulerOptions(), parmSet.getSchedulerOptions());

    // Verify envVariables
    verifyKeyValuePairs("Env Var", parmSet0.getEnvVariables(), parmSet.getEnvVariables());

    // Verify archiveFilter in parameterSet
    ArchiveFilter archiveFilter = parmSet.getArchiveFilter();
    ArchiveFilter archiveFilter0 = parmSet0.getArchiveFilter();
    Assert.assertNotNull(archiveFilter, "archiveFilter was null");
    Assert.assertEquals(archiveFilter.isIncludeLaunchFiles(), archiveFilter0.isIncludeLaunchFiles());
    // Verify archiveIncludes
    String[] tmpArchiveIncludes = archiveFilter.getIncludes();
    Assert.assertNotNull(tmpArchiveIncludes, "archiveIncludes value was null");
    var archiveIncludesList = Arrays.asList(tmpArchiveIncludes);
    Assert.assertEquals(tmpArchiveIncludes.length, archiveIncludes1.length, "Wrong number of archiveIncludes");
    for (String archiveIncludeStr : archiveIncludes1)
    {
      Assert.assertTrue(archiveIncludesList.contains(archiveIncludeStr));
      System.out.println("Found archiveInclude: " + archiveIncludeStr);
    }
    // Verify archiveExcludes
    String[] tmpArchiveExcludes = archiveFilter.getExcludes();
    Assert.assertNotNull(tmpArchiveExcludes, "archiveExcludes value was null");
    var archiveExcludesList = Arrays.asList(tmpArchiveExcludes);
    Assert.assertEquals(tmpArchiveExcludes.length, archiveExcludes1.length, "Wrong number of archiveExcludes");
    for (String archiveExcludeStr : archiveExcludes1)
    {
      Assert.assertTrue(archiveExcludesList.contains(archiveExcludeStr));
      System.out.println("Found archiveExclude: " + archiveExcludeStr);
    }

    // Verify file inputs
    verifyFileInputs(app0.getFileInputs(), tmpApp.getFileInputs());

    Assert.assertEquals(tmpApp.getNodeCount(), app0.getNodeCount());
    Assert.assertEquals(tmpApp.getCoresPerNode(), app0.getCoresPerNode());
    Assert.assertEquals(tmpApp.getMemoryMb(), app0.getMemoryMb());
    Assert.assertEquals(tmpApp.getMaxMinutes(), app0.getMaxMinutes());
    // Verify jobTags
    String[] tmpJobTags = tmpApp.getJobTags();
    Assert.assertNotNull(tmpJobTags, "JobTags value was null");
    var jobTagsList = Arrays.asList(tmpJobTags);
    Assert.assertEquals(tmpJobTags.length, jobTags1.length, "Wrong number of jobTags");
    for (String jobTagStr : jobTags1)
    {
      Assert.assertTrue(jobTagsList.contains(jobTagStr));
      System.out.println("Found jobTag: " + jobTagStr);
    }
    // Verify tags
    String[] tmpTags = tmpApp.getTags();
    Assert.assertNotNull(tmpTags, "Tags value was null");
    var tagsList = Arrays.asList(tmpTags);
    Assert.assertEquals(tmpTags.length, tags1.length, "Wrong number of tags");
    for (String tagStr : tags1)
    {
      Assert.assertTrue(tagsList.contains(tagStr));
      System.out.println("Found tag: " + tagStr);
    }
    // Verify notes
    JsonObject obj = (JsonObject) tmpApp.getNotes();
    Assert.assertNotNull(obj, "Notes object was null");
    Assert.assertTrue(obj.has("project"));
    Assert.assertEquals(obj.get("project").getAsString(), notes1Obj.get("project").getAsString());
    Assert.assertTrue(obj.has("testdata"));
    Assert.assertEquals(obj.get("testdata").getAsString(), notes1Obj.get("testdata").getAsString());

    // ===============================================================================================
    // Verify data in aux tables: notification_subscriptions
    // ===============================================================================================
    // Verify notification subscriptions
    List<NotificationSubscription> origNotificationSubs = app0.getSubscriptions();
    List<NotificationSubscription> tmpSubs = tmpApp.getSubscriptions();
    Assert.assertNotNull(origNotificationSubs, "Orig notificationSubscriptions was null");
    Assert.assertNotNull(tmpSubs, "Fetched notificationSubscriptions was null");
    Assert.assertEquals(tmpSubs.size(), origNotificationSubs.size());
    var filtersFound = new ArrayList<String>();
    for (NotificationSubscription itemFound : tmpSubs) {filtersFound.add(itemFound.getFilter());}
    for (NotificationSubscription itemSeedItem : origNotificationSubs)
    {
      Assert.assertTrue(filtersFound.contains(itemSeedItem.getFilter()),
              "List of notificationSubscriptions did not contain an item with filter: " + itemSeedItem.getFilter());
    }
  }

  // Test retrieving all app IDs
  @Test
  public void testGetAppIDs() throws Exception {
    // Create 2 apps
    App app0 = apps[2];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    app0 = apps[3];
    appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    // Get all apps
    Set<String> appNames = dao.getAppIDs(tenantName, showDeletedFalse);
    for (String name : appNames)
    {
      System.out.println("Found item: " + name);
    }
    Assert.assertTrue(appNames.contains(apps[2].getId()), "List of apps did not contain app name: " + apps[2].getId());
    Assert.assertTrue(appNames.contains(apps[3].getId()), "List of apps did not contain app name: " + apps[3].getId());
  }

  // Test retrieving all apps
  @Test
  public void testGetApps() throws Exception {
    App app0 = apps[4];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    List<App> apps = dao.getApps(tenantName, null, null, null, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP,
                                 startAfterNull, versionSpecifiedNull, showDeletedFalse);
    for (App app : apps)
    {
      System.out.println("Found item with appId: " + app.getId() + " appVer: " + app.getVersion());
    }
  }

  // Test retrieving all apps in a list of IDs
  @Test
  public void testGetAppsInIDList() throws Exception {
    var appIdList = new HashSet<String>();
    // Create 2 apps
    App app0 = apps[5];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    appIdList.add(app0.getId());
    app0 = apps[6];
    appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    appIdList.add(app0.getId());
    // Get all apps in list of IDs
    List<App> apps = dao.getApps(tenantName, null, null, appIdList, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP,
                                 startAfterNull, versionSpecifiedNull, showDeletedFalse);
    for (App app : apps)
    {
      System.out.println("Found item with appId: " + app.getId() + " and appVer: " + app.getVersion());
      Assert.assertTrue(appIdList.contains(app.getId()));
    }
    Assert.assertEquals(apps.size(), appIdList.size());
  }

  // Test enable/disable/delete/undelete
  @Test
  public void testEnableDisableDeleteUndelete() throws Exception {
    App app0 = apps[12];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion() +
                       " enabled: " + app0.isEnabled());
    // Enabled should start off true, then become false and finally true again.
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertTrue(tmpApp.isEnabled());
    dao.updateEnabled(rUser, tenantName, app0.getId(), false);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertFalse(tmpApp.isEnabled());
    dao.updateEnabled(rUser, tenantName, app0.getId(), true);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertTrue(tmpApp.isEnabled());

    // Deleted should start off false, then become true and finally false again.
    tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(app0.isDeleted());
    dao.updateDeleted(rUser, tenantName, app0.getId(), true);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion(), true);
    Assert.assertTrue(tmpApp.isDeleted());
    dao.updateDeleted(rUser, tenantName, app0.getId(), false);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(tmpApp.isDeleted());
  }

  // Test change app owner
  @Test
  public void testChangeAppOwner() throws Exception {
    App app0 = apps[7];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    dao.updateAppOwner(rUser, tenantName, app0.getId(), "newOwner");
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertEquals(tmpApp.getOwner(), "newOwner");
  }

  // Test hard deleting a single item
  @Test
  public void testHardDelete() throws Exception {
    App app0 = apps[9];
    boolean appCreated = dao.createApp(rUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());

    dao.hardDeleteApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(dao.checkForApp(app0.getTenant(),app0.getId(),true),"App not deleted. App id: " + app0.getId());
  }

  // Test retrieving apps with multiple versions
  @Test
  public void testMultipleVersions() throws Exception {
    var appIdList = new HashSet<String>();
    var appVerList = new HashSet<String>();
    // Create 2 versions of 2 apps
    App app1a = apps[10];
    App app1b = new App(app1a, tenantName, app1a.getId(), appVersion2, appType);
    boolean appCreated = dao.createApp(rUser, app1a, gson.toJson(app1a), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app1a.getId() + " version: " + app1a.getVersion());
    System.out.println("Created item, id: " + app1a.getId() + " version: " + app1a.getVersion());
    appVerList.add(app1a.getVersion());
    appIdList.add(app1a.getId());

    appCreated = dao.createApp(rUser, app1b, gson.toJson(app1a), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app1b.getId() + " version: " + app1b.getVersion());
    System.out.println("Created item, id: " + app1b.getId() + " version: " + app1b.getVersion());
    appVerList.add(app1b.getVersion());
    appIdList.add(app1b.getId());

    App app2a = apps[11];
    App app2b = new App(app2a, tenantName, app2a.getId(), appVersion2, appType);
    appCreated = dao.createApp(rUser, app2a, gson.toJson(app2a), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app2a.getId() + " version: " + app2a.getVersion());
    System.out.println("Created item, id: " + app2a.getId() + " version: " + app2a.getVersion());
    appVerList.add(app2a.getVersion());
    appIdList.add(app2a.getId());

    appCreated = dao.createApp(rUser, app2b, gson.toJson(app2b), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app2b.getId() + " version: " + app2b.getVersion());
    System.out.println("Created item, id: " + app2b.getId() + " version: " + app2b.getVersion());
    appVerList.add(app2b.getVersion());
    appIdList.add(app2b.getId());

    // When retrieving singly make sure we get back latest versions
    App tmpApp = dao.getApp(app1a.getTenant(), app1a.getId());
    Assert.assertNotNull(tmpApp, "Failed to create item, id: " + app1a.getId() + " version: " + app1a.getVersion());
    System.out.println("Found item: " + app1a.getId());
    Assert.assertEquals(tmpApp.getVersion(), appVersion2);
    tmpApp = dao.getApp(app2a.getTenant(), app2a.getId());
    Assert.assertNotNull(tmpApp, "Failed to create item, id: " + app2a.getId() + " version: " + app2a.getVersion());
    System.out.println("Found item: " + app2a.getId() + " version: " + app2a.getVersion());
    Assert.assertEquals(tmpApp.getVersion(), appVersion2);

    // Use search to pick out an app and make sure we get just the latest version
    var searchList = Collections.singletonList("id.eq." + app1a.getId());
    List<App> apps = dao.getApps(tenantName, searchList, null, null, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP,
                                 startAfterNull, versionSpecifiedNull, showDeletedFalse);
    Assert.assertEquals(apps.size(), 1);
    tmpApp = apps.get(0);
    System.out.println("Found item with Id: " + tmpApp.getId() + " Version: " + tmpApp.getVersion());
    Assert.assertEquals(tmpApp.getId(), app1a.getId());
    Assert.assertEquals(tmpApp.getVersion(), appVersion2);

    // Now add version to the searchList and confirm we get back all versions
    searchList = Arrays.asList("id.eq." + app1a.getId(), "version.like.%");
    apps = dao.getApps(tenantName, searchList, null, null, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP,
                       startAfterNull, versionSpecifiedNull, showDeletedFalse);
    Assert.assertEquals(apps.size(), 2);
    for (App app : apps) {
      System.out.println("Found item with Id: " + app.getId() + " Version: " + app.getVersion());
      Assert.assertEquals(app.getId(), app1a.getId());
    }
  }

  // Test behavior when app is missing, especially for cases where service layer depends on the behavior.
  //  putApp - throws not found exception
  //  patchApp - throws not found exception
  //  getApp - returns null
  //  check - returns false
  //  getOwner - returns null
  @Test
  public void testMissingApp() throws Exception {
    String fakeAppId = "AMissingAppId";
    String fakeAppVersion = "AMissingAppVersion";
    App patchedApp = new App(1, 1, tenantName, fakeAppId, fakeAppVersion, "description", AppType.BATCH, owner2, enabledTrue,
            containerizedTrue, runtime1, runtimeVersion1, runtimeOptions1, containerImage1,
            maxJobs1, maxJobsPerUser1, strictFileInputsFalse, IntegrationUtils.jobDescription1, dynamicExecSystemTrue,
            execSystemConstraints1, execSystemId1, execSystemExecDir1, execSystemInputDir1, execSystemOutputDir1,
            execSystemLogicalQueue1, archiveSystemIdNull, archiveSystemDir1, archiveOnAppErrorTrue,
            parameterSet1, finList1, nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, notifList1, jobTags1,
            tags1, notes1, uuidNull, isDeletedFalse, createdNull, updatedNull);
    // Make sure app does not exist
    Assert.assertFalse(dao.checkForApp(tenantName, fakeAppId, true));
    Assert.assertFalse(dao.checkForApp(tenantName, fakeAppId, false));
    // update should throw not found exception
    boolean pass = false;
    try { dao.patchApp(rUser, fakeAppId, fakeAppVersion, patchedApp, scrubbedJson, null); }
    catch (IllegalStateException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { dao.putApp(rUser, patchedApp, scrubbedJson, null); }
    catch (IllegalStateException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);
    Assert.assertNull(dao.getApp(tenantName, fakeAppId, fakeAppVersion));
    Assert.assertNull(dao.getAppOwner(tenantName, fakeAppId));
  }
}
