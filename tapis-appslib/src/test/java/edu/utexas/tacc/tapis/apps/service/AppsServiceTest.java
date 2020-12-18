package edu.utexas.tacc.tapis.apps.service;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
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
 *
 *    TODO: Remove filesSvc related code or useful for svc to svc testing?
 *          For Systems was used to test retrieving credentials.
 */
@Test(groups={"integration"})
public class AppsServiceTest
{
  private AppsService svc;
  private AppsServiceImpl svcImpl;
  private AuthenticatedUser authenticatedOwnerUsr, authenticatedTestUsr0, authenticatedTestUsr1, authenticatedTestUsr2,
          authenticatedTestUsr3, authenticatedAdminUsr, authenticatedFilesSvc;
  // Test data
  private static final String svcName = "apps";
  private static final String siteId = "tacc";
  // TODO: Currently admin user for a tenant is hard coded to be 'testuser9'
  private static final String adminUser = "testuser9";
  private static final String adminTenantName = "admin";
  private static final String filesSvcName = "files";
  private static final String testUser0 = "testuser0";
  private static final String testUser1 = "testuser1";
  private static final String testUser2 = "testuser2";
  private static final String testUser3 = "testuser3";
  private static final Set<Permission> testPermsREADMODIFY = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final Set<Permission> testPermsREAD = new HashSet<>(Set.of(Permission.READ));
  private static final Set<Permission> testPermsMODIFY = new HashSet<>(Set.of(Permission.MODIFY));
  private static final String[] tags2 = {"value3", "value4"};
  private static final Object notes2 = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj2\", \"testdata\": \"abc2\"}", JsonObject.class);

//  private static final Capability capA2 = new Capability(Category.SCHEDULER, "Type", "Condor");
//  private static final Capability capB2 = new Capability(Category.HARDWARE, "CoresPerNode", "128");
//  private static final Capability capC2 = new Capability(Category.SOFTWARE, "OpenMP", "3.1");
//  private static final List<Capability> cap2List = new ArrayList<>(List.of(capA2, capB2, capC2));

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
        bindFactory(AppsServiceContextFactory.class).to(ServiceContext.class);
        bind(SKClient.class).to(SKClient.class);
      }
    });
    locator.inject(this);

    // Initialize TenantManager and services
    String url = RuntimeParameters.getInstance().getTenantsSvcURL();
    TenantManager.getInstance(url).getTenants();

    // Initialize services
    svc = locator.getService(AppsService.class);
    svcImpl = locator.getService(AppsServiceImpl.class);
    svcImpl.initService(siteId);

    // Initialize authenticated user and service
    authenticatedOwnerUsr = new AuthenticatedUser(ownerUser, tenantName, TapisThreadContext.AccountType.user.name(), null, ownerUser, tenantName, null, null, null);
    authenticatedAdminUsr = new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(), null, adminUser, tenantName, null, null, null);
    authenticatedTestUsr0 = new AuthenticatedUser(testUser0, tenantName, TapisThreadContext.AccountType.user.name(), null, testUser0, tenantName, null, null, null);
    authenticatedTestUsr1 = new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(), null, testUser1, tenantName, null, null, null);
    authenticatedTestUsr2 = new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(), null, testUser2, tenantName, null, null, null);
    authenticatedTestUsr3 = new AuthenticatedUser(testUser3, tenantName, TapisThreadContext.AccountType.user.name(), null, testUser3, tenantName, null, null, null);
    authenticatedFilesSvc = new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(), null, ownerUser, tenantName, null, null, null);

    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + AppsServiceTest.class.getSimpleName());
    // Remove non-owner permissions granted during the tests
    svc.revokeUserPermissions(authenticatedOwnerUsr, apps[9].getId(), testUser1, testPermsREADMODIFY, scrubbedJson);
    svc.revokeUserPermissions(authenticatedOwnerUsr, apps[9].getId(), testUser2, testPermsREADMODIFY, scrubbedJson);
    svc.revokeUserPermissions(authenticatedOwnerUsr, apps[12].getId(), testUser1, testPermsREADMODIFY, scrubbedJson);
    svc.revokeUserPermissions(authenticatedOwnerUsr, apps[12].getId(), testUser2, testPermsREADMODIFY, scrubbedJson);
// TODO why is following revoke causing an exception?
    //    svc.revokeUserPermissions(authenticatedOwnerUsr, appF.getName(), testUser1, testPermsREADMODIFY, scrubbedJson);
    svc.revokeUserPermissions(authenticatedOwnerUsr, apps[14].getId(), testUser2, testPermsREADMODIFY, scrubbedJson);

    //Remove all objects created by tests
    for (int i = 0; i < numApps; i++)
    {
      svcImpl.hardDeleteApp(authenticatedAdminUsr, apps[i].getId());
    }

    App tmpApp = svc.getApp(authenticatedAdminUsr, apps[0].getId(), false);
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  @Test
  public void testCreateApp() throws Exception
  {
    App app0 = apps[0];
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
  }

  // Create an app using minimal attributes:
  //   name, appType
  @Test
  public void testCreateAppMinimal() throws Exception
  {
    App app0 = apps[11];//1
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
  }

  // Test retrieving an app including default access method
  //   and test retrieving for specified access method.
  @Test
  public void testGetAppByName() throws Exception
  {
    App app0 = apps[1];//2
//    app0.setJobCapabilities(capList);
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Retrieve the app
    App tmpApp = svc.getApp(authenticatedFilesSvc, app0.getId(), false);
    checkCommonAppAttrs(app0, tmpApp);
  }

  // Test updating an app
  @Test
  public void testUpdateApp() throws Exception
  {
    App app0 = apps[13];//3
//    app0.setJobCapabilities(capList);
    String createText = "{\"testUpdate\": \"0-create\"}";
    String patch1Text = "{\"testUpdate\": \"1-patch1\"}";
    PatchApp patchApp = new PatchApp(appVersion, "description PATCHED", false,
//            cap2List,
            tags2, notes2);
    patchApp.setId(app0.getId());
    patchApp.setTenant(tenantName);
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, createText);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Update using updateApp
    svc.updateApp(authenticatedOwnerUsr, patchApp, patch1Text);
    App tmpApp = svc.getApp(authenticatedOwnerUsr, app0.getId(), false);
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
    app0.setDescription("description PATCHED");
    app0.setEnabled(false);
    app0.setTags(tags2);
    app0.setNotes(notes2);
    // Check common app attributes:
    checkCommonAppAttrs(app0, tmpApp);
  }

  // Test changing app owner
  @Test
  public void testChangeAppOwner() throws Exception
  {
    App app0 = apps[15];//4
//    app0.setJobCapabilities(capList);
    String createText = "{\"testChangeOwner\": \"0-create\"}";
    String newOwnerName = testUser2;
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, createText);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Change owner using api
    svc.changeAppOwner(authenticatedOwnerUsr, app0.getId(), newOwnerName);
    App tmpApp = svc.getApp(authenticatedTestUsr2, app0.getId(), false);
    Assert.assertEquals(tmpApp.getOwner(), newOwnerName);
    // Check expected auxillary updates have happened
    // New owner should be able to retrieve permissions and have the ALL permission
    Set<Permission> userPerms = svc.getUserPermissions(authenticatedTestUsr2, app0.getId(), newOwnerName);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    Assert.assertTrue(userPerms.contains(Permission.ALL));
    // Original owner should no longer have the ALL permission
    userPerms = svc.getUserPermissions(authenticatedTestUsr2, app0.getId(), ownerUser);
    Assert.assertFalse(userPerms.contains(Permission.ALL));
  }

  // Check that when an app is created variable substitution is correct for:
  //   owner, bucketName, rootDir, ...
  // And when app is retrieved effectiveUserId is resolved
  @Test
  public void testGetAppByNameWithVariables() throws Exception
  {
    App app0 = apps[7];//5
    app0.setOwner("${apiUserId}");
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    App tmpApp = svc.getApp(authenticatedOwnerUsr, app0.getId(), false);
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getId());
    System.out.println("Found item: " + app0.getId());

// app8 = {tenantName, "Sapp8", "description 8", AppType.LINUX.name(), "${apiUserId}", "host8",
//         "${owner}", prot1AccessMethName, "fakePassword8", "bucket8-${tenant}-${apiUserId}", "/root8/${tenant}", prot1TxfrMethods,
//         "jobLocalWorkDir8/${owner}/${tenant}/${apiUserId}", "jobLocalArchDir8/${apiUserId}", "jobRemoteArchApp8",
//         "jobRemoteArchDir8${owner}${tenant}${apiUserId}", tags, notes, "{}"};
    Assert.assertEquals(tmpApp.getId(), app0.getId());
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), ownerUser);
    Assert.assertEquals(tmpApp.isEnabled(), app0.isEnabled());
  }

  @Test
  public void testGetAppNames() throws Exception
  {
    App app0 = apps[2];//6
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    app0 = apps[3];//7
    itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    List<String> appNames = svc.getAppNames(authenticatedOwnerUsr);
    for (String name : appNames) {
      System.out.println("Found item: " + name);
    }
    Assert.assertTrue(appNames.contains(apps[2].getId()), "List of apps did not contain app name: " + apps[2].getId());
    Assert.assertTrue(appNames.contains(apps[3].getId()), "List of apps did not contain app name: " + apps[3].getId());
  }

  @Test
  public void testGetApps() throws Exception
  {
    App app0 = apps[4];//8
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    List<App> apps = svc.getApps(authenticatedOwnerUsr, null);
    for (App app : apps) {
      System.out.println("Found item with id: " + app.getSeqId() + " and name: " + app.getId());
    }
  }

  // Check that user only sees apps they are authorized to see.
  @Test
  public void testGetAppsAuth() throws Exception
  {
    // Create 3 apps, 2 of which are owned by testUser3.
    App app0 = apps[16];//9
    String app1Name = app0.getId();
    app0.setOwner(authenticatedTestUsr3.getName());
    int itemId =  svc.createApp(authenticatedTestUsr3, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    app0 = apps[17];//10
    String app2Name = app0.getId();
    app0.setOwner(authenticatedTestUsr3.getName());
    itemId =  svc.createApp(authenticatedTestUsr3, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    app0 = apps[18];//11
    itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // When retrieving apps as testUser3 only 2 should be returned
    List<App> apps = svc.getApps(authenticatedTestUsr3, null);
    System.out.println("Total number of apps retrieved: " + apps.size());
    for (App app : apps)
    {
      System.out.println("Found item with id: " + app.getSeqId() + " and name: " + app.getId());
      Assert.assertTrue(app.getId().equals(app1Name) || app.getId().equalsIgnoreCase(app2Name));
    }
    Assert.assertEquals(2, apps.size());
  }

  @Test
  public void testSoftDelete() throws Exception
  {
    // Create the app
    App app0 = apps[5];//12
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);

    // Soft delete the app
    int changeCount = svc.softDeleteApp(authenticatedOwnerUsr, app0.getId());
    Assert.assertEquals(changeCount, 1, "Change count incorrect when deleting an app.");
    App tmpApp2 = svc.getApp(authenticatedOwnerUsr, app0.getId(), false);
    Assert.assertNull(tmpApp2, "App not deleted. App name: " + app0.getId());
  }

  @Test
  public void testAppExists() throws Exception
  {
    // If app not there we should get false
    Assert.assertFalse(svc.checkForApp(authenticatedOwnerUsr, apps[6].getId()));
    // After creating app we should get true
    App app0 = apps[6];//13
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    Assert.assertTrue(svc.checkForApp(authenticatedOwnerUsr, apps[6].getId()));
  }

  // Check that if apps already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^APPLIB_APP_EXISTS.*")
  public void testCreateAppAlreadyExists() throws Exception
  {
    // Create the app
    App app0 = apps[8];//14
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    Assert.assertTrue(svc.checkForApp(authenticatedOwnerUsr, app0.getId()));
    // Now attempt to create again, should get IllegalStateException with msg APPLIB_APP_EXISTS
    svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
  }

  // Test creating, reading and deleting user permissions for an app
  @Test
  public void testUserPerms() throws Exception
  {
    // Create an app
    App app0 = apps[9];//15
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Create user perms for the app
    svc.grantUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1, testPermsREADMODIFY, scrubbedJson);
    // Get the app perms for the user and make sure permissions are there
    Set<Permission> userPerms = svc.getUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1);
    Assert.assertNotNull(userPerms, "Null returned when retrieving perms.");
    Assert.assertEquals(userPerms.size(), testPermsREADMODIFY.size(), "Incorrect number of perms returned.");
    for (Permission perm: testPermsREADMODIFY) { if (!userPerms.contains(perm)) Assert.fail("User perms should contain permission: " + perm.name()); }
    // Remove perms for the user. Should return a change count of 2
    int changeCount = svc.revokeUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 2, "Change count incorrect when revoking permissions.");
    // Get the app perms for the user and make sure permissions are gone.
    userPerms = svc.getUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1);
    for (Permission perm: testPermsREADMODIFY) { if (userPerms.contains(perm)) Assert.fail("User perms should not contain permission: " + perm.name()); }
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
    String fakeUserName = "AMissingUserName";
    // Make sure app does not exist
    Assert.assertFalse(svc.checkForApp(authenticatedOwnerUsr, fakeAppName));

    // Get App with no app should return null
    App tmpApp = svc.getApp(authenticatedOwnerUsr, fakeAppName, false);
    Assert.assertNull(tmpApp, "App not null for non-existent app");

    // Delete app with no app should return 0 changes
    int changeCount = svc.softDeleteApp(authenticatedOwnerUsr, fakeAppName);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when deleting non-existent app.");

    // Get owner with no app should return null
    String owner = svc.getAppOwner(authenticatedOwnerUsr, fakeAppName);
    Assert.assertNull(owner, "Owner not null for non-existent app.");

    // Get perms with no app should return null
    Set<Permission> perms = svc.getUserPermissions(authenticatedOwnerUsr, fakeAppName, fakeUserName);
    Assert.assertNull(perms, "Perms list was not null for non-existent app");

    // Revoke perm with no app should return 0 changes
    changeCount = svc.revokeUserPermissions(authenticatedOwnerUsr, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson);
    Assert.assertEquals(changeCount, 0, "Change count incorrect when revoking perms for non-existent app.");

    // Grant perm with no app should throw an exception
    boolean pass = false;
    try { svc.grantUserPermissions(authenticatedOwnerUsr, fakeAppName, fakeUserName, testPermsREADMODIFY, scrubbedJson); }
    catch (TapisException tce)
    {
      Assert.assertTrue(tce.getMessage().startsWith("APPLIB_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);

  }

  // Test Auth denials
  // testUsr0 - no perms
  // testUsr1 - READ perm
  // testUsr2 - MODIFY perm
  @Test
  public void testAuthDeny() throws Exception
  {
    App app0 = apps[12];//17
    PatchApp patchApp = new PatchApp(appVersion, "description PATCHED", false,
//            cap2List,
            tags2, notes2);
    patchApp.setId(app0.getId());
    patchApp.setTenant(tenantName);
    // CREATE - Deny user not owner/admin, deny service
    boolean pass = false;
    try { svc.createApp(authenticatedTestUsr0, app0, scrubbedJson); }
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

    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Grant Usr1 - READ and Usr2 - MODIFY
    svc.grantUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1, testPermsREAD, scrubbedJson);
    svc.grantUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser2, testPermsMODIFY, scrubbedJson);

    // READ - deny user not owner/admin and no READ or MODIFY access
    pass = false;
    try { svc.getApp(authenticatedTestUsr0, app0.getId(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // MODIFY Deny user with no READ or MODIFY, deny user with only READ, deny service
    pass = false;
    try { svc.updateApp(authenticatedTestUsr0, patchApp, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
    try { svc.updateApp(authenticatedTestUsr1, patchApp, scrubbedJson); }
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
    try { svc.softDeleteApp(authenticatedTestUsr1, app0.getId()); }
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
    try { svc.changeAppOwner(authenticatedTestUsr1, app0.getId(), testUser2); }
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
    try { svc.getUserPermissions(authenticatedTestUsr0, app0.getId(), ownerUser); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = true;
    }
    Assert.assertTrue(pass);

    // GRANT_PERMS - deny user not owner/admin, deny service
    pass = false;
    try { svc.grantUserPermissions(authenticatedTestUsr1, app0.getId(), testUser1, testPermsREADMODIFY, scrubbedJson); }
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
    try { svc.revokeUserPermissions(authenticatedTestUsr1, app0.getId(), ownerUser, testPermsREADMODIFY, scrubbedJson); }
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
  // testUsr0 - no perms
  // testUsr1 - READ perm
  // testUsr2 - MODIFY perm
  @Test
  public void testAuthAllow() throws Exception
  {
    App app0 = apps[14];//18
    // Create app for remaining auth access tests
    int itemId = svc.createApp(authenticatedOwnerUsr, app0, scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    // Grant Usr1 - READ and Usr2 - MODIFY
    svc.grantUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser1, testPermsREAD, scrubbedJson);
    svc.grantUserPermissions(authenticatedOwnerUsr, app0.getId(), testUser2, testPermsMODIFY, scrubbedJson);

    // READ - allow owner, service, with READ only, with MODIFY only
    boolean pass = true;
    try { svc.getApp(authenticatedOwnerUsr, app0.getId(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedFilesSvc, app0.getId(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedTestUsr1, app0.getId(), false); }
    catch (NotAuthorizedException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("HTTP 401 Unauthorized"));
      pass = false;
    }
    Assert.assertTrue(pass);
    pass = true;
    try { svc.getApp(authenticatedTestUsr2, app0.getId(), false); }
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
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), app0.getOwner());
    Assert.assertEquals(tmpApp.isEnabled(), app0.isEnabled());
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
//    // Verify capabilities
//    List<Capability> origCaps = app0.getJobCapabilities();
//    List<Capability> jobCaps = tmpApp.getJobCapabilities();
//    Assert.assertNotNull(origCaps, "Orig Caps was null");
//    Assert.assertNotNull(jobCaps, "Fetched Caps was null");
//    Assert.assertEquals(jobCaps.size(), origCaps.size());
//    var capNamesFound = new ArrayList<String>();
//    for (Capability capFound : jobCaps) {capNamesFound.add(capFound.getName());}
//    for (Capability capSeedItem : origCaps)
//    {
//      Assert.assertTrue(capNamesFound.contains(capSeedItem.getName()),
//              "List of capabilities did not contain a capability named: " + capSeedItem.getName());
//    }
  }
}
