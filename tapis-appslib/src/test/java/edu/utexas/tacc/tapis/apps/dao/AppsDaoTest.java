package edu.utexas.tacc.tapis.apps.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

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
  private AuthenticatedUser authenticatedUser;

  // Test data
  int numApps = 13;
  App[] apps = IntegrationUtils.makeApps(numApps, "Dao");

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + AppsDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    authenticatedUser = new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null, null);
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
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
  }

  // Test retrieving a single item
  @Test
  public void testGet() throws Exception {
    App app0 = apps[1];
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertNotNull(tmpApp, "Failed to get item, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Found item, id: " + app0.getId() + " version: " + app0.getVersion());

    // Verify data in main table
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
    Assert.assertEquals(tmpApp.getArchiveIncludeLaunchFiles(), app0.getArchiveIncludeLaunchFiles());
    // Verify envVariables
    String[] tmpEnvVariables = tmpApp.getEnvVariables();
    Assert.assertNotNull(tmpEnvVariables, "envVariables value was null");
    var envVariablesList = Arrays.asList(tmpEnvVariables);
    Assert.assertEquals(tmpEnvVariables.length, envVariables1.length, "Wrong number of envVariables");
    for (String envVariableStr : envVariables1)
    {
      Assert.assertTrue(envVariablesList.contains(envVariableStr));
      System.out.println("Found envVariable: " + envVariableStr);
    }
    // Verify archiveIncludes
    String[] tmpArchiveIncludes = tmpApp.getArchiveIncludes();
    Assert.assertNotNull(tmpArchiveIncludes, "archiveIncludes value was null");
    var archiveIncludesList = Arrays.asList(tmpArchiveIncludes);
    Assert.assertEquals(tmpArchiveIncludes.length, archiveIncludes1.length, "Wrong number of archiveIncludes");
    for (String archiveIncludeStr : archiveIncludes1)
    {
      Assert.assertTrue(archiveIncludesList.contains(archiveIncludeStr));
      System.out.println("Found archiveInclude: " + archiveIncludeStr);
    }
    // Verify archiveExcludes
    String[] tmpArchiveExcludes = tmpApp.getArchiveExcludes();
    Assert.assertNotNull(tmpArchiveExcludes, "archiveExcludes value was null");
    var archiveExcludesList = Arrays.asList(tmpArchiveExcludes);
    Assert.assertEquals(tmpArchiveExcludes.length, archiveExcludes1.length, "Wrong number of archiveExcludes");
    for (String archiveExcludeStr : archiveExcludes1)
    {
      Assert.assertTrue(archiveExcludesList.contains(archiveExcludeStr));
      System.out.println("Found archiveExclude: " + archiveExcludeStr);
    }
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
    // Verify data in aux tables: file_inputs, notification_subscriptions, app_args, container_args, scheduler_options
    // ===============================================================================================
    // Verify file inputs
    List<FileInput> origFileInputs = app0.getFileInputs();
    List<FileInput> tmpInputs = tmpApp.getFileInputs();
    Assert.assertNotNull(origFileInputs, "Orig fileInputs was null");
    Assert.assertNotNull(tmpInputs, "Fetched fileInputs was null");
    Assert.assertEquals(tmpInputs.size(), origFileInputs.size());
    var metaNamesFound = new ArrayList<String>();
    for (FileInput itemFound : tmpInputs) {metaNamesFound.add(itemFound.getMetaName());}
    for (FileInput itemSeedItem : origFileInputs)
    {
      Assert.assertTrue(metaNamesFound.contains(itemSeedItem.getMetaName()),
              "List of fileInputs did not contain an item with metaName: " + itemSeedItem.getMetaName());
    }
    // Verify app args
    List<AppArg> origArgs = app0.getAppArgs();
    List<AppArg> tmpArgs = tmpApp.getAppArgs();
    Assert.assertNotNull(origArgs, "Orig appArgs was null");
    Assert.assertNotNull(tmpArgs, "Fetched appArgs was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    var argValuesFound = new ArrayList<String>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getArgValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getArgValue()),
              "List of appArgs did not contain an item with value: " + itemSeedItem.getArgValue());
    }
    // Verify container args
    origArgs = app0.getContainerArgs();
    tmpArgs = tmpApp.getAppArgs();
    Assert.assertNotNull(origArgs, "Orig containerArgs was null");
    Assert.assertNotNull(tmpArgs, "Fetched containerArgs was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    argValuesFound = new ArrayList<>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getArgValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getArgValue()),
              "List of containerArgs did not contain an item with value: " + itemSeedItem.getArgValue());
    }
    // Verify scheduler options
    origArgs = app0.getSchedulerOptions();
    tmpArgs = tmpApp.getSchedulerOptions();
    Assert.assertNotNull(origArgs, "Orig schedulerOptions was null");
    Assert.assertNotNull(tmpArgs, "Fetched schedulerOptions was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    argValuesFound = new ArrayList<>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getArgValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getArgValue()),
              "List of schedulerOptions did not contain an item with value: " + itemSeedItem.getArgValue());
    }
    // Verify notification subscriptions
    List<NotifSubscription> origNotificationSubs = app0.getNotificationSubscriptions();
    List<NotifSubscription> tmpSubs = tmpApp.getNotificationSubscriptions();
    Assert.assertNotNull(origNotificationSubs, "Orig notificationSubscriptions was null");
    Assert.assertNotNull(tmpSubs, "Fetched notificationSubscriptions was null");
    Assert.assertEquals(tmpSubs.size(), origNotificationSubs.size());
    var filtersFound = new ArrayList<String>();
    for (NotifSubscription itemFound : tmpSubs) {filtersFound.add(itemFound.getFilter());}
    for (NotifSubscription itemSeedItem : origNotificationSubs)
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
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    app0 = apps[3];
    appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    // Get all apps
    Set<String> appNames = dao.getAppIDs(tenantName);
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
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    List<App> apps = dao.getApps(tenantName, null, null, null, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP, startAfterNull);
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
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    appIdList.add(app0.getId());
    app0 = apps[6];
    appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    appIdList.add(app0.getId());
    // Get all apps in list of IDs
    List<App> apps = dao.getApps(tenantName, null, null, appIdList, DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP, startAfterNull);
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
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion() +
                       " enabled: " + app0.isEnabled());
    // Enabled should start off true, then become false and finally true again.
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertTrue(tmpApp.isEnabled());
    dao.updateEnabled(authenticatedUser, app0.getId(), false);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertFalse(tmpApp.isEnabled());
    dao.updateEnabled(authenticatedUser, app0.getId(), true);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertTrue(tmpApp.isEnabled());

    // Deleted should start off false, then become true and finally false again.
    tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(app0.isDeleted());
    dao.updateDeleted(authenticatedUser, app0.getId(), true);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion(), true);
    Assert.assertTrue(tmpApp.isDeleted());
    dao.updateDeleted(authenticatedUser, app0.getId(), false);
    tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(tmpApp.isDeleted());
  }

  // Test change app owner
  @Test
  public void testChangeAppOwner() throws Exception {
    App app0 = apps[7];
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app0.getId() + " version: " + app0.getVersion());
    System.out.println("Created item, id: " + app0.getId() + " version: " + app0.getVersion());
    dao.updateAppOwner(authenticatedUser, app0.getId(), "newOwner");
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId(), app0.getVersion());
    Assert.assertEquals(tmpApp.getOwner(), "newOwner");
  }

  // Test hard deleting a single item
  @Test
  public void testHardDelete() throws Exception {
    App app0 = apps[9];
    boolean appCreated = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
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
    App app1 = apps[10];
    boolean appCreated = dao.createApp(authenticatedUser, app1, gson.toJson(app1), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app1.getId() + " version: " + app1.getVersion());
    System.out.println("Created item, id: " + app1.getId() + " version: " + app1.getVersion());
    appVerList.add(app1.getVersion());
    appIdList.add(app1.getId());

    app1.setVersion(appVersion2);
    appCreated = dao.createApp(authenticatedUser, app1, gson.toJson(app1), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app1.getId() + " version: " + app1.getVersion());
    System.out.println("Created item, id: " + app1.getId() + " version: " + app1.getVersion());
    appVerList.add(app1.getVersion());
    appIdList.add(app1.getId());

    App app2 = apps[11];
    appCreated = dao.createApp(authenticatedUser, app2, gson.toJson(app2), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app2.getId() + " version: " + app2.getVersion());
    System.out.println("Created item, id: " + app2.getId() + " version: " + app2.getVersion());
    appVerList.add(app2.getVersion());
    appIdList.add(app2.getId());

    app2.setVersion(appVersion2);
    appCreated = dao.createApp(authenticatedUser, app2, gson.toJson(app2), scrubbedJson);
    Assert.assertTrue(appCreated, "Item not created, id: " + app2.getId() + " version: " + app2.getVersion());
    System.out.println("Created item, id: " + app2.getId() + " version: " + app2.getVersion());
    appVerList.add(app2.getVersion());
    appIdList.add(app2.getId());

    // When retrieving singly make sure we get back latest versions
    App tmpApp = dao.getApp(app1.getTenant(), app1.getId());
    Assert.assertNotNull(tmpApp, "Failed to create item, id: " + app1.getId() + " version: " + app1.getVersion());
    System.out.println("Found item: " + app1.getId());
    Assert.assertEquals(tmpApp.getVersion(), appVersion2);
    tmpApp = dao.getApp(app2.getTenant(), app2.getId());
    Assert.assertNotNull(tmpApp, "Failed to create item, id: " + app2.getId() + " version: " + app2.getVersion());
    System.out.println("Found item: " + app2.getId() + " version: " + app2.getVersion());
    Assert.assertEquals(tmpApp.getVersion(), appVersion2);

    // TODO
//    // Use search to pick out an app and make sure we get back all versions
//    var searchList = Collections.singletonList("id.eq." + app1.getId());
//    List<App> apps = dao.getApps(tenantName, searchList, null);
//    for (App app : apps) {
//      System.out.println("Found item with Id: " + app.getId() + " Version: " + app.getVersion());
//      Assert.assertTrue(appIdList.contains(app.getId()));
//    }
//    Assert.assertEquals(apps.size(), 2);
  }

  // Test behavior when app is missing, especially for cases where service layer depends on the behavior.
  //  update - throws not found exception
  //  getApp - returns null
  //  check - returns false
  //  getOwner - returns null
  @Test
  public void testMissingApp() throws Exception {
    String fakeAppId = "AMissingAppId";
    String fakeAppVersion = "AMissingAppVersion";
    PatchApp patchApp = IntegrationUtils.makePatchAppFull();
    patchApp.setTenant(tenantName);
    patchApp.setId(fakeAppId);
    patchApp.setVersion(fakeAppVersion);
    App patchedApp = new App(1, 1, tenantName, fakeAppId, fakeAppVersion, "description", AppType.BATCH, owner2, enabledTrue,
            containerizedTrue, runtime1, runtimeVersion1, runtimeOptions1, containerImage1,
            maxJobs1, maxJobsPerUser1, strictFileInputsFalse, IntegrationUtils.jobDescription1, dynamicExecSystemTrue,
            execSystemConstraints1, execSystemId1, execSystemExecDir1, execSystemInputDir1, execSystemOutputDir1,
            execSystemLogicalQueue1, archiveSystemIdNull, archiveSystemDir1, archiveOnAppErrorTrue,
            envVariables1, archiveIncludes1, archiveExcludes1, archiveIncludeLaunchFilesTrue,
            nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, jobTags1,
            tags1, notes1, uuidNull, isDeletedFalse, createdNull, updatedNull);
    // Make sure app does not exist
    Assert.assertFalse(dao.checkForApp(tenantName, fakeAppId, true));
    Assert.assertFalse(dao.checkForApp(tenantName, fakeAppId, false));
    // update should throw not found exception
    boolean pass = false;
    try { dao.updateApp(authenticatedUser, patchedApp, patchApp, scrubbedJson, null); }
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
