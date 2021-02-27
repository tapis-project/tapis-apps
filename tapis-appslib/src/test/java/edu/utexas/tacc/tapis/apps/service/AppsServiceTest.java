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
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
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

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.notes2;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.tags2;

/**
 * Test the AppsService implementation class against a DB running locally
 * Note that this test has the following dependencies running locally or in dev
 *    Database - typically local
 *    Tenants service - typically dev
 *    Tokens service - typically dev and obtained from tenants service
 *    Security Kernel service - typically dev and obtained from tenants service
 *
 */
@Test(groups={"integration"})
public class AppsServiceTest
{
  private AppsService svc;
  private AppsServiceImpl svcImpl;
  private AuthenticatedUser authenticatedOwner1, authenticatedTestUser0, authenticatedTestUser1,
          authenticatedTestUser2, authenticatedTestUser3, authenticatedAdminUser, authenticatedFilesSvc;
  // Test data
  private static final String filesSvcName = "files";
  private static final String adminUser = "testuser9";
//TODO  private static final String adminUser = "admin";
  private static final String adminTenantName = "admin";
  private static final String testUser0 = "testuser0";
  private static final String testUser1 = "testuser1";
  private static final String testUser2 = "testuser2";
  private static final String testUser3 = "testuser3";
  private static final Set<Permission> testPermsALL = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY, Permission.EXECUTE));
  private static final Set<Permission> testPermsREADMODIFY = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final Set<Permission> testPermsREADEXECUTE = new HashSet<>(Set.of(Permission.READ, Permission.EXECUTE));
  private static final Set<Permission> testPermsREAD = new HashSet<>(Set.of(Permission.READ));
  private static final Set<Permission> testPermsMODIFY = new HashSet<>(Set.of(Permission.MODIFY));
  private static final String[] tags2 = {"value3", "value4"};
  private static final Object notes2 = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj2\", \"testdata\": \"abc2\"}", JsonObject.class);

  int numApps = 19;
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
    svcImpl.initService(RuntimeParameters.getInstance());

    // Initialize authenticated user and service
    authenticatedOwner1 = new AuthenticatedUser(ownerUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                  null, ownerUser, tenantName, null, null, null);
    authenticatedAdminUser = new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                    null, adminUser, tenantName, null, null, null);
    authenticatedTestUser0 = new AuthenticatedUser(testUser0, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser0, tenantName, null, null, null);
    authenticatedTestUser1 = new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser1, tenantName, null, null, null);
    authenticatedTestUser2 = new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser2, tenantName, null, null, null);
    authenticatedTestUser3 = new AuthenticatedUser(testUser3, tenantName, TapisThreadContext.AccountType.user.name(),
                                                   null, testUser3, tenantName, null, null, null);
    authenticatedFilesSvc = new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                                  null, ownerUser, tenantName, null, null, null);

    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + AppsServiceTest.class.getSimpleName());
    // Remove non-owner permissions granted during the tests
    try { svc.revokeUserPermissions(authenticatedOwner1, apps[9].getId(), testUser1, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedOwner1, apps[9].getId(), testUser2, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedOwner1, apps[12].getId(), testUser1, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedOwner1, apps[12].getId(), testUser2, testPermsREADMODIFY, scrubbedJson); }
    catch (Exception e) { }
    try { svc.revokeUserPermissions(authenticatedOwner1, apps[14].getId(), testUser2, testPermsREADMODIFY, scrubbedJson); }
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
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
  }

  // Create an app using minimal attributes:
  @Test
  public void testCreateAppMinimal() throws Exception
  {
    App app0 = makeMinimalApp(apps[11]);
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
  }

  // Test retrieving an app.
  @Test
  public void testGetApp() throws Exception
  {
    App app0 = apps[1];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    // Retrieve the app as filesSvc and as owner
    App tmpApp = svc.getApp(authenticatedFilesSvc, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), false);
    checkCommonAppAttrs(app0, tmpApp);
    tmpApp = svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), true);
    checkCommonAppAttrs(app0, tmpApp);
  }

  // Test updating an app
  @Test
  public void testUpdateApp() throws Exception
  {
    App app0 = apps[13];
    String createText = "{\"testUpdate\": \"0-create\"}";
    String patch1Text = "{\"testUpdate\": \"1-patch1\"}";
    PatchApp patchApp = new PatchApp("description PATCHED", enabledFalse, runtime, runtimeVersion, containerImage,
                                     maxJobs, maxJobsPerUser, strictFileInputsFalse, tags2, notes2);
    patchApp.setTenant(tenantName);
    patchApp.setId(app0.getId());
    patchApp.setVersion(app0.getVersion());
    svc.createApp(authenticatedOwner1, app0, createText);
    // Update using updateApp
    svc.updateApp(authenticatedOwner1, patchApp, patch1Text);
    App tmpApp = svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), false);
//  App appE = new App(-1, tenantName, "SappE", "description E", AppType.LINUX, ownerUser, "hostE", true,
//          "effUserE", prot1.getAccessMethod(), "bucketE", "/rootE", prot1.getTransferMethods(),
//          prot1.getPort(), prot1.isUseProxy(), prot1.getProxyHost(), prot1.getProxyPort(),false,
//          "jobLocalWorkDirE", "jobLocalArchDirE", "jobRemoteArchAppE","jobRemoteArchDirE",
//          tags1, notes1, false, null, null);
//  App appE2 = new App(-1, tenantName, "SappE", "description PATCHED", AppType.LINUX, ownerUser, "hostPATCHED", false,
//          "effUserPATCHED", prot2.getAccessMethod(), "bucketE", "/rootE", prot2.getTransferMethods(),
//          prot2.getPort(), prot2.isUseProxy(), prot2.getProxyHost(), prot2.getProxyPort(),false,
//          "jobLocalWorkDirE", "jobLocalArchDirE", "jobRemoteArchAppE","jobRemoteArchDirE",
//          tags2, notes2, false, null, null);
    // Update original app definition with patched values
//    app0.setJobCapabilities(cap2List);
//    app0.setDescription("description PATCHED");
//    app0.setTags(tags2);
//    app0.setNotes(notes2);
    //TODO Check common app attributes:
//    checkCommonAppAttrs(app0, tmpApp);

    // TODO: For now only enabled and containerized are being updated
    Assert.assertFalse(tmpApp.isEnabled());
//    Assert.assertFalse(tmpApp.isContainerized());
  }

  // Test changing app owner
  @Test
  public void testChangeAppOwner() throws Exception
  {
    App app0 = apps[15];
    String createText = "{\"testChangeOwner\": \"0-create\"}";
    String newOwnerName = testUser2;
    svc.createApp(authenticatedOwner1, app0, createText);
    // Change owner using api
    svc.changeAppOwner(authenticatedOwner1, app0.getId(), newOwnerName);
    App tmpApp = svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    Assert.assertEquals(tmpApp.getOwner(), newOwnerName);
    // Check expected auxiliary updates have happened
    // New owner should be able to retrieve permissions and have all permissions
    Set<Permission> userPerms = svc.getUserPermissions(authenticatedTestUser2, app0.getId(), newOwnerName);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    for (Permission perm : Permission.values())
    {
      Assert.assertTrue(userPerms.contains(perm));
    }
    // Original owner should no longer have the modify permission
    // TODO/TBD: what about EXECUTE?
    userPerms = svc.getUserPermissions(authenticatedTestUser2, app0.getId(), ownerUser);
    Assert.assertFalse(userPerms.contains(Permission.MODIFY));
    // Original owner should not be able to modify app
    try {
      svc.softDeleteApp(authenticatedOwner1, app0.getId());
      Assert.fail("Original owner should not have permission to update app after change of ownership. App name: " + app0.getId() +
              " Old owner: " + authenticatedOwner1.getName() + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "HTTP 401 Unauthorized");
    }
    // Original owner should not be able to read system
    try {
      svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), false);
      Assert.fail("Original owner should not have permission to read app after change of ownership. App name: " + app0.getId() +
              " Old owner: " + authenticatedOwner1.getName() + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "HTTP 401 Unauthorized");
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
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    app0 = apps[3];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    Set<String> appIDs = svc.getAppIDs(authenticatedOwner1);
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
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    List<App> apps = svc.getApps(authenticatedOwner1, null);
    for (App app : apps)
    {
      System.out.println("Found item with id: " + app.getId() + " and version: " + app.getVersion());
    }
  }

  // Check that user only sees apps they are authorized to see.
  @Test
  public void testGetAppsAuth() throws Exception
  {
    // Create 3 apps, 2 of which are owned by testUser3.
    App app0 = apps[16];
    String app1Name = app0.getId();
    app0.setOwner(authenticatedTestUser3.getName());
    svc.createApp(authenticatedTestUser3, app0, scrubbedJson);

    app0 = apps[17];
    String app2Name = app0.getId();
    app0.setOwner(authenticatedTestUser3.getName());
    svc.createApp(authenticatedTestUser3, app0, scrubbedJson);

    app0 = apps[18];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);

    // When retrieving apps as testUser3 only 2 should be returned
    List<App> apps = svc.getApps(authenticatedTestUser3, null);
    System.out.println("Total number of apps retrieved: " + apps.size());
    for (App app : apps)
    {
      System.out.println("Found item with appId: " + app.getId() + " and appVer: " + app.getVersion());
      Assert.assertTrue(app.getId().equals(app1Name) || app.getId().equalsIgnoreCase(app2Name));
    }
    Assert.assertEquals(apps.size(), 2);
  }

  @Test
  public void testSoftDelete() throws Exception
  {
    // Create the app
    App app0 = apps[5];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);

    // Soft delete the app
    int changeCount = svc.softDeleteApp(authenticatedOwner1, app0.getId());
    Assert.assertEquals(changeCount, 1, "Change count incorrect when deleting an app.");
    App tmpApp2 = svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), false);
    Assert.assertNull(tmpApp2, "App not deleted. App name: " + app0.getId());
  }

  @Test
  public void testAppExists() throws Exception
  {
    // If app not there we should get false
    Assert.assertFalse(svc.checkForApp(authenticatedOwner1, apps[6].getId()));
    // After creating app we should get true
    App app0 = apps[6];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(authenticatedOwner1, apps[6].getId()));
  }

  // Check that if apps already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^APPLIB_APP_EXISTS.*")
  public void testCreateAppAlreadyExists() throws Exception
  {
    // Create the app
    App app0 = apps[8];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    Assert.assertTrue(svc.checkForApp(authenticatedOwner1, app0.getId()));
    // Now attempt to create again, should get IllegalStateException with msg APPLIB_APP_EXISTS
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
  }

  // Test creating, reading and deleting user permissions for an app
  @Test
  public void testUserPerms() throws Exception
  {
    // Create an app
    App app0 = apps[9];
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    // Create user perms for the app
    Set<Permission> permsToCheck = testPermsALL;
    svc.grantUserPermissions(authenticatedOwner1, app0.getId(), testUser1, permsToCheck, scrubbedJson);
    // Get the app perms for the user and make sure permissions are there
    Set<Permission> userPerms = svc.getUserPermissions(authenticatedOwner1, app0.getId(), testUser1);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    Assert.assertEquals(userPerms.size(), permsToCheck.size(), "Incorrect number of perms returned.");
    for (Permission perm: permsToCheck) { if (!userPerms.contains(perm)) Assert.fail("User perms should contain permission: " + perm.name()); }
    // Remove perms for the user. Should return a change count of 2
    int changeCount = svc.revokeUserPermissions(authenticatedOwner1, app0.getId(), testUser1, permsToCheck, scrubbedJson);
    Assert.assertEquals(changeCount, permsToCheck.size(), "Change count incorrect when revoking permissions.");
    // Get the app perms for the user and make sure permissions are gone.
    userPerms = svc.getUserPermissions(authenticatedOwner1, app0.getId(), testUser1);
    for (Permission perm: permsToCheck) { if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm.name()); }
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
    Assert.assertFalse(svc.checkForApp(authenticatedOwner1, fakeAppName));

    // Get App with no app should return null
    App tmpApp = svc.getApp(authenticatedOwner1, fakeAppName, fakeAppVersion, false);
    Assert.assertNull(tmpApp, "App not null for non-existent app");

    // Delete app with no app should return 0 changes
    int changeCount = svc.softDeleteApp(authenticatedOwner1, fakeAppName);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when deleting non-existent app.");

    // Get owner with no app should return null
    String owner = svc.getAppOwner(authenticatedOwner1, fakeAppName);
    Assert.assertNull(owner, "Owner not null for non-existent app.");

    // Get perms with no app should return null
    Set<Permission> perms = svc.getUserPermissions(authenticatedOwner1, fakeAppName, fakeUserName);
    Assert.assertNull(perms, "Perms list was not null for non-existent app");

    // Revoke perm with no app should return 0 changes
    changeCount = svc.revokeUserPermissions(authenticatedOwner1, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when revoking perms for non-existent app.");

    // Grant perm with no app should throw an exception
    boolean pass = false;
    try { svc.grantUserPermissions(authenticatedOwner1, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson); }
    catch (TapisException tce)
    {
      Assert.assertTrue(tce.getMessage().startsWith("APPLIB_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);

  }

  // Test Auth denials
  // testUser0 - no perms
  // testUser1 - READ perm
  // testUser2 - MODIFY perm
  @Test
  public void testAuthDeny() throws Exception
  {
    App app0 = apps[12];
    PatchApp patchApp = new PatchApp("description PATCHED", enabledFalse, runtime2, runtimeVersion2, containerImage2,
            maxJobs2, maxJobsPerUser2, strictFileInputsTrue, tags2, notes2);
    patchApp.setTenant(tenantName);
    patchApp.setId(app0.getId());
    patchApp.setVersion(app0.getVersion());
    // CREATE - Deny user not owner/admin, deny service
    boolean pass = false;
    try { svc.createApp(authenticatedTestUser0, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.createApp(authenticatedFilesSvc, app0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    // Grant User1 - READ and User2 - MODIFY
    svc.grantUserPermissions(authenticatedOwner1, app0.getId(), testUser1, testPermsREAD, scrubbedJson);
    svc.grantUserPermissions(authenticatedOwner1, app0.getId(), testUser2, testPermsMODIFY, scrubbedJson);

    // READ - deny user not owner/admin and no READ or MODIFY access
    pass = false;
    try { svc.getApp(authenticatedTestUser0, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // EXECUTE - deny user not owner/admin with READ but not EXECUTE
    pass = false;
    try { svc.getApp(authenticatedTestUser1, app0.getId(), app0.getVersion(), true); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // MODIFY Deny user with no READ or MODIFY, deny user with only READ, deny service
    pass = false;
    try { svc.updateApp(authenticatedTestUser0, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.updateApp(authenticatedTestUser1, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.updateApp(authenticatedFilesSvc, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // DELETE - deny user not owner/admin, deny service
    pass = false;
    try { svc.softDeleteApp(authenticatedTestUser1, app0.getId()); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.softDeleteApp(authenticatedFilesSvc, app0.getId()); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // CHANGE_OWNER - deny user not owner/admin, deny service
    pass = false;
    try { svc.changeAppOwner(authenticatedTestUser1, app0.getId(), testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.changeAppOwner(authenticatedFilesSvc, app0.getId(), testUser2); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GET_PERMS - deny user not owner/admin and no READ or MODIFY access
    pass = false;
    try { svc.getUserPermissions(authenticatedTestUser0, app0.getId(), ownerUser); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GRANT_PERMS - deny user not owner/admin, deny service
    pass = false;
    try { svc.grantUserPermissions(authenticatedTestUser1, app0.getId(), testUser1, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(authenticatedFilesSvc, app0.getId(), testUser1, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // REVOKE_PERMS - deny user not owner/admin, deny service
    pass = false;
    try { svc.revokeUserPermissions(authenticatedTestUser1, app0.getId(), ownerUser, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.grantUserPermissions(authenticatedFilesSvc, app0.getId(), ownerUser, testPermsREADMODIFY, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  // Test Auth allow
  // Many cases covered during other tests
  // Test special cases here:
  //    MODIFY implies READ
  // testUser0 - no perms
  // testUser1 - READ,EXECUTE perm
  // testUser2 - MODIFY perm
  @Test
  public void testAuthAllow() throws Exception
  {
    App app0 = apps[14];
    // Create app for remaining auth access tests
    svc.createApp(authenticatedOwner1, app0, scrubbedJson);
    // Grant User1 - READ,EXECUTE and User2 - MODIFY
    svc.grantUserPermissions(authenticatedOwner1, app0.getId(), testUser1, testPermsREADEXECUTE, scrubbedJson);
    svc.grantUserPermissions(authenticatedOwner1, app0.getId(), testUser2, testPermsMODIFY, scrubbedJson);

    // READ - allow owner, service, with READ only, with MODIFY only
    boolean pass = true;
    try
    {
      svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), false);
      svc.getApp(authenticatedOwner1, app0.getId(), app0.getVersion(), true);
    }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedFilesSvc, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try
    {
      svc.getApp(authenticatedTestUser1, app0.getId(), app0.getVersion(), false);
      svc.getApp(authenticatedTestUser1, app0.getId(), app0.getVersion(), true);
    }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedTestUser2, app0.getId(), app0.getVersion(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
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
}
