package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.apps.IntegrationUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_LIMIT;
import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.DEFAULT_SKIP;
import static org.testng.Assert.assertEquals;

/**
 * Test the AppsDao getApps() call for various search use cases against a DB running locally
 * NOTE: This test pre-processes the search list just as is done in AppsServiceImpl before it calls the Dao,
 *       including calling SearchUtils.validateAndProcessSearchCondition(cond)
 *       For this reason there is currently no need to have a SearchAppsTest suite.
 *       If this changes then we will need to create another suite and move the test data into IntegrationUtils so that
 *       it can be re-used.
 */
@Test(groups={"integration"})
public class SearchDaoTest
{
  private AppsDaoImpl dao;

  // Test data
  private static final String testKey = "SrchGet";
  private static final String appIdLikeAll = "id.like.*" + testKey + "*";

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

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + SearchDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    ResourceRequestUser rUser = new ResourceRequestUser(new AuthenticatedUser(apiUser, tenantName,
            TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null, null));

    // Cleanup anything leftover from previous failed run
    teardown();

    // Vary maxJobs for checking numeric relational searches
    for (int i = 0; i < numApps; i++) { apps[i].setMaxJobs(i+1); }
    // For half the apps change the owner
    for (int i = 0; i < numApps/2; i++) { apps[i].setOwner(owner2); }

    // For one app update description to have some special characters. 7 special chars in value: ,()~*!\
    //   and update runtimeVersions for testing an escaped comma in a list value
    apps[numApps-1].setDescription(specialChar7Str);
    apps[numApps-1].setRuntimeVersion(escapedCommanInListValue);

    // Create all the apps in the dB using the in-memory objects, recording start and end times
    LocalDateTime createBegin = TapisUtils.getUTCTimeNow();
    Thread.sleep(500);
    for (App app : apps)
    {
      boolean itemCreated = dao.createApp(rUser, app, gson.toJson(app), scrubbedJson);
      Assert.assertTrue(itemCreated, "Item not created, id: " + app.getId());
    }
    Thread.sleep(500);
    LocalDateTime createEnd = TapisUtils.getUTCTimeNow();
    System.out.println("Total time taken for BeforeSuite setup method: " + Duration.between(createBegin, createEnd));
  }

  @AfterSuite
  public void teardown() throws Exception {
    System.out.println("Executing AfterSuite teardown for " + SearchDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (App app : apps)
    {
      dao.hardDeleteApp(tenantName, app.getId());
    }
    Assert.assertFalse(dao.checkForApp(tenantName, apps[0].getId(), true),
                       "App not deleted. App id: " + apps[0].getId());
  }

  /*
   * Check valid cases
   */
  @Test(groups={"integration"})
  public void testValidCases() throws Exception
  {
    App app0 = apps[0];
    String app0Id = app0.getId();
    String nameList = "noSuchName1,noSuchName2," + app0Id + ",noSuchName3";
    // Create all input and validation data for tests
    // NOTE: Some cases require appNameLikeAll in the list of conditions since maven runs the tests in
    //       parallel and not all attribute names are unique across integration tests
    class CaseData
    {
      public final int count;
      public final List<String> searchList;
      CaseData(int c, List<String> r) { count = c; searchList = r; }
    }
    var validCaseInputs = new HashMap<Integer, CaseData>();
    // Test basic types and operators
    validCaseInputs.put( 1,new CaseData(1, List.of("id.eq." + app0Id))); // 1 has specific id
    validCaseInputs.put( 2,new CaseData(1, List.of("version.eq." + app0.getVersion())));
    validCaseInputs.put( 3,new CaseData(1, List.of("description.eq." + app0.getDescription())));
    validCaseInputs.put( 4,new CaseData(1, List.of("runtime_version.eq." + app0.getRuntimeVersion())));
    validCaseInputs.put( 5,new CaseData(1, List.of("container_image.eq." + app0.getContainerImage())));
    validCaseInputs.put( 6,new CaseData(1, List.of("job_description.eq." + app0.getJobDescription())));
    validCaseInputs.put( 7,new CaseData(numApps, Arrays.asList(appIdLikeAll, "exec_system_id.eq." + app0.getExecSystemId())));
    validCaseInputs.put( 8,new CaseData(1, List.of("exec_system_exec_dir.eq." + app0.getExecSystemExecDir())));
    validCaseInputs.put( 9,new CaseData(1, List.of("exec_system_input_dir.eq." + app0.getExecSystemInputDir())));
    validCaseInputs.put(10,new CaseData(numApps/2, Arrays.asList(appIdLikeAll, "owner.eq." + owner1)));  // Half owned by one user
    validCaseInputs.put(11,new CaseData(numApps/2, Arrays.asList(appIdLikeAll, "owner.eq." + owner2))); // and half owned by another
    validCaseInputs.put(12,new CaseData(numApps, Arrays.asList(appIdLikeAll, "enabled.eq.true")));  // All are enabled
    validCaseInputs.put(13,new CaseData(numApps, Arrays.asList(appIdLikeAll, "deleted.eq.false"))); // none are deleted
    validCaseInputs.put(14,new CaseData(numApps, Arrays.asList(appIdLikeAll, "deleted.neq.true"))); // none are deleted
    validCaseInputs.put(15,new CaseData(0, Arrays.asList(appIdLikeAll, "deleted.eq.true")));           // none are deleted
    validCaseInputs.put(16,new CaseData(1, List.of("id.like." + app0Id)));
    validCaseInputs.put(17,new CaseData(0, List.of("id.like.NOSUCHAPPxFM2c29bc8RpKWeE2sht7aZrJzQf3s")));
    validCaseInputs.put(18,new CaseData(numApps, List.of(appIdLikeAll)));
    validCaseInputs.put(19,new CaseData(numApps-1, Arrays.asList(appIdLikeAll, "id.nlike." + app0Id)));
    validCaseInputs.put(20,new CaseData(1, Arrays.asList(appIdLikeAll, "id.in." + nameList)));
    validCaseInputs.put(21,new CaseData(numApps-1, Arrays.asList(appIdLikeAll, "id.nin." + nameList)));
    validCaseInputs.put(22,new CaseData(numApps, Arrays.asList(appIdLikeAll, "app_type.eq.BATCH")));
    validCaseInputs.put(23,new CaseData(numApps/2, Arrays.asList(appIdLikeAll, "app_type.eq.BATCH","owner.neq." + owner2)));
    // Test numeric relational
    validCaseInputs.put(40,new CaseData(numApps/2, Arrays.asList(appIdLikeAll, "max_jobs.between.1," + numApps/2)));
    validCaseInputs.put(41,new CaseData(numApps/2-1, Arrays.asList(appIdLikeAll, "max_jobs.between.2," + numApps/2)));
    validCaseInputs.put(42,new CaseData(numApps/2, Arrays.asList(appIdLikeAll, "max_jobs.nbetween.1," + numApps/2)));
    validCaseInputs.put(43,new CaseData(13, Arrays.asList(appIdLikeAll, "enabled.eq.true","max_jobs.lte.13")));
    validCaseInputs.put(44,new CaseData(5, Arrays.asList(appIdLikeAll,"enabled.eq.true","max_jobs.gt.1","max_jobs.lt.7")));
    // Test char relational
    validCaseInputs.put(50,new CaseData(1, Arrays.asList(appIdLikeAll,"container_image.lte."+containerImageName1)));
    validCaseInputs.put(51,new CaseData(numApps-7, Arrays.asList(appIdLikeAll,"enabled.eq.true","container_image.gt."+containerImageName7)));
    validCaseInputs.put(52,new CaseData(5, Arrays.asList(appIdLikeAll,"container_image.gt."+containerImageName1,"container_image.lt."+containerImageName7)));
    validCaseInputs.put(53,new CaseData(0, Arrays.asList(appIdLikeAll,"container_image.lte."+containerImageName1,"container_image.gt."+containerImageName7)));
    validCaseInputs.put(54,new CaseData(7, Arrays.asList(appIdLikeAll,"container_image.between."+containerImageName1+","+containerImageName7)));
    validCaseInputs.put(55,new CaseData(numApps-7, Arrays.asList(appIdLikeAll,"container_image.nbetween."+containerImageName1+","+containerImageName7)));
    // Test timestamp relational
    validCaseInputs.put(60,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.gt." + longPast1)));
    validCaseInputs.put(61,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture1)));
    validCaseInputs.put(62,new CaseData(0, Arrays.asList(appIdLikeAll, "created.lte." + longPast1)));
    validCaseInputs.put(63,new CaseData(0, Arrays.asList(appIdLikeAll, "created.gte." + farFuture1)));
    validCaseInputs.put(64,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.between." + longPast1 + "," + farFuture1)));
    validCaseInputs.put(65,new CaseData(0, Arrays.asList(appIdLikeAll, "created.nbetween." + longPast1 + "," + farFuture1)));
    // Variations of timestamp format
    validCaseInputs.put(66,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture2)));
    validCaseInputs.put(67,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture3)));
    validCaseInputs.put(68,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture4)));
    validCaseInputs.put(69,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture5)));
    validCaseInputs.put(70,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture6)));
    validCaseInputs.put(71,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture7)));
    validCaseInputs.put(72,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture8)));
    validCaseInputs.put(73,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture9)));
    validCaseInputs.put(74,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture10)));
    validCaseInputs.put(75,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture11)));
    validCaseInputs.put(76,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture12)));
    validCaseInputs.put(77,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture13)));
    validCaseInputs.put(78,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture14)));
    validCaseInputs.put(79,new CaseData(numApps, Arrays.asList(appIdLikeAll, "created.lt." + farFuture15)));
    // Test wildcards
    validCaseInputs.put(80,new CaseData(numApps, Arrays.asList("enabled.eq.true","container_image.like.containerImage" + testKey + "*")));
    validCaseInputs.put(81,new CaseData(0, Arrays.asList(appIdLikeAll, "enabled.eq.true","container_image.nlike.containerImage" + testKey + "*")));
    validCaseInputs.put(82,new CaseData(9, Arrays.asList(appIdLikeAll, "enabled.eq.true","container_image.like.containerImage" + testKey + "_00!")));
    validCaseInputs.put(83,new CaseData(11, Arrays.asList(appIdLikeAll, "enabled.eq.true","container_image.nlike.containerImage" + testKey + "_00!")));
    // Test that underscore and % get escaped as needed before being used as SQL
    validCaseInputs.put(90,new CaseData(0, Arrays.asList(appIdLikeAll, "container_image.like.containerImage" + testKey + "_00_")));
    validCaseInputs.put(91,new CaseData(0, Arrays.asList(appIdLikeAll, "container_image.like.containerImage" + testKey + "_00%")));
    // Check various special characters in description. 7 special chars in value: ,()~*!\
    validCaseInputs.put(101,new CaseData(1, Arrays.asList(appIdLikeAll, "description.like." + specialChar7LikeSearchStr)));
    validCaseInputs.put(102,new CaseData(numApps-1, Arrays.asList(appIdLikeAll, "description.nlike." + specialChar7LikeSearchStr)));
    validCaseInputs.put(103,new CaseData(1, Arrays.asList(appIdLikeAll, "description.eq." + specialChar7EqSearchStr)));
    validCaseInputs.put(104,new CaseData(numApps-1, Arrays.asList(appIdLikeAll, "description.neq." + specialChar7EqSearchStr)));
    // Escaped comma in a list of values
    validCaseInputs.put(110,new CaseData(1, Arrays.asList(appIdLikeAll, "runtime_version.in." + "noSuchDir," + escapedCommanInListValue)));

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
      List<App> searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, DEFAULT_LIMIT, orderByListNull,
                                            DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
      System.out.println("  Result size: " + searchResults.size());
      assertEquals(searchResults.size(), cd.count,  "SearchDaoTest.testValidCases: Incorrect result count for case number: " + caseNum);
    }
  }

  /*
   * Test pagination options: limit, skip
   */
  @Test(groups={"integration"})
  public void testLimitSkip() throws Exception
  {
    String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(appIdLikeAll);
    var verifiedSearchList = Collections.singletonList(verifiedCondStr);
    System.out.println("VerfiedInput: " + verifiedSearchList);
    List<App> searchResults;

    int limit = -1;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    limit = 0;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    limit = 1;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    limit = 5;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    limit = 19;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    limit = 20;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    limit = 200;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    // Test limit + skip combination that reduces result size
    int resultSize = 3;
    limit = numApps;
    int skip = limit - resultSize;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), resultSize, "Incorrect result count");

    // Check some corner cases
    limit = 100;
    skip = 0;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    limit = 0;
    skip = 1;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), 0, "Incorrect result count");
    limit = 10;
    skip = 15;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps - skip, "Incorrect result count");
    limit = 10;
    skip = 100;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListNull, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), 0, "Incorrect result count");
  }

  /*
   * Test sorting: limit, orderBy, skip
   */
  @Test(groups={"integration"})
  public void testSortingSkip() throws Exception
  {
    String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(appIdLikeAll);
    var verifiedSearchList = Collections.singletonList(verifiedCondStr);
    System.out.println("VerfiedInput: " + verifiedSearchList);
    List<App> searchResults;

    int limit;
    int skip;
    // Sort and check order with no limit or skip
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, DEFAULT_LIMIT, orderByListAsc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    checkOrder(searchResults, 1, numApps);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, DEFAULT_LIMIT, orderByListDesc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    checkOrder(searchResults, numApps, 1);
    // Sort and check order with limit and no skip
    limit = 4;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListAsc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    checkOrder(searchResults, 1, limit);
    limit = 19;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListDesc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    checkOrder(searchResults, numApps, numApps - (limit-1));
    // Sort and check order with limit and skip
    limit = 2;
    skip = 5;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListAsc, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get systems named SrchGet_006 to SrchGet_007
    checkOrder(searchResults, skip + 1, skip + limit);
    limit = 4;
    skip = 3;
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListDesc, skip, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get systems named SrchGet_017 to SrchGet_014
    checkOrder(searchResults, numApps - skip, numApps - limit);

    // Sort and check multiple orderBy
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, DEFAULT_LIMIT, orderByList2Asc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    checkOrder(searchResults, 1, numApps);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, DEFAULT_LIMIT, orderByList2Desc, DEFAULT_SKIP, startAfterNull, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), numApps, "Incorrect result count");
    checkOrder(searchResults, numApps, 1);
  }

  /*
   * Test sorting: limit, orderBy, startAfter
   */
  @Test(groups={"integration"})
  public void testSortingStartAfter() throws Exception
  {
    String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(appIdLikeAll);
    var verifiedSearchList = Collections.singletonList(verifiedCondStr);
    System.out.println("VerfiedInput: " + verifiedSearchList);
    List<App> searchResults;

    int limit;
    int startAfterIdx;
    String startAfter;
    // Sort and check order with limit and startAfter
    limit = 2;
    startAfterIdx = 5;
    startAfter = getAppName(testKey, startAfterIdx);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListAsc, DEFAULT_SKIP, startAfter, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get systems named SrchGet_006 to SrchGet_007
    checkOrder(searchResults, startAfterIdx + 1, startAfterIdx + limit);
    limit = 4;
    startAfterIdx = 18;
    int startWith = numApps - startAfterIdx + 1;
    startAfter = getAppName(testKey, startAfterIdx);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByListDesc, DEFAULT_SKIP, startAfter, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get apps named SrchGet_017 to SrchGet_014
    checkOrder(searchResults, numApps - startWith, numApps - limit);

    // Sort and check multiple orderBy (second order orderBy column has no effect but at least we can check that
    //    having it does not break things for startAfter
    limit = 2;
    startAfterIdx = 5;
    startAfter = getAppName(testKey, startAfterIdx);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByList3Asc, DEFAULT_SKIP, startAfter, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get systems named SrchGet_006 to SrchGet_007
    checkOrder(searchResults, startAfterIdx + 1, startAfterIdx + limit);
    limit = 4;
    startAfterIdx = 18;
    startWith = numApps - startAfterIdx + 1;
    startAfter = getContainerImage(testKey, startAfterIdx);
    searchResults = dao.getApps(tenantName, verifiedSearchList, null, null, limit, orderByList3Desc, DEFAULT_SKIP, startAfter, versionSpecifiedNull, showDeletedFalse);
    assertEquals(searchResults.size(), limit, "Incorrect result count");
    // Should get systems named SrchGet_017 to SrchGet_014
    checkOrder(searchResults, numApps - startWith, numApps - limit);
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /**
   * Check that results were sorted in correct order when sorting on app id
   */
  private void checkOrder(List<App> searchResults, int start, int end)
  {
    int idx = 0; // Position in result
    // Name should match for loop counter i
    if (start < end)
    {
      for (int i = start; i <= end; i++)
      {
        String sysName = getAppName(testKey, i);
        assertEquals(searchResults.get(idx).getId(), sysName, "Incorrect app Id at position: " + (idx+1));
        idx++;
      }
    }
    else
    {
      for (int i = start; i >= end; i--)
      {
        String sysName = getAppName(testKey, i);
        assertEquals(searchResults.get(idx).getId(), sysName, "Incorrect app Id at position: " + (idx+1));
        idx++;
      }
    }
  }
}
