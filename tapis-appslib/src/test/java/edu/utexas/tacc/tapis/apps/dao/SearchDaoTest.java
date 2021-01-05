package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;
import static org.testng.Assert.assertEquals;

/**
 * Test the AppsDao getApps() call for various search use cases against a DB running locally
 * NOTE: This test pre-processes the search list just as is done in AppsServiceImpl before it calls the Dao,
 *       including calling SearchUtils.validateAndProcessSearchCondition(cond)
 *       For this reason there is currently no need to have a SearchAppsTest suite.
 *       If this changes then we will need to create another suite and move the test data into IntegrationUtils so that
 *       it can be re-used.
 * TODO: Test that timestamps are handling timezone correctly.
 */
@Test(groups={"integration"})
public class SearchDaoTest
{
  private AppsDaoImpl dao;
  private AuthenticatedUser authenticatedUser;

  // Test data
  private static final String testKey = "SrchGet";
  private static final String appNameLikeAll = "*" + testKey + "*";

  // Strings for searches involving special characters
  private static final String specialChar7Str = ",()~*!\\"; // These 7 may need escaping
  private static final String specialChar7LikeSearchStr = "\\,\\(\\)\\~\\*\\!\\\\"; // All need escaping for LIKE/NLIKE
  private static final String specialChar7EqSearchStr = "\\,\\(\\)\\~*!\\"; // All but *! need escaping for other operators

  // Timestamps in various formats
  private static final String longPast1 =   "1800-01-01T00:00:00.123456Z";
  private static final String farFuture1 =  "2200-04-29T14:15:52.123456-06:00";
  private static final String farFuture2 =  "2200-04-29T14:15:52.123Z";
  private static final String farFuture3 =  "2200-04-29T14:15:52.123";
  private static final String farFuture4 =  "2200-04-29T14:15:52-06:00";
  private static final String farFuture5 =  "2200-04-29T14:15:52";
  private static final String farFuture6 =  "2200-04-29T14:15-06:00";
  private static final String farFuture7 =  "2200-04-29T14:15";
  private static final String farFuture8 =  "2200-04-29T14-06:00";
  private static final String farFuture9 =  "2200-04-29T14";
  private static final String farFuture10 = "2200-04-29-06:00";
  private static final String farFuture11 = "2200-04-29";
  private static final String farFuture12 = "2200-04Z";
  private static final String farFuture13 = "2200-04";
  private static final String farFuture14 = "2200Z";
  private static final String farFuture15 = "2200";

  // String for search involving an escaped comma in a list of values
  private static final String escapedCommanInListValue = "abc\\,def";

  // Strings for char relational testings
  private static final String containerImageName1 = "containerImage" + testKey + "_001";
  private static final String containerImageName7 = "containerImage" + testKey + "_007";

  int numApps = 20;
  App[] apps = IntegrationUtils.makeApps(numApps, testKey);

  private LocalDateTime createBegin;
  private LocalDateTime createEnd;

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + SearchDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    authenticatedUser = new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null, null);

    // Cleanup anything leftover from previous failed run
    teardown();

    // Vary maxJobs for checking numeric relational searches
    for (int i = 0; i < numApps; i++) { apps[i].setMaxJobs(i+1); }
    // For half the apps change the owner
    for (int i = 0; i < numApps/2; i++) { apps[i].setOwner(ownerUser2); }

    // For one app update description to have some special characters. 7 special chars in value: ,()~*!\
    //   and update runtimeVersions for testing an escaped comma in a list value
    apps[numApps-1].setDescription(specialChar7Str);
    apps[numApps-1].setRuntimeVersion(escapedCommanInListValue);

    // Create all the apps in the dB using the in-memory objects, recording start and end times
    createBegin = TapisUtils.getUTCTimeNow();
    Thread.sleep(500);
    for (App app : apps)
    {
      int itemId = dao.createApp(authenticatedUser, app, gson.toJson(app), scrubbedJson);
      Assert.assertTrue(itemId > 0, "Invalid app id: " + itemId);
    }
    Thread.sleep(500);
    createEnd = TapisUtils.getUTCTimeNow();
  }

  @AfterSuite
  public void teardown() throws Exception {
    System.out.println("Executing AfterSuite teardown for " + SearchDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (App app : apps)
    {
      dao.hardDeleteApp(tenantName, app.getId());
    }

    App tmpApp = dao.getApp(tenantName, apps[0].getId(), apps[0].getVersion(), true);
    Assert.assertNull(tmpApp, "App not deleted. App name: " + apps[0].getId());
  }

  /*
   * Check valid cases
   */
  @Test(groups={"integration"})
  public void testValidCases() throws Exception
  {
    App app0 = apps[0];
    String app0Name = app0.getId();
    String nameList = "noSuchName1,noSuchName2," + app0Name + ",noSuchName3";
    // Create all input and validation data for tests
    // NOTE: Some cases require "id.like." + appNameLikeAll in the list of conditions since maven runs the tests in
    //       parallel and not all attribute names are unique across integration tests
    class CaseData {public final int count; public final List<String> searchList; CaseData(int c, List<String> r) { count = c; searchList = r; }}
    var validCaseInputs = new HashMap<Integer, CaseData>();
    // Test basic types and operators
    validCaseInputs.put( 1,new CaseData(1, Arrays.asList("id.eq." + app0Name))); // 1 has specific id
    validCaseInputs.put( 2,new CaseData(1, Arrays.asList("version.eq." + app0.getVersion())));
    validCaseInputs.put( 3,new CaseData(1, Arrays.asList("description.eq." + app0.getDescription())));
    validCaseInputs.put( 4,new CaseData(1, Arrays.asList("runtime_version.eq." + app0.getRuntimeVersion())));
    validCaseInputs.put( 5,new CaseData(1, Arrays.asList("container_image.eq." + app0.getContainerImage())));
    validCaseInputs.put( 6,new CaseData(1, Arrays.asList("job_description.eq." + app0.getJobDescription())));
    validCaseInputs.put( 7,new CaseData(1, Arrays.asList("exec_system_id.eq." + app0.getExecSystemId())));
    validCaseInputs.put( 8,new CaseData(1, Arrays.asList("exec_system_exec_dir.eq." + app0.getExecSystemExecDir())));
    validCaseInputs.put( 9,new CaseData(1, Arrays.asList("exec_system_input_dir.eq." + app0.getExecSystemInputDir())));
    validCaseInputs.put(10,new CaseData(numApps/2, Arrays.asList("id.like." + appNameLikeAll, "owner.eq." + ownerUser)));  // Half owned by one user
    validCaseInputs.put(11,new CaseData(numApps/2, Arrays.asList("id.like." + appNameLikeAll, "owner.eq." + ownerUser2))); // and half owned by another
    validCaseInputs.put(12,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "enabled.eq.true")));  // All are enabled
    validCaseInputs.put(13,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "deleted.eq.false"))); // none are deleted
    validCaseInputs.put(14,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "deleted.neq.true"))); // none are deleted
    validCaseInputs.put(15,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "deleted.eq.true")));           // none are deleted
    validCaseInputs.put(16,new CaseData(1, Arrays.asList("id.like." + app0Name)));
    validCaseInputs.put(17,new CaseData(0, Arrays.asList("id.like.NOSUCHAPPxFM2c29bc8RpKWeE2sht7aZrJzQf3s")));
    validCaseInputs.put(18,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll)));
    validCaseInputs.put(19,new CaseData(numApps-1, Arrays.asList("id.like." + appNameLikeAll, "id.nlike." + app0Name)));
    validCaseInputs.put(20,new CaseData(1, Arrays.asList("id.like." + appNameLikeAll, "id.in." + nameList)));
    validCaseInputs.put(21,new CaseData(numApps-1, Arrays.asList("id.like." + appNameLikeAll, "id.nin." + nameList)));
    validCaseInputs.put(22,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "app_type.eq.BATCH")));
    validCaseInputs.put(23,new CaseData(numApps/2, Arrays.asList("id.like." + appNameLikeAll, "app_type.eq.BATCH","owner.neq." + ownerUser2)));
    // Test numeric relational
    validCaseInputs.put(40,new CaseData(numApps/2, Arrays.asList("id.like." + appNameLikeAll, "max_jobs.between.1," + numApps/2)));
    validCaseInputs.put(41,new CaseData(numApps/2-1, Arrays.asList("id.like." + appNameLikeAll, "max_jobs.between.2," + numApps/2)));
    validCaseInputs.put(42,new CaseData(numApps/2, Arrays.asList("id.like." + appNameLikeAll, "max_jobs.nbetween.1," + numApps/2)));
    validCaseInputs.put(43,new CaseData(13, Arrays.asList("id.like." + appNameLikeAll, "enabled.eq.true","max_jobs.lte.13")));
    validCaseInputs.put(44,new CaseData(5, Arrays.asList("id.like." + appNameLikeAll,"enabled.eq.true","max_jobs.gt.1","max_jobs.lt.7")));
    // Test char relational
    validCaseInputs.put(50,new CaseData(1, Arrays.asList("id.like." + appNameLikeAll,"container_image.lte."+containerImageName1)));
    validCaseInputs.put(51,new CaseData(numApps-7, Arrays.asList("id.like." + appNameLikeAll,"enabled.eq.true","container_image.gt."+containerImageName7)));
    validCaseInputs.put(52,new CaseData(5, Arrays.asList("id.like." + appNameLikeAll,"container_image.gt."+containerImageName1,"container_image.lt."+containerImageName7)));
    validCaseInputs.put(53,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll,"container_image.lte."+containerImageName1,"container_image.gt."+containerImageName7)));
    validCaseInputs.put(54,new CaseData(7, Arrays.asList("id.like." + appNameLikeAll,"container_image.between."+containerImageName1+","+containerImageName7)));
    validCaseInputs.put(55,new CaseData(numApps-7, Arrays.asList("id.like." + appNameLikeAll,"container_image.nbetween."+containerImageName1+","+containerImageName7)));
    // Test timestamp relational
    validCaseInputs.put(60,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.gt." + longPast1)));
    validCaseInputs.put(61,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture1)));
    validCaseInputs.put(62,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "created.lte." + longPast1)));
    validCaseInputs.put(63,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "created.gte." + farFuture1)));
    validCaseInputs.put(64,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.between." + longPast1 + "," + farFuture1)));
    validCaseInputs.put(65,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "created.nbetween." + longPast1 + "," + farFuture1)));
    // Variations of timestamp format
    validCaseInputs.put(66,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture2)));
    validCaseInputs.put(67,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture3)));
    validCaseInputs.put(68,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture4)));
    validCaseInputs.put(69,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture5)));
    validCaseInputs.put(70,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture6)));
    validCaseInputs.put(71,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture7)));
    validCaseInputs.put(72,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture8)));
    validCaseInputs.put(73,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture9)));
    validCaseInputs.put(74,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture10)));
    validCaseInputs.put(75,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture11)));
    validCaseInputs.put(76,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture12)));
    validCaseInputs.put(77,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture13)));
    validCaseInputs.put(78,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture14)));
    validCaseInputs.put(79,new CaseData(numApps, Arrays.asList("id.like." + appNameLikeAll, "created.lt." + farFuture15)));
    // Test wildcards
    validCaseInputs.put(80,new CaseData(numApps, Arrays.asList("enabled.eq.true","container_image.like.containerImage" + testKey + "*")));
    validCaseInputs.put(81,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "enabled.eq.true","container_image.nlike.containerImage" + testKey + "*")));
    validCaseInputs.put(82,new CaseData(9, Arrays.asList("id.like." + appNameLikeAll, "enabled.eq.true","container_image.like.containerImage" + testKey + "_00!")));
    validCaseInputs.put(83,new CaseData(11, Arrays.asList("id.like." + appNameLikeAll, "enabled.eq.true","container_image.nlike.containerImage" + testKey + "_00!")));
    // Test that underscore and % get escaped as needed before being used as SQL
    validCaseInputs.put(90,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "container_image.like.containerImage" + testKey + "_00_")));
    validCaseInputs.put(91,new CaseData(0, Arrays.asList("id.like." + appNameLikeAll, "container_image.like.containerImage" + testKey + "_00%")));
    // Check various special characters in description. 7 special chars in value: ,()~*!\
    validCaseInputs.put(101,new CaseData(1, Arrays.asList("id.like." + appNameLikeAll, "description.like." + specialChar7LikeSearchStr)));
    validCaseInputs.put(102,new CaseData(numApps-1, Arrays.asList("id.like." + appNameLikeAll, "description.nlike." + specialChar7LikeSearchStr)));
    validCaseInputs.put(103,new CaseData(1, Arrays.asList("id.like." + appNameLikeAll, "description.eq." + specialChar7EqSearchStr)));
    validCaseInputs.put(104,new CaseData(numApps-1, Arrays.asList("id.like." + appNameLikeAll, "description.neq." + specialChar7EqSearchStr)));
    // Escaped comma in a list of values
    validCaseInputs.put(110,new CaseData(1, Arrays.asList("id.like." + appNameLikeAll, "runtime_version.in." + "noSuchDir," + escapedCommanInListValue)));

    // Iterate over valid cases
    for (Map.Entry<Integer,CaseData> item : validCaseInputs.entrySet())
    {
      CaseData cd = item.getValue();
      int caseNum = item.getKey();
      System.out.println("Checking case # " + caseNum + " Input:        " + cd.searchList);
      // Build verified list of search conditions
      var verifiedSearchList = new ArrayList<String>();
      for (String cond : cd.searchList)
      {
        // Use SearchUtils to validate condition
        String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(cond);
        verifiedSearchList.add(verifiedCondStr);
      }
      System.out.println("  For case    # " + caseNum + " VerfiedInput: " + verifiedSearchList);
      List<App> searchResults = dao.getApps(tenantName, verifiedSearchList, null);
      System.out.println("  Result size: " + searchResults.size());
      assertEquals(searchResults.size(), cd.count,  "SearchDaoTest.testValidCases: Incorrect result count for case number: " + caseNum);
    }
  }
}
