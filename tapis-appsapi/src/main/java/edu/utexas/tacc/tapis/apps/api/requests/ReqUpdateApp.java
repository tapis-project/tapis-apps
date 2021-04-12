package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import java.util.List;

/*
 * Class representing all app attributes that can be set in an incoming patch request json body
 */
public final class ReqUpdateApp
{
  public String description;
  public App.Runtime runtime;
  public String runtimeVersion;
  public List<RuntimeOption> runtimeOptions;
  public String containerImage;
  public int maxJobs;
  public int maxJobsPerUser;
  public boolean strictFileInputs;
  public JobAttributes jobAttributes;
  public String[] tags;
  public Object notes;
}
