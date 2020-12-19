package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.FileInput;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.apps.model.App.EMPTY_STR_ARRAY;

/*
 * Class representing all app attributes that can be set in an incoming create request json body
 */
public final class ReqCreateApp
{
  public String id;
  public String version;
  public String description;
  public AppType appType;
  public String owner = DEFAULT_OWNER;
  public boolean enabled = DEFAULT_ENABLED;
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
  public int memoryMb;
  public int maxMinutes;
  public String[] jobTags = EMPTY_STR_ARRAY;       // List of arbitrary tags as strings
  // === End jobAttributes ===


  public List<FileInput> fileInputs;

  public String[] tags = EMPTY_STR_ARRAY;       // List of arbitrary tags as strings
  public Object notes = DEFAULT_NOTES;      // Simple metadata as json
  public String refImportId; // Optional reference ID for an app created via import
}
