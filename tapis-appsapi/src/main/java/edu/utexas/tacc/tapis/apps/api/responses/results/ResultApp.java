package edu.utexas.tacc.tapis.apps.api.responses.results;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

import edu.utexas.tacc.tapis.shared.utils.JsonObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_TAGS;

/*
    Class representing a returned TSystem result
 */
public final class ResultApp
{
  public int seqId;
  public String tenant;
  public String id;
  public String description;
  public AppType appType;
  public String owner;
  public boolean enabled;
  public Runtime runtime;
  public String runtimeVersion;
  public String containerImage;
  public int maxJobs;
  public int maxJobsPerUser;
  // === Start jobAttributes ===
  public String jobDescription;
  public boolean dynamicExecSystem;
  public String[] execSystemConstraints;
  public String execSystemId;
  public String execSystemExecDir;
  public String execSystemInputDir;
  public String execSystemOutputDir;
  public String execSystemLogicalQueue;
  public String archiveSystemId;
  public String archiveSystemDir;
  public boolean archiveOnAppError;
  public String[] envVariables; // from parameterSet
  public String[] archiveIncludes; // from parameterSet
  public String[] archiveExcludes; // from parameterSet
  public int nodeCount;
  public int coresPerNode;
  public int memoryMB;
  public int maxMinutes;
  public String[] jobTags = DEFAULT_TAGS;       // List of arbitrary tags as strings
  // === End jobAttributes ===


  public String[] tags;
  // Json objects require special serializer for Jackson to handle properly in outgoing response.
  @JsonSerialize(using = JsonObjectSerializer.class)
  public Object notes;
  public String refImportId;

  // Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
  public ResultApp() { }

  public ResultApp(App a)
  {
    seqId = a.getSeqId();
    tenant = a.getTenant();
    id = a.getId();
    description = a.getDescription();
    appType = a.getAppType();
    owner = a.getOwner();
    enabled = a.isEnabled();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    jobDescription = a.getJobDescription();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = a.getExecSystemConstraints();
    execSystemId = a.getExecSystemId();
    execSystemExecDir = a.getExecSystemExecDir();
    execSystemInputDir = a.getExecSystemInputDir();
    execSystemOutputDir = a.getExecSystemOutputDir();
    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
    archiveSystemId = a.getArchiveSystemId();
    archiveSystemDir = a.getArchiveSystemDir();
    archiveOnAppError = a.isArchiveOnAppError();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMB();
    maxMinutes = a.getMaxMinutes();
    archiveIncludes = a.getArchiveIncludes();
    archiveExcludes = a.getArchiveExcludes();
    jobTags = a.getJobTags();
    tags = a.getTags();
    notes = a.getNotes();
    refImportId = a.getImportRefId();
  }
}
