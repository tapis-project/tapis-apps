package edu.utexas.tacc.tapis.apps.service;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.ArchiveFilter;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.apps.model.ParameterSet;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.config.RuntimeParameters;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.dao.AppsDaoImpl;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;

/**
 * Test the AppsService implementation class against a DB running locally
 * Note that this test has the following dependencies running locally or in dev
 *    Database - typically local
 *    Tenants service - typically dev
 *    Tokens service - typically dev and obtained from tenants service
 *    Security Kernel service - typically dev and obtained from tenants service
 *    Systems service - typically dev, used for validating execSystemId, archiveSystemId
 *
 * Applications are mostly owned by testuser1
 *   testuser1, testuser3 and testuser4 are also used
 */
@Test(groups={"integration"})
public class AppsServiceTest
{
  private AppsService svc;
  private AppsServiceImpl svcImpl;
  private ResourceRequestUser rUser0, rUser1, rUser2, rUser3, rUser4, rUser5, rAdminUser, rFilesSvc;
  // Test data
  private static final String testKey = "Svc";
  // Special case IDs that have caused problems.
  private static final String specialId1 = testKey + appIdPrefix + "-app";
  private static final String filesSvcName = "files";
  private static final String adminUser = "testadmin";
  private static final String siteId = "tacc";
  private static final String adminTenantName = "admin";
  private static final String testUser0 = "testuser0";
  private static final String testUser1 = "testuser1";
  private static final String testUser2 = "testuser2";
  private static final String testUser3 = "testuser3";
  private static final String testUser4 = "testuser4";
  private static final String testUser5 = "testuser5";
  private static final Set<Permission> testPermsALL = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY, Permission.EXECUTE));
  private static final Set<Permission> testPermsREADMODIFY = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final Set<Permission> testPermsREADEXECUTE = new HashSet<>(Set.of(Permission.READ, Permission.EXECUTE));
  private static final Set<Permission> testPermsREAD = new HashSet<>(Set.of(Permission.READ));
  private static final Set<Permission> testPermsMODIFY = new HashSet<>(Set.of(Permission.MODIFY));

  // Create test app definitions in memory
  int numApps = 27;
  App[] apps = IntegrationUtils.makeApps(numApps, testKey);

  @BeforeSuite
  public void setUp() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + AppsServiceTest.class.getSimpleName());
    // Setup for HK2 dependency injection
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure() {
        bind(AppsServiceImpl.class).to(AppsService.class);
        bind(AppsServiceImpl.class).to(AppsServiceImpl.class);
        bind(AppsDaoImpl.class).to(AppsDao.class);
        bindFactory(ServiceContextFactory.class).to(ServiceContext.class);
        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class);
      }
    });
    locator.inject(this);

    // Initialize TenantManager and services
    String url = RuntimeParameters.getInstance().getTenantsSvcURL();
    TenantManager.getInstance(url).getTenants();

    // Initialize services
    svc = locator.getService(AppsService.class);
    svcImpl = locator.getService(AppsServiceImpl.class);
    svcImpl.initService(siteId, adminTenantName, RuntimeParameters.getInstance().getServicePassword());

    // Initialize authenticated user and service
    rAdminUser = new ResourceRequestUser(new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                          null, adminUser, tenantName, null, null, null));
    rFilesSvc = new ResourceRequestUser(new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                         null, owner2, tenantName, null, null, null));
    rUser0 = new ResourceRequestUser(new AuthenticatedUser(testUser0, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser0, tenantName, null, null, null));
    rUser1 = new ResourceRequestUser(new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser1, tenantName, null, null, null));
    rUser2 = new ResourceRequestUser(new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser2, tenantName, null, null, null));
    rUser3 = new ResourceRequestUser(new AuthenticatedUser(testUser3, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser3, tenantName, null, null, null));
    rUser4 = new ResourceRequestUser(new AuthenticatedUser(testUser4, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser4, tenantName, null, null, null));
    rUser5 = new ResourceRequestUser(new AuthenticatedUser(testUser5, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser5, tenantName, null, null, null));

    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + AppsServiceTest.class.getSimpleName());
    // Remove non-owner permissions granted during the tests
    // testUserPerms
    try { svc.revokeUserPermissions(rUser1, apps[9].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(rUser1, apps[9].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    // testAuthDeny
    try { svc.revokeUserPermissions(rUser1, apps[12].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(rUser1, apps[12].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    // testAuthAllow
    try { svc.revokeUserPermissions(rUser1, apps[14].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(rUser1, apps[14].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }

    //Remove all objects created by tests
    for (int i = 0; i < numApps; i++)
    {
      svcImpl.hardDeleteApp(rAdminUser, tenantName, apps[i].getId());
    }
    svcImpl.hardDeleteApp(rAdminUser, tenantName, specialId1);

    App tmpApp = svc.getApp(rAdminUser, apps[0].getId(), apps[0].getVersion(), false);
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  @Test
  public void testCreateApp() throws Exception
  {
    App app0 = apps[0];
    svc.createApp(rUser1, app0, scrubbedJson);
    App tmpApp = svc.getApp(rUser1, app0.getId(), app0.getVersion(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());
  }

  // Create an app using minimal attributes:
  @Test
  public void testCreateAppMinimal() throws Exception
  {
    App app0 = makeMinimalApp(apps[11], apps[11].getId());
    svc.createApp(rUser1, app0, scrubbedJson);
    App tmpApp = svc.getApp(rUser1, app0.getId(), app0.getVersion(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());
    // Make sure we can create and get an app ending with "-app"
    app0 = IntegrationUtils.makeMinimalApp(app0, specialId1);
    svc.createApp(rUser1, app0, scrubbedJson);
    tmpApp = svc.getApp(rUser1, app0.getId(), app0.getVersion(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());
  }

  // Test retrieving an app.
  @Test
  public void testGetApp() throws Exception
  {
    App app0 = apps[1];
    svc.createApp(rUser1, app0, scrubbedJson);
    // Retrieve the app as filesSvc and as owner (with and without requireExecPerm)
    App tmpApp = svc.getApp(rFilesSvc, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(rUser1, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(rUser1, app0.getId(), app0.getVersion(), true);
    checkCommonAppAttrs(app0, tmpApp);
  }

  // Test updating an app using put
  // Both update of all possible attributes and only some attributes
  @Test
  public void testPutApp() throws Exception
  {
    App app0 = apps[26];
    String appId = app0.getId();
    String appVersion = app0.getVersion();
    String createText = "{\"testPut\": \"0-create1\"}";
    svc.createApp(rUser1, app0, createText);
    App tmpApp = svc.getApp(rUser1, appId, appVersion, false);
    checkCommonAppAttrs(app0, tmpApp);

    // Get last updated timestamp
    LocalDateTime updated = LocalDateTime.ofInstant(tmpApp.getUpdated(), ZoneOffset.UTC);
    String updatedStr1 = TapisUtils.getSQLStringFromUTCTime(updated);
    Thread.sleep(300);

    // Create putApp where all updatable attributes are changed
    String put1Text = "{\"testPut\": \"1-put1\"}";
    App putApp = IntegrationUtils.makePutAppFull(tmpApp);
    // Update using putApp
    svc.putApp(rUser1, putApp, put1Text);
    tmpApp = svc.getApp(rUser1, appId, appVersion, false);

    // Get last updated timestamp
    updated = LocalDateTime.ofInstant(tmpApp.getUpdated(), ZoneOffset.UTC);
    String updatedStr2 = TapisUtils.getSQLStringFromUTCTime(updated);
    // Make sure update timestamp has been modified
    System.out.println("Updated timestamp before: " + updatedStr1 + " after: " + updatedStr2);
    Assert.assertNotEquals(updatedStr1, updatedStr2, "Update timestamp was not updated. Both are: " + updatedStr1);

    // Update original app definition with patched values so we can use the checkCommon method.
    app0.setDescription(description2);
    app0.setRuntime(runtime2);
    app0.setRuntimeVersion(runtimeVersion2);
    app0.setRuntimeOptions(runtimeOptions2);
    app0.setContainerImage(containerImage2);
    app0.setMaxJobs(maxJobs2);
    app0.setMaxJobsPerUser(maxJobsPerUser2);
    app0.setStrictFileInputs(strictFileInputsTrue);
    app0.setJobDescription(jobDescription2);
    app0.setDynamicExecSystem(dynamicExecSystemFalse);
    app0.setExecSystemConstraints(execSystemConstraints2);
    app0.setExecSystemId(execSystemId2);
    app0.setExecSystemExecDir(execSystemExecDir2);
    app0.setExecSystemInputDir(execSystemInputDir2);
    app0.setExecSystemOutputDir(execSystemOutputDir2);
    app0.setExecSystemLogicalQueue(execSystemLogicalQueue2);
    app0.setArchiveSystemId(archiveSystemId2);
    app0.setArchiveSystemDir(archiveSystemDir2);
    app0.setArchiveOnAppError(archiveOnAppErrorFalse);
    app0.setParameterSet(parameterSet2);
    app0.setFileInputs(finList2);
    app0.setNodeCount(nodeCount2);
    app0.setCoresPerNode(coresPerNode2);
    app0.setMemoryMb(memoryMb2);
    app0.setMaxMinutes(maxMinutes2);
    app0.setSubscriptions(notifList2);
    app0.setJobTags(jobTags2);
    app0.setTags(tags2);
    app0.setNotes(notes2);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpApp);

  }

  // Test updating an app using patch
  // Both update of all possible attributes and only some attributes
  @Test
  public void testPatchApp() throws Exception
  {
    // Test updating all attributes that can be updated.
    App app0 = apps[13];
    String appId = app0.getId();
    String appVersion = app0.getVersion();
    String createText = "{\"testPatch\": \"0-createFull\"}";
    svc.createApp(rUser1, app0, createText);
    App tmpApp = svc.getApp(rUser1, appId, appVersion, false);
    // Get last updated timestamp
    LocalDateTime updated = LocalDateTime.ofInstant(tmpApp.getUpdated(), ZoneOffset.UTC);
    String updatedStr1 = TapisUtils.getSQLStringFromUTCTime(updated);
    Thread.sleep(300);
    // ===========================================================
    // Create patchApp where all updatable attributes are changed
    // ===========================================================
    String patchFullText = "{\"testPatch\": \"1-patchFull\"}";
    PatchApp patchAppFull = IntegrationUtils.makePatchAppFull();
    // Update using patchApp
    svc.patchApp(rUser1, appId, appVersion, patchAppFull, patchFullText);
    App tmpAppFull = svc.getApp(rUser1, appId, appVersion, false);
    // Get last updated timestamp
    updated = LocalDateTime.ofInstant(tmpAppFull.getUpdated(), ZoneOffset.UTC);
    String updatedStr2 = TapisUtils.getSQLStringFromUTCTime(updated);
    // Make sure update timestamp has been modified
    System.out.println("Updated timestamp before: " + updatedStr1 + " after: " + updatedStr2);
    Assert.assertNotEquals(updatedStr1, updatedStr2, "Update timestamp was not updated. Both are: " + updatedStr1);
    // Update original app definition with patched values so we can use the checkCommon method.
    app0.setDescription(description2);
    app0.setRuntime(runtime2);
    app0.setRuntimeVersion(runtimeVersion2);
    app0.setRuntimeOptions(runtimeOptions2);
    app0.setContainerImage(containerImage2);
    app0.setMaxJobs(maxJobs2);
    app0.setMaxJobsPerUser(maxJobsPerUser2);
    app0.setStrictFileInputs(strictFileInputsTrue);
    app0.setJobDescription(jobDescription2);
    app0.setDynamicExecSystem(dynamicExecSystemFalse);
    app0.setExecSystemConstraints(execSystemConstraints2);
    app0.setExecSystemId(execSystemId2);
    app0.setExecSystemExecDir(execSystemExecDir2);
    app0.setExecSystemInputDir(execSystemInputDir2);
    app0.setExecSystemOutputDir(execSystemOutputDir2);
    app0.setExecSystemLogicalQueue(execSystemLogicalQueue2);
    app0.setArchiveSystemId(archiveSystemId2);
    app0.setArchiveSystemDir(archiveSystemDir2);
    app0.setArchiveOnAppError(archiveOnAppErrorFalse);
    app0.setParameterSet(parameterSet2);
    app0.setFileInputs(finList2);
    app0.setNodeCount(nodeCount2);
    app0.setCoresPerNode(coresPerNode2);
    app0.setMemoryMb(memoryMb2);
    app0.setMaxMinutes(maxMinutes2);
    app0.setSubscriptions(notifList2);
    app0.setJobTags(jobTags2);
    app0.setTags(tags2);
    app0.setNotes(notes2);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppFull);

    // ===========================================================
    // Test updating just a few attributes
    // ===========================================================
    app0 = apps[22];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testPatch\": \"0-createPartial1\"}";
    svc.createApp(rUser1, app0, createText);
    // Create patchApp where some attributes are changed
    //   * Some attributes are to be updated: description, containerImage, execSystemId,
    String patchPartialText1 = "{\"testPatch\": \"1-patchPartial1\"}";
    PatchApp patchAppPartial1 = IntegrationUtils.makePatchAppPartial1();
    // Update using patchApp
    svc.patchApp(rUser1, appId, appVersion, patchAppPartial1, patchPartialText1);
    App tmpAppPartial = svc.getApp(rUser1, appId, appVersion, false);
    // Update original app definition with patched values
    app0.setDescription(description2);
    app0.setContainerImage(containerImage2);
    app0.setExecSystemId(execSystemId2);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);

    // ===========================================================
    // Test updating a few more attributes including a collection in JobAttributes
    //   and a collection in JobAttributes.ParameterSet.
    //   jobAttributes.fileInputs, jobAttributes.parameterSet.containerArgs
    // ===========================================================
    app0 = apps[23];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testPatch\": \"0-createPartial2\"}";
    svc.createApp(rUser1, app0, createText);
    // Create patchApp where some attributes are changed
    String patchPartialText2 = "{\"testPatch\": \"1-patchPartial2\"}";
    PatchApp patchAppPartial2 = IntegrationUtils.makePatchAppPartial2();
    // Update using patchApp
    svc.patchApp(rUser1, appId, appVersion, patchAppPartial2, patchPartialText2);
    tmpAppPartial = svc.getApp(rUser1, appId, appVersion, false);
    // Update original app definition with patched values
    app0.setDescription(description2);
    app0.setContainerImage(containerImage2);
    app0.setExecSystemId(execSystemId2);
    app0.getParameterSet().setContainerArgs(containerArgList3);
    app0.setFileInputs(finList3);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);

    // ===========================================================
    // Test updating just one of the collections in JobAttributes.ParameterSet.
    //   jobAttributes.parameterSet.appArgs
    // ===========================================================
    app0 = apps[24];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testPatch\": \"0-createPartial3\"}";
    svc.createApp(rUser1, app0, createText);
    // Create patchApp where some attributes are changed
    String patchPartialText3 = "{\"testPatch\": \"1-patchPartial3\"}";
    PatchApp patchAppPartial3 = IntegrationUtils.makePatchAppPartial3();
    // Update using patchApp
    svc.patchApp(rUser1, appId, appVersion, patchAppPartial3, patchPartialText3);
    tmpAppPartial = svc.getApp(rUser1, appId, appVersion, false);
    // Update original app definition with patched values
    app0.getParameterSet().setAppArgs(appArgList3);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);
  }

  // Test changing app owner
  @Test
  public void testChangeAppOwner() throws Exception
  {
    App app0 = apps[15];
    String createText = "{\"testChangeOwner\": \"0-create\"}";
    String origOwnerName = testUser1;
    String newOwnerName = testUser3;
    ResourceRequestUser origOwnerAuth = rUser1;
    ResourceRequestUser newOwnerAuth = rUser3;

    svc.createApp(origOwnerAuth, app0, createText);
    App tmpApp = svc.getApp(origOwnerAuth, app0.getId(), app0.getVersion(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());

    // Change owner using api
    svc.changeAppOwner(origOwnerAuth, app0.getId(), newOwnerName);

    // Confirm new owner
    tmpApp = svc.getApp(newOwnerAuth, app0.getId(), app0.getVersion(), false);
    Assert.assertEquals(tmpApp.getOwner(), newOwnerName);

    // Check expected auxiliary updates have happened
    // New owner should be able to retrieve permissions and have all permissions
    Set<Permission> userPerms = svc.getUserPermissions(newOwnerAuth, app0.getId(), newOwnerName);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    for (Permission perm : Permission.values())
    {
      Assert.assertTrue(userPerms.contains(perm));
    }
    // Original owner should no longer have the modify permission
    userPerms = svc.getUserPermissions(newOwnerAuth, app0.getId(), origOwnerName);
    Assert.assertFalse(userPerms.contains(Permission.MODIFY));
    // Original owner should not be able to modify app
    try {
      svc.deleteApp(origOwnerAuth, app0.getId());
      Assert.fail("Original owner should not have permission to update app after change of ownership. App name: " + app0.getId() +
              " Old owner: " + origOwnerName + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
    }
    // Original owner should not be able to read system
    try {
      svc.getApp(origOwnerAuth, app0.getId(), app0.getVersion(), false);
      Assert.fail("Original owner should not have permission to read app after change of ownership. App name: " + app0.getId() +
              " Old owner: " + origOwnerName + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
    }
  }

//  // Check that when an app is created variable substitution is correct for:
//  //   owner, bucketName, rootDir, ...
//  // And when app is retrieved effectiveUserId is resolved
//  @Test
//  public void testGetAppVariables() throws Exception
//  {
//    App app0 = apps[7];//5
//    app0.setOwner("${apiUserId}");
//    int appVerSeqId = svc.createApp(authenticatedOwnerUser1, app0, scrubbedJson);
//    Assert.assertTrue(appVerSeqId > 0, "Invalid appVerSeqId: " + appVerSeqId);
//    App tmpApp = svc.getApp(authenticatedOwnerUser1, app0.getId(), app0.getVersion(), false);
//    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
//    System.out.println("Found item: " + app0.getId());
//
//// app8 = {tenantName, "Sapp8", "description 8", AppType.LINUX.name(), "${apiUserId}", "host8",
////         "${owner}", prot1AccessMethName, "fakePassword8", "bucket8-${tenant}-${apiUserId}", "/root8/${tenant}", prot1TxfrMethods,
////         "jobLocalWorkDir8/${owner}/${tenant}/${apiUserId}", "jobLocalArchDir8/${apiUserId}", "jobRemoteArchApp8",
////         "jobRemoteArchDir8${owner}${tenant}${apiUserId}", tags, notes, "{}"};
//    Assert.assertEquals(tmpApp.getId(), app0.getId());
//    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
//    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
//    Assert.assertEquals(tmpApp.getOwner(), ownerUser);
//    Assert.assertEquals(tmpApp.isEnabled(), app0.isEnabled());
//  }

  @Test
  public void testGetAppIDs() throws Exception
  {
    App app0 = apps[2];
    svc.createApp(rUser1, app0, scrubbedJson);
    app0 = apps[3];
    svc.createApp(rUser1, app0, scrubbedJson);
    Set<String> appIDs = svc.getAppIDs(rUser1, showDeletedFalse);
    for (String name : appIDs)
    {
      System.out.println("Found item: " + name);
    }
    Assert.assertTrue(appIDs.contains(apps[2].getId()), "List of apps did not contain app name: " + apps[2].getId());
    Assert.assertTrue(appIDs.contains(apps[3].getId()), "List of apps did not contain app name: " + apps[3].getId());
  }

  @Test
  public void testGetApps() throws Exception
  {
    App app0 = apps[4];
    svc.createApp(rUser1, app0, scrubbedJson);
    List<App> apps = svc.getApps(rUser1, null, -1, null, -1, null, showDeletedFalse);
    for (App app : apps)
    {
      System.out.println("Found item with id: " + app.getId() + " and version: " + app.getVersion());
    }
  }

  // Check that user only sees apps they are authorized to see.
  @Test
  public void testGetAppsAuth() throws Exception
  {
    // Create 3 apps, 2 of which are owned by testUser5.
    App app0 = apps[16];
    String app1Name = app0.getId();
    app0.setOwner(rUser5.getOboUserId());
    svc.createApp(rUser5, app0, scrubbedJson);

    app0 = apps[17];
    String app2Name = app0.getId();
    app0.setOwner(rUser5.getOboUserId());
    svc.createApp(rUser5, app0, scrubbedJson);

    app0 = apps[18];
    svc.createApp(rUser1, app0, scrubbedJson);

    // When retrieving apps as testUser5 only 2 should be returned
    List<App> apps = svc.getApps(rUser5, searchListNull, -1, orderByListNull, -1, startAfterNull, showDeletedFalse);
    System.out.println("Total number of apps retrieved: " + apps.size());
    Assert.assertEquals(apps.size(), 2);
    for (App app : apps)
    {
      System.out.println("Found item with appId: " + app.getId() + " and appVer: " + app.getVersion());
      Assert.assertTrue(app.getId().equals(app1Name) || app.getId().equalsIgnoreCase(app2Name));
    }
  }

  // Check enable/disable/delete/undelete as well as isEnabled
  // When resource deleted isEnabled should throw a NotFound exception
  @Test
  public void testEnableDisableDeleteUndelete() throws Exception
  {
    // Create the app
    App app0 = apps[20];
    String appId = app0.getId();
    String appVer = app0.getVersion();
    svc.createApp(rUser1, app0, scrubbedJson);
    // Enabled should start off true, then become false and finally true again.
    App tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertTrue(tmpApp.isEnabled());
    Assert.assertTrue(svc.isEnabled(rUser1, appId));
    int changeCount = svc.disableApp(rUser1, appId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertFalse(tmpApp.isEnabled());
    Assert.assertFalse(svc.isEnabled(rUser1, appId));
    changeCount = svc.enableApp(rUser1, appId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertTrue(tmpApp.isEnabled());
    Assert.assertTrue(svc.isEnabled(rUser1, appId));

    // Deleted should start off false, then become true and finally false again.
    tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertFalse(tmpApp.isDeleted());
    changeCount = svc.deleteApp(rUser1, appId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertNull(tmpApp);
    changeCount = svc.undeleteApp(rUser1, appId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(rUser1, appId, appVer, false);
    Assert.assertFalse(tmpApp.isDeleted());

    // When deleted isEnabled should throw NotFound exception
    svc.deleteApp(rUser1, appId);
    boolean pass = false;
    try { svc.isEnabled(rUser1, appId); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  @Test
  public void testAppExists() throws Exception
  {
    // If app not there we should get false
    Assert.assertFalse(svc.checkForApp(rUser1, apps[6].getId()));
    // After creating app we should get true
    App app0 = apps[6];
    svc.createApp(rUser1, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(rUser1, apps[6].getId()));
  }

  // Check that if apps already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^APPLIB_APP_EXISTS.*")
  public void testCreateAppAlreadyExists() throws Exception
  {
    // Create the app
    App app0 = apps[8];
    svc.createApp(rUser1, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(rUser1, app0.getId()));
    // Now attempt to create again, should get IllegalStateException with msg APPLIB_APP_EXISTS
    svc.createApp(rUser1, app0, scrubbedJson);
  }

  // Check that reserved names are honored.
  // Because of endpoints certain IDs should not be allowed: healthcheck, readycheck, search
  @Test
  public void testReservedNames() throws Exception
  {
    App app0 = apps[19];
    for (String id : App.RESERVED_ID_SET)
    {
      System.out.println("Testing that create fails for reserved ID: " + id);
      App tmpApp = IntegrationUtils.makeMinimalApp(app0, id);
      System.out.println("  - Created in-memory app object with ID: " + tmpApp.getId());
      try
      {
        svc.createApp(rUser1, tmpApp, scrubbedJson);
        Assert.fail("App create call should have thrown an exception when using a reserved ID. Id: " + id);
      } catch (IllegalStateException e)
      {
        Assert.assertTrue(e.getMessage().contains("APPLIB_CREATE_RESERVED"));
      }
    }
  }

  // Test that attempting to create an app with invalid attribute combinations fails
  // Note that these checks are in addition to other similar tests: testReservedNames, testCheckSystemsInvalid
  // - If containerized is true then containerImage must be set
  // - If containerized and SINGULARITY then RuntimeOptions must include one of SINGULARITY_START or SINGULARITY_RUN
  // - If dynamicExecSystem then execSystemConstraints must be given
  // - If not dynamicExecSystem then execSystemId must be given
  // - If archiveSystem given then archive dir must be given
  // - Validation of queue limits: NodeCount, CoresPerNode, MemoryMB, MaxMinutes
  @Test
  public void testCreateInvalidMiscFail()
  {
    App app0 = apps[25];
    // If containerized is true then containerImage must be set
    app0.setContainerImage(null);
    boolean pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_CONTAINERIZED_NOIMAGE"));
      pass = true;
    }
    Assert.assertTrue(pass);
    // Reset in prep for continued checking
    app0.setContainerImage(containerImage1);

    // If containerized and SINGULARITY then RuntimeOptions must include one of SINGULARITY_START or SINGULARITY_RUN
    app0.setRuntime(Runtime.SINGULARITY);
    app0.setRuntimeOptions(runtimeOptionsSingNeither);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_CONTAINERIZED_SING_OPT"));
      pass = true;
    }
    Assert.assertTrue(pass);
    app0.setRuntimeOptions(runtimeOptionsSingBoth);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_CONTAINERIZED_SING_OPT"));
      pass = true;
    }
    Assert.assertTrue(pass);
    // Reset in prep for continued checking
    app0.setRuntimeOptions(runtimeOptions1);
    app0.setRuntime(runtime1);

    // If dynamicExecSystem then execSystemConstraints must be given
    app0.setExecSystemConstraints(null);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_DYNAMIC_NOCONSTRAINTS"));
      pass = true;
    }
    Assert.assertTrue(pass);
    // Reset in prep for continued checking
    app0.setExecSystemConstraints(execSystemConstraints1);

    // If not dynamicExecSystem then execSystemId must be given
    app0.setDynamicExecSystem(false);
    app0.setExecSystemId(null);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_NOTDYNAMIC_NOSYSTEMID"));
      pass = true;
    }
    Assert.assertTrue(pass);
    // Reset in prep for continued checking
    app0.setExecSystemId(execSystemId1);
    app0.setDynamicExecSystem(true);

    // If archiveSystem given then archive dir must be given
    // Note we save dir name because it varies with each test app.
    String tmpArchiveSystemDir = app0.getArchiveSystemDir();
    app0.setArchiveSystemDir(null);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_ARCHIVE_NODIR"));
      pass = true;
    }
    Assert.assertTrue(pass);
    // Reset in prep for continued checking
    app0.setArchiveSystemDir(tmpArchiveSystemDir);

    // Various LogicalQueue related attributes must be in range defined for ExecSystemsLogicalQueue
    // Start with maxNodeCount.
    //   Each subsequent value that is out of range should be cumulative in the list of error messages.
    // Save original values from app0 so we can reset and re-use app0 later.
    String tmpExecSystemLogicalQueue = app0.getExecSystemLogicalQueue();
    int tmpNodeCount =  app0.getNodeCount();
    int tmpCoresPerNode =  app0.getCoresPerNode();
    int tmpMemoryMB =  app0.getMemoryMb();
    int tmpMaxMinutes =  app0.getMaxMinutes();
    // Update logical queue
    app0.setExecSystemLogicalQueue(execSystemLogicalQueue1); // queue defined on execSystemId (id: tapisv3-exec3, queue dsnormal)

    // Check Max limits: nodeCount, coresPerNode, memoryMB, maxMinutes
    app0.setNodeCount(nodeCount2 + 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: NodeCount"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setCoresPerNode(coresPerNode2 + 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: CoresPerNode"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setMemoryMb(memoryMb2 + 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: CoresPerNode"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: MemoryMB"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setMaxMinutes(maxMinutes2 + 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: CoresPerNode"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: MemoryMB"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_HIGH Value exceeds upper limit for LogicalQueue. Attribute: MaxMinutes"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // Reset in prep for continued checking
    app0.setNodeCount(tmpNodeCount);
    app0.setCoresPerNode(tmpCoresPerNode);
    app0.setMemoryMb(tmpMemoryMB);
    app0.setMaxMinutes(tmpMaxMinutes);

    // Check Min limits: nodeCount, coresPerNode, memoryMB, maxMinutes
    app0.setNodeCount(nodeCount1 - 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: NodeCount"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setCoresPerNode(coresPerNode1 - 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: CoresPerNode"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setMemoryMb(memoryMb1 - 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: CoresPerNode"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: MemoryMB"));
      pass = true;
    }
    Assert.assertTrue(pass);

    app0.setMaxMinutes(maxMinutes1 - 1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: NodeCount"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: CoresPerNode"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: MemoryMB"));
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECQ_LIMIT_LOW Value exceeds lower limit for LogicalQueue. Attribute: MaxMinutes"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // Reset in prep for continued checking
    app0.setNodeCount(tmpNodeCount);
    app0.setCoresPerNode(tmpCoresPerNode);
    app0.setMemoryMb(tmpMemoryMB);
    app0.setMaxMinutes(tmpMaxMinutes);

    // Reset in prep for continued checking
    // NOTE: Below resets not necessary but if we add to this method they should be done.
    // Reset app attribute values
    app0.setNodeCount(tmpNodeCount);
    app0.setCoresPerNode(tmpCoresPerNode);
    app0.setMemoryMb(tmpMemoryMB);
    app0.setMaxMinutes(tmpMaxMinutes);
    app0.setExecSystemLogicalQueue(tmpExecSystemLogicalQueue);
  }

  // Test creating, reading and deleting user permissions for an app
  @Test
  public void testUserPerms() throws Exception
  {
    // Create an app
    App app0 = apps[9];
    svc.createApp(rUser1, app0, scrubbedJson);
    // Create user perms for the app
    Set<Permission> permsToCheck = testPermsALL;
    svc.grantUserPermissions(rUser1, app0.getId(), testUser4, permsToCheck, scrubbedJson);
    // Get the app perms for the user and make sure permissions are there
    Set<Permission> userPerms = svc.getUserPermissions(rUser1, app0.getId(), testUser4);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    Assert.assertEquals(userPerms.size(), permsToCheck.size(), "Incorrect number of perms returned.");
    for (Permission perm: permsToCheck) { if (!userPerms.contains(perm)) Assert.fail("User perms should contain permission: " + perm.name()); }
    // Remove perms for the user. Should return a change count of 2
    int changeCount = svc.revokeUserPermissions(rUser1, app0.getId(), testUser4, permsToCheck, scrubbedJson);
    Assert.assertEquals(changeCount, permsToCheck.size(), "Change count incorrect when revoking permissions.");
    // Get the app perms for the user and make sure permissions are gone.
    userPerms = svc.getUserPermissions(rUser1, app0.getId(), testUser4);
    for (Permission perm: permsToCheck) { if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm.name()); }

    // Owner should not be able to update perms for themselves. Could be confusing since owner always authorized. Perms not checked.
    boolean pass = false;
    try {
      svc.grantUserPermissions(rUser1, app0.getId(), app0.getOwner(), testPermsREAD, scrubbedJson);
      Assert.fail("Update of perms by owner for owner should have thrown an exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("APPLIB_PERM_OWNER_UPDATE"));
      pass = true;
    }
    Assert.assertTrue(pass, "Update of perms by owner for owner did not throw correct exception");
    pass = false;
    try {
      svc.revokeUserPermissions(rUser1, app0.getId(), app0.getOwner(), testPermsREAD, scrubbedJson);
      Assert.fail("Update of perms by owner for owner should have thrown an exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("APPLIB_PERM_OWNER_UPDATE"));
      pass = true;
    }
    Assert.assertTrue(pass, "Update of perms by owner for owner did not throw correct exception");

    // Give testuser3 back some perms so we can test revokePerms auth when user is not the owner and is target user
    svc.grantUserPermissions(rUser1, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson);

    // Have testuser3 remove their own perms. Should return a change count of 2
    changeCount = svc.revokeUserPermissions(rUser3, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 2, "Change count incorrect when revoking permissions as user - not owner.");
    // Get the system perms for the user and make sure permissions are gone.
    userPerms = svc.getUserPermissions(rUser1, app0.getId(), testUser3);
    for (Permission perm: testPermsREADMODIFY) { if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm.name()); }

    // Give testuser3 back some perms so we can test revokePerms auth when user is not the owner and is not target user
    svc.grantUserPermissions(rUser1, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson);
    try {
      svc.revokeUserPermissions(rUser2, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson);
      Assert.fail("Update of perms by non-owner user who is not target user should have thrown an exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("APPLIB_UNAUTH"));
    }
  }

  // Test various cases when app is missing
  //  - isEnabled
  //  - get owner with no app
  //  - get perm with no app
  //  - grant perm with no app
  //  - revoke perm with no app
  @Test
  public void testMissingApp() throws Exception
  {
    String fakeAppName = "AMissingAppName";
    String fakeAppVersion = "AMissingAppVersion";
    String fakeUserName = "AMissingUserName";
    int changeCount;
    boolean pass;
    // Make sure app does not exist
    Assert.assertFalse(svc.checkForApp(rUser1, fakeAppName, true));

    // Get App with no app should return null
    App tmpApp = svc.getApp(rUser1, fakeAppName, fakeAppVersion, false);
    Assert.assertNull(tmpApp, "App not null for non-existent app");

    // Delete app with no app should throw NotFound exception
    pass = false;
    try { svc.deleteApp(rUser1, fakeAppName); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);

    // isEnabled check with no resource should throw a NotFound exception
    pass = false;
    try { svc.isEnabled(rUser1, fakeAppName); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);

    // Get owner with no app should return null
    String owner = svc.getAppOwner(rUser1, fakeAppName);
    Assert.assertNull(owner, "Owner not null for non-existent app.");

    // Get perms with no app should return null
    Set<Permission> perms = svc.getUserPermissions(rUser1, fakeAppName, fakeUserName);
    Assert.assertNull(perms, "Perms list was not null for non-existent app");

    // Revoke perm with no app should return 0 changes
    changeCount = svc.revokeUserPermissions(rUser1, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when revoking perms for non-existent app.");

    // Grant perm with no app should throw an exception
    pass = false;
    try { svc.grantUserPermissions(rUser1, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  // Test that app cannot be created when execSystem or archiveSystem is missing or invalid
  @Test
  public void testCheckSystemsInvalid()
  {
    String fakeSysName = "AMissingSystemName";
    App app0 = apps[21];

    // Create should fail when execSystemId does not exist
    app0.setExecSystemId(fakeSysName);
    boolean pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECSYS_NO_SYSTEM"));
      pass = true;
    }
    Assert.assertTrue(pass);
    app0.setExecSystemId(execSystemId1);

    // Create should fail when archiveSystemId does not exist
    app0.setArchiveSystemId(fakeSysName);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_ARCHSYS_NO_SYSTEM"));
      pass = true;
    }
    Assert.assertTrue(pass);
    app0.setArchiveSystemId(archiveSystemId1);

    // Create should fail when execSystemId cannot exec
    app0.setExecSystemId(archiveSystemId1);
    pass = false;
    try { svc.createApp(rUser1, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECSYS_NOT_EXEC"));
      pass = true;
    }
    Assert.assertTrue(pass);
    app0.setExecSystemId(execSystemId1);
  }

  // Test Auth denials
  // testUser1 - owner
  // testUser0 - no perms
  // testUser3 - READ perm
  // testUser4 - MODIFY perm
  @Test
  public void testAuthDeny() throws Exception
  {
    App app0 = apps[12];
    String app0Id = app0.getId();
    String app0Version = app0.getVersion();
    PatchApp patchApp = IntegrationUtils.makePatchAppFull();
    // CREATE - Deny user not owner/admin, deny service
    boolean pass = false;
    try { svc.createApp(rUser0, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.createApp(rFilesSvc, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // Create an app and grant permissions
    svc.createApp(rUser1, app0, scrubbedJson);
    // Grant User3 - READ and User4 - MODIFY
    svc.grantUserPermissions(rUser1, app0Id, testUser3, testPermsREAD, scrubbedJson);
    svc.grantUserPermissions(rUser1, app0Id, testUser4, testPermsMODIFY, scrubbedJson);

    // READ - deny user not owner/admin and no READ or MODIFY access (testuser0)
    pass = false;
    try { svc.getApp(rUser0, app0Id, app0Version, false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // EXECUTE - deny user not owner/admin with READ but not EXECUTE (testuser3)
    pass = false;
    try { svc.getApp(rUser3, app0Id, app0Version, true); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // MODIFY Deny user with no READ or MODIFY (testuser0), deny user with only READ (testuser3), deny service
    pass = false;
    try { svc.patchApp(rUser0, app0Id, app0Version, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.patchApp(rUser3, app0Id, app0Version, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.patchApp(rFilesSvc, app0Id, app0Version, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // DELETE - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.deleteApp(rUser3, app0Id); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.deleteApp(rFilesSvc, app0Id); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // CHANGE_OWNER - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.changeAppOwner(rUser3, app0Id, testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.changeAppOwner(rFilesSvc, app0Id, testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GET_PERMS - deny user not owner/admin and no READ or MODIFY access (testuser0)
    pass = false;
    try { svc.getUserPermissions(rUser0, app0Id, testUser1); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GRANT_PERMS - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.grantUserPermissions(rUser3, app0Id, testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(rFilesSvc, app0Id, testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // REVOKE_PERMS - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.revokeUserPermissions(rUser3, app0Id, testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(rFilesSvc, app0Id, testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  // Test Auth allow
  // Many cases covered during other tests
  // Test special cases here:
  //    MODIFY implies READ

  // testUser1 - owner
  // testUser0 - no perms
  // testUser3 - READ,EXECUTE perm
  // testUser4 - MODIFY perm
  @Test
  public void testAuthAllow() throws Exception
  {
    App app0 = apps[14];
    // Create app for remaining auth access tests
    svc.createApp(rUser1, app0, scrubbedJson);
    // Grant User3 - READ,EXECUTE and User4 - MODIFY
    svc.grantUserPermissions(rUser1, app0.getId(), testUser3, testPermsREADEXECUTE, scrubbedJson);
    svc.grantUserPermissions(rUser1, app0.getId(), testUser4, testPermsMODIFY, scrubbedJson);

    // READ - allow owner, service, with READ only, with MODIFY only
    svc.getApp(rUser1, app0.getId(), app0.getVersion(), false);
    svc.getApp(rUser1, app0.getId(), app0.getVersion(), true);
    svc.getApp(rFilesSvc, app0.getId(), app0.getVersion(), false);
    svc.getApp(rUser3, app0.getId(), app0.getVersion(), false);
    svc.getApp(rUser3, app0.getId(), app0.getVersion(), true);
    svc.getApp(rUser4, app0.getId(), app0.getVersion(), false);
  }

  /**
   * Check common attributes after creating and retrieving an app
   * @param app0 - Test app
   * @param tmpApp - Retrieved app
   */
  private static void checkCommonAppAttrs(App app0, App tmpApp)
  {
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());
    Assert.assertEquals(tmpApp.getTenant(), app0.getTenant());
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
    List<RuntimeOption> app0RTOptions = app0.getRuntimeOptions();
    Assert.assertNotNull(app0RTOptions);
    for (RuntimeOption rtOption : app0RTOptions)
    {
      Assert.assertTrue(rtOps.contains(rtOption), "List of runtime options did not contain: " + rtOption.name());
    }
    Assert.assertEquals(tmpApp.getContainerImage(), app0.getContainerImage());
    Assert.assertEquals(tmpApp.getMaxJobs(), app0.getMaxJobs());
    Assert.assertEquals(tmpApp.getMaxJobsPerUser(), app0.getMaxJobsPerUser());
    Assert.assertEquals(tmpApp.isStrictFileInputs(), app0.isStrictFileInputs());

    // ========== JobAttributes
    Assert.assertEquals(tmpApp.getJobDescription(), app0.getJobDescription());
    Assert.assertEquals(tmpApp.isDynamicExecSystem(), app0.isDynamicExecSystem());
    // Verify execSystemConstraints
    String[] origExecSystemConstraints = app0.getExecSystemConstraints();
    String[] tmpExecSystemConstraints = tmpApp.getExecSystemConstraints();
    Assert.assertNotNull(tmpExecSystemConstraints, "execSystemConstraints value was null");
    var execSystemConstraintsList = Arrays.asList(tmpExecSystemConstraints);
    Assert.assertEquals(tmpExecSystemConstraints.length, origExecSystemConstraints.length, "Wrong number of constraints");
    for (String execSystemConstraintStr : origExecSystemConstraints)
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
    ArchiveFilter tmpArchiveFilter = parmSet.getArchiveFilter();
    ArchiveFilter archiveFilter0 = parmSet0.getArchiveFilter();
    Assert.assertNotNull(tmpArchiveFilter, "archiveFilter was null");
    Assert.assertEquals(tmpArchiveFilter.isIncludeLaunchFiles(), archiveFilter0.isIncludeLaunchFiles());
    // Verify archiveIncludes
    String[] tmpArchiveIncludes = tmpArchiveFilter.getIncludes();
    Assert.assertNotNull(tmpArchiveIncludes, "archiveIncludes value was null");
    var archiveIncludesList = Arrays.asList(tmpArchiveIncludes);
    Assert.assertEquals(tmpArchiveIncludes.length, archiveFilter0.getIncludes().length, "Wrong number of archiveIncludes");
    for (String archiveIncludeStr : archiveFilter0.getIncludes())
    {
      Assert.assertTrue(archiveIncludesList.contains(archiveIncludeStr));
      System.out.println("Found archiveInclude: " + archiveIncludeStr);
    }
    // Verify archiveExcludes
    String[] tmpArchiveExcludes = tmpArchiveFilter.getExcludes();
    Assert.assertNotNull(tmpArchiveExcludes, "archiveExcludes value was null");
    var archiveExcludesList = Arrays.asList(tmpArchiveExcludes);
    Assert.assertEquals(tmpArchiveExcludes.length, archiveFilter0.getExcludes().length, "Wrong number of archiveExcludes");
    for (String archiveExcludeStr : archiveFilter0.getExcludes())
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
    String[] origJobTags = app0.getJobTags();
    String[] tmpJobTags = tmpApp.getJobTags();
    Assert.assertNotNull(tmpJobTags, "JobTags value was null");
    var jobTagsList = Arrays.asList(tmpJobTags);
    Assert.assertEquals(tmpJobTags.length, origJobTags.length, "Wrong number of jobTags");
    for (String jobTagStr : origJobTags)
    {
      Assert.assertTrue(jobTagsList.contains(jobTagStr));
      System.out.println("Found jobTag: " + jobTagStr);
    }
    // Verify tags
    String[] origTags = app0.getTags();
    String[] tmpTags = tmpApp.getTags();
    Assert.assertNotNull(origTags, "Orig Tags should not be null");
    Assert.assertNotNull(tmpTags, "Fetched Tags value should not be null");
    var tagsList = Arrays.asList(tmpTags);
    Assert.assertEquals(tmpTags.length, origTags.length, "Wrong number of tags.");
    for (String tagStr : origTags)
    {
      Assert.assertTrue(tagsList.contains(tagStr));
      System.out.println("Found tag: " + tagStr);
    }
    // Verify notes
    Assert.assertNotNull(app0.getNotes(), "Orig Notes should not be null");
    Assert.assertNotNull(tmpApp.getNotes(), "Fetched Notes should not be null");
    System.out.println("Found notes: " + app0.getNotes().toString());
    JsonObject tmpObj = (JsonObject) tmpApp.getNotes();
    JsonObject origNotes = (JsonObject) app0.getNotes();
    Assert.assertTrue(tmpObj.has("project"));
    String projStr = origNotes.get("project").getAsString();
    Assert.assertEquals(tmpObj.get("project").getAsString(), projStr);
    Assert.assertTrue(tmpObj.has("testdata"));
    String testdataStr = origNotes.get("testdata").getAsString();
    Assert.assertEquals(tmpObj.get("testdata").getAsString(), testdataStr);

    // ===============================================================================================
    // Verify data in aux tables: notification_subscriptions
    // ===============================================================================================
    // Verify notification subscriptions
    List<NotifSubscription> origNotificationSubs = app0.getSubscriptions();
    List<NotifSubscription> tmpSubs = tmpApp.getSubscriptions();
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
}
