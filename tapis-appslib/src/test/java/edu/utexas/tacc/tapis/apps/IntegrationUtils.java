package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotificationSubscription;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.NotificationMechanism;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/*
 * Utilities and data for integration testing
 */
public final class IntegrationUtils
{
  public static final Gson gson =  TapisGsonUtils.getGson();
  // Test data
  public static final String tenantName = "dev";
  public static final String ownerUser = "owner1";
  public static final String ownerUser2 = "owner2";
  public static final String apiUser = "testApiUser";
  public static final String appIdPrefix = "TestApp";
  public static final String appVersion = "0.0.1";
  public static final boolean enabledTrue = true;
  public static final boolean enabledFalse = false;
  public static final boolean strictFileInputsFalse = false;
  public static final boolean inPlaceTrue = true;
  public static final boolean inPlaceFalse = false;
  public static final boolean metaRequiredTrue = true;
  public static final boolean metaRequiredFalse = false;
  public static final Runtime runtime = Runtime.DOCKER;
  public static final String runtimeVersion = "0.0.1";
  public static final String containerImage = "containerImage";
  public static final boolean dynamicExecSystem = true;
  public static final String[] execSystemConstraints = {"Constraint1 AND", "Constraint2"};
  public static final String execSystemId = "execSystem";
  public static final String execSystemExecDir = "execSystemExecDir";
  public static final String execSystemInputDir = "execSystemInputDir";
  public static final String execSystemOutputDir = "execSystemOutputDir";
  public static final String execSystemLogicalQueue = "execSystemLogicalQueue";
  public static final String archiveSystemId = "archiveSystem";
  public static final String archiveSystemDir = "archiveSystemDir";
  public static final boolean archiveOnAppError = true;
  public static final String jobDescription = "job description 1";
  public static final int maxJobs = 1;
  public static final int maxJobsPerUser = 1;
  public static final int nodeCount = 1;
  public static final int coresPerNode = 1;
  public static final int memoryMb = 1;
  public static final int maxMinutes = 1;
  public static final String importRefIdNull = null;
  public static final boolean deletedFalse = false;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;

  public static final String[] metaKVPairs = {"key1=val1", "key2=val2"};
  public static final String[] envVariables = {"key1=val1", "key2=val2"};
  public static final String[] archiveIncludes = {"/include1", "/include2"};
  public static final String[] archiveExcludes = {"/exclude1", "/exclude2"};

  public static final String[] jobTags = {"jobtag1", "jobtag2"};
  public static final String[] tags = {"value1", "value2", "a",
            "a long tag with spaces and numbers (1 3 2) and special characters " +
            " [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final Object notes =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final JsonObject notesObj = (JsonObject) notes;
  public static final String scrubbedJson = "{}";

  public static final FileInput finA1 = new FileInput("/srcA1", "/targetA1", inPlaceTrue, "finA1", "File input A1",
                                                      metaRequiredTrue, metaKVPairs);
  public static final FileInput finB1 = new FileInput("/srcB1", "/targetB1", inPlaceFalse, "finB1", "File input B1",
                                                      metaRequiredFalse, metaKVPairs);
  public static final List<FileInput> finList1 = new ArrayList<>(List.of(finA1, finB1));
  public static final NotificationSubscription notifA1 =
          new NotificationSubscription("filterA1", NotificationMechanism.WEBHOOK, "webhookUrlA1", "emailAddressA1");
  public static final NotificationSubscription notifB1 =
          new NotificationSubscription("filterB1", NotificationMechanism.EMAIL, "webhookUrlB1", "emailAddressB1");
  public static final List<NotificationSubscription> notifList1 = new ArrayList<>(List.of(notifA1, notifB1));
  public static final AppArg appArgA1 = new AppArg("valueA1", "appArgA1", "App arg A1", metaRequiredTrue, metaKVPairs);
  public static final AppArg appArgB1 = new AppArg("valueB1", "appArgB1", "App arg B1", metaRequiredFalse, metaKVPairs);
  public static final List<AppArg> appArgList1 = new ArrayList<>(List.of(appArgA1, appArgB1));
  public static final AppArg containerArgA1 = new AppArg("valueA1", "containerArgA1", "Container arg A1",
                                                          metaRequiredTrue, metaKVPairs);
  public static final AppArg containerArgB1 = new AppArg("valueB1", "containerArgB1", "Container arg B1",
                                                         metaRequiredFalse, metaKVPairs);
  public static final List<AppArg> containerArgList1 = new ArrayList<>(List.of(containerArgA1, containerArgB1));
  public static final AppArg schedulerOptionA1 = new AppArg("valueA1", "schedulerOptionA1", "Scheduler option A1",
                                                            metaRequiredTrue, metaKVPairs);
  public static final AppArg schedulerOptionB1 = new AppArg("valueB1", "schedulerOptionB1", "Scheduler option B1",
                                                            metaRequiredFalse, metaKVPairs);
  public static final List<AppArg> schedulerOptionList1 = new ArrayList<>(List.of(schedulerOptionA1, schedulerOptionB1));

  /**
   * Create an array of App objects in memory
   * Names will be of format TestApp_K_NNN where K is the key and NNN runs from 000 to 999
   * We need a key because maven runs the tests in parallel so each set of apps created by an integration
   *   test will need its own namespace.
   * @param n number of apps to create
   * @return array of App objects
   */
  public static App[] makeApps(int n, String key)
  {
    App[] apps = new App[n];
    for (int i = 0; i < n; i++)
    {
      // Suffix which should be unique for each app within each integration test
      String suffix = key + "_" + String.format("%03d", i+1);
      String appId = appIdPrefix + "_" + suffix;
      // Constructor initializes all attributes except for JobCapabilities
      apps[i] = new App(-1, tenantName, appId, appVersion+suffix, "description "+suffix, AppType.BATCH, ownerUser, enabledTrue,
                        runtime, runtimeVersion+suffix, containerImage+suffix, maxJobs, maxJobsPerUser, strictFileInputsFalse,
                        jobDescription+suffix, dynamicExecSystem,
                        execSystemConstraints, execSystemId+suffix, execSystemExecDir+suffix, execSystemInputDir+suffix,
                        execSystemOutputDir+suffix, execSystemLogicalQueue+suffix, archiveSystemId+suffix,
                        archiveSystemDir+suffix, archiveOnAppError, nodeCount, coresPerNode, memoryMb, maxMinutes,
                        envVariables, archiveIncludes, archiveExcludes, jobTags,
                        tags, notes, importRefIdNull, deletedFalse, createdNull, updatedNull);
      // Aux table data
      apps[i].setFileInputs(finList1);
      apps[i].setNotifcationSubscriptions(notifList1);
      apps[i].setAppArgs(appArgList1);
      apps[i].setContainerArgs(containerArgList1);
      apps[i].setSchedulerOptions(schedulerOptionList1);
    }
    return apps;
  }
}
