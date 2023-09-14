package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.*;
import edu.utexas.tacc.tapis.apps.model.App.JobType;
import edu.utexas.tacc.tapis.apps.model.App.ArgInputMode;
import edu.utexas.tacc.tapis.apps.model.App.FileInputMode;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;
import edu.utexas.tacc.tapis.apps.model.DeliveryTarget.NotifDeliveryMethod;
import edu.utexas.tacc.tapis.apps.model.KeyValuePair.KeyValueInputMode;
import edu.utexas.tacc.tapis.apps.model.ReqSubscribe.JobEventCategoryFilter;
import edu.utexas.tacc.tapis.apps.service.AppsServiceImpl;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import org.testng.Assert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.KeyValuePair.DEFAULT_INPUT_MODE;

/*
 * Utilities and data for integration testing
 *
 * Following systems must be present and shared publicly in the DEV environment:
 *   tapisv3-storage-testuser1
 *   tapisv3-storage2-testuser1
 *   tapisv3-exec1-testuser1
 *   tapisv3-exec4-testuser1
 * For system definitions see json files in tapis-appslib/src/test/resources
 */
public final class IntegrationUtils
{
  public static final Gson gson =  TapisGsonUtils.getGson();
  // Test data
  public static final String tenantName = "dev";
  public static final String owner0 = "owner0";
  public static final String owner1 = "owner1";
  public static final String owner2 = "owner2";
  public static final String owner3 = "owner3";
  public static final String owner4 = "owner4";
  public static final String owner5 = "owner5";
  public static final String owner6 = "owner6";
  public static final String apiUser = "testuser2";
  public static final String impersonationIdTestUser9 = "testuser9";
  public static final String appIdPrefix = "TestApp";
  public static final String appVersion = "0.0.1";
  public static final String appVersion2 = "0.0.2";
  public static final String description1 = "App description 1";
  public static final String description2 = "App description 2";
  public static final String descriptionNull = null;
  public static final JobType jobType1 = JobType.BATCH;
  public static final JobType jobType2 = JobType.FORK;
  public static final JobType jobTypeNull = null;
  public static final String ownerNull = null;
  public static final boolean enabledTrue = true;
  public static final boolean enabledFalse = false;
  public static final boolean enabledDefault = App.DEFAULT_ENABLED;
  public static final boolean lockedTrue = true;
  public static final boolean lockedFalse = false;
  public static final boolean lockedDefault = App.DEFAULT_LOCKED;
  public static final boolean isDeletedFalse = false;
  public static final boolean containerizedTrue = true;
  public static final boolean containerizedDefault = App.DEFAULT_CONTAINERIZED;
  public static final boolean strictFileInputsTrue = true;
  public static final boolean strictFileInputsFalse = false;
  public static final boolean strictFileInputsDefault = App.DEFAULT_STRICT_FILE_INPUTS;
  public static final Boolean strictFileInputsNull = null;
  public static final boolean autoMountLocalTrue = true;
  public static final boolean autoMountLocalFalse = false;
  public static final FileInputMode fileInputModeRequired = FileInputMode.REQUIRED;
  public static final FileInputMode fileInputModeOptional = FileInputMode.OPTIONAL;
  public static final FileInputMode fileInputModeFixed = FileInputMode.FIXED;
  public static final FileInputMode fileInputModeDefault = FileInputMode.OPTIONAL;
  public static final ArgInputMode argInputModeRequired = ArgInputMode.REQUIRED;
  public static final ArgInputMode argInputModeFixed = ArgInputMode.FIXED;
  public static final ArgInputMode argInputModeDefault = ArgInputMode.INCLUDE_ON_DEMAND;

  // Sets for testing of search listType query parameter.
  public static final Set<String> viewableIDsNull = null;
  public static final Set<String> viewableIDsEmpty = Collections.emptySet();
  public static final Set<String> sharedIDsNull = null;
  public static final Set<String> sharedIDsEmpty = Collections.emptySet();

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
  public static final boolean dynamicExecSystemDefault = false;
  public static final Boolean dynamicExecSystemNull = null;
  public static final String[] execSystemConstraints1 = {"Constraint1a AND", "Constraint1b"};
  public static final String[] execSystemConstraints2 = {"Constraint2a AND", "Constraint2b"};
  public static final String[] execSystemConstraintsNull = null;
  public static final String execSystemId1 = "tapisv3-exec1-testuser1"; // Exec system with most attributes populated, including LogicalQueues
  public static final String execSystemId2 = "tapisv3-exec4-testuser1"; // Exec system with most attributes populated, including LogicalQueues
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
  public static final String dtnSystemInputDir1 = "dtnSystemInputDir1";
  public static final String dtnSystemInputDir2 = "dtnSystemInputDir2";
  public static final String dtnSystemInputDirNull = null;
  public static final String dtnSystemOutputDir1 = "dtnSystemOutputDir1";
  public static final String dtnSystemOutputDir2 = "dtnSystemOutputDir2";
  public static final String dtnSystemOutputDirNull = null;
  public static final String execSystemLogicalQueue1 = "dsnormal"; // A LogicalQueue defined for tapisv3-exec3
  public static final String execSystemLogicalQueue2 = "dslarge";
  public static final String execSystemLogicalQueueNull = null;
  public static final String archiveSystemId1 = "tapisv3-storage-testuser1";
  public static final String archiveSystemId2 = "tapisv3-storage2-testuser1";
  public static final String archiveSystemIdNull = null;
  public static final String archiveSystemDir1 = "archiveSystemDir1";
  public static final String archiveSystemDir2 = "archiveSystemDir2";
  public static final String archiveSystemDirNull = null;
  public static final boolean archiveOnAppErrorTrue = true;
  public static final boolean archiveOnAppErrorFalse = false;
  public static final boolean archiveOnAppErrorDefault = false;
  public static final Boolean archiveOnAppErrorNull = null;
  public static final Boolean isMpiTrue = true;
  public static final Boolean isMpiFalse = false;
  public static final Boolean isMpiDefault = false;
  public static final Boolean isMpiNull = null;
  public static final String mpiCmd1 = "ibrun1 -n 1";
  public static final String mpiCmd2 = "ibrun2 -n 2";
  public static final String mpiCmdNull = null;
  public static final String cmdPrefix1 = "ibrun1";
  public static final String cmdPrefix2 = "ibrun2";
  public static final String cmdPrefixNull = null;
  public static final boolean archiveIncludeLaunchFilesTrue = true;
  public static final boolean archiveIncludeLaunchFilesFalse = false;
  public static final Boolean archiveIncludeLaunchFilesNull = null;
  public static final int maxJobsDefault = App.DEFAULT_MAX_JOBS;
  public static final int maxJobs1 = 5; // from tapisv3-exec3 dsnormal LogicalQueue maxJobs
  public static final int maxJobs2 = 2;
  public static final Integer maxJobsNull = null;
  public static final int maxJobsPerUserDefault = App.DEFAULT_MAX_JOBS_PER_USER;
  public static final int maxJobsPerUser1 = 2; // from tapisv3-exec3 dsnormal LogicalQueue maxJobsPerUser
  public static final int maxJobsPerUser2 = 2;
  public static final Integer maxJobsPerUserNull = null;
  public static final int nodeCount1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minNodeCount
  public static final int nodeCount2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxNodeCount
  public static final int nodeCountDefault = App.DEFAULT_NODE_COUNT; // from tapisv3-exec3 dsnormal LogicalQueue minNodeCount
  public static final Integer nodeCountNull = null;
  public static final int coresPerNodeDefault = App.DEFAULT_CORES_PER_NODE;
  public static final int coresPerNode1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minCoresPerNode
  public static final int coresPerNode2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxCoresPerNode
  public static final Integer coresPerNodeNull = null;
  public static final int memoryMBDefault = App.DEFAULT_MEMORY_MB;
  public static final int memoryMB1 = 32; // from tapisv3-exec3 dsnormal LogicalQueue minMemoryMB
  public static final int memoryMB2 = 64; // from tapisv3-exec3 dsnormal LogicalQueue maxMemoryMB
  public static final Integer memoryMBNull = null;
  public static final int maxMinutesDefault = App.DEFAULT_MAX_MINUTES;
  public static final int maxMinutes1 = 10; // from tapisv3-exec3 dsnormal LogicalQueue minMinutes
  public static final int maxMinutes2 = 20; // from tapisv3-exec3 dsnormal LogicalQueue maxMinutes
  public static final Integer maxMinutesNull = null;
  public static final boolean deletedFalse = false;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;
  public static final UUID uuidNull = null;

  public static final JsonObject notes1 =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"my proj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final JsonObject notes2 =
          TapisGsonUtils.getGson().fromJson("{\"project\": \"my proj2\", \"testdata\": \"abc2\"}", JsonObject.class);
  public static final JsonObject notes1Obj = (JsonObject) notes1;
  public static final JsonObject notes2Obj = (JsonObject) notes2;
  public static final JsonObject notesNull = null;
  public static final String rawDataEmptyJson = "{}";
  public static final KeyValuePair keyValuePairFull1 = new KeyValuePair("key_fullKeyVal1", "val_fullKeyVal1",
                                                                  "Fully populated keyValuePair1", KeyValueInputMode.FIXED, notes1Obj);
  public static final KeyValuePair keyValuePairFull2 = new KeyValuePair("key_fullKeyVal2", "val_fullKeyVal2",
                                                                  "Fully populated keyValuePair2", KeyValueInputMode.REQUIRED, notes2Obj);
  public static final List<KeyValuePair> envVariables1 = List.of(new KeyValuePair("a1","b1", null, DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 new KeyValuePair("HOME","/home/testuser1", "", DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 new KeyValuePair("TMP","/tmp1", "my keyvalue pair", DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 keyValuePairFull1);
  public static final List<KeyValuePair> envVariables2 = List.of(new KeyValuePair("a2","b2", "my 2nd key-value pair", DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 new KeyValuePair("HOME","/home/testuser2", null, DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 new KeyValuePair("TMP","/tmp2", null, DEFAULT_INPUT_MODE, DEFAULT_NOTES),
                                                                 keyValuePairFull2);
  public static final List<KeyValuePair> envVariablesReject =
          List.of(new KeyValuePair("rejectMe", App.VALUE_NOT_SET, null, KeyValueInputMode.FIXED, null));
  public static final List<KeyValuePair> envVariablesNull = null;
  public static final String[] archiveIncludes1 = {"/include1A", "/include1B"};
  public static final String[] archiveIncludes2 = {"/include2A", "/include2B"};
  public static final String[] archiveIncludesNull = null;
  public static final String[] archiveExcludes1 = {"/exclude1A", "/exclude1B"};
  public static final String[] archiveExcludes2 = {"/exclude2A", "/exclude2B"};
  public static final String[] archiveExcludesNull = null;
  public static final boolean includeLaunchFilesTrue = true;
  public static final boolean includeLaunchFilesFalse = false;

  public static final ArchiveFilter archiveFilter1 = new ArchiveFilter(archiveIncludes1, archiveExcludes1, includeLaunchFilesTrue);
  public static final ArchiveFilter archiveFilter2 = new ArchiveFilter(archiveIncludes2, archiveExcludes2, includeLaunchFilesFalse);
  public static final ArchiveFilter archiveFilterNull = null;

  public static final String[] jobTags1 = {"jobtag1a", "jobtag1b"};
  public static final String[] jobTags2 = {"jobtag2b", "jobtag2b"};
  public static final String[] jobTagsNull = null;
  public static final String tagVal1 = "value1";
  public static final String tagVal2 = "value2";
  public static final String tagVal3Space = "value 3";
  public static final String tagValNotThere = "no such tag value";
  public static final String[] tags1 = {tagVal1, tagVal2, tagVal3Space, "a",
    "Long tag (1 3 2) special chars [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final String[] tags2 = {"value4", "value5"};
  public static final String[] tags3 = {tagVal1};
  public static final String[] tagsNull = null;

  // FileInputs
  public static final FileInput fin1A = new FileInput("fin1A", "File input 1A", fileInputModeRequired, autoMountLocalTrue,
          notes1Obj, "/src1A", "/target1A");
  public static final FileInput fin1B = new FileInput("fin1B", "File input 1B", fileInputModeOptional, autoMountLocalFalse,
          notes1Obj, "/src1B", "/target1B");
  public static final List<FileInput> finList1 = new ArrayList<>(List.of(fin1A, fin1B));
  public static final FileInput fin2A = new FileInput("fin2A", "File input 2A", fileInputModeRequired, autoMountLocalTrue,
          notes1Obj, "/src2A", "/target2A");
  public static final FileInput fin2B = new FileInput("fin2B", "File input 2B", fileInputModeOptional, autoMountLocalFalse,
          notes1Obj, "/src2B", "/targetBA");
  public static final List<FileInput> finList2 = new ArrayList<>(List.of(fin2A, fin2B));
  public static final FileInput fin3A = new FileInput("fin3A", "File input 3A", fileInputModeOptional, autoMountLocalTrue,
          notes1Obj, "/src3A", "/target3A");
  public static final FileInput fin3B = new FileInput("fin3B", "File input 3B", fileInputModeFixed, autoMountLocalFalse,
          notes1Obj, "/src3B", "/target3B");
  public static final List<FileInput> finList3 = new ArrayList<>(List.of(fin3A, fin3B));
  public static final List<FileInput> finListNull = null;

  // FileInputArrays
  public static final FileInputArray fia1A = new FileInputArray("fia1A", "File input array 1A", fileInputModeRequired, notes1Obj,
          List.of("/src1Aa","/src1Ab"), "/targetDir1A");
  public static final FileInputArray fia1B = new FileInputArray("fia1B", "File input array 1B", fileInputModeOptional, notes1Obj,
          List.of("/src1Ba","/src1Bb"), "/targetDir1B");
  public static final List<FileInputArray> fiaList1 = new ArrayList<>(List.of(fia1A, fia1B));
  public static final FileInputArray fia2A = new FileInputArray("fia2A", "File input array 2A", fileInputModeRequired, notes1Obj,
          List.of("/src2Aa","/src2Ab"), "/targetDir2A");
  public static final FileInputArray fia2B = new FileInputArray("fia2B", "File input array 2B", fileInputModeOptional, notes1Obj,
          List.of("/src2Ba","/src2Bb"), "/targetDir2B");
  public static final List<FileInputArray> fiaList2 = new ArrayList<>(List.of(fia2A, fia2B));

  public static final FileInputArray fia3A = new FileInputArray("fia3A", "File input array 3A", fileInputModeRequired, notes1Obj,
          List.of("/src3Aa","/src3Ab"), "/targetDir3A");
  public static final FileInputArray fia3B = new FileInputArray("fia3B", "File input array 3B", fileInputModeOptional, notes1Obj,
          List.of("/src3Ba","/src3Bb"), "/targetDir3B");
  public static final List<FileInputArray> fiaList3 = new ArrayList<>(List.of(fia3A, fia3B));

  public static final List<FileInputArray> fiaListNull = null;

  // NotificationSubscriptions
  public static final int ttl = 0;
  public static final DeliveryTarget notifTarget1Aa = new DeliveryTarget(NotifDeliveryMethod.WEBHOOK, "webhookUrl1Aa");
  public static final DeliveryTarget notifTarget1Ab = new DeliveryTarget(NotifDeliveryMethod.EMAIL, "emailAddress1Ab");
  public static final List<DeliveryTarget> notifTargetList1A = new ArrayList<>(List.of(notifTarget1Aa, notifTarget1Ab));
  public static final DeliveryTarget notifTarget1Ba = new DeliveryTarget(NotifDeliveryMethod.EMAIL, "emailAddress1Ba");
  public static final DeliveryTarget notifTarget1Bb = new DeliveryTarget(NotifDeliveryMethod.WEBHOOK, "webhookUrl1Ba");
  public static final List<DeliveryTarget> notifTargetList1B = new ArrayList<>(List.of(notifTarget1Ba, notifTarget1Bb));
  public static final ReqSubscribe notif1A = new ReqSubscribe(null, enabledTrue, JobEventCategoryFilter.JOB_NEW_STATUS,
                                                              notifTargetList1A, ttl);
  public static final ReqSubscribe notif1B = new ReqSubscribe(null, enabledTrue, JobEventCategoryFilter.ALL,
                                                              notifTargetList1B, ttl);
  public static final List<ReqSubscribe> notifList1 = new ArrayList<>(List.of(notif1A, notif1B));

  public static final DeliveryTarget notifTarget2Aa = new DeliveryTarget(NotifDeliveryMethod.WEBHOOK, "webhookUrl2Aa");
  public static final DeliveryTarget notifTarget2Ab = new DeliveryTarget(NotifDeliveryMethod.EMAIL, "emailAddress2Ab");
  public static final List<DeliveryTarget> notifTargetList2A = new ArrayList<>(List.of(notifTarget2Aa, notifTarget2Ab));
  public static final DeliveryTarget notifTarget2Ba = new DeliveryTarget(NotifDeliveryMethod.EMAIL, "emailAddress2Ba");
  public static final DeliveryTarget notifTarget2Bb = new DeliveryTarget(NotifDeliveryMethod.WEBHOOK, "webhookUrl2Ba");
  public static final List<DeliveryTarget> notifTargetList2B = new ArrayList<>(List.of(notifTarget2Ba, notifTarget2Bb));
  public static final ReqSubscribe notif2A = new ReqSubscribe(null, enabledTrue, JobEventCategoryFilter.JOB_ERROR_MESSAGE,
                                                              notifTargetList2A, ttl);
  public static final ReqSubscribe notif2B = new ReqSubscribe(null, enabledTrue, JobEventCategoryFilter.JOB_SUBSCRIPTION,
                                                              notifTargetList2B, ttl);
  public static final List<ReqSubscribe> notifList2 = new ArrayList<>(List.of(notif2A, notif2B));
  public static final List<ReqSubscribe> notifListNull = null;

  // AppArgs, ContainerArgs, SchedulerOptions
  public static final JsonObject argNotes1 =
          TapisGsonUtils.getGson().fromJson("{\"argName\": \"my arg1\", \"argType\": \"bool\"}", JsonObject.class);
  public static final JsonObject argNotes2 =
          TapisGsonUtils.getGson().fromJson("{\"argName\": \"my arg2\", \"argType\": \"this, _ has spaces. - testing!@#$%*()[]{}<>:;\"}", JsonObject.class);
  public static final JsonObject argNotes3 =
          TapisGsonUtils.getGson().fromJson("{\"argName\": \"my arg3\", \"argType\": \"int\"}", JsonObject.class);
  public static final ArgSpec appArg1A = new ArgSpec("argValue1A", "appArg1A", "App arg 1A", argInputModeRequired, argNotes1);
  public static final ArgSpec appArg1B = new ArgSpec("argValue1B", "appArg1B", "App arg 1B", argInputModeRequired, argNotes1);
  public static final List<ArgSpec> appArgList1 = new ArrayList<>(List.of(appArg1A, appArg1B));
  public static final ArgSpec appArg2A = new ArgSpec("argValue2A", "appArg2A", "App arg 2A", argInputModeRequired, argNotes2);
  public static final ArgSpec appArg2B = new ArgSpec("argValue2B", "appArg2B", "App arg 2B", argInputModeFixed, argNotes2);
  public static final List<ArgSpec> appArgList2 = new ArrayList<>(List.of(appArg2A, appArg2B));
  public static final ArgSpec appArg3A = new ArgSpec("argValue3A", "appArg3A", "App arg 3A", argInputModeRequired, argNotes3);
  public static final ArgSpec appArg3B = new ArgSpec("argValue3B", "appArg3B", "App arg 3B", argInputModeFixed, argNotes3);
  public static final List<ArgSpec> appArgList3 = new ArrayList<>(List.of(appArg3A, appArg3B));
  public static final List<ArgSpec> appArgListNull = null;

  public static final ArgSpec containerArg1A = new ArgSpec("argValue1A", "containerArg1A", "Container arg 1A",
          argInputModeRequired, argNotes1);
  public static final ArgSpec containerArg1B = new ArgSpec("argValue1B", "containerArg1B", "Container arg 1B",
          argInputModeFixed, argNotes1);
  public static final List<ArgSpec> containerArgList1 = new ArrayList<>(List.of(containerArg1A, containerArg1B));
  public static final ArgSpec containerArg2A = new ArgSpec("argValue2A", "containerArg2A", "Container arg 2A",
          argInputModeRequired, argNotes2);
  public static final ArgSpec containerArg2B = new ArgSpec("argValue2B", "containerArg2B", "Container arg 2B",
          argInputModeFixed, argNotes2);
  public static final List<ArgSpec> containerArgList2 = new ArrayList<>(List.of(containerArg2A, containerArg2B));
  public static final ArgSpec containerArg3A = new ArgSpec("argValue3A", "containerArg3A", "Container arg 3A",
          argInputModeRequired, argNotes3);
  public static final ArgSpec containerArg3B = new ArgSpec("argValue3B", "containerArg3B", "Container arg 3B",
          argInputModeFixed, argNotes3);
  public static final List<ArgSpec> containerArgList3 = new ArrayList<>(List.of(containerArg3A, containerArg3B));
  public static final List<ArgSpec> containerArgListNull = null;

  public static final ArgSpec schedulerOption1A = new ArgSpec("argValue1A", "schedulerOption1A", "Scheduler option 1A",
          argInputModeRequired, argNotes1);
  public static final ArgSpec schedulerOption1B = new ArgSpec("argValue1B", "schedulerOption1B", "Scheduler option 1B",
          argInputModeFixed, argNotes1);
  public static final List<ArgSpec> schedulerOptionList1 = new ArrayList<>(List.of(schedulerOption1A, schedulerOption1B));
  public static final ArgSpec schedulerOption2A = new ArgSpec("argValue2A", "schedulerOption2A", "Scheduler option 2A",
          argInputModeRequired, argNotes2);
  public static final ArgSpec schedulerOption2B = new ArgSpec("argValue2B", "schedulerOption2B", "Scheduler option 2B",
          argInputModeFixed, argNotes2);
  public static final List<ArgSpec> schedulerOptionList2 = new ArrayList<>(List.of(schedulerOption2A, schedulerOption2B));
  public static final List<ArgSpec> schedulerOptionListNull = null;

  public static LogConfig logConfig1 = new LogConfig("stdout_file1", "stderr_file1");
  public static LogConfig logConfig2 = new LogConfig("stdout_file2", "stderr_file2");
  public static final LogConfig logConfigNull = null;

  public static final ParameterSet parameterSet1 = new ParameterSet(appArgList1, containerArgList1, schedulerOptionList1,
                                                                    envVariables1, archiveFilter1, logConfig1);
  public static final ParameterSet parameterSet2 = new ParameterSet(appArgList2, containerArgList2, schedulerOptionList2,
                                                                    envVariables2, archiveFilter2, logConfig2);
  public static final ParameterSet parameterSetReject = new ParameterSet(appArgList1, containerArgList1, schedulerOptionList1,
                                                                         envVariablesReject, archiveFilter1, logConfig1);
  public static final ParameterSet parameterSetNull = null;

  public static final List<OrderBy> orderByListNull = null;
  public static final List<OrderBy> orderByListAsc = Collections.singletonList(OrderBy.fromString("id(asc)"));
  public static final List<OrderBy> orderByListDesc = Collections.singletonList(OrderBy.fromString("id(desc)"));
  public static final List<OrderBy> orderByList2Asc = new ArrayList<>(List.of(OrderBy.fromString("job_type(asc)"),
                                                                              OrderBy.fromString("container_image(asc)")));
  public static final List<OrderBy> orderByList2Desc = new ArrayList<>(List.of(OrderBy.fromString("job_type(asc)"),
                                                                               OrderBy.fromString("container_image(desc)")));
  public static final List<OrderBy> orderByList3Asc = new ArrayList<>(List.of(OrderBy.fromString("id(asc)"),
                                                                              OrderBy.fromString("owner(asc)")));
  public static final List<OrderBy> orderByList3Desc = new ArrayList<>(List.of(OrderBy.fromString("container_image(desc)"),
                                                                               OrderBy.fromString("job_type(desc)")));
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
  public static final AppsServiceImpl.AuthListType listTypeOwned = AppsServiceImpl.AuthListType.OWNED;
  public static final AppsServiceImpl.AuthListType listTypeSharedPublic = AppsServiceImpl.AuthListType.SHARED_PUBLIC;
  public static final AppsServiceImpl.AuthListType listTypeSharedDirect = AppsServiceImpl.AuthListType.SHARED_DIRECT;
  public static final AppsServiceImpl.AuthListType listTypeMine = AppsServiceImpl.AuthListType.MINE;
  public static final AppsServiceImpl.AuthListType listTypeReadPerm = AppsServiceImpl.AuthListType.READ_PERM;
  public static final AppsServiceImpl.AuthListType listTypeAll = AppsServiceImpl.AuthListType.ALL;

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
      apps[i] = new App(-1, -1, tenantName, appId, appVersion+suffix, description1 + suffix, jobType1, owner1,
                 enabledTrue, lockedFalse, containerizedTrue, runtime1, runtimeVersion1 +suffix, runtimeOptions1,
                 containerImage1 +suffix, maxJobs1, maxJobsPerUser1, strictFileInputsFalse,
                 jobDescription1 +suffix, dynamicExecSystemTrue, execSystemConstraints1, execSystemId1,
                 execSystemExecDir1 +suffix, execSystemInputDir1 +suffix, execSystemOutputDir1 +suffix,
                 dtnSystemInputDir1 + suffix, dtnSystemOutputDir1 + suffix,
                 execSystemLogicalQueue1, archiveSystemId1, archiveSystemDir1 +suffix, archiveOnAppErrorTrue,
                 isMpiTrue, mpiCmd1, cmdPrefix1,
                 parameterSet1, finList1, fiaList1, nodeCount1, coresPerNode1, memoryMB1, maxMinutes1, notifList1, jobTags1,
                 tags1, notes1, uuidNull, deletedFalse, createdNull, updatedNull);
    }
    return apps;
  }

  /**
   * Create App in memory with minimal attributes set based on App given
   *   id, version
   *   containerized==true -> containterImage
   * NOTE: many args to constructor are primitives so cannot be set to null.
   */
  public static App makeMinimalApp(App app, String id)
  {
    return new App(-1, -1, tenantName, id, app.getVersion(), descriptionNull, App.DEFAULT_JOB_TYPE, ownerNull,
            App.DEFAULT_ENABLED, App.DEFAULT_LOCKED, App.DEFAULT_CONTAINERIZED, runtimeNull, runtimeVersionNull, runtimeOptionsNull,
            containerImage1, App.DEFAULT_MAX_JOBS, App.DEFAULT_MAX_JOBS_PER_USER, App.DEFAULT_STRICT_FILE_INPUTS,
            jobDescriptionNull, dynamicExecSystemDefault, execSystemConstraintsNull,
            execSystemIdNull, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            dtnSystemInputDirNull, dtnSystemOutputDirNull,
            execSystemLogicalQueueNull, archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorDefault,
            isMpiDefault, mpiCmdNull, cmdPrefixNull, parameterSetNull, finListNull, fiaListNull, App.DEFAULT_NODE_COUNT,
            App.DEFAULT_CORES_PER_NODE, App.DEFAULT_MEMORY_MB, App.DEFAULT_MAX_MINUTES, notifListNull, jobTagsNull,
            tagsNull, notesNull, uuidNull, deletedFalse, createdNull, updatedNull);
  }

  /**
   * Create an App in memory for use in testing the PUT operation.
   * All updatable attributes are updated.
   */
  public static App makePutAppFull(App app)
  {
    App putApp = new App(-1, -1, tenantName, app.getId(), app.getVersion(), description2, jobType2, app.getOwner(),
            app.isEnabled(), app.isLocked(), app.isContainerized(), runtime2, runtimeVersion2, runtimeOptions2, containerImage2,
            maxJobs2, maxJobsPerUser2, strictFileInputsTrue,
            jobDescription2, dynamicExecSystemFalse, execSystemConstraints2,
            execSystemId2, execSystemExecDir2, execSystemInputDir2, execSystemOutputDir2,
            dtnSystemInputDir2, dtnSystemOutputDir2, execSystemLogicalQueue2,
            archiveSystemId2, archiveSystemDir2, archiveOnAppErrorFalse,
            isMpiFalse, mpiCmd2, cmdPrefix2,
            parameterSet2, finList2, fiaList2, nodeCount2, coresPerNode2, memoryMB2, maxMinutes2, notifList2, jobTags2,
            tags2, notes2, uuidNull, deletedFalse, createdNull, updatedNull);
    return putApp;
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * All attributes are to be updated.
   */
  public static PatchApp makePatchAppFull()
  {
    JobAttributes jobAttributes = new JobAttributes(jobDescription2, dynamicExecSystemFalse, execSystemConstraints2,
            execSystemId2, execSystemExecDir2, execSystemInputDir2, execSystemOutputDir2,
            dtnSystemInputDir2, dtnSystemOutputDir2, execSystemLogicalQueue2,
            archiveSystemId2, archiveSystemDir2, archiveOnAppErrorFalse,
            isMpiFalse, mpiCmd2, cmdPrefix2,
            parameterSet2, finList2, fiaList2, nodeCount2,
            coresPerNode2, memoryMB2, maxMinutes2, notifList2, jobTags2);

    return new PatchApp(description2, runtime2, runtimeVersion2, runtimeOptions2, containerImage2, jobType2,
             maxJobs2, maxJobsPerUser2, strictFileInputsTrue, jobAttributes, tags2, notes2);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: description, containerImage, execSystemId
   */
  public static PatchApp makePatchAppPartial1()
  {
    JobAttributes jobAttributes = new JobAttributes(jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemId2, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            dtnSystemInputDirNull, dtnSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull,
            isMpiNull, mpiCmdNull, cmdPrefixNull,
            parameterSetNull, finListNull, fiaListNull, nodeCountNull,
            coresPerNodeNull, memoryMBNull, maxMinutesNull, notifListNull, jobTagsNull);

    return new PatchApp(description2, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImage2, jobType2,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull, jobAttributes, tagsNull, notesNull);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: description, containerImage, execSystemId,
   *   jobAttributes.fileInputs, jobAttributes.parameterSet.containerArgs
   */
  public static PatchApp makePatchAppPartial2()
  {
    ParameterSet parameterSet = new ParameterSet(appArgListNull, containerArgList3, schedulerOptionListNull,
                                                 envVariablesNull, archiveFilterNull, logConfigNull);
    JobAttributes jobAttributes = new JobAttributes(jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemId2, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            dtnSystemInputDirNull, dtnSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull,
            isMpiNull, mpiCmdNull, cmdPrefixNull,
            parameterSet, finList3, fiaList3, nodeCountNull,
            coresPerNodeNull, memoryMBNull, maxMinutesNull, notifListNull, jobTagsNull);
    return new PatchApp(description2, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImage2, JobType.UNSET,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull, jobAttributes, tagsNull, notesNull);
  }

  /**
   * Create a PatchApp in memory for use in testing.
   * Some attributes are to be updated: jobAttributes.parameterSet.appArgs
   */
  public static PatchApp makePatchAppPartial3()
  {
    ParameterSet parameterSet = new ParameterSet(appArgList3, containerArgListNull, schedulerOptionListNull,
                                                 envVariablesNull, archiveFilterNull, logConfigNull);
    JobAttributes jobAttributes = new JobAttributes(jobDescriptionNull, dynamicExecSystemNull, execSystemConstraintsNull,
            execSystemIdNull, execSystemExecDirNull, execSystemInputDirNull, execSystemOutputDirNull,
            dtnSystemInputDirNull, dtnSystemOutputDirNull, execSystemLogicalQueueNull,
            archiveSystemIdNull, archiveSystemDirNull, archiveOnAppErrorNull,
            isMpiNull, mpiCmdNull, cmdPrefixNull,
            parameterSet, finListNull, fiaListNull, nodeCountNull,
            coresPerNodeNull, memoryMBNull, maxMinutesNull, notifListNull, jobTagsNull);
    return new PatchApp(descriptionNull, runtimeNull, runtimeVersionNull, runtimeOptionsNull, containerImageNull, jobType2,
            maxJobsNull, maxJobsPerUserNull, strictFileInputsNull, jobAttributes, tagsNull, notesNull);
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

  // Verify that original list of AppArgs matches the fetched list
  public static void verifyAppArgs(String argType, List<ArgSpec> origArgs, List<ArgSpec> fetchedArgs)
  {
    System.out.println("Verifying fetched AppArgs of type: " + argType);
    Assert.assertNotNull(origArgs, "Orig AppArgs is null");
    Assert.assertNotNull(fetchedArgs, "Fetched AppArgs is null");
    Assert.assertEquals(fetchedArgs.size(), origArgs.size());
    // Create hash maps of orig and fetched with name as key
    var origMap = new HashMap<String, ArgSpec>();
    var fetchedMap = new HashMap<String, ArgSpec>();
    for (ArgSpec a : origArgs) origMap.put(a.getName(), a);
    for (ArgSpec a : fetchedArgs) fetchedMap.put(a.getName(), a);
    // Go through origMap and check properties
    for (String argName : origMap.keySet())
    {
      Assert.assertTrue(fetchedMap.containsKey(argName), "Fetched list does not contain original item: " + argName);
      ArgSpec fetchedArg = fetchedMap.get(argName);
      ArgSpec origArg = origMap.get(argName);
      System.out.println("Found fetched item: " + argName);
      Assert.assertEquals(fetchedArg.getArg(), origArg.getArg());
      Assert.assertEquals(fetchedArg.getDescription(), origArg.getDescription());
      Assert.assertEquals(fetchedArg.getInputMode(), origArg.getInputMode());
      // Verify notes
      JsonObject fetchedNotesObj = fetchedArg.getNotes();
      JsonObject origNotesObj = origArg.getNotes();
      if (origNotesObj == null)
      {
        Assert.assertNull(fetchedNotesObj);
        System.out.println("############################################ OrigArgNotes and FetchedArgNotes were null");
      }
      else
      {
        Assert.assertNotNull(fetchedNotesObj, "ArgSpec Notes object was null");
        Assert.assertTrue(fetchedNotesObj.has("argName"));
        Assert.assertEquals(fetchedNotesObj.get("argName").getAsString(), origNotesObj.get("argName").getAsString());
        Assert.assertTrue(fetchedNotesObj.has("argType"));
        Assert.assertEquals(fetchedNotesObj.get("argType").getAsString(), origNotesObj.get("argType").getAsString());
      }
    }
  }

  // Verify that original list of KeyValuePairs matches the fetched list
  public static void verifyKeyValuePairs(String argType, List<KeyValuePair> origKVs, List<KeyValuePair> fetchedKVs)
  {
    System.out.println("Verifying fetched KV pairs of type: " + argType);
    Assert.assertNotNull(origKVs, "Orig KVs is null");
    Assert.assertNotNull(fetchedKVs, "Fetched KVs is null");
    Assert.assertEquals(fetchedKVs.size(), origKVs.size());
    // Create hash maps of orig and fetched with KV key as key
    var origMap = new HashMap<String, KeyValuePair>();
    var fetchedMap = new HashMap<String, KeyValuePair>();
    for (KeyValuePair kv : origKVs) origMap.put(kv.getKey(), kv);
    for (KeyValuePair kv : fetchedKVs) fetchedMap.put(kv.getKey(), kv);
    // Go through origMap and check properties
    for (String kvKey : origMap.keySet())
    {
      Assert.assertTrue(fetchedMap.containsKey(kvKey), "Fetched list does not contain original item: " + kvKey);
      KeyValuePair fetchedKV = fetchedMap.get(kvKey);
      System.out.println("Found fetched KeyValuePair: " + fetchedKV);
      Assert.assertEquals(fetchedMap.get(kvKey).toString(), origMap.get(kvKey).toString());
    }
  }

  // Verify that original list of FileInputs matches the fetched list
  public static void verifyFileInputs(List<FileInput> origFileInputs, List<FileInput> fetchedFileInputs)
  {
    System.out.println("Verifying list of FileInputs");
    Assert.assertNotNull(origFileInputs, "Orig FileInputs is null");
    Assert.assertNotNull(fetchedFileInputs, "Fetched FileInputs is null");
    Assert.assertEquals(fetchedFileInputs.size(), origFileInputs.size());
    // Create hash maps of orig and fetched with name as key
    var origMap = new HashMap<String, FileInput>();
    var fetchedMap = new HashMap<String, FileInput>();
    for (FileInput fi : origFileInputs) origMap.put(fi.getName(), fi);
    for (FileInput fi : fetchedFileInputs) fetchedMap.put(fi.getName(), fi);
    // Go through origMap and check properties
    for (String fiName : origMap.keySet())
    {
      Assert.assertTrue(fetchedMap.containsKey(fiName), "Fetched list does not contain original item: " + fiName);
      FileInput fetchedFileInput = fetchedMap.get(fiName);
      FileInput origFileInput = origMap.get(fiName);
      System.out.println("Found fetched FileInput: " + fiName);
      Assert.assertEquals(fetchedFileInput.getSourceUrl(), origFileInput.getSourceUrl());
      Assert.assertEquals(fetchedFileInput.getTargetPath(), origFileInput.getTargetPath());
      Assert.assertEquals(fetchedFileInput.getDescription(), origFileInput.getDescription());
      Assert.assertEquals(fetchedFileInput.getInputMode(), origFileInput.getInputMode());
      Assert.assertEquals(fetchedFileInput.isAutoMountLocal(), origFileInput.isAutoMountLocal());
    }
  }

  // Verify that original list of FileInputs matches the fetched list
  public static void verifyFileInputArrays(List<FileInputArray> origFia, List<FileInputArray> fetchedFia)
  {
    System.out.println("Verifying list of FileInputArrays");
    Assert.assertNotNull(origFia, "Orig FileInputArrays is null");
    Assert.assertNotNull(fetchedFia, "Fetched FileInputArrays is null");
    Assert.assertEquals(fetchedFia.size(), origFia.size());
    // Create hash maps of orig and fetched with name as key
    var origMap = new HashMap<String, FileInputArray>();
    var fetchedMap = new HashMap<String, FileInputArray>();
    for (FileInputArray fia : origFia) origMap.put(fia.getName(), fia);
    for (FileInputArray fia : fetchedFia) fetchedMap.put(fia.getName(), fia);
    // Go through origMap and check properties
    for (String fiaName : origMap.keySet())
    {
      Assert.assertTrue(fetchedMap.containsKey(fiaName), "Fetched list does not contain original item: " + fiaName);
      FileInputArray fetchedItem = fetchedMap.get(fiaName);
      FileInputArray origItem = origMap.get(fiaName);
      System.out.println("Found fetched FileInputArray: " + fiaName);
//      Assert.assertEquals(fetchedFia.getSourceUrl(), origFia.getSourceUrl());
      Assert.assertEquals(fetchedItem.getTargetDir(), origItem.getTargetDir());
      Assert.assertEquals(fetchedItem.getDescription(), origItem.getDescription());
      Assert.assertEquals(fetchedItem.getInputMode(), origItem.getInputMode());
    }
  }

  // Verify that original list of subscriptions matches the fetched list
  public static void verifySubscriptions(List<ReqSubscribe> origSubscriptions, List<ReqSubscribe> fetchedSubscriptions)
  {
    System.out.println("Verifying list of ReqSubscribe");
    Assert.assertNotNull(origSubscriptions, "Orig Subscriptions is null");
    Assert.assertNotNull(fetchedSubscriptions, "Fetched Subscriptionss is null");
    Assert.assertEquals(fetchedSubscriptions.size(), origSubscriptions.size());
    var filtersFound = new ArrayList<String>();
    for (ReqSubscribe itemFound : fetchedSubscriptions) {filtersFound.add(itemFound.getJobEventCategoryFilter().name());}
    for (ReqSubscribe itemSeedItem : origSubscriptions)
    {
      Assert.assertTrue(filtersFound.contains(itemSeedItem.getJobEventCategoryFilter().name()),
              "List of ReqSubscribe did not contain an item with jobEventCategoryFilter: " + itemSeedItem.getJobEventCategoryFilter());
      System.out.println("Found fetched subscription with jobEventCategoryFilter: " + itemSeedItem.getJobEventCategoryFilter());
    }
  }
}
