package edu.utexas.tacc.tapis.apps.model;

import java.util.List;

import edu.utexas.tacc.tapis.apps.model.App.Runtime;
/*
 * Class representing an update to a Tapis App.
 * Fields set to null indicate attribute not updated.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class PatchApp
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private String tenant;
  private String id;
  private String version;
  private final String description;
  private final Boolean enabled;
  private Runtime runtime;
  private String runtimeVersion;
  private String containerImage;
  private Integer maxJobs;
  private Integer maxJobsPerUser;
  private Boolean strictFileInputs;
  // === Start jobAttributes ==========
  private String jobDescription;
  private Boolean dynamicExecSystem;
  private String[] execSystemConstraints;
  private String execSystemId;
  private String execSystemExecDir;
  private String execSystemInputDir;
  private String execSystemOutputDir;
  private String execSystemLogicalQueue;
  private String archiveSystemId;
  private String archiveSystemDir;
  private Boolean archiveOnAppError;
  private String[] envVariables;
  private String[] archiveIncludes;
  private String[] archiveExcludes;
  private Integer nodeCount;
  private Integer coresPerNode;
  private Integer memoryMb;
  private Integer maxMinutes;
  private String[] jobTags;
  // === End jobAttributes ==========

  // Aux tables
  private List<FileInput> fileInputs;
  private List<NotifSubscription> notificationSubscriptions;
  private List<AppArg> appArgs;
  private List<AppArg> containerArgs;
  private List<AppArg> schedulerOptions;
  private final String[] tags;
  private Object notes;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all final attributes.
   */
  public PatchApp(String description1, Boolean enabled1, Runtime runtime1, String runtimeVersion1,
                  String containerImage, Integer maxJobs1, Integer maxJobsPerUser1, Boolean strictFileInputs1,
                  String[] tags1, Object notes1)
  {
    description = description1;
    enabled = enabled1;
    runtime = runtime1;
    runtimeVersion = runtimeVersion1;
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    strictFileInputs = strictFileInputs1;
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getTenant() { return tenant; }
  public void setTenant(String s) { tenant = s; }

  public String getId() { return id; }
  public void setId(String s) { id = s; }

  public String getVersion() { return version; }
  public void setVersion(String s) { version = s; }

  public String getDescription() { return description; }
  public Boolean isEnabled() { return enabled; }
  public Runtime getRuntime() { return runtime; }
  public String getRuntimeVersion() { return runtimeVersion; }
  public String getContainerImage() { return containerImage; }
  public Integer getMaxJobs() { return maxJobs; }
  public Integer getMaxJobsPerUser() { return maxJobsPerUser; }
  public Boolean isStrictFileInputs() { return strictFileInputs; }
  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }

  public Object getNotes() {
    return notes;
  }
  public void setNotes(Object n) { notes = n; }
}
