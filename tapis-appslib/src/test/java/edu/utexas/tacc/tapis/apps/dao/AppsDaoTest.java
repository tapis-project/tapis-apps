package edu.utexas.tacc.tapis.apps.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
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
import java.util.List;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;

/**
 * Test the AppsDao class against a DB running locally
 */
@Test(groups={"integration"})
public class AppsDaoTest
{
  private AppsDaoImpl dao;
  private AuthenticatedUser authenticatedUser;

  // Test data
  int numApps = 11;
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

    App tmpApp = dao.getApp(tenantName, apps[0].getId());
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  // Test create for a single item
  @Test
  public void testCreate() throws Exception
  {
    App app0 = apps[0];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
  }

  // Test retrieving a single item
  @Test
  public void testGet() throws Exception {
    App app0 = apps[1];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());

    // Verify data in main table
    // ===========================
    Assert.assertEquals(tmpApp.getId(), app0.getId());
    Assert.assertEquals(tmpApp.getVersion(), app0.getVersion());
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), app0.getOwner());
    Assert.assertEquals(tmpApp.getRuntime().name(), app0.getRuntime().name());
    Assert.assertEquals(tmpApp.getRuntimeVersion(), app0.getRuntimeVersion());
    Assert.assertEquals(tmpApp.getContainerImage(), app0.getContainerImage());
    Assert.assertEquals(tmpApp.getMaxJobs(), app0.getMaxJobs());
    Assert.assertEquals(tmpApp.getMaxJobsPerUser(), app0.getMaxJobsPerUser());
    Assert.assertEquals(tmpApp.getJobDescription(), app0.getJobDescription());
    Assert.assertEquals(tmpApp.isDynamicExecSystem(), app0.isDynamicExecSystem());
    // Verify execSystemConstraints
    String[] tmpExecSystemConstraints = tmpApp.getExecSystemConstraints();
    Assert.assertNotNull(tmpExecSystemConstraints, "execSystemConstraints value was null");
    var execSystemConstraintsList = Arrays.asList(tmpExecSystemConstraints);
    Assert.assertEquals(tmpExecSystemConstraints.length, execSystemConstraints.length, "Wrong number of constraints");
    for (String execSystemConstraintStr : execSystemConstraints)
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
    // Verify envVariables
    String[] tmpEnvVariables = tmpApp.getEnvVariables();
    Assert.assertNotNull(tmpEnvVariables, "envVariables value was null");
    var envVariablesList = Arrays.asList(tmpEnvVariables);
    Assert.assertEquals(tmpEnvVariables.length, envVariables.length, "Wrong number of envVariables");
    for (String envVariableStr : envVariables)
    {
      Assert.assertTrue(envVariablesList.contains(envVariableStr));
      System.out.println("Found envVariable: " + envVariableStr);
    }
    // Verify archiveIncludes
    String[] tmpArchiveIncludes = tmpApp.getArchiveIncludes();
    Assert.assertNotNull(tmpArchiveIncludes, "archiveIncludes value was null");
    var archiveIncludesList = Arrays.asList(tmpArchiveIncludes);
    Assert.assertEquals(tmpArchiveIncludes.length, archiveIncludes.length, "Wrong number of archiveIncludes");
    for (String archiveIncludeStr : archiveIncludes)
    {
      Assert.assertTrue(archiveIncludesList.contains(archiveIncludeStr));
      System.out.println("Found archiveInclude: " + archiveIncludeStr);
    }
    // Verify archiveExcludes
    String[] tmpArchiveExcludes = tmpApp.getArchiveExcludes();
    Assert.assertNotNull(tmpArchiveExcludes, "archiveExcludes value was null");
    var archiveExcludesList = Arrays.asList(tmpArchiveExcludes);
    Assert.assertEquals(tmpArchiveExcludes.length, archiveExcludes.length, "Wrong number of archiveExcludes");
    for (String archiveExcludeStr : archiveExcludes)
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
    Assert.assertEquals(tmpJobTags.length, jobTags.length, "Wrong number of jobTags");
    for (String jobTagStr : jobTags)
    {
      Assert.assertTrue(jobTagsList.contains(jobTagStr));
      System.out.println("Found jobTag: " + jobTagStr);
    }
    // Verify tags
    String[] tmpTags = tmpApp.getTags();
    Assert.assertNotNull(tmpTags, "Tags value was null");
    var tagsList = Arrays.asList(tmpTags);
    Assert.assertEquals(tmpTags.length, tags.length, "Wrong number of tags");
    for (String tagStr : tags)
    {
      Assert.assertTrue(tagsList.contains(tagStr));
      System.out.println("Found tag: " + tagStr);
    }
    // Verify notes
    JsonObject obj = (JsonObject) tmpApp.getNotes();
    Assert.assertNotNull(obj, "Notes object was null");
    Assert.assertTrue(obj.has("project"));
    Assert.assertEquals(obj.get("project").getAsString(), notesObj.get("project").getAsString());
    Assert.assertTrue(obj.has("testdata"));
    Assert.assertEquals(obj.get("testdata").getAsString(), notesObj.get("testdata").getAsString());

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

    // Verify notification subscriptions

    // Verify app args
    List<AppArg> origArgs = app0.getAppArgs();
    List<AppArg> tmpArgs = tmpApp.getAppArgs();
    Assert.assertNotNull(origArgs, "Orig appArgs was null");
    Assert.assertNotNull(tmpArgs, "Fetched appArgs was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    var argValuesFound = new ArrayList<String>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getValue()),
              "List of appArgs did not contain an item with value: " + itemSeedItem.getValue());
    }
    // Verify container args
    origArgs = app0.getContainerArgs();
    tmpArgs = tmpApp.getAppArgs();
    Assert.assertNotNull(origArgs, "Orig containerArgs was null");
    Assert.assertNotNull(tmpArgs, "Fetched containerArgs was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    argValuesFound = new ArrayList<String>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getValue()),
              "List of containerArgs did not contain an item with value: " + itemSeedItem.getValue());
    }

    // Verify scheduler options
    origArgs = app0.getSchedulerOptions();
    tmpArgs = tmpApp.getSchedulerOptions();
    Assert.assertNotNull(origArgs, "Orig schedulerOptions was null");
    Assert.assertNotNull(tmpArgs, "Fetched schedulerOptions was null");
    Assert.assertEquals(tmpArgs.size(), origArgs.size());
    argValuesFound = new ArrayList<String>();
    for (AppArg itemFound : tmpArgs) {argValuesFound.add(itemFound.getValue());}
    for (AppArg itemSeedItem : origArgs)
    {
      Assert.assertTrue(argValuesFound.contains(itemSeedItem.getValue()),
              "List of schedulerOptions did not contain an item with value: " + itemSeedItem.getValue());
    }

  }

  // Test retrieving all app names
  @Test
  public void testGetAppNames() throws Exception {
    // Create 2 apps
    App app0 = apps[2];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    app0 = apps[3];
    itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Get all apps
    List<String> appNames = dao.getAppNames(tenantName);
    for (String name : appNames) {
      System.out.println("Found item: " + name);
    }
    Assert.assertTrue(appNames.contains(apps[2].getId()), "List of apps did not contain app name: " + apps[2].getId());
    Assert.assertTrue(appNames.contains(apps[3].getId()), "List of apps did not contain app name: " + apps[3].getId());
  }

  // Test retrieving all apps
  @Test
  public void testGetApps() throws Exception {
    App app0 = apps[4];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    List<App> apps = dao.getApps(tenantName, null, null);
    for (App app : apps) {
      System.out.println("Found item with id: " + app.getSeqId() + " and name: " + app.getId());
    }
  }

  // Test retrieving all apps in a list of sequenceIDs
  @Test
  public void testGetAppsInSeqIDList() throws Exception {
    var seqIdList = new ArrayList<Integer>();
    // Create 2 apps
    App app0 = apps[5];
    int itemSeqId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemSeqId > 0, "Invalid app seqId: " + itemSeqId);
    seqIdList.add(itemSeqId);
    app0 = apps[6];
    itemSeqId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemSeqId > 0, "Invalid app seqId: " + itemSeqId);
    seqIdList.add(itemSeqId);
    // Get all apps in list of IDs
    List<App> apps = dao.getApps(tenantName, null, seqIdList);
    for (App app : apps) {
      System.out.println("Found item with seqId: " + app.getSeqId() + " and Id: " + app.getId());
      Assert.assertTrue(seqIdList.contains(app.getSeqId()));
    }
    Assert.assertEquals(apps.size(), seqIdList.size());
  }

  // Test change app owner
  @Test
  public void testChangeAppOwner() throws Exception {
    App app0 = apps[7];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    System.out.println("Created item with id: " + itemId);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    dao.updateAppOwner(authenticatedUser, itemId, "newOwner");
    App tmpApp = dao.getApp(app0.getTenant(), app0.getId());
    Assert.assertEquals(tmpApp.getOwner(), "newOwner");
  }

  // Test soft deleting a single item
  @Test
  public void testSoftDelete() throws Exception {
    App app0 = apps[8];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    System.out.println("Created item with id: " + itemId);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    int numDeleted = dao.softDeleteApp(authenticatedUser, itemId);
    Assert.assertEquals(numDeleted, 1);
    numDeleted = dao.softDeleteApp(authenticatedUser, itemId);
    Assert.assertEquals(numDeleted, 0);
    Assert.assertFalse(dao.checkForApp(app0.getTenant(), app0.getId(), false ),
            "App not deleted. App name: " + app0.getId());
  }

  // Test hard deleting a single item
  @Test
  public void testHardDelete() throws Exception {
    App app0 = apps[9];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    System.out.println("Created item with id: " + itemId);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    dao.hardDeleteApp(app0.getTenant(), app0.getId());
    Assert.assertFalse(dao.checkForApp(app0.getTenant(), app0.getId(), true),"App not deleted. App name: " + app0.getId());
  }

  // Test behavior when app is missing, especially for cases where service layer depends on the behavior.
  //  update - throws not found exception
  //  getApp - returns null
  //  checkByName - returns false
  //  getOwner - returns null
  @Test
  public void testMissingApp() throws Exception {
    String fakeAppId = "AMissingAppId";
    PatchApp patchApp = new PatchApp(appVersion, "description PATCHED", enabledFalse, tags, notes);
    patchApp.setTenant(tenantName);
    patchApp.setId(fakeAppId);
    App patchedApp = new App(1, tenantName, fakeAppId, appVersion, "description", AppType.BATCH, ownerUser, enabledTrue,
            runtime, runtimeVersion, containerImage, maxJobs, maxJobsPerUser, strictFileInputsFalse, jobDescription, dynamicExecSystem,
            execSystemConstraints, execSystemId, execSystemExecDir, execSystemInputDir, execSystemOutputDir,
            execSystemLogicalQueue, archiveSystemId, archiveSystemDir, archiveOnAppError, nodeCount, coresPerNode,
            memoryMb, maxMinutes, envVariables, archiveIncludes, archiveExcludes, jobTags,
            tags, notes, null, false, null, null);
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
    Assert.assertNull(dao.getApp(tenantName, fakeAppId));
    Assert.assertNull(dao.getAppOwner(tenantName, fakeAppId));
  }
}