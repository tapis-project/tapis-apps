package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_RUNTIME;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_STRICT_FILE_INPUTS;
import static edu.utexas.tacc.tapis.apps.model.App.EMPTY_STR_ARRAY;

/*
 * Class representing all app attributes that can be set in an incoming PUT request json body
 */
public final class ReqPutApp
{
  public String description;
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
