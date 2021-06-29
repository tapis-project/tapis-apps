package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.apps.model.ResourceRequestUser;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.versionSpecifiedNull;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_LIMIT;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_SKIP;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.apiUser;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.gson;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.orderByListNull;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.scrubbedJson;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.searchASTNull;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.setOfIDsNull;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.showDeletedFalse;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.showDeletedTrue;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.startAfterNull;
import static edu.utexas.tacc.tapis.apps.IntegrationUtils.tenantName;
import static org.testng.Assert.assertEquals;

/**
 * Test the AppsDao getApps() call to verify that deleted resources are filtered properly.
 */
@Test(groups={"integration"})
public class ShowDeletedDaoTest
{
  private AppsDaoImpl dao;
  private ResourceRequestUser rUser;

  // Test data
  private static final String testKey = "SrchDel";
  private static final String appIdLikeAll = "id.like.*" + testKey + "*";

  // Number of resources
  int numApps = 3;

  App[] apps = IntegrationUtils.makeApps(numApps, testKey);

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + ShowDeletedDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    rUser = new ResourceRequestUser(new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                          null, apiUser, tenantName, null, null, null));

    // Cleanup anything leftover from previous failed run
    teardown();

    for (App app : apps)
    {
      boolean itemCreated = dao.createApp(rUser, app, gson.toJson(app), scrubbedJson);
      Assert.assertTrue(itemCreated, "Item not created, id: " + app.getId());
    }
  }

  @AfterSuite
  public void teardown() throws Exception {
    System.out.println("Executing AfterSuite teardown for " + ShowDeletedDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (App app : apps)
    {
      dao.hardDeleteApp(tenantName, app.getId());
    }
    Assert.assertFalse(dao.checkForApp(tenantName, apps[0].getId(), true),
                       "App not deleted. App id: " + apps[0].getId());
  }

  /*
   * All cases
   * Confirm search/get with none deleted.
   * Delete one app.
   * Confirm Get and Search return one less app.
   * Undelete the app.
   * Confirm get/search counts are back to total.
   */
  @Test(groups={"integration"})
  public void testSearchGetDeleted() throws Exception
  {
    App app0 = apps[0];
    String app0Id = app0.getId();
    var searchListAll= Collections.singletonList(SearchUtils.validateAndProcessSearchCondition(appIdLikeAll));

    // None deleted yet so should have all apps

    // First check counts. showDeleted = true or false should have total number of resources.
    int count = dao.getAppsCount(tenantName, searchListAll, searchASTNull, setOfIDsNull, orderByListNull,
                                 startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(count, numApps, "Incorrect count for getAppsCount/showDel=false before delete of app");
    count = dao.getAppsCount(tenantName, searchListAll, searchASTNull, setOfIDsNull, orderByListNull,
                             startAfterNull, versionSpecifiedNull, showDeletedTrue);
    assertEquals(count, numApps, "Incorrect count for getAppsCount/showDel=true before delete of app");
    // Check retrieving all apps
    List<App> searchResults = dao.getApps(tenantName, searchListAll, searchASTNull, setOfIDsNull, DEFAULT_LIMIT,
                                          orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count for getApps/showDel=false before delete of app");
    searchResults = dao.getApps(tenantName, searchListAll, searchASTNull, setOfIDsNull, DEFAULT_LIMIT,
                                orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedTrue);
    assertEquals(searchResults.size(), numApps, "Incorrect result count for getApps/showDel=true before delete of app");
// No filtering, cannot easily do this if any other apps present    // Check retrieving App IDs
//    Set<String> appIDs = dao.getAppIDs(tenantName, showDeletedFalse);
//    assertEquals(appIDs.size(), numApps, "Incorrect result count for getAppIDs/showDel=false before delete of app");
//    appIDs = dao.getAppIDs(tenantName, showDeletedTrue);
//    assertEquals(appIDs.size(), numApps, "Incorrect result count for getAppIDs/showDel=true before delete of app");

    // Now delete an app
    dao.updateDeleted(rUser, tenantName, app0Id, true);

    // First check counts. showDeleted = false should return 1 less than total.
    count = dao.getAppsCount(tenantName, searchListAll, searchASTNull, setOfIDsNull, orderByListNull,
                             startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(count, numApps-1, "Incorrect count for getAppsCount/showDel=false after delete of app");
    count = dao.getAppsCount(tenantName, searchListAll, searchASTNull, setOfIDsNull, orderByListNull,
                             startAfterNull, versionSpecifiedNull, showDeletedTrue);
    assertEquals(count, numApps, "Incorrect count for getAppsCount/showDel=true after delete of app");

    // Check retrieving all apps
    searchResults = dao.getApps(tenantName, searchListAll, searchASTNull, setOfIDsNull, DEFAULT_LIMIT,
                                 orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps-1, "Incorrect result count for getApps after delete of app");
    searchResults = dao.getApps(tenantName, searchListAll, searchASTNull, setOfIDsNull, DEFAULT_LIMIT,
                                 orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedTrue);
    assertEquals(searchResults.size(), numApps, "Incorrect result count for getApps after delete of app");

// No filtering, cannot easily do this if any other apps present    // Check retrieving App IDs
//    appIDs = dao.getAppIDs(tenantName, showDeletedFalse);
//    assertEquals(appIDs.size(), numApps, "Incorrect result count for getAppIDs/showDel=false after delete of app");
//    appIDs = dao.getAppIDs(tenantName, showDeletedTrue);
//    assertEquals(appIDs.size(), numApps, "Incorrect result count for getAppIDs/showDel=true after delete of app");
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

}
