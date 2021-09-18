package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;
import edu.utexas.tacc.tapis.apps.model.App.InputMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * Utilities and data for integration testing
 */
public final class IntegrationUtils
{
  public static final Gson gson =  TapisGsonUtils.getGson();
  // Test data
  public static final String tenantName = "dev";
  public static final String owner1 = "testuser1";
  public static final String owner2 = "testuser2";
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
  public static final Boolean strictFileInputsNull = null;
  public static final boolean inPlaceTrue = true;
  public static final boolean inPlaceFalse = false;
  public static final InputMode inputModeRequired = InputMode.REQUIRED;
  public static final InputMode inputModeOptional = InputMode.OPTIONAL;
  public static final boolean metaRequiredTrue = true;
  public static final boolean metaRequiredFalse = false;
  public static final Runtime runtime1 = Runtime.DOCKER;
  public static final Runtime runtime2 = Runtime.SINGULARITY;
  public static final Runtime runtimeNull = null;
  public static final List<RuntimeOption> runtimeOptions1 = new ArrayList<>(List.of(RuntimeOption.SINGULARITY_RUN));
  public static final List<RuntimeOption> runtimeOptions2 = new ArrayList<>(List.of(RuntimeOption.SINGULARITY_START));
  public static final List<RuntimeOption> runtimeOptionsSingBoth = new ArrayList<>(List.of(RuntimeOption.SINGULARITY_START, RuntimeOption.SINGULARITY_RUN));
  public static final List<RuntimeOption> runtimeOptionsSingNeither = new ArrayList<>();
  public static final List<RuntimeOption> runtimeOptionsNull = null;
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
  public static final Boolean dynamicExecSystemNull = null;
  public static final String[] execSystemConstraints1 = {"Constraint1a AND", "Constraint1b"};
  public static final String[] execSystemConstraints2 = {"Constraint2a AND", "Constraint2b"};
  public static final String[] execSystemConstraintsNull = null;
  public static final String execSystemId1 = "tapisv3-exec3"; // Exec system with most attributes populated, including LogicalQueues
  public static final String execSystemId2 = "tapisv3-exec4"; // Exec system with most attributes populated, including LogicalQueues
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
  public static final String execSystemLogicalQueue1 = "dsnormal"; // A LogicalQueue defined for tapisv3-exec3
  public static final String execSystemLogicalQueue2 = "dslarge";
  public static final String execSystemLogicalQueueNull = null;
  public static final String archiveSystemId1 = "tapisv3-storage";
  public static final String archiveSystemId2 = "tapisv3-storage-dev";
  public static final String archiveSystemIdNull = null;
  public static final String archiveSystemDir1 = "archiveSystemDir1";
  public static final String archiveSystemDir2 = "archiveSystemDir2";
  public static final String archiveSystemDirNull = null;
  public static final boolean archiveOnAppErrorTrue = true;
  public static final boolean archiveOnAppErrorFalse = false;
  public static final Boolean archiveOnAppErrorNull = null;
  public static final boolean archiveIncludeLaunchFilesTrue = true;
  public static final boolean archiveIncludeLaunchFilesFalse = false;
  public static final Boolean archiveIncludeLaunchFilesNull = null;
  public static final int maxJobs1 = 5; // from tapisv3-exec3 dsnormal LogicalQueue maxJobs
  public static final int maxJobs2 = 2;
  public static final Integer maxJobsNull = null;
  public static final int maxJobsPerUser1 = 2; // from tapisv3-exec3 dsnormal LogicalQueue maxJobsPerUser
  public static final int maxJobsPerUser2 = 2;
  public static final Integer maxJobsPerUserNull = null;
  public static final int nodeCount1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minNodeCount
  public static final int nodeCount2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxNodeCount
  public static final Integer nodeCountNull = null;
  public static final int coresPerNode1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minCoresPerNode
  public static final int coresPerNode2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxCoresPerNode
  public static final Integer coresPerNodeNull = null;
  public static final int memoryMb1 = 32; // from tapisv3-exec3 dsnormal LogicalQueue minMemoryMB
  public static final int memoryMb2 = 64; // from tapisv3-exec3 dsnormal LogicalQueue maxMemoryMB
  public static final Integer memoryMbNull = null;
  public static final int maxMinutes1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minMinutes
  public static final int maxMinutes2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxMinutes
  public static final Integer maxMinutesNull = null;
  public static final boolean deletedFalse = false;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;
  public static final UUID uuidNull = null;

  public static final String[] metaKVPairs1 = {"key1A=val1A", "key1B=val1B"};
  public static final String[] metaKVPairs2 = {"key2A=val2A", "key2B=val2B"};
  public static final String[] metaKVPairs3 = {"key3A=val3A", "key3B=val3B"};
  public static final String[] envVariables1 = {"key1A=val1A", "key1B=val1B", "key1C="};
  public static final String[] envVariables2 = {"key2A=val2A", "key2B=val2B", "key2C="};
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
                                                      inputModeRequired, metaKVPairs1);
  public static final FileInput fin1B = new FileInput("/src1B", "/target1B", inPlaceFalse, "fin1B", "File input 1B",
                                                      inputModeOptional, metaKVPairs1);
  public static final List<FileInput> finList1 = new ArrayList<>(List.of(fin1A, fin1B));
  public static final FileInput finA2 = new FileInput("/srcA2", "/targetA2", inPlaceTrue, "finA2", "File input A2",
          inputModeRequired, metaKVPairs2);
  public static final FileInput finB2 = new FileInput("/srcB2", "/targetB2", inPlaceFalse, "finB2", "File input B2",
          inputModeOptional, metaKVPairs2);
  public static final List<FileInput> finList2 = new ArrayList<>(List.of(finA2, finB2));
  public static final FileInput finA3 = new FileInput("/srcA3", "/targetA3", inPlaceTrue, "finA3", "File input A3",
          inputModeRequired, metaKVPairs3);
  public static final FileInput finB3 = new FileInput("/srcB3", "/targetB3", inPlaceFalse, "finB3", "File input B3",
          inputModeRequired, metaKVPairs3);
  public static final List<FileInput> finList3 = new ArrayList<>(List.of(finA3, finB3));
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
  public static final AppArg appArg1A = new AppArg("value1A", "appArg1A", "App arg 1A", inputModeRequired, metaKVPairs1);
  public static final AppArg appArg1B = new AppArg("value1B", "appArg1B", "App arg 1B", inputModeOptional, metaKVPairs1);
  public static final List<AppArg> appArgList1 = new ArrayList<>(List.of(appArg1A, appArg1B));
  public static final AppArg appArg2A = new AppArg("value2A", "appArg2A", "App arg 2A", inputModeRequired, metaKVPairs2);
  public static final AppArg appArg2B = new AppArg("value2B", "appArg2B", "App arg 2B", inputModeOptional, metaKVPairs2);
  public static final List<AppArg> appArgList2 = new ArrayList<>(List.of(appArg2A, appArg2B));
  public static final AppArg appArg3A = new AppArg("value3A", "appArg3A", "App arg 3A", inputModeRequired, metaKVPairs3);
  public static final AppArg appArg3B = new AppArg("value3B", "appArg3B", "App arg 3B", inputModeOptional, metaKVPairs3);
  public static final List<AppArg> appArgList3 = new ArrayList<>(List.of(appArg3A, appArg3B));
  public static final List<AppArg> appArgListNull = null;

  public static final AppArg containerArg1A = new AppArg("value1A", "containerArg1A", "Container arg 1A",
          inputModeRequired, metaKVPairs1);
  public static final AppArg containerArg1B = new AppArg("value1B", "containerArg1B", "Container arg 1B",
          inputModeOptional, metaKVPairs1);
  public static final List<AppArg> containerArgList1 = new ArrayList<>(List.of(containerArg1A, containerArg1B));
  public static final AppArg containerArg2A = new AppArg("value2A", "containerArg2A", "Container arg 2A",
          inputModeRequired, metaKVPairs2);
  public static final AppArg containerArg2B = new AppArg("value2B", "containerArg2B", "Container arg 2B",
          inputModeOptional, metaKVPairs2);
  public static final List<AppArg> containerArgList2 = new ArrayList<>(List.of(containerArg2A, containerArg2B));
  public static final AppArg containerArg3A = new AppArg("value3A", "containerArg3A", "Container arg 3A",
          inputModeRequired, metaKVPairs3);
  public static final AppArg containerArg3B = new AppArg("value3B", "containerArg3B", "Container arg 3B",
          inputModeOptional, metaKVPairs3);
  public static final List<AppArg> containerArgList3 = new ArrayList<>(List.of(containerArg3A, containerArg3B));
  public static final List<AppArg> containerArgListNull = null;

  public static final AppArg schedulerOption1A = new AppArg("value1A", "schedulerOption1A", "Scheduler option 1A",
          inputModeRequired, metaKVPairs1);
  public static final AppArg schedulerOption1B = new AppArg("value1B", "schedulerOption1B", "Scheduler option 1B",
          inputModeOptional, metaKVPairs1);
  public static final List<AppArg> schedulerOptionList1 = new ArrayList<>(List.of(schedulerOption1A, schedulerOption1B));
  public static final AppArg schedulerOption2A = new AppArg("value2A", "schedulerOption2A", "Scheduler option 2A",
          inputModeRequired, metaKVPairs2);
  public static final AppArg schedulerOption2B = new AppArg("value2B", "schedulerOption2B", "Scheduler option 2B",
          inputModeOptional, metaKVPairs2);
  public static final List<AppArg> schedulerOptionList2 = new ArrayList<>(List.of(schedulerOption2A, schedulerOption2B));
  public static final List<AppArg> schedulerOptionListNull = null;

  public static final List<OrderBy> orderByListNull = null;
  public static final List<OrderBy> orderByListAsc = Collections.singletonList(OrderBy.fromString("id(asc)"));
  public static final List<OrderBy> orderByListDesc = Collections.singletonList(OrderBy.fromString("id(desc)"));
  public static final List<OrderBy> orderByList2Asc = new ArrayList<>(List.of(OrderBy.fromString("app_type(asc)"),
                                                                              OrderBy.fromString("container_image(asc)")));
  public static final List<OrderBy> orderByList2Desc = new ArrayList<>(List.of(OrderBy.fromString("app_type(asc)"),
                                                                               OrderBy.fromString("container_image(desc)")));
  public static final List<OrderBy> orderByList3Asc = new ArrayList<>(List.of(OrderBy.fromString("id(asc)"),
                                                                              OrderBy.fromString("owner(asc)")));
  public static final List<OrderBy> orderByList3Desc = new ArrayList<>(List.of(OrderBy.fromString("container_image(desc)"),
                                                                               OrderBy.fromString("app_type(desc)")));
  public static final String startAfterNull = null;

  public static final Boolean versionSpecifiedNull = null;

  // Search and sort
  public static final List<String> searchListNull = null;
  public static final ASTNode searchASTNull = null;
  public static final Set<String> setOfIDsNull = null;
  public static final int limitNone = -1;
  public static final List<String> orderByAttrEmptyList = Arrays.asList("");
  public static final List<String> orderByDirEmptyList = Arrays.asList("");
  public static final int skipZero = 0;
  public static final String startAferEmpty = "";
  public static final boolean showDeletedFalse = false;
  public static final boolean showDeletedTrue = true;
  public static final boolean versionSpecifiedFalse = false;

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
      apps[i] = new App(-1, -1, tenantName, appId, appVersion+suffix, description1 + suffix, appType, owner1,
                 enabledTrue, containerizedTrue, runtime1, runtimeVersion1 +suffix, runtimeOptions1,
                 containerImage1 +suffix, maxJobs1, maxJobsPerUser1, strictFileInputsFalse,
                 jobDescription1 +suffix, dynamicExecSystemTrue, execSystemConstraints1, execSystemId1,
                 execSystemExecDir1 +suffix, execSystemInputDir1 +suffix, execSystemOutputDir1 +suffix,
                 execSystemLogicalQueue1, archiveSystemId1, archiveSystemDir1 +suffix, archiveOnAppErrorTrue,
                 envVariables1, archiveIncludes1, archiveExcludes1, archiveIncludeLaunchFilesTrue,
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
  public static App makeMinimalApp(App app, String id)
  {
    return new App(-1, -1, tenantName, id, app.getVersion(), descriptionNull, appType, ownerNull, enabledTrue,
            containerizedTrue, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImage1, maxJobs1, maxJobsPerUser1,
            strictFileInputsFalse, jobDescriptionNull, dynamicExecSystemFalse, execSystemConstraintsNull,
            execSystemId1, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            execSystemLogicalQueueNull, archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorFalse,
            envVariablesNull, archiveIncludesNull, archiveExcludesNull, archiveIncludeLaunchFilesFalse,
            nodeCount1, coresPerNode1, memoryMb1, maxMinutes1, jobTagsNull,
            tagsNull, notesNull, uuidNull, deletedFalse, createdNull, updatedNull);
  }

  /**
   * Create an App in memory for use in testing the PUT operation.
   * All updatable attributes are updated.
   */
  public static App makePutAppFull(App app)
  {
    App putApp = new App(-1, -1, tenantName, app.getId(), app.getVersion(), description2, app.getAppType(), app.getOwner(),
            app.isEnabled(), app.isContainerized(), runtime2, runtimeVersion2, runtimeOptions2, containerImage2,
            maxJobs2, maxJobsPerUser2, strictFileInputsTrue,
            jobDescription2, dynamicExecSystemFalse, execSystemConstraints2,
            execSystemId2, execSystemExecDir2, execSystemInputDir2, execSystemOutputDir2, execSystemLogicalQueue2,
            archiveSystemId2, archiveSystemDir2, archiveOnAppErrorFalse, envVariables2,
            archiveIncludes2, archiveExcludes2, archiveIncludeLaunchFilesFalse,
            nodeCount2, coresPerNode2, memoryMb2, maxMinutes2, jobTags2,
            tags2, notes2, uuidNull, deletedFalse, createdNull, updatedNull);
    // Aux table data
    putApp.setFileInputs(finList2);
    putApp.setNotificationSubscriptions(notifList2);
    putApp.setAppArgs(appArgList2);
    putApp.setContainerArgs(containerArgList2);
    putApp.setSchedulerOptions(schedulerOptionList2);
    return putApp;
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * All attributes are to be updated.
   */
  public static PatchApp makePatchAppFull(String id, String version)
  {
     return new PatchApp(tenantName, id, version, description2, runtime2, runtimeVersion2, runtimeOptions2, containerImage2,
             maxJobs2, maxJobsPerUser2, strictFileInputsTrue,
             jobDescription2, dynamicExecSystemFalse, execSystemConstraints2,
             execSystemId2, execSystemExecDir2, execSystemInputDir2, execSystemOutputDir2, execSystemLogicalQueue2,
             archiveSystemId2, archiveSystemDir2, archiveOnAppErrorFalse, appArgList2, containerArgList2,
             schedulerOptionList2, envVariables2, archiveIncludes2, archiveExcludes2, archiveIncludeLaunchFilesFalse,
             finList2, nodeCount2, coresPerNode2, memoryMb2, maxMinutes2, notifList2, jobTags2,
             tags2, notes2);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: description, containerImage, execSystemId
   */
  public static PatchApp makePatchAppPartial1(String id, String version)
  {
    return new PatchApp(tenantName, id, version, description2, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImage2,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull,
            jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemId2, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull, appArgListNull, containerArgListNull,
            schedulerOptionListNull, envVariablesNull, archiveIncludesNull, archiveExcludesNull, archiveIncludeLaunchFilesNull,
            finListNull, nodeCountNull, coresPerNodeNull, memoryMbNull, maxMinutesNull, notifListNull, jobTagsNull,
            tagsNull, notesNull);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: description, containerImage, execSystemId,
   *   jobAttributes.fileInputs, jobAttributes.parameterSet.containerArgs
   */
  public static PatchApp makePatchAppPartial2(String id, String version)
  {
    return new PatchApp(tenantName, id, version, description2, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImage2,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull,
            jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemId2, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull, appArgListNull, containerArgList3,
            schedulerOptionListNull, envVariablesNull, archiveIncludesNull, archiveExcludesNull, archiveIncludeLaunchFilesNull,
            finList3, nodeCountNull, coresPerNodeNull, memoryMbNull, maxMinutesNull, notifListNull, jobTagsNull,
            tagsNull, notesNull);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: jobAttributes.parameterSet.appArgs
   */
  public static PatchApp makePatchAppPartial3(String id, String version)
  {
    return new PatchApp(tenantName, id, version, descriptionNull, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImageNull,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull,
            jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemIdNull, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull, appArgList3, containerArgListNull,
            schedulerOptionListNull, envVariablesNull, archiveIncludesNull, archiveExcludesNull, archiveIncludeLaunchFilesNull,
            finListNull, nodeCountNull, coresPerNodeNull, memoryMbNull, maxMinutesNull, notifListNull, jobTagsNull,
            tagsNull, notesNull);
  }

  public static String getContainerImage(String key, int idx)
  {
    String suffix = key + "_" + String.format("%03d", idx);
    return containerImage1 + suffix;
  }

  public static String getAppName(String key, int idx)
  {
    String suffix = key + "_" + String.format("%03d", idx);
    return appIdPrefix + "_" + suffix;
  }
}
