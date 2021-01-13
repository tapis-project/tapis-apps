package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

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
  public boolean strictFileInputs;
  public JobAttributes jobAttributes;
  public String[] tags = EMPTY_STR_ARRAY;       // List of arbitrary tags as strings
  public Object notes = DEFAULT_NOTES;      // Simple metadata as json
  public String importRefId; // Optional reference ID for an app created via import
}
