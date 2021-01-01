package edu.utexas.tacc.tapis.apps.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.parser.ASTParser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

import static edu.utexas.tacc.tapis.apps.IntegrationUtils.*;

/**
 * Test the AppsDao getAppsUsingSearchAST() call for various search use cases against a DB running locally
 * NOTE: This test pre-processes the sql-like search string just as is done in AppsServiceImpl before it calls the Dao.
 *       For this reason there is currently no need to have a SearchAppsTest suite.
 *       If this changes then we will need to create another suite and move the test data into IntegrationUtils so that
 *       it can be re-used.
 * TODO: Test that timestamps are handling timezone correctly.
 */
@Test(groups={"integration"})
public class SearchASTDaoTest
{
  private AppsDaoImpl dao;
  private AuthenticatedUser authenticatedUser;

  // Test data
  private static final String testKey = "SrchAST";
  private static final String appNameLikeAll = sq("%" + testKey + "%");

  // Strings for searches involving special characters
  private static final String specialChar7Str = ",()~*!\\"; // These 7 may need escaping
  private static final String specialChar7LikeSearchStr = "\\,\\(\\)\\~\\*\\!\\\\"; // All need escaping for LIKE/NLIKE
  private static final String specialChar7EqSearchStr = "\\,\\(\\)\\~*!\\"; // All but *! need escaping for other operators

  // String for search involving an escaped comma in a list of values
  private static final String escapedCommanInListValue = "abc\\,def";
//
//  // Timestamps in various formats
//  private static final String longPast1 =   "1800-01-01T00:00:00.123456Z";
//  private static final String farFuture1 =  "2200-04-29T14:15:52.123456-06:00";
//  private static final String farFuture2 =  "2200-04-29T14:15:52.123Z";
//  private static final String farFuture3 =  "2200-04-29T14:15:52.123";
//  private static final String farFuture4 =  "2200-04-29T14:15:52-06:00";
//  private static final String farFuture5 =  "2200-04-29T14:15:52";
//  private static final String farFuture6 =  "2200-04-29T14:15-06:00";
//  private static final String farFuture7 =  "2200-04-29T14:15";
//  private static final String farFuture8 =  "2200-04-29T14-06:00";
//  private static final String farFuture9 =  "2200-04-29T14";
//  private static final String farFuture10 = "2200-04-29-06:00";
//  private static final String farFuture11 = "2200-04-29";
//  private static final String farFuture12 = "2200-04Z";
//  private static final String farFuture13 = "2200-04";
//  private static final String farFuture14 = "2200Z";
//  private static final String farFuture15 = "2200";
//
//  // Strings for char relational testings
//  private static final String hostName1 = "host" + testKey + "_001";
//  private static final String hostName7 = "host" + testKey + "_007";

  int numApps = 20;
  App[] apps = IntegrationUtils.makeApps(numApps, testKey);

  LocalDateTime createBegin;
  LocalDateTime createEnd;

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + SearchASTDaoTest.class.getSimpleName());
    dao = new AppsDaoImpl();
    // Initialize authenticated user
    authenticatedUser = new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(), null, apiUser, tenantName, null, null, null);

    // Cleanup anything leftover from previous failed run
    teardown();

 //    Vary maxJobs for checking numeric relational searches
    for (int i = 0; i < numApps; i++) { apps[i].setMaxJobs(i+1); }
    // For half the apps change the owner
    for (int i = 0; i < numApps/2; i++) { apps[i].setOwner(ownerUser2); }

    // For one app update description to have some special characters. 7 special chars in value: ,()~*!\
    //   and update archiveLocalDir for testing an escaped comma in a list value
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
    System.out.println("Executing AfterSuite teardown for " + SearchASTDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (App app : apps)
    {
      dao.hardDeleteApp(tenantName, app.getId());
    }

    App tmpapp = dao.getApp(tenantName, apps[0].getId(), true);
    Assert.assertNull(tmpapp, "app not deleted. app name: " + apps[0].getId());
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
    // NOTE: Some cases require "id LIKE " + appNameLikeAll in the list of conditions since maven runs the tests in
    //       parallel and not all attribute names are unique across integration tests
    class CaseData {public final int count; public final String sqlSearchStr; CaseData(int c, String s) { count = c; sqlSearchStr = s; }}
    var validCaseInputs = new HashMap<Integer, CaseData>();
    // Test basic types and operators
    validCaseInputs.put( 1,new CaseData(1, "id = " + app0Name)); // 1 has specific name
    validCaseInputs.put( 2,new CaseData(1, "version = " + sq(app0.getVersion())));
    validCaseInputs.put( 3,new CaseData(1, "description = " + sq(app0.getDescription())));
    validCaseInputs.put( 4,new CaseData(1, "runtime_version = " + sq(app0.getRuntimeVersion())));
    validCaseInputs.put( 5,new CaseData(1, "container_image = " + sq(app0.getContainerImage())));
    validCaseInputs.put( 6,new CaseData(1, "exec_system_id = " + sq(app0.getExecSystemId())));
    validCaseInputs.put( 7,new CaseData(1, "exec_system_exec_dir = " + sq(app0.getExecSystemExecDir())));
    validCaseInputs.put( 8,new CaseData(1, "exec_system_input_dir = " + app0.getExecSystemInputDir()));
    validCaseInputs.put( 9,new CaseData(1, "exec_system_output_dir = " + app0.getExecSystemOutputDir()));
    validCaseInputs.put(10,new CaseData(numApps/2, "id LIKE " + appNameLikeAll + " AND owner = " + sq(ownerUser)));  // Half owned by one user
    validCaseInputs.put(11,new CaseData(numApps/2, "id LIKE " + appNameLikeAll + " AND owner = " + sq(ownerUser2))); // and half owned by another
    validCaseInputs.put(12,new CaseData(numApps, "id LIKE " + appNameLikeAll + " AND enabled = true"));  // All are enabled
    validCaseInputs.put(13,new CaseData(numApps, "id LIKE " + appNameLikeAll + " AND deleted = false")); // none are deleted
    validCaseInputs.put(14,new CaseData(numApps, "id LIKE " + appNameLikeAll + " AND deleted <> true")); // none are deleted
    validCaseInputs.put(15,new CaseData(0, "id LIKE " + appNameLikeAll + " AND deleted = true"));        // none are deleted
    validCaseInputs.put(16,new CaseData(1, "id LIKE " + sq(app0Name)));
    validCaseInputs.put(17,new CaseData(0, "id LIKE 'NOSUCHAPPxFM2c29bc8RpKWeE2sht7aZrJzQf3s'"));
    validCaseInputs.put(18,new CaseData(numApps, "id LIKE " + appNameLikeAll));
// TODO - continue
//    validCaseInputs.put(19,new CaseData(numApps-1, "id LIKE " + appNameLikeAll + " AND id NLIKE " + app0Name)); // TODO support NLIKE
//    validCaseInputs.put(20,new CaseData(1, "id LIKE " + appNameLikeAll + " AND id IN " + nameList)); // TODO
//    validCaseInputs.put(21,new CaseData(numApps-1, "id LIKE " + appNameLikeAll, "name.nin." + nameList));
//    validCaseInputs.put(22,new CaseData(numApps, "id LIKE " + appNameLikeAll, "app_type = LINUX"));
//    validCaseInputs.put(23,new CaseData(numApps/2, "id LIKE " + appNameLikeAll, "app_type = LINUX","owner <> " + sq(ownerUser2)));
//    // Test numeric relational
//    validCaseInputs.put(50,new CaseData(numApps/2, "id LIKE " + appNameLikeAll, "max_jobs.between.1," + numApps/2));
//    validCaseInputs.put(51,new CaseData(numApps/2-1, "id LIKE " + appNameLikeAll, "max_jobs.between.2," + numApps/2));
//    validCaseInputs.put(52,new CaseData(numApps/2, "id LIKE " + appNameLikeAll, "max_jobs.nbetween.1," + numApps/2));
//    validCaseInputs.put(53,new CaseData(13, "id LIKE " + appNameLikeAll, "enabled = true","max_jobs.lte.13"));
//    validCaseInputs.put(54,new CaseData(5, "id LIKE " + appNameLikeAll,"enabled = true","max_jobs.gt.1","max_jobs.lt.7"));
//    // Test char relational
//    validCaseInputs.put(70,new CaseData(1, "id LIKE " + appNameLikeAll,"host.lt."+hostName1));
//    validCaseInputs.put(71,new CaseData(numApps-8, "id LIKE " + appNameLikeAll,"enabled = true","host.gt."+hostName7));
//    validCaseInputs.put(72,new CaseData(5, "id LIKE " + appNameLikeAll,"host.gt."+hostName1,"host.lt."+hostName7));
//    validCaseInputs.put(73,new CaseData(0, "id LIKE " + appNameLikeAll,"host.lt."+hostName1,"host.gt."+hostName7));
//    validCaseInputs.put(74,new CaseData(7, "id LIKE " + appNameLikeAll,"host.between."+hostName1+","+hostName7));
//    validCaseInputs.put(75,new CaseData(numApps-7, "id LIKE " + appNameLikeAll,"host.nbetween."+hostName1+","+hostName7));
//    // Test timestamp relational
//    validCaseInputs.put(90,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.gt." + longPast1));
//    validCaseInputs.put(91,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture1));
//    validCaseInputs.put(92,new CaseData(0, "id LIKE " + appNameLikeAll, "created.lte." + longPast1));
//    validCaseInputs.put(93,new CaseData(0, "id LIKE " + appNameLikeAll, "created.gte." + farFuture1));
//    validCaseInputs.put(94,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.between." + longPast1 + "," + farFuture1));
//    validCaseInputs.put(95,new CaseData(0, "id LIKE " + appNameLikeAll, "created.nbetween." + longPast1 + "," + farFuture1));
//    // Variations of timestamp format
//    validCaseInputs.put(96,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture2));
//    validCaseInputs.put(97,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture3));
//    validCaseInputs.put(98,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture4));
//    validCaseInputs.put(99,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture5));
//    validCaseInputs.put(100,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture6));
//    validCaseInputs.put(101,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture7));
//    validCaseInputs.put(102,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture8));
//    validCaseInputs.put(103,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture9));
//    validCaseInputs.put(104,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture10));
//    validCaseInputs.put(105,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture11));
//    validCaseInputs.put(106,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture12));
//    validCaseInputs.put(107,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture13));
//    validCaseInputs.put(108,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture14));
//    validCaseInputs.put(109,new CaseData(numApps, "id LIKE " + appNameLikeAll, "created.lt." + farFuture15));
//    // Test wildcards
//    validCaseInputs.put(130,new CaseData(numApps, "enabled = true","host LIKE host" + testKey + "*"));
//    validCaseInputs.put(131,new CaseData(0, "id LIKE " + appNameLikeAll, "enabled = true","host NLIKE host" + testKey + "*"));
//    validCaseInputs.put(132,new CaseData(10, "id LIKE " + appNameLikeAll, "enabled = true","host LIKE host" + testKey + "_00!"));
//    validCaseInputs.put(133,new CaseData(10, "id LIKE " + appNameLikeAll, "enabled = true","host NLIKE host" + testKey + "_00!"));
//    // Test that underscore and % get escaped as needed before being used as SQL
//    validCaseInputs.put(150,new CaseData(0, "id LIKE " + appNameLikeAll, "host LIKE host" + testKey + "_00_"));
//    validCaseInputs.put(151,new CaseData(0, "id LIKE " + appNameLikeAll, "host LIKE host" + testKey + "_00%"));
//    // Check various special characters in description. 7 special chars in value: ,()~*!\
//    validCaseInputs.put(171,new CaseData(1, "id LIKE " + appNameLikeAll, "description LIKE " + specialChar7LikeSearchStr));
//    validCaseInputs.put(172,new CaseData(numApps-1, "id LIKE " + appNameLikeAll, "description NLIKE " + specialChar7LikeSearchStr));
//    validCaseInputs.put(173,new CaseData(1, "id LIKE " + appNameLikeAll, "description = " + specialChar7EqSearchStr));
//    validCaseInputs.put(174,new CaseData(numApps-1, "id LIKE " + appNameLikeAll, "description <> " + specialChar7EqSearchStr));
//    // Escaped comma in a list of values
//    validCaseInputs.put(200,new CaseData(1, "id LIKE " + appNameLikeAll, "job_local_archive_dir IN " + "noSuchDir," + escapedCommanInListValue));

    // Iterate over valid cases
    for (Map.Entry<Integer,CaseData> item : validCaseInputs.entrySet())
    {
      CaseData cd = item.getValue();
      int caseNum = item.getKey();
      System.out.println("Checking case # " + caseNum + " Input:        " + cd.sqlSearchStr);
      // Build an AST from the sql-like search string
      ASTNode searchAST = ASTParser.parse(cd.sqlSearchStr);
      System.out.println("  Created AST with leaf node count: " + searchAST.countLeaves());
      List<App> searchResults = dao.getAppsUsingSearchAST(tenantName, searchAST, null);
      System.out.println("  Result size: " + searchResults.size());
      assertEquals(searchResults.size(), cd.count, "SearchASTDaoTest.testValidCases: Incorrect result count for case number: " + caseNum);
    }
  }

  private static String sq(String s) { return "'" + s + "'"; }
}
