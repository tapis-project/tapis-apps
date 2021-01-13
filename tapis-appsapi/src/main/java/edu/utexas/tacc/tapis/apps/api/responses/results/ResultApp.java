package edu.utexas.tacc.tapis.apps.api.responses.results;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

import edu.utexas.tacc.tapis.shared.utils.JsonObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/*
  Classes representing a returned App result
 */
public final class ResultApp
{
  public int seqId;
  public String tenant;
  public String id;
  public String version;
  public String description;
  public AppType appType;
  public String owner;
  public boolean enabled;
  public Runtime runtime;
  public String runtimeVersion;
  public String containerImage;
  public int maxJobs;
  public int maxJobsPerUser;
  public boolean strictFileInputs;
  public JobAttributes jobAttributes;
  public String[] tags;
  // Json objects require special serializer for Jackson to handle properly in outgoing response.
  @JsonSerialize(using = JsonObjectSerializer.class)
  public Object notes;
  public String importRefId;

  // Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
  public ResultApp() { }

  public ResultApp(App a)
  {
    seqId = a.getSeqId();
    tenant = a.getTenant();
    id = a.getId();
    version = a.getVersion();
    description = a.getDescription();
    appType = a.getAppType();
    owner = a.getOwner();
    enabled = a.isEnabled();
    runtime = a.getRuntime();
    runtimeVersion = a.getRuntimeVersion();
    containerImage = a.getContainerImage();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    strictFileInputs = a.isStrictFileInputs();
    jobAttributes = new JobAttributes(a);
    tags = a.getTags();
    notes = a.getNotes();
    importRefId = a.getImportRefId();
  }
}