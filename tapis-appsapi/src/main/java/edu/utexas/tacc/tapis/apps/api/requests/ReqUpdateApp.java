package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributesUpdate;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import java.util.List;

/*
 * Class representing all app attributes that can be set in an incoming patch request json body
 * Use classes for attribute types instead of primitives so that null can be use to indicate
 *   that the value has not been included in the update request.
 */
public final class ReqUpdateApp
{
  public String description;
  public App.Runtime runtime;
  public String runtimeVersion;
  public List<RuntimeOption> runtimeOptions;
  public String containerImage;
  public Integer maxJobs;
  public Integer maxJobsPerUser;
  public Boolean strictFileInputs;
  public JobAttributesUpdate jobAttributes;
  public String[] tags;
  public Object notes;
}
