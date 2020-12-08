package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.model.App.AppType;
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
  public String name;       // Name of the app
  public String version;    // Version of the app
  public String description; // Full description of the app
  public AppType appType; // Type of app, e.g.
  public String owner = DEFAULT_OWNER;      // User who owns the app and has full privileges
  public boolean enabled = DEFAULT_ENABLED; // Indicates if the app is currently enabled
  private boolean dynamicExecSystem; // Indicates if constraints are to be used
  private String[] execSystemConstraints; // List of constraints
  private String execSystemId;
  private String execSystemExecDir;
  private String execSystemInputDir;
  private String execSystemOutputDir;
  private String archiveSystemId;
  private String archiveSystemDir;
  private boolean archiveOnAppError;
  private boolean useDTNIfDefined;
  private String[] envVariables;
  private String jobDescriptionTemplate;
  private int maxJobs;
  private int maxJobsPerUser;
  private int nodeCount;
  private int coresPerNode;
  private int memoryMB;
  private int maxMinutes;

  public List<FileInput> fileInputs;
  public String[] tags = DEFAULT_TAGS;       // List of arbitrary tags as strings
  public Object notes = DEFAULT_NOTES;      // Simple metadata as json
  public String refImportId; // Optional reference ID for an app created via import
}
