package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;

import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

/*
 * Class representing an update to a Tapis App.
 * Fields set to null indicate attribute not updated.
 *
 * Note that fields tenant, id and version are not for update. They are for reference during the update process.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class PatchApp
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private String tenant; // No update - reference only
  private String id; // No update - reference only
  private String version; // No update - reference only
  private final String description;
  private final Runtime runtime;
  private final String runtimeVersion;
  private List<RuntimeOption> runtimeOptions;
  private final String containerImage;
  private final Integer maxJobs;
  private final Integer maxJobsPerUser;
  private final Boolean strictFileInputs;
  // === Start jobAttributes ==========
  private final String jobDescription;
  private final Boolean dynamicExecSystem;
  private final String[] execSystemConstraints;
  private final String execSystemId;
  private final String execSystemExecDir;
  private final String execSystemInputDir;
  private final String execSystemOutputDir;
  private final String execSystemLogicalQueue;
  private final String archiveSystemId;
  private final String archiveSystemDir;
  private final Boolean archiveOnAppError;
  private final String[] envVariables;
  private final String[] archiveIncludes;
  private final String[] archiveExcludes;
  private final Boolean archiveIncludeLaunchFiles;
  private final Integer nodeCount;
  private final Integer coresPerNode;
  private final Integer memoryMb;
  private final Integer maxMinutes;
  private final String[] jobTags;
  // === End jobAttributes ==========

  // Aux tables
  private final List<FileInput> fileInputs;
  private final List<NotifSubscription> notifSubscriptions;
  private final List<AppArg> appArgs;
  private final List<AppArg> containerArgs;
  private final List<AppArg> schedulerOptions;
  private final String[] tags;
  private final Object notes;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all final attributes.
   * Note that because PatchApp does not have a corresponding table in the DB we can pass in
   *   FileInputs, AppArgs, ContainerArgs, SchedulerOptions and Subscriptions.
   */
  public PatchApp(String tenantId1, String id1, String version1, String description1, Runtime runtime1, String runtimeVersion1, List<RuntimeOption> runtimeOptions1,
                  String containerImage1, Integer maxJobs1, Integer maxJobsPerUser1, Boolean strictFileInputs1,
                  // == Start jobAttributes
                  String jobDescription1, Boolean dynamicExecSystem1, String[] execSystemConstraints1,
                  String execSystemId1, String execSystemExecDir1, String execSystemInputDir1, String execSystemOutputDir1,
                  String execSystemLogicalQueue1, String archiveSystemId1, String archiveSystemDir1,
                  Boolean archiveOnAppError1,
                  List<AppArg> appArgs1, List<AppArg> containerArgs1, List<AppArg> schedulerOptions1,
                  String[] envVariables1, String[] archiveIncludes1, String[] archiveExcludes1,
                  Boolean archiveIncludeLaunchFiles1, List<FileInput> fileInputs1,
                  Integer nodeCount1, Integer coresPerNode1, Integer memoryMb1, Integer maxMinutes1,
                  List<NotifSubscription> notifSubscriptions1, String[] jobTags1,
                  // == End jobAttributes
                  String[] tags1, Object notes1)
  {
    tenant = tenantId1;
    id = id1;
    version = version1;
    description = description1;
    runtime = runtime1;
    runtimeVersion = runtimeVersion1;
    runtimeOptions = (runtimeOptions1 == null) ? null : new ArrayList<>(runtimeOptions1);
    containerImage = containerImage1;
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    strictFileInputs = strictFileInputs1;
    jobDescription = jobDescription1;
    dynamicExecSystem = dynamicExecSystem1;
    execSystemConstraints = (execSystemConstraints1 == null) ? null : execSystemConstraints1.clone();
    execSystemId = execSystemId1;
    execSystemExecDir = execSystemExecDir1;
    execSystemInputDir = execSystemInputDir1;
    execSystemOutputDir = execSystemOutputDir1;
    execSystemLogicalQueue = execSystemLogicalQueue1;
    archiveSystemId = archiveSystemId1;
    archiveSystemDir = archiveSystemDir1;
    archiveOnAppError = archiveOnAppError1;
    appArgs = appArgs1;
    containerArgs = containerArgs1;
    schedulerOptions = schedulerOptions1;
    envVariables = (envVariables1 == null) ? null : envVariables1.clone();
    archiveIncludes = (archiveIncludes1 == null) ? null : archiveIncludes1.clone();
    archiveExcludes = (archiveExcludes1 == null) ? null : archiveExcludes1.clone();
    archiveIncludeLaunchFiles = archiveIncludeLaunchFiles1;
    fileInputs = fileInputs1;
    nodeCount = nodeCount1;
    coresPerNode = coresPerNode1;
    memoryMb = memoryMb1;
    maxMinutes = maxMinutes1;
    notifSubscriptions = notifSubscriptions1;
    jobTags = (jobTags1 == null) ? null : jobTags1.clone();
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getTenant() { return tenant; }
  public String getId() { return id; }
  public String getVersion() { return version; }

  public String getDescription() { return description; }
  public Runtime getRuntime() { return runtime; }
  public String getRuntimeVersion() { return runtimeVersion; }
  public List<RuntimeOption> getRuntimeOptions()
  {
    return (runtimeOptions == null) ? null : new ArrayList<>(runtimeOptions);
  }
  public String getContainerImage() { return containerImage; }
  public Integer getMaxJobs() { return maxJobs; }
  public Integer getMaxJobsPerUser() { return maxJobsPerUser; }
  public Boolean isStrictFileInputs() { return strictFileInputs; }
  public String getJobDescription() { return jobDescription; }
  public Boolean isDynamicExecSystem() { return dynamicExecSystem; }
  public String[] getExecSystemConstraints()
  {
    return (execSystemConstraints == null) ? null : execSystemConstraints.clone();
  }
  public String getExecSystemId() { return execSystemId; }
  public String getExecSystemExecDir() { return execSystemExecDir; }
  public String getExecSystemInputDir() { return execSystemInputDir; }
  public String getExecSystemOutputDir() { return execSystemOutputDir; }
  public String getExecSystemLogicalQueue() { return execSystemLogicalQueue; }
  public String getArchiveSystemId() { return archiveSystemId; }
  public String getArchiveSystemDir() { return archiveSystemDir; }
  public Boolean getArchiveOnAppError() { return archiveOnAppError; }
  public List<AppArg> getAppArgs() { return (appArgs == null) ? null : new ArrayList<>(appArgs); }
  public List<AppArg> getContainerArgs() { return (containerArgs == null) ? null : new ArrayList<>(containerArgs); }
  public List<AppArg> getSchedulerOptions() { return (schedulerOptions == null) ? null : new ArrayList<>(schedulerOptions); }
  public String[] getEnvVariables() {
    return (envVariables == null) ? null : envVariables.clone();
  }
  public String[] getArchiveIncludes() {
    return (archiveIncludes == null) ? null : archiveIncludes.clone();
  }
  public String[] getArchiveExcludes() {
    return (archiveExcludes == null) ? null : archiveExcludes.clone();
  }
  public Boolean getArchiveIncludeLaunchFiles() { return archiveIncludeLaunchFiles; }
  public List<FileInput> getFileInputs() { return (fileInputs == null) ? null : new ArrayList<>(fileInputs); }
  public Integer getNodeCount() { return nodeCount; }
  public Integer getCoresPerNode() { return coresPerNode; }
  public Integer getMemoryMb() { return memoryMb; }
  public Integer getMaxMinutes() { return maxMinutes; }
  public List<NotifSubscription> getNotifSubscriptions()
  {
    return (notifSubscriptions == null) ? null : new ArrayList<>(notifSubscriptions);
  }
  public String[] getJobTags() {
    return (jobTags == null) ? null : jobTags.clone();
  }
  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }
  public Object getNotes() {
    return notes;
  }
}
