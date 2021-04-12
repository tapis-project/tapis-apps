package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_CONTAINERIZED;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_RUNTIME;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_STRICT_FILE_INPUTS;
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
  public boolean containerized = DEFAULT_CONTAINERIZED;
  public Runtime runtime = DEFAULT_RUNTIME;
  public String runtimeVersion;
  public List<RuntimeOption> runtimeOptions;
  public String containerImage;
  public int maxJobs;
  public int maxJobsPerUser;
  public boolean strictFileInputs = DEFAULT_STRICT_FILE_INPUTS;
  public JobAttributes jobAttributes;
  public String[] tags = EMPTY_STR_ARRAY;
  public Object notes = DEFAULT_NOTES;
}
