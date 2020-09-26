package edu.utexas.tacc.tapis.apps.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
//import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.model.Capability;
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

//import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;

/**
 * Test the AppsDao class against a DB running locally
 */
@Test(groups={"integration"})
public class AppsDaoTest
{
//  private AppsDaoImpl dao;
//  private AuthenticatedUser authenticatedUser;
//
//  // Test data
//  int numApps = 11;
//  App[] apps = IntegrationUtils.makeApps(numApps, "Dao");
//
//  @BeforeSuite
//  public void setup() throws Exception
//  {
//    System.out.println("Executing BeforeSuite setup method: " + AppsDaoTest.class.getSimpleName());
//    dao = new AppsDaoImpl();
//    // Initialize authenticated user
//    authenticatedUser = new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null);
//    // Cleanup anything leftover from previous failed run
//    teardown();
//  }
//
//  @AfterSuite
//  public void teardown() throws Exception {
//    System.out.println("Executing AfterSuite teardown for " + AppsDaoTest.class.getSimpleName());
//    //Remove all objects created by tests
//    for (int i = 0; i < numApps; i++)
//    {
//      dao.hardDeleteApp(tenantName, apps[i].getName());
//    }
//
//    App tmpApp = dao.getAppByName(tenantName, apps[0].getName());
//    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getName());
//  }
//
//  // Test create for a single item
//  @Test
//  public void testCreate() throws Exception
//  {
//    App sys0 = systems[0];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//  }
//
//  // Test retrieving a single item
//  @Test
//  public void testGetByName() throws Exception {
//    App sys0 = systems[1];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    App tmpSys = dao.getAppByName(sys0.getTenant(), sys0.getName());
//    Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0.getName());
//    System.out.println("Found item: " + sys0.getName());
//    Assert.assertEquals(tmpSys.getName(), sys0.getName());
//    Assert.assertEquals(tmpSys.getDescription(), sys0.getDescription());
//    Assert.assertEquals(tmpSys.geappType().name(), sys0.geappType().name());
//    Assert.assertEquals(tmpSys.getOwner(), sys0.getOwner());
//    Assert.assertEquals(tmpSys.getHost(), sys0.getHost());
//    Assert.assertEquals(tmpSys.getEffectiveUserId(), sys0.getEffectiveUserId());
//    Assert.assertEquals(tmpSys.getBucketName(), sys0.getBucketName());
//    Assert.assertEquals(tmpSys.getRootDir(), sys0.getRootDir());
//    Assert.assertEquals(tmpSys.getJobLocalWorkingDir(), sys0.getJobLocalWorkingDir());
//    Assert.assertEquals(tmpSys.getJobLocalArchiveDir(), sys0.getJobLocalArchiveDir());
//    Assert.assertEquals(tmpSys.getJobRemoteArchiveSystem(), sys0.getJobRemoteArchiveSystem());
//    Assert.assertEquals(tmpSys.getJobRemoteArchiveDir(), sys0.getJobRemoteArchiveDir());
//    Assert.assertEquals(tmpSys.getDefaultAccessMethod(), sys0.getDefaultAccessMethod());
//    Assert.assertEquals(tmpSys.getPort(), sys0.getPort());
//    Assert.assertEquals(tmpSys.isUseProxy(), sys0.isUseProxy());
//    Assert.assertEquals(tmpSys.getProxyHost(), sys0.getProxyHost());
//    Assert.assertEquals(tmpSys.getProxyPort(), sys0.getProxyPort());
//    // Verify txfr methods
//    List<TransferMethod> tMethodsList = tmpSys.getTransferMethods();
//    Assert.assertNotNull(tMethodsList);
//    List<TransferMethod> sys0TMethodsList = sys0.getTransferMethods();
//    Assert.assertNotNull(sys0TMethodsList);
//    for (TransferMethod txfrMethod : sys0TMethodsList)
//    {
//      Assert.assertTrue(tMethodsList.contains(txfrMethod), "List of transfer methods did not contain: " + txfrMethod.name());
//    }
//    // Verify tags
//    String[] tmpTags = tmpSys.getTags();
//    Assert.assertNotNull(tmpTags, "Tags value was null");
//    var tagsList = Arrays.asList(tmpTags);
//    Assert.assertEquals(tmpTags.length, tags.length, "Wrong number of tags");
//    for (String tagStr : tags)
//    {
//      Assert.assertTrue(tagsList.contains(tagStr));
//      System.out.println("Found tag: " + tagStr);
//    }
//    // Verify notes
//    JsonObject obj = (JsonObject) tmpSys.getNotes();
//    Assert.assertNotNull(obj, "Notes object was null");
//    Assert.assertTrue(obj.has("project"));
//    Assert.assertEquals(obj.get("project").getAsString(), notesObj.get("project").getAsString());
//    Assert.assertTrue(obj.has("testdata"));
//    Assert.assertEquals(obj.get("testdata").getAsString(), notesObj.get("testdata").getAsString());
//    // Verify capabilities
//    List<Capability> origCaps = sys0.getJobCapabilities();
//    List<Capability> jobCaps = tmpSys.getJobCapabilities();
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
//  }
//
//  // Test retrieving all system names
//  @Test
//  public void testGetSystemNames() throws Exception {
//    // Create 2 systems
//    App sys0 = systems[2];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    sys0 = systems[3];
//    itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    // Get all systems
//    List<String> systemNames = dao.getAppNames(tenantName);
//    for (String name : systemNames) {
//      System.out.println("Found item: " + name);
//    }
//    Assert.assertTrue(systemNames.contains(systems[2].getName()), "List of systems did not contain system name: " + systems[2].getName());
//    Assert.assertTrue(systemNames.contains(systems[3].getName()), "List of systems did not contain system name: " + systems[3].getName());
//  }
//
//  // Test retrieving all systems
//  @Test
//  public void testGetSystems() throws Exception {
//    App sys0 = systems[4];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    List<App> systems = dao.getApps(tenantName, null, null);
//    for (App system : systems) {
//      System.out.println("Found item with id: " + system.getId() + " and name: " + system.getName());
//    }
//  }
//
//  // Test retrieving all systems in a list of IDs
//  @Test
//  public void testGetSystemsInIDList() throws Exception {
//    var idList = new ArrayList<Integer>();
//    // Create 2 systems
//    App sys0 = systems[5];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    idList.add(itemId);
//    sys0 = systems[6];
//    itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    idList.add(itemId);
//    // Get all systems in list of IDs
//    List<App> systems = dao.getApps(tenantName, null, idList);
//    for (App system : systems) {
//      System.out.println("Found item with id: " + system.getId() + " and name: " + system.getName());
//      Assert.assertTrue(idList.contains(system.getId()));
//    }
//    Assert.assertEquals(idList.size(), systems.size());
//  }
//
//  // Test change system owner
//  @Test
//  public void testChangeSystemOwner() throws Exception {
//    App sys0 = systems[7];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    System.out.println("Created item with id: " + itemId);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    dao.updateSystemOwner(authenticatedUser, itemId, "newOwner");
//    App tmpSystem = dao.getAppByName(sys0.getTenant(), sys0.getName());
//    Assert.assertEquals(tmpSystem.getOwner(), "newOwner");
//  }
//
//  // Test soft deleting a single item
//  @Test
//  public void testSoftDelete() throws Exception {
//    App sys0 = systems[8];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    System.out.println("Created item with id: " + itemId);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    int numDeleted = dao.softDeleteApp(authenticatedUser, itemId);
//    Assert.assertEquals(numDeleted, 1);
//    numDeleted = dao.softDeleteApp(authenticatedUser, itemId);
//    Assert.assertEquals(numDeleted, 0);
//    Assert.assertFalse(dao.checkForAppByName(sys0.getTenant(), sys0.getName(), false ),
//            "System not deleted. System name: " + sys0.getName());
//  }
//
//  // Test hard deleting a single item
//  @Test
//  public void testHardDelete() throws Exception {
//    App sys0 = systems[9];
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    System.out.println("Created item with id: " + itemId);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    dao.hardDeleteApp(sys0.getTenant(), sys0.getName());
//    Assert.assertFalse(dao.checkForAppByName(sys0.getTenant(), sys0.getName(), true),"System not deleted. System name: " + sys0.getName());
//  }
//
//  // Test create and get for a single item with no transfer methods supported and unusual port settings
//  @Test
//  public void testNoTxfr() throws Exception
//  {
//    App sys0 = systems[10];
//    sys0.setTransferMethods(txfrMethodsEmpty);
//    sys0.setPort(-1);
//    sys0.setProxyPort(-1);
//    int itemId = dao.createApp(authenticatedUser, sys0, gson.toJson(sys0), scrubbedJson);
//    Assert.assertTrue(itemId > 0, "Invalid system id: " + itemId);
//    App tmpSys = dao.getAppByName(sys0.getTenant(), sys0.getName());
//    Assert.assertNotNull(tmpSys, "Failed to create item: " + sys0.getName());
//    System.out.println("Found item: " + sys0.getName());
//    Assert.assertEquals(tmpSys.getName(), sys0.getName());
//    Assert.assertEquals(tmpSys.getDescription(), sys0.getDescription());
//    Assert.assertEquals(tmpSys.geappType().name(), sys0.geappType().name());
//    Assert.assertEquals(tmpSys.getOwner(), sys0.getOwner());
//    Assert.assertEquals(tmpSys.getHost(), sys0.getHost());
//    Assert.assertEquals(tmpSys.getEffectiveUserId(), sys0.getEffectiveUserId());
//    Assert.assertEquals(tmpSys.getBucketName(), sys0.getBucketName());
//    Assert.assertEquals(tmpSys.getRootDir(), sys0.getRootDir());
//    Assert.assertEquals(tmpSys.getJobLocalWorkingDir(), sys0.getJobLocalWorkingDir());
//    Assert.assertEquals(tmpSys.getJobLocalArchiveDir(), sys0.getJobLocalArchiveDir());
//    Assert.assertEquals(tmpSys.getJobRemoteArchiveSystem(), sys0.getJobRemoteArchiveSystem());
//    Assert.assertEquals(tmpSys.getJobRemoteArchiveDir(), sys0.getJobRemoteArchiveDir());
//    Assert.assertEquals(tmpSys.getDefaultAccessMethod(), sys0.getDefaultAccessMethod());
//    Assert.assertEquals(tmpSys.getPort(), sys0.getPort());
//    Assert.assertEquals(tmpSys.isUseProxy(), sys0.isUseProxy());
//    Assert.assertEquals(tmpSys.getProxyHost(), sys0.getProxyHost());
//    Assert.assertEquals(tmpSys.getProxyPort(), sys0.getProxyPort());
//    List<TransferMethod> txfrMethodsList = tmpSys.getTransferMethods();
//    Assert.assertNotNull(txfrMethodsList);
//    Assert.assertEquals(txfrMethodsList.size(), 0);
//  }
//
//  // Test behavior when system is missing, especially for cases where service layer depends on the behavior.
//  //  update - throws not found exception
//  //  getByName - returns null
//  //  checkByName - returns false
//  //  getOwner - returns null
//  @Test
//  public void testMissingSystem() throws Exception {
//    String fakeSystemName = "AMissingSystemName";
//    PatchSystem patchSys = new PatchSystem("description PATCHED", "hostPATCHED", false, "effUserPATCHED",
//            prot2.getAccessMethod(), prot2.getTransferMethods(), prot2.getPort(), prot2.isUseProxy(), prot2.getProxyHost(),
//            prot2.getProxyPort(), capList, tags, notes);
//    patchSys.setTenant(tenantName);
//    patchSys.setName(fakeSystemName);
//    App patchedSystem = new App(1, tenantName, fakeSystemName, "description", SystemType.LINUX, "owner", "host", true,
//            "effUser", prot2.getAccessMethod(), "bucket", "/root", prot2.getTransferMethods(),
//            prot2.getPort(), prot2.isUseProxy(), prot2.getProxyHost(), prot2.getProxyPort(), false,
//            "jobLocalWorkDir", "jobLocalArchDir", "jobRemoteArchSystem","jobRemoteArchDir",
//            tags, notes, null, false, null, null);
//    // Make sure system does not exist
//    Assert.assertFalse(dao.checkForAppByName(tenantName, fakeSystemName, true));
//    Assert.assertFalse(dao.checkForAppByName(tenantName, fakeSystemName, false));
//    // update should throw not found exception
//    boolean pass = false;
//    try { dao.updateApp(authenticatedUser, patchedSystem, patchSys, scrubbedJson, null); }
//    catch (IllegalStateException e)
//    {
//      Assert.assertTrue(e.getMessage().startsWith("SYSLIB_NOT_FOUND"));
//      pass = true;
//    }
//    Assert.assertTrue(pass);
//    Assert.assertNull(dao.getAppByName(tenantName, fakeSystemName));
//    Assert.assertNull(dao.getAppOwner(tenantName, fakeSystemName));
//  }
}