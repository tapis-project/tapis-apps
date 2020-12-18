package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.ContainerRuntime;
import edu.utexas.tacc.tapis.apps.model.FileInput;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_TAGS;

/*
 * Class representing all app attributes that can be set in an incoming create request json body
 */
public final class ReqCreateApp
{
  public String id;       // Name of the app
  public String version;    // Version of the app
  public String description; // Full description of the app
  public AppType appType; // Type of app, e.g.
  public String owner = DEFAULT_OWNER;      // User who owns the app and has full privileges
  public boolean enabled = DEFAULT_ENABLED; // Indicates if the app is currently enabled
  public boolean isInteractive;
  public boolean containerized;
  public ContainerRuntime containerRuntime;
  public String containerImage;
  public String command;
  public String[] envVariables;
  public boolean dynamicExecSystem; // Indicates if constraints are to be used
  public String[] execSystemConstraints; // List of constraints
  public String execSystemId;
  public String execSystemExecDir;
  public String execSystemInputDir;
  public String execSystemOutputDir;
  public String execSystemLogicalQueue;
  public String archiveSystemId;
  public String archiveSystemDir;
  public boolean archiveOnAppError;
  public String jobDescription;
  public int maxJobs;
  public int maxJobsPerUser;
  public int nodeCount;
  public int coresPerNode;
  public int memoryMB;
  public int maxMinutes;
  public String[] archiveIncludes;
  public String[] archiveExcludes;

  public List<FileInput> fileInputs;
  public String[] tags = DEFAULT_TAGS;       // List of arbitrary tags as strings
  public Object notes = DEFAULT_NOTES;      // Simple metadata as json
  public String refImportId; // Optional reference ID for an app created via import
}
