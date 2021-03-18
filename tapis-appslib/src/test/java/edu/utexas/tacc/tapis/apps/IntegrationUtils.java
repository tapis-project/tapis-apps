package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
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
  public static final String ownerUser2 = "testuser2";
  public static final String apiUser = "testuser2";
  public static final String appIdPrefix = "TestApp";
  public static final String appVersion = "0.0.1";
  public static final String appVersion2 = "0.0.2";
  public static final String description1 = "App description 1";
  public static final String description2 = "App description 2";
  public static final String descriptionNull = null;
  public static final AppType appType = AppType.BATCH;
  public static final String ownerNull = null;
  public static final boolean enabledTrue = true;
  public static final boolean enabledFalse = false;
  public static final boolean isDeletedFalse = false;
  public static final boolean containerizedTrue = true;
  public static final boolean strictFileInputsTrue = true;
  public static final boolean strictFileInputsFalse = false;
  public static final boolean inPlaceTrue = true;
  public static final boolean inPlaceFalse = false;
  public static final boolean metaRequiredTrue = true;
  public static final boolean metaRequiredFalse = false;
  public static final Runtime runtime1 = Runtime.DOCKER;
  public static final Runtime runtime2 = Runtime.SINGULARITY;
  public static final Runtime runtimeNull = null;
  public static final String runtimeVersion1 = "0.0.1";
  public static final String runtimeVersion2 = "0.0.2";
  public static final String runtimeVersionNull = null;
  public static final String containerImage1 = "containerImage";
  public static final String containerImage2 = "containerImage2";
  public static final String containerImageNull = null;
  public static final String jobDescription1 = "Description of job 1";
  public static final String jobDescription2 = "Description of job 2";
  public static final String jobDescriptionNull = null;
  public static final boolean dynamicExecSystemTrue = true;
  public static final boolean dynamicExecSystemFalse = false;
  public static final String[] execSystemConstraints1 = {"Constraint1a AND", "Constraint1b"};
  public static final String[] execSystemConstraints2 = {"Constraint2a AND", "Constraint2b"};
  public static final String[] execSystemConstraintsNull = null;
  public static final String execSystemId1 = "tapisv3-exec";
  public static final String execSystemId2 = "tapisv3-exec2";
  public static final String execSystemIdNull = null;
  public static final String execSystemExecDir1 = "execSystemExecDir1";
  public static final String execSystemExecDir2 = "execSystemExecDir2";
  public static final String execSystemExecDirNull = null;
  public static final String execSystemInputDir1 = "execSystemInputDir1";
  public static final String execSystemInputDir2 = "execSystemInputDir2";
  public static final String execSystemInputDirNull = null;
  public static final String execSystemOutputDir1 = "execSystemOutputDir1";
  public static final String execSystemOutputDir2 = "execSystemOutputDir2";
  public static final String execSystemOutputDirNull = null;
  public static final String execSystemLogicalQueue1 = "execSystemLogicalQueue1";
  public static final String execSystemLogicalQueue2 = "execSystemLogicalQueue2";
  public static final String execSystemLogicalQueueNull = null;
  public static final String archiveSystemId1 = "tapisv3-storage";
  public static final String archiveSystemId2 = "tapisv3-storage-dev";
  public static final String archiveSystemIdNull = null;
  public static final String archiveSystemDir1 = "archiveSystemDir1";
  public static final String archiveSystemDir2 = "archiveSystemDir2";
  public static final String archiveSystemDirNull = null;
  public static final boolean archiveOnAppErrorTrue = true;
  public static final boolean archiveOnAppErrorFalse = false;
  public static final int maxJobs1 = 1;
  public static final int maxJobs2 = 2;
  public static final int maxJobsPerUser1 = 1;
  public static final int maxJobsPerUser2 = 2;
  public static final int nodeCount1 = 1;
  public static final int nodeCount2 = 2;
  public static final int coresPerNode1 = 1;
  public static final int coresPerNode2 = 2;
  public static final int memoryMb1 = 1;
  public static final int memoryMb2 = 2;
  public static final int maxMinutes1 = 1;
  public static final int maxMinutes2 = 2;
  public static final boolean deletedFalse = false;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;
  public static final UUID uuidNull = null;

  public static final String[] metaKVPairs1 = {"key1A=val1A", "key1B=val1B"};
  public static final String[] metaKVPairs2 = {"key2A=val2A", "key2B=val2B"};
  public static final String[] envVariables1 = {"key1A=val1A", "key1B=val1B"};
  public static final String[] envVariables2 = {"key2A=val2A", "key2B=val2B"};
  public static final String[] envVariablesNull = null;
  public static final String[] archiveIncludes1 = {"/include1A", "/include1B"};
  public static final String[] archiveIncludes2 = {"/include2A", "/include2B"};
  public static final String[] archiveIncludesNull = null;
  public static final String[] archiveExcludes1 = {"/exclude1A", "/exclude1B"};
  public static final String[] archiveExcludes2 = {"/exclude2A", "/exclude2B"};
  public static final String[] archiveExcludesNull = null;

  public static final String[] jobTags1 = {"jobtag1a", "jobtag1b"};
  public static final String[] jobTags2 = {"jobtag2b", "jobtag2b"};
  public static final String[] jobTagsNull = null;
  public static final String[] tags1 = {"value1a", "value1b", "a",
            "a long tag with spaces and numbers (1 3 2) and special characters " +
            " [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final String[] tags2 = {"value2a", "value2b", "b"};
  public static final String[] tagsNull = null;
  public static final Object notes1 =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final Object notes2 =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj2\", \"testdata\": \"abc2\"}", JsonObject.class);
  public static final JsonObject notes1Obj = (JsonObject) notes1;
  public static final String[] notesNull = null;
  public static final String scrubbedJson = "{}";

  // FileInputs
  public static final FileInput fin1A = new FileInput("/src1A", "/target1A", inPlaceTrue, "fin1A", "File input 1A",
                                                      metaRequiredTrue, metaKVPairs1);
  public static final FileInput fin1B = new FileInput("/src1B", "/target1B", inPlaceFalse, "fin1B", "File input 1B",
                                                      metaRequiredFalse, metaKVPairs1);
  public static final List<FileInput> finList1 = new ArrayList<>(List.of(fin1A, fin1B));
  public static final FileInput finA2 = new FileInput("/srcA2", "/targetA2", inPlaceTrue, "finA2", "File input A2",
                                                      metaRequiredTrue, metaKVPairs2);
  public static final FileInput finB2 = new FileInput("/srcB2", "/targetB2", inPlaceFalse, "finB2", "File input B2",
                                                      metaRequiredFalse, metaKVPairs2);
  public static final List<FileInput> finList2 = new ArrayList<>(List.of(finA2, finB2));
  public static final List<FileInput> finListNull = null;

  // NotificationSubscriptions
  public static final NotifMechanism notifMech1Aa = new NotifMechanism(NotifMechanismType.WEBHOOK, "webhookUrl1Aa", "emailAddress1Aa");
  public static final NotifMechanism notifMech1Ab = new NotifMechanism(NotifMechanismType.ACTOR, "webhookUrl1Ab", "emailAddress1Ab");
  public static final List<NotifMechanism> notifMechList1A = new ArrayList<>(List.of(notifMech1Aa, notifMech1Ab));
  public static final NotifMechanism notifMech1Ba = new NotifMechanism(NotifMechanismType.EMAIL, "webhookUrl1Ba", "emailAddress1Ba");
  public static final NotifMechanism notifMech1Bb = new NotifMechanism(NotifMechanismType.QUEUE, "webhookUrl1Ba", "emailAddress1Bb");
  public static final List<NotifMechanism> notifMechList1B = new ArrayList<>(List.of(notifMech1Ba, notifMech1Bb));
  public static final NotifSubscription notif1A = new NotifSubscription("filter1A");
  public static final NotifSubscription notif1B = new NotifSubscription("filter1B");
  static {
    notif1A.setNotificationMechanisms(notifMechList1A);
    notif1B.setNotificationMechanisms(notifMechList1B);
  }
  public static final List<NotifSubscription> notifList1 = new ArrayList<>(List.of(notif1A, notif1B));

  public static final NotifMechanism notifMech2Aa = new NotifMechanism(NotifMechanismType.WEBHOOK, "webhookUrl2Aa", "emailAddress2Aa");
  public static final NotifMechanism notifMech2Ab = new NotifMechanism(NotifMechanismType.ACTOR, "webhookUrl2Ab", "emailAddress2Ab");
  public static final List<NotifMechanism> notifMechList2A = new ArrayList<>(List.of(notifMech2Aa, notifMech2Ab));
  public static final NotifMechanism notifMech2Ba = new NotifMechanism(NotifMechanismType.EMAIL, "webhookUrl2Ba", "emailAddress2Ba");
  public static final NotifMechanism notifMech2Bb = new NotifMechanism(NotifMechanismType.QUEUE, "webhookUrl2Ba", "emailAddress2Bb");
  public static final List<NotifMechanism> notifMechList2B = new ArrayList<>(List.of(notifMech2Ba, notifMech2Bb));
  public static final NotifSubscription notif2A = new NotifSubscription("filter2A");
  public static final NotifSubscription notif2B = new NotifSubscription("filter2B");
  static {
    notif2A.setNotificationMechanisms(notifMechList2A);
    notif2B.setNotificationMechanisms(notifMechList2B);
  }
  public static final List<NotifSubscription> notifList2 = new ArrayList<>(List.of(notif2A, notif2B));
  public static final List<NotifSubscription> notifListNull = null;

  // AppArgs, ContainerArgs, SchedulerOptions
  public static final AppArg appArg1A = new AppArg("value1A", "appArg1A", "App arg 1A", metaRequiredTrue, metaKVPairs1);
  public static final AppArg appArg1B = new AppArg("value1B", "appArg1B", "App arg 1B", metaRequiredFalse, metaKVPairs1);
  public static final List<AppArg> appArgList1 = new ArrayList<>(List.of(appArg1A, appArg1B));
  public static final AppArg appArg2A = new AppArg("value2A", "appArg2A", "App arg 2A", metaRequiredTrue, metaKVPairs2);
  public static final AppArg appArg2B = new AppArg("value2B", "appArg2B", "App arg 2B", metaRequiredFalse, metaKVPairs2);
  public static final List<AppArg> appArgList2 = new ArrayList<>(List.of(appArg2A, appArg2B));
  public static final List<AppArg> appArgListNull = null;

  public static final AppArg containerArg1A = new AppArg("value1A", "containerArg1A", "Container arg 1A",
                                                          metaRequiredTrue, metaKVPairs1);
  public static final AppArg containerArg1B = new AppArg("value1B", "containerArg1B", "Container arg 1B",
                                                         metaRequiredFalse, metaKVPairs1);
  public static final List<AppArg> containerArgList1 = new ArrayList<>(List.of(containerArg1A, containerArg1B));
  public static final AppArg containerArg2A = new AppArg("value2A", "containerArg2A", "Container arg 2A",
          metaRequiredTrue, metaKVPairs2);
  public static final AppArg containerArg2B = new AppArg("value2B", "containerArg2B", "Container arg 2B",
          metaRequiredFalse, metaKVPairs2);
  public static final List<AppArg> containerArgList2 = new ArrayList<>(List.of(containerArg2A, containerArg2B));
  public static final List<AppArg> containerArgListNull = null;

  public static final AppArg schedulerOption1A = new AppArg("value1A", "schedulerOption1A", "Scheduler option 1A",
                                                            metaRequiredTrue, metaKVPairs1);
  public static final AppArg schedulerOption1B = new AppArg("value1B", "schedulerOption1B", "Scheduler option 1B",
                                                            metaRequiredFalse, metaKVPairs1);
  public static final List<AppArg> schedulerOptionList1 = new ArrayList<>(List.of(schedulerOption1A, schedulerOption1B));
  public static final AppArg schedulerOption2A = new AppArg("value2A", "schedulerOption2A", "Scheduler option 2A",
          metaRequiredTrue, metaKVPairs2);
  public static final AppArg schedulerOption2B = new AppArg("value2B", "schedulerOption2B", "Scheduler option 2B",
          metaRequiredFalse, metaKVPairs2);
  public static final List<AppArg> schedulerOptionList2 = new ArrayList<>(List.of(schedulerOption2A, schedulerOption2B));
  public static final List<AppArg> schedulerOptionListNull = null;

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
      apps[i] = new App(-1, -1, tenantName, appId, appVersion+suffix, description1 + suffix, AppType.BATCH, ownerUser2, enabledTrue,
                        containerizedTrue,
              runtime1, runtimeVersion1 +suffix, containerImage1 +suffix, maxJobs1, maxJobsPerUser1, strictFileInputsFalse,
                        jobDescription1 +suffix, dynamicExecSystemTrue, execSystemConstraints1, execSystemId1,
                        execSystemExecDir1 +suffix, execSystemInputDir1 +suffix, execSystemOutputDir1 +suffix,
                        execSystemLogicalQueue1 +suffix, archiveSystemId1, archiveSystemDir1 +suffix, archiveOnAppErrorTrue,
              envVariables1, archiveIncludes1, archiveExcludes1,
              nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, jobTags1,
              tags1, notes1, uuidNull, deletedFalse, createdNull, updatedNull);
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
   *   containerized==true -> containterImage
   *   dynamicExec==false -> execSystemId
   * NOTE: many args to constructor are primitives so cannot be set to null.
   */
  public static App makeMinimalApp(App app)
  {
    return new App(-1, -1, tenantName, app.getId(), app.getVersion(), descriptionNull, appType, ownerNull, enabledTrue,
            containerizedTrue, runtimeNull, runtimeVersionNull, containerImage1, maxJobs1, maxJobsPerUser1,
            strictFileInputsFalse, jobDescriptionNull, dynamicExecSystemFalse, execSystemConstraintsNull,
            execSystemId1, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            execSystemLogicalQueueNull, archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorFalse,
            envVariablesNull, archiveIncludesNull, archiveExcludesNull,
            nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, jobTagsNull,
            tagsNull, notesNull, uuidNull, deletedFalse, createdNull, updatedNull);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * All attributes are to be updated.
   */
  public static PatchApp makePatchAppFull()
  {
     return new PatchApp(description2, runtime2, runtimeVersion2, containerImage2,
             maxJobs2, maxJobsPerUser2, strictFileInputsTrue,
             jobDescription2, dynamicExecSystemFalse, execSystemConstraints2,
             execSystemId2, execSystemExecDir2, execSystemInputDir2, execSystemOutputDir2, execSystemLogicalQueue2,
             archiveSystemId2, archiveSystemDir2, archiveOnAppErrorFalse,
             appArgList2, containerArgList2, schedulerOptionList2, envVariables2, archiveIncludes2, archiveExcludes2,
             finList2, nodeCount2, coresPerNode2, memoryMb2, maxMinutes2, notifList2, jobTags2,
             tags2, notes2);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: description, containerImage, execSystemId,
   */
  public static PatchApp makePatchAppPartial()
  {
    return new PatchApp(description2, runtimeNull, runtimeVersionNull, containerImage2,
            maxJobs1, maxJobsPerUser1, strictFileInputsFalse,
            jobDescriptionNull, dynamicExecSystemTrue, execSystemConstraints1,
            execSystemId2, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorTrue,
            appArgListNull, containerArgListNull, schedulerOptionListNull, envVariablesNull, archiveIncludesNull, archiveExcludesNull,
            finListNull, nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, notifListNull, jobTagsNull,
            tagsNull, notesNull);
  }
}
