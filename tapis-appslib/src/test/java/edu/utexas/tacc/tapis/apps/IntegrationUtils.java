package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
  public static final String appVersion2 = "0.0.2";
  public static final String descriptionNull = null;
  public static final AppType appType = AppType.BATCH;
  public static final String ownerNull = null;
  public static final boolean enabledTrue = true;
  public static final boolean enabledFalse = false;
  public static final boolean isDeletedFalse = false;
  public static final boolean containerizedTrue = true;
  public static final boolean containerizedFalse = false;
  public static final boolean strictFileInputsTrue = true;
  public static final boolean strictFileInputsFalse = false;
  public static final boolean inPlaceTrue = true;
  public static final boolean inPlaceFalse = false;
  public static final boolean metaRequiredTrue = true;
  public static final boolean metaRequiredFalse = false;
  public static final Runtime runtime = Runtime.DOCKER;
  public static final Runtime runtime2 = Runtime.SINGULARITY;
  public static final Runtime runtimeNull = null;
  public static final String runtimeVersion = "0.0.1";
  public static final String runtimeVersion2 = "0.0.2";
  public static final String runtimeVersionNull = null;
  public static final String containerImage = "containerImage";
  public static final String containerImage2 = "containerImage2";
  public static final String containerImageNull = null;
  public static final boolean dynamicExecSystemTrue = true;
  public static final boolean dynamicExecSystemFalse = false;
  public static final String[] execSystemConstraints = {"Constraint1a AND", "Constraint1b"};
  public static final String[] execSystemConstraints2 = {"Constraint2a AND", "Constraint2b"};
  public static final String[] execSystemConstraintsNull = null;
  public static final String execSystemId = "exec.system.org";
  public static final String execSystemId2 = "exec2.system.org";
  public static final String execSystemIdNull = null;
  public static final String execSystemExecDir = "execSystemExecDir";
  public static final String execSystemExecDirNull = null;
  public static final String execSystemInputDir = "execSystemInputDir";
  public static final String execSystemInputDirNull = null;
  public static final String execSystemOutputDir = "execSystemOutputDir";
  public static final String execSystemOutputDirNull = null;
  public static final String execSystemLogicalQueue = "execSystemLogicalQueue";
  public static final String execSystemLogicalQueueNull = null;
  public static final String archiveSystemId = "archive.system.org";
  public static final String archiveSystemIdNull = null;
  public static final String archiveSystemDir = "archiveSystemDir";
  public static final String archiveSystemDirNull = null;
  public static final boolean archiveOnAppError = true;
  public static final String jobDescription = "job description 1";
  public static final String jobDescriptionNull = null;
  public static final int maxJobs = 1;
  public static final int maxJobs2 = 2;
  public static final int maxJobsPerUser = 1;
  public static final int maxJobsPerUser2 = 2;
  public static final int nodeCount = 1;
  public static final int coresPerNode = 1;
  public static final int memoryMb = 1;
  public static final int maxMinutes = 1;
  public static final String importRefIdNull = null;
  public static final boolean deletedFalse = false;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;
  public static final UUID uuidNull = null;

  public static final String[] metaKVPairs = {"key1=val1", "key2=val2"};
  public static final String[] envVariables = {"key1=val1", "key2=val2"};
  public static final String[] envVariablesNull = null;
  public static final String[] archiveIncludes = {"/include1", "/include2"};
  public static final String[] archiveExcludes = {"/exclude1", "/exclude2"};
  public static final String[] archiveIncludesNull = null;
  public static final String[] archiveExcludesNull = null;

  public static final String[] jobTags = {"jobtag1a", "jobtag1b"};
  public static final String[] jobTags2 = {"jobtag2b", "jobtag2b"};
  public static final String[] jobTagsNull = null;
  public static final String[] tags = {"value1a", "value1b", "a",
            "a long tag with spaces and numbers (1 3 2) and special characters " +
            " [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final String[] tags2 = {"value2a", "value2b", "b"};
  public static final String[] tagsNull = null;
  public static final Object notes =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final Object notes2 =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj2\", \"testdata\": \"abc2\"}", JsonObject.class);
  public static final JsonObject notesObj = (JsonObject) notes;
  public static final String[] notesNull = null;
  public static final String scrubbedJson = "{}";

  public static final FileInput finA1 = new FileInput("/srcA1", "/targetA1", inPlaceTrue, "finA1", "File input A1",
                                                      metaRequiredTrue, metaKVPairs);
  public static final FileInput finB1 = new FileInput("/srcB1", "/targetB1", inPlaceFalse, "finB1", "File input B1",
                                                      metaRequiredFalse, metaKVPairs);
  public static final List<FileInput> finList1 = new ArrayList<>(List.of(finA1, finB1));

  public static final NotifMechanism notifMechA1 =
          new NotifMechanism(NotifMechanismType.WEBHOOK, "webhookUrlA1", "emailAddressA1");
  public static final NotifMechanism notifMechA2 =
          new NotifMechanism(NotifMechanismType.ACTOR, "webhookUrlA2", "emailAddressA2");
  public static final List<NotifMechanism> notifMechListA1 = new ArrayList<>(List.of(notifMechA1, notifMechA2));
  public static final NotifMechanism notifMechB1 =
          new NotifMechanism(NotifMechanismType.EMAIL, "webhookUrlB1", "emailAddressB1");
  public static final NotifMechanism notifMechB2 =
          new NotifMechanism(NotifMechanismType.QUEUE, "webhookUrlB2", "emailAddressB2");
  public static final List<NotifMechanism> notifMechListB1 = new ArrayList<>(List.of(notifMechB1, notifMechB2));
  public static final NotifSubscription notifA1 = new NotifSubscription("filterA1");
  public static final NotifSubscription notifB1 = new NotifSubscription("filterB1");
  static {
    notifA1.setNotificationMechanisms(notifMechListA1);
    notifB1.setNotificationMechanisms(notifMechListB1);
  }

  public static final List<NotifSubscription> notifList1 = new ArrayList<>(List.of(notifA1, notifB1));
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
      apps[i] = new App(-1, -1, tenantName, appId, appVersion+suffix, "description "+suffix, AppType.BATCH, ownerUser, enabledTrue,
                        containerizedTrue,
                        runtime, runtimeVersion+suffix, containerImage+suffix, maxJobs, maxJobsPerUser, strictFileInputsFalse,
                        jobDescription+suffix, dynamicExecSystemTrue,
                        execSystemConstraints, execSystemId, execSystemExecDir+suffix, execSystemInputDir+suffix,
                        execSystemOutputDir+suffix, execSystemLogicalQueue+suffix, archiveSystemId,
                        archiveSystemDir+suffix, archiveOnAppError, nodeCount, coresPerNode, memoryMb, maxMinutes,
                        envVariables, archiveIncludes, archiveExcludes, jobTags,
                        tags, notes, uuidNull, importRefIdNull, deletedFalse, createdNull, updatedNull);
      // Aux table data
      apps[i].setFileInputs(finList1);
      apps[i].setNotificationSubscriptions(notifList1);
      apps[i].setAppArgs(appArgList1);
      apps[i].setContainerArgs(containerArgList1);
      apps[i].setSchedulerOptions(schedulerOptionList1);
    }
    return apps;
  }

  /**
   * Create App in memory with minimal attributes set based on App given
   *   id, version, appType
   * NOTE: many args to constructor are primitives so cannot be set to null.
   */
  public static App makeMinimalApp(App app)
  {
    return new App(-1, -1, tenantName, app.getId(), app.getVersion(), descriptionNull, appType, ownerNull, enabledTrue,
            containerizedTrue, runtimeNull, runtimeVersionNull, containerImage, maxJobs, maxJobsPerUser,
            strictFileInputsFalse, jobDescriptionNull, dynamicExecSystemFalse, execSystemConstraintsNull,
            execSystemId, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            execSystemLogicalQueueNull, archiveSystemIdNull, archiveSystemDirNull, archiveOnAppError,
            nodeCount, coresPerNode, memoryMb, maxMinutes, envVariablesNull, archiveIncludesNull, archiveExcludesNull,
            jobTagsNull, tagsNull, notesNull, uuidNull, importRefIdNull, deletedFalse, createdNull, updatedNull);
  }
}
