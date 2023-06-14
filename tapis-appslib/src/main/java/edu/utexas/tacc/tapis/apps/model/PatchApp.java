package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

import edu.utexas.tacc.tapis.apps.model.App.JobType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

/*
 * Class representing an update to a Tapis App.
 * Fields set to null indicate attribute not updated.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 *
 * Allow notes, jobType to be set because these fields require special handling when
 * object is being created based on a jax-rs request.
 */
public final class PatchApp
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final String description;
  private final Runtime runtime;
  private final String runtimeVersion;
  private final List<RuntimeOption> runtimeOptions;
  private final String containerImage;
  private JobType jobType;
  private final Integer maxJobs;
  private final Integer maxJobsPerUser;
  private final Boolean strictFileInputs;
  private final JobAttributes jobAttributes;

  private final String[] tags;
  private JsonObject notes; // Not final since may require special handling and need to be updated. See AppResource.java

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all attributes.
   */
  public PatchApp(String description1, Runtime runtime1, String runtimeVersion1, List<RuntimeOption> runtimeOptions1,
                  String containerImage1, JobType jobType1, Integer maxJobs1, Integer maxJobsPerUser1, Boolean strictFileInputs1,
                  JobAttributes jobAttributes1, String[] tags1, JsonObject notes1)
  {
    description = description1;
    runtime = runtime1;
    runtimeVersion = runtimeVersion1;
    runtimeOptions = (runtimeOptions1 == null) ? null : new ArrayList<>(runtimeOptions1);
    containerImage = containerImage1;
    jobType = jobType1;
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    strictFileInputs = strictFileInputs1;
    jobAttributes = jobAttributes1;
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getDescription() { return description; }
  public Runtime getRuntime() { return runtime; }
  public String getRuntimeVersion() { return runtimeVersion; }
  public List<RuntimeOption> getRuntimeOptions()
  {
    return (runtimeOptions == null) ? null : new ArrayList<>(runtimeOptions);
  }
  public String getContainerImage() { return containerImage; }
  public JobType getJobType() { return jobType; }
  public void setJobType(JobType jt) { jobType = jt; }
  public Integer getMaxJobs() { return maxJobs; }
  public Integer getMaxJobsPerUser() { return maxJobsPerUser; }
  public Boolean isStrictFileInputs() { return strictFileInputs; }
  public JobAttributes getJobAttributes() { return jobAttributes; }
  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }
  public JsonObject getNotes() { return notes; }
  public void setNotes(JsonObject o) { notes = o; }
}
