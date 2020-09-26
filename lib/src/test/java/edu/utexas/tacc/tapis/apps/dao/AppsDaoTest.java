package edu.utexas.tacc.tapis.apps.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
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
    authenticatedUser = new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null);
    // Cleanup anything leftover from previous failed run
    teardown();
  }

  @AfterSuite
  public void teardown() throws Exception {
    System.out.println("Executing AfterSuite teardown for " + AppsDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (int i = 0; i < numApps; i++)
    {
      dao.hardDeleteApp(tenantName, apps[i].getName());
    }

    App tmpApp = dao.getAppByName(tenantName, apps[0].getName());
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getName());
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
  public void testGetByName() throws Exception {
    App app0 = apps[1];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    App tmpApp = dao.getAppByName(app0.getTenant(), app0.getName());
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getName());
    System.out.println("Found item: " + app0.getName());
    Assert.assertEquals(tmpApp.getName(), app0.getName());
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), app0.getOwner());
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
    // Verify capabilities
    List<Capability> origCaps = app0.getJobCapabilities();
    List<Capability> jobCaps = tmpApp.getJobCapabilities();
    Assert.assertNotNull(origCaps, "Orig Caps was null");
    Assert.assertNotNull(jobCaps, "Fetched Caps was null");
    Assert.assertEquals(jobCaps.size(), origCaps.size());
    var capNamesFound = new ArrayList<String>();
    for (Capability capFound : jobCaps) {capNamesFound.add(capFound.getName());}
    for (Capability capSeedItem : origCaps)
    {
      Assert.assertTrue(capNamesFound.contains(capSeedItem.getName()),
              "List of capabilities did not contain a capability named: " + capSeedItem.getName());
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
    Assert.assertTrue(appNames.contains(apps[2].getName()), "List of apps did not contain app name: " + apps[2].getName());
    Assert.assertTrue(appNames.contains(apps[3].getName()), "List of apps did not contain app name: " + apps[3].getName());
  }

  // Test retrieving all apps
  @Test
  public void testGetApps() throws Exception {
    App app0 = apps[4];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    List<App> apps = dao.getApps(tenantName, null, null);
    for (App app : apps) {
      System.out.println("Found item with id: " + app.getId() + " and name: " + app.getName());
    }
  }

  // Test retrieving all apps in a list of IDs
  @Test
  public void testGetAppsInIDList() throws Exception {
    var idList = new ArrayList<Integer>();
    // Create 2 apps
    App app0 = apps[5];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    idList.add(itemId);
    app0 = apps[6];
    itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    idList.add(itemId);
    // Get all apps in list of IDs
    List<App> apps = dao.getApps(tenantName, null, idList);
    for (App app : apps) {
      System.out.println("Found item with id: " + app.getId() + " and name: " + app.getName());
      Assert.assertTrue(idList.contains(app.getId()));
    }
    Assert.assertEquals(idList.size(), apps.size());
  }

  // Test change app owner
  @Test
  public void testChangeAppOwner() throws Exception {
    App app0 = apps[7];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    System.out.println("Created item with id: " + itemId);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    dao.updateAppOwner(authenticatedUser, itemId, "newOwner");
    App tmpApp = dao.getAppByName(app0.getTenant(), app0.getName());
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
    Assert.assertFalse(dao.checkForAppByName(app0.getTenant(), app0.getName(), false ),
            "App not deleted. App name: " + app0.getName());
  }

  // Test hard deleting a single item
  @Test
  public void testHardDelete() throws Exception {
    App app0 = apps[9];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    System.out.println("Created item with id: " + itemId);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    dao.hardDeleteApp(app0.getTenant(), app0.getName());
    Assert.assertFalse(dao.checkForAppByName(app0.getTenant(), app0.getName(), true),"App not deleted. App name: " + app0.getName());
  }

  // Test create and get for a single item with no transfer methods supported and unusual port settings
  @Test
  public void testNoTxfr() throws Exception
  {
    App app0 = apps[10];
    int itemId = dao.createApp(authenticatedUser, app0, gson.toJson(app0), scrubbedJson);
    Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    App tmpApp = dao.getAppByName(app0.getTenant(), app0.getName());
    Assert.assertNotNull(tmpApp, "Failed to create item: " + app0.getName());
    System.out.println("Found item: " + app0.getName());
    Assert.assertEquals(tmpApp.getName(), app0.getName());
    Assert.assertEquals(tmpApp.getDescription(), app0.getDescription());
    Assert.assertEquals(tmpApp.getAppType().name(), app0.getAppType().name());
    Assert.assertEquals(tmpApp.getOwner(), app0.getOwner());
  }

  // Test behavior when app is missing, especially for cases where service layer depends on the behavior.
  //  update - throws not found exception
  //  getByName - returns null
  //  checkByName - returns false
  //  getOwner - returns null
  @Test
  public void testMissingApp() throws Exception {
    String fakeAppName = "AMissingAppName";
    PatchApp patchApp = new PatchApp("description PATCHED", false, capList, tags, notes);
    patchApp.setTenant(tenantName);
    patchApp.setName(fakeAppName);
    App patchedApp = new App(1, tenantName, fakeAppName, "description", AppType.INTERACTIVE, "owner", true,
            tags, notes, null, false, null, null);
    // Make sure app does not exist
    Assert.assertFalse(dao.checkForAppByName(tenantName, fakeAppName, true));
    Assert.assertFalse(dao.checkForAppByName(tenantName, fakeAppName, false));
    // update should throw not found exception
    boolean pass = false;
    try { dao.updateApp(authenticatedUser, patchedApp, patchApp, scrubbedJson, null); }
    catch (IllegalStateException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("APPLIB_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);
    Assert.assertNull(dao.getAppByName(tenantName, fakeAppName));
    Assert.assertNull(dao.getAppOwner(tenantName, fakeAppName));
  }
}