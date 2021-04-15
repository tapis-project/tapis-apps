package edu.utexas.tacc.tapis.apps.service;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
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
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import javax.ws.rs.NotAuthorizedException;
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
 * Applications are mostly owned by testuser2
 *   testuser1, testuser3 and testuser4 are also used
 */
@Test(groups={"integration"})
public class AppsServiceTest
{
  private AppsService svc;
  private AppsServiceImpl svcImpl;
  private AuthenticatedUser authenticatedTestUser0, authenticatedTestUser1,
          authenticatedTestUser2, authenticatedTestUser3, authenticatedTestUser4,
          authenticatedAdminUser, authenticatedFilesSvc;
  // Test data
  private static final String filesSvcName = "files";
  private static final String adminUser = "testuser9";
//TODO  private static final String adminUser = "admin";
  private static final String siteId = "tacc";
  private static final String adminTenantName = "admin";
  private static final String testUser0 = "testuser0";
  private static final String testUser1 = "testuser1";
  private static final String testUser2 = "testuser2";
  private static final String testUser3 = "testuser3";
  private static final String testUser4 = "testuser4";
  private static final Set<Permission> testPermsALL = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY, Permission.EXECUTE));
  private static final Set<Permission> testPermsREADMODIFY = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final Set<Permission> testPermsREADEXECUTE = new HashSet<>(Set.of(Permission.READ, Permission.EXECUTE));
  private static final Set<Permission> testPermsREAD = new HashSet<>(Set.of(Permission.READ));
  private static final Set<Permission> testPermsMODIFY = new HashSet<>(Set.of(Permission.MODIFY));

  // Create test app definitions in memory
  int numApps = 25;
  App[] apps = IntegrationUtils.makeApps(numApps, "Svc");

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
    authenticatedAdminUser = new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                    null, adminUser, tenantName, null, null, null);
    authenticatedFilesSvc = new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                                  null, ownerUser2, tenantName, null, null, null);
    authenticatedTestUser0 = new AuthenticatedUser(testUser0, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser0, tenantName, null, null, null);
    authenticatedTestUser1 = new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser1, tenantName, null, null, null);
    authenticatedTestUser2 = new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser2, tenantName, null, null, null);
    authenticatedTestUser3 = new AuthenticatedUser(testUser3, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser3, tenantName, null, null, null);
    authenticatedTestUser4 = new AuthenticatedUser(testUser4, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser4, tenantName, null, null, null);

    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + AppsServiceTest.class.getSimpleName());
    // Remove non-owner permissions granted during the tests
    // testUserPerms
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[9].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[9].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    // testAuthDeny
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[12].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[12].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    // testAuthAllow
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[14].getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedTestUser2, apps[14].getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }

    //Remove all objects created by tests
    for (int i = 0; i < numApps; i++)
    {
      svcImpl.hardDeleteApp(authenticatedAdminUser, apps[i].getId());
    }

    App tmpApp = svc.getApp(authenticatedAdminUser, apps[0].getId(), apps[0].getVersion(), false);
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  @Test
  public void testCreateApp() throws Exception
  {
    App app0 = apps[0];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
  }

  // Create an app using minimal attributes:
  @Test
  public void testCreateAppMinimal() throws Exception
  {
    App app0 = makeMinimalApp(apps[11]);
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
  }

  // Test retrieving an app.
  @Test
  public void testGetApp() throws Exception
  {
    App app0 = apps[1];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    // Retrieve the app as filesSvc and as owner (with and without requireExecPerm)
    App tmpApp = svc.getApp(authenticatedFilesSvc, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), true);
    checkCommonAppAttrs(app0, tmpApp);
  }

  // Test updating an app
  // Both update of all possible attributes and only some attributes
  @Test
  public void testUpdateApp() throws Exception
  {
    // Test updating all attributes that can be updated.
    App app0 = apps[13];
    String appId = app0.getId();
    String appVersion = app0.getVersion();
    String createText = "{\"testUpdate\": \"0-createFull\"}";
    svc.createApp(authenticatedTestUser2, app0, createText);
    App tmpApp = svc.getApp(authenticatedTestUser2, appId, appVersion, false);
    // Get last updated timestamp
    LocalDateTime updated = LocalDateTime.ofInstant(tmpApp.getUpdated(), ZoneOffset.UTC);
    String updatedStr1 = TapisUtils.getSQLStringFromUTCTime(updated);
    Thread.sleep(300);
    // Create patchApp where all updatable attributes are changed
    String patchFullText = "{\"testUpdate\": \"1-patchFull\"}";
    PatchApp patchAppFull = IntegrationUtils.makePatchAppFull();
    patchAppFull.setTenant(tenantName);
    patchAppFull.setId(appId);
    patchAppFull.setVersion(appVersion);
    // Update using updateApp
    svc.updateApp(authenticatedTestUser2, patchAppFull, patchFullText);
    App tmpAppFull = svc.getApp(authenticatedTestUser2, appId, appVersion, false);
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
    app0.setAppArgs(appArgList2);
    app0.setContainerArgs(containerArgList2);
    app0.setSchedulerOptions(schedulerOptionList2);
    app0.setEnvVariables(envVariables2);
    app0.setArchiveIncludes(archiveIncludes2);
    app0.setArchiveExcludes(archiveExcludes2);
    app0.setArchiveIncludeLaunchFiles(archiveIncludeLaunchFilesFalse);
    app0.setFileInputs(finList2);
    app0.setNodeCount(nodeCount2);
    app0.setCoresPerNode(coresPerNode2);
    app0.setMemoryMb(memoryMb2);
    app0.setMaxMinutes(maxMinutes2);
    app0.setNotificationSubscriptions(notifList2);
    app0.setJobTags(jobTags2);
    app0.setTags(tags2);
    app0.setNotes(notes2);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppFull);

    // Test updating just a few attributes
    app0 = apps[22];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testUpdate\": \"0-createPartial1\"}";
    svc.createApp(authenticatedTestUser2, app0, createText);
    // Create patchApp where some attributes are changed
    //   * Some attributes are to be updated: description, containerImage, execSystemId,
    String patchPartialText1 = "{\"testUpdate\": \"1-patchPartial1\"}";
    PatchApp patchAppPartial1 = IntegrationUtils.makePatchAppPartial1();
    patchAppPartial1.setTenant(tenantName);
    patchAppPartial1.setId(appId);
    patchAppPartial1.setVersion(appVersion);
    // Update using updateApp
    svc.updateApp(authenticatedTestUser2, patchAppPartial1, patchPartialText1);
    App tmpAppPartial = svc.getApp(authenticatedTestUser2, appId, appVersion, false);
    // Update original app definition with patched values
    app0.setDescription(description2);
    app0.setContainerImage(containerImage2);
    app0.setExecSystemId(execSystemId2);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);

    // Test updating a few more attributes including a collection in JobAttributes
    //   and a collection in JobAttributes.ParameterSet.
    //   jobAttributes.fileInputDefinitions, jobAttributes.parameterSet.containerArgs
    app0 = apps[23];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testUpdate\": \"0-createPartial2\"}";
    svc.createApp(authenticatedTestUser2, app0, createText);
    // Create patchApp where some attributes are changed
    String patchPartialText2 = "{\"testUpdate\": \"1-patchPartial2\"}";
    PatchApp patchAppPartial2 = IntegrationUtils.makePatchAppPartial2();
    patchAppPartial2.setTenant(tenantName);
    patchAppPartial2.setId(appId);
    patchAppPartial2.setVersion(appVersion);
    // Update using updateApp
    svc.updateApp(authenticatedTestUser2, patchAppPartial2, patchPartialText2);
    tmpAppPartial = svc.getApp(authenticatedTestUser2, appId, appVersion, false);
    // Update original app definition with patched values
    app0.setDescription(description2);
    app0.setContainerImage(containerImage2);
    app0.setExecSystemId(execSystemId2);
    app0.setFileInputs(finList3);
    app0.setContainerArgs(containerArgList3);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);

    // Test updating just one of the collections in JobAttributes.ParameterSet.
    //   jobAttributes.parameterSet.appArgs
    app0 = apps[24];
    appId = app0.getId();
    appVersion = app0.getVersion();
    createText = "{\"testUpdate\": \"0-createPartial3\"}";
    svc.createApp(authenticatedTestUser2, app0, createText);
    // Create patchApp where some attributes are changed
    String patchPartialText3 = "{\"testUpdate\": \"1-patchPartial3\"}";
    PatchApp patchAppPartial3 = IntegrationUtils.makePatchAppPartial3();
    patchAppPartial3.setTenant(tenantName);
    patchAppPartial3.setId(appId);
    patchAppPartial3.setVersion(appVersion);
    // Update using updateApp
    svc.updateApp(authenticatedTestUser2, patchAppPartial3, patchPartialText3);
    tmpAppPartial = svc.getApp(authenticatedTestUser2, appId, appVersion, false);
    // Update original app definition with patched values
    app0.setAppArgs(appArgList3);
    //Check common app attributes:
    checkCommonAppAttrs(app0, tmpAppPartial);
  }

  // Test changing app owner
  @Test
  public void testChangeAppOwner() throws Exception
  {
    App app0 = apps[15];
    String createText = "{\"testChangeOwner\": \"0-create\"}";
    String origOwnerName = testUser2;
    String newOwnerName = testUser3;
    AuthenticatedUser origOwnerAuth = authenticatedTestUser2;
    AuthenticatedUser newOwnerAuth = authenticatedTestUser3;

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
    // TODO/TBD: what about EXECUTE?
    userPerms = svc.getUserPermissions(newOwnerAuth, app0.getId(), origOwnerName);
    Assert.assertFalse(userPerms.contains(Permission.MODIFY));
    // Original owner should not be able to modify app
    try {
      svc.softDeleteApp(origOwnerAuth, app0.getId());
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
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    app0 = apps[3];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    Set<String> appIDs = svc.getAppIDs(authenticatedTestUser2);
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
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    List<App> apps = svc.getApps(authenticatedTestUser2, null);
    for (App app : apps)
    {
      System.out.println("Found item with id: " + app.getId() + " and version: " + app.getVersion());
    }
  }

  // Check that user only sees apps they are authorized to see.
  @Test
  public void testGetAppsAuth() throws Exception
  {
    // Create 3 apps, 2 of which are owned by testUser1.
    App app0 = apps[16];
    String app1Name = app0.getId();
    app0.setOwner(authenticatedTestUser1.getName());
    svc.createApp(authenticatedTestUser1, app0, scrubbedJson);

    app0 = apps[17];
    String app2Name = app0.getId();
    app0.setOwner(authenticatedTestUser1.getName());
    svc.createApp(authenticatedTestUser1, app0, scrubbedJson);

    app0 = apps[18];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);

    // When retrieving apps as testUser3 only 2 should be returned
    List<App> apps = svc.getApps(authenticatedTestUser1, null);
    System.out.println("Total number of apps retrieved: " + apps.size());
    for (App app : apps)
    {
      System.out.println("Found item with appId: " + app.getId() + " and appVer: " + app.getVersion());
      Assert.assertTrue(app.getId().equals(app1Name) || app.getId().equalsIgnoreCase(app2Name));
    }
    Assert.assertEquals(apps.size(), 2);
  }

  @Test
  public void testEnableDisable() throws Exception
  {
    // Create the app
    App app0 = apps[20];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    // Enabled should start off true, then become false and finally true again.
    App tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    Assert.assertTrue(tmpApp.isEnabled());
    int changeCount = svc.disableApp(authenticatedTestUser2, app0.getId());
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    Assert.assertFalse(tmpApp.isEnabled());
    changeCount = svc.enableApp(authenticatedTestUser2, app0.getId());
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating the app.");
    tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    Assert.assertTrue(tmpApp.isEnabled());
  }

  @Test
  public void testSoftDelete() throws Exception
  {
    // Create the app
    App app0 = apps[5];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);

    // Soft delete the app
    int changeCount = svc.softDeleteApp(authenticatedTestUser2, app0.getId());
    Assert.assertEquals(changeCount, 1, "Change count incorrect when deleting an app.");
    App tmpApp2 = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    Assert.assertNull(tmpApp2, "App not deleted. App name: " + app0.getId());
  }

  @Test
  public void testAppExists() throws Exception
  {
    // If app not there we should get false
    Assert.assertFalse(svc.checkForApp(authenticatedTestUser2, apps[6].getId()));
    // After creating app we should get true
    App app0 = apps[6];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(authenticatedTestUser2, apps[6].getId()));
  }

  // Check that if apps already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^APPLIB_APP_EXISTS.*")
  public void testCreateAppAlreadyExists() throws Exception
  {
    // Create the app
    App app0 = apps[8];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(authenticatedTestUser2, app0.getId()));
    // Now attempt to create again, should get IllegalStateException with msg APPLIB_APP_EXISTS
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
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
      app0.setId(id);
      try
      {
        svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
        Assert.fail("App create call should have thrown an exception when using a reserved ID. Id: " + id);
      } catch (IllegalStateException e)
      {
        Assert.assertTrue(e.getMessage().contains("APPLIB_CREATE_RESERVED"));
      }
    }
  }

  // Test creating, reading and deleting user permissions for an app
  @Test
  public void testUserPerms() throws Exception
  {
    // Create an app
    App app0 = apps[9];
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    // Create user perms for the app
    Set<Permission> permsToCheck = testPermsALL;
    svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), testUser4, permsToCheck, scrubbedJson);
    // Get the app perms for the user and make sure permissions are there
    Set<Permission> userPerms = svc.getUserPermissions(authenticatedTestUser2, app0.getId(), testUser4);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    Assert.assertEquals(userPerms.size(), permsToCheck.size(), "Incorrect number of perms returned.");
    for (Permission perm: permsToCheck) { if (!userPerms.contains(perm)) Assert.fail("User perms should contain permission: " + perm.name()); }
    // Remove perms for the user. Should return a change count of 2
    int changeCount = svc.revokeUserPermissions(authenticatedTestUser2, app0.getId(), testUser4, permsToCheck, scrubbedJson);
    Assert.assertEquals(changeCount, permsToCheck.size(), "Change count incorrect when revoking permissions.");
    // Get the app perms for the user and make sure permissions are gone.
    userPerms = svc.getUserPermissions(authenticatedTestUser2, app0.getId(), testUser4);
    for (Permission perm: permsToCheck) { if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm.name()); }

    // Owner should not be able to update perms. It would be confusing since owner always authorized. Perms not checked.
    boolean pass = false;
    try {
      svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), app0.getOwner(), testPermsREAD, scrubbedJson);
      Assert.fail("Update of perms by owner for owner should have thrown an exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("APPLIB_PERM_OWNER_UPDATE"));
      pass = true;
    }
    Assert.assertTrue(pass, "Update of perms by owner for owner did not throw correct exception");
    pass = false;
    try {
      svc.revokeUserPermissions(authenticatedTestUser2, app0.getId(), app0.getOwner(), testPermsREAD, scrubbedJson);
      Assert.fail("Update of perms by owner for owner should have thrown an exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("APPLIB_PERM_OWNER_UPDATE"));
      pass = true;
    }
    Assert.assertTrue(pass, "Update of perms by owner for owner did not throw correct exception");
  }

  // Test various cases when app is missing
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
    // Make sure app does not exist
    Assert.assertFalse(svc.checkForApp(authenticatedTestUser2, fakeAppName));

    // Get App with no app should return null
    App tmpApp = svc.getApp(authenticatedTestUser2, fakeAppName, fakeAppVersion, false);
    Assert.assertNull(tmpApp, "App not null for non-existent app");

    // Delete app with no app should return 0 changes
    int changeCount = svc.softDeleteApp(authenticatedTestUser2, fakeAppName);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when deleting non-existent app.");

    // Get owner with no app should return null
    String owner = svc.getAppOwner(authenticatedTestUser2, fakeAppName);
    Assert.assertNull(owner, "Owner not null for non-existent app.");

    // Get perms with no app should return null
    Set<Permission> perms = svc.getUserPermissions(authenticatedTestUser2, fakeAppName, fakeUserName);
    Assert.assertNull(perms, "Perms list was not null for non-existent app");

    // Revoke perm with no app should return 0 changes
    changeCount = svc.revokeUserPermissions(authenticatedTestUser2, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when revoking perms for non-existent app.");

    // Grant perm with no app should throw an exception
    boolean pass = false;
    try { svc.grantUserPermissions(authenticatedTestUser2, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson); }
    catch (TapisException tce)
    {
      Assert.assertTrue(tce.getMessage().startsWith("APPLIB_NOT_FOUND"));
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
    try { svc.createApp(authenticatedTestUser2, app0, scrubbedJson); }
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
    try { svc.createApp(authenticatedTestUser2, app0, scrubbedJson); }
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
    try { svc.createApp(authenticatedTestUser2, app0, scrubbedJson); }
    catch (Exception e)
    {
      Assert.assertTrue(e.getMessage().contains("APPLIB_EXECSYS_NOT_EXEC"));
      pass = true;
    }
    Assert.assertTrue(pass);
    app0.setExecSystemId(execSystemId1);
  }

  // Test Auth denials
  // testUser2 - owner
  // testUser0 - no perms
  // testUser3 - READ perm
  // testUser4 - MODIFY perm
  @Test
  public void testAuthDeny() throws Exception
  {
    App app0 = apps[12];
    PatchApp patchApp = IntegrationUtils.makePatchAppFull();
    patchApp.setTenant(tenantName);
    patchApp.setId(app0.getId());
    patchApp.setVersion(app0.getVersion());
    // CREATE - Deny user not owner/admin, deny service
    boolean pass = false;
    try { svc.createApp(authenticatedTestUser0, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.createApp(authenticatedFilesSvc, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // Create an app and grant permissions
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    // Grant User3 - READ and User4 - MODIFY
    svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), testUser3, testPermsREAD, scrubbedJson);
    svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), testUser4, testPermsMODIFY, scrubbedJson);

    // READ - deny user not owner/admin and no READ or MODIFY access (testuser0)
    pass = false;
    try { svc.getApp(authenticatedTestUser0, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // EXECUTE - deny user not owner/admin with READ but not EXECUTE (testuser3)
    pass = false;
    try { svc.getApp(authenticatedTestUser3, app0.getId(), app0.getVersion(), true); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // MODIFY Deny user with no READ or MODIFY (testuser0), deny user with only READ (testuser3), deny service
    pass = false;
    try { svc.updateApp(authenticatedTestUser0, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.updateApp(authenticatedTestUser3, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.updateApp(authenticatedFilesSvc, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // DELETE - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.softDeleteApp(authenticatedTestUser3, app0.getId()); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.softDeleteApp(authenticatedFilesSvc, app0.getId()); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // CHANGE_OWNER - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.changeAppOwner(authenticatedTestUser3, app0.getId(), testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.changeAppOwner(authenticatedFilesSvc, app0.getId(), testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GET_PERMS - deny user not owner/admin and no READ or MODIFY access (testuser0)
    pass = false;
    try { svc.getUserPermissions(authenticatedTestUser0, app0.getId(), ownerUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GRANT_PERMS - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.grantUserPermissions(authenticatedTestUser3, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(authenticatedFilesSvc, app0.getId(), testUser3, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // REVOKE_PERMS - deny user not owner/admin (testuser3), deny service
    pass = false;
    try { svc.revokeUserPermissions(authenticatedTestUser3, app0.getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(authenticatedFilesSvc, app0.getId(), testUser4, testPermsREADMODIFY, scrubbedJson); }
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

  // testUser2 - owner
  // testUser0 - no perms
  // testUser3 - READ,EXECUTE perm
  // testUser4 - MODIFY perm
  @Test
  public void testAuthAllow() throws Exception
  {
    App app0 = apps[14];
    // Create app for remaining auth access tests
    svc.createApp(authenticatedTestUser2, app0, scrubbedJson);
    // Grant User3 - READ,EXECUTE and User4 - MODIFY
    svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), testUser3, testPermsREADEXECUTE, scrubbedJson);
    svc.grantUserPermissions(authenticatedTestUser2, app0.getId(), testUser4, testPermsMODIFY, scrubbedJson);

    // READ - allow owner, service, with READ only, with MODIFY only
    boolean pass = true;
    try
    {
      svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
      svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), true);
    }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedFilesSvc, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try
    {
      svc.getApp(authenticatedTestUser3, app0.getId(), app0.getVersion(), false);
      svc.getApp(authenticatedTestUser3, app0.getId(), app0.getVersion(), true);
    }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedTestUser4, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_UNAUTH"));
      pass = false;
    }
    Assert.assertTrue(pass);
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
    Assert.assertEquals(tmpApp.getArchiveIncludeLaunchFiles(), app0.getArchiveIncludeLaunchFiles());
    // Verify envVariables
    String[] origEnvVariables = app0.getEnvVariables();
    String[] tmpEnvVariables = tmpApp.getEnvVariables();
    Assert.assertNotNull(tmpEnvVariables, "envVariables value was null");
    var envVariablesList = Arrays.asList(tmpEnvVariables);
    Assert.assertEquals(tmpEnvVariables.length, origEnvVariables.length, "Wrong number of envVariables");
    for (String envVariableStr : origEnvVariables)
    {
      Assert.assertTrue(envVariablesList.contains(envVariableStr));
      System.out.println("Found envVariable: " + envVariableStr);
    }
    // Verify archiveIncludes
    String[] origArchiveIncludes = app0.getArchiveIncludes();
    String[] tmpArchiveIncludes = tmpApp.getArchiveIncludes();
    Assert.assertNotNull(tmpArchiveIncludes, "archiveIncludes value was null");
    var archiveIncludesList = Arrays.asList(tmpArchiveIncludes);
    Assert.assertEquals(tmpArchiveIncludes.length, origArchiveIncludes.length, "Wrong number of archiveIncludes");
    for (String archiveIncludeStr : origArchiveIncludes)
    {
      Assert.assertTrue(archiveIncludesList.contains(archiveIncludeStr));
      System.out.println("Found archiveInclude: " + archiveIncludeStr);
    }
    // Verify archiveExcludes
    String[] origArchiveExcludes = app0.getArchiveExcludes();
    String[] tmpArchiveExcludes = tmpApp.getArchiveExcludes();
    Assert.assertNotNull(tmpArchiveExcludes, "archiveExcludes value was null");
    var archiveExcludesList = Arrays.asList(tmpArchiveExcludes);
    Assert.assertEquals(tmpArchiveExcludes.length, origArchiveExcludes.length, "Wrong number of archiveExcludes");
    for (String archiveExcludeStr : origArchiveExcludes)
    {
      Assert.assertTrue(archiveExcludesList.contains(archiveExcludeStr));
      System.out.println("Found archiveExclude: " + archiveExcludeStr);
    }
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
    tmpArgs = tmpApp.getContainerArgs();
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
}
