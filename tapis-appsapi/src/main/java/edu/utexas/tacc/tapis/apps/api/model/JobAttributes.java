package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotificationSubscription;
import edu.utexas.tacc.tapis.apps.model.ParameterSet;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_CORES_PER_NODE;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_MAX_MINUTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_MEMORY_MB;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NODE_COUNT;
import static edu.utexas.tacc.tapis.apps.model.App.EMPTY_STR_ARRAY;

/*
 * Class for JobAttributes in an App definition contained in a request.
 */
public final class JobAttributes
{
  public String description;
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
  public ParameterSet parameterSet;
  public List<FileInput> fileInputs;
  public int nodeCount = DEFAULT_NODE_COUNT;
  public int coresPerNode = DEFAULT_CORES_PER_NODE;
  public int memoryMb = DEFAULT_MEMORY_MB;
  public int maxMinutes = DEFAULT_MAX_MINUTES;
  public List<NotificationSubscription> subscriptions;
  public String[] tags;

  // Default constructor
  public JobAttributes()
  {
    tags = EMPTY_STR_ARRAY;
  }

  // Constructor to build an api model object from the lib model App object
  public JobAttributes(App a)
  {
    description = a.getJobDescription();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = a.getExecSystemConstraints();
    execSystemId = a.getExecSystemId();
    execSystemExecDir = a.getExecSystemExecDir();
    execSystemInputDir = a.getExecSystemInputDir();
    execSystemOutputDir = a.getExecSystemOutputDir();
    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
    archiveSystemId = a.getArchiveSystemId();
    archiveSystemDir = a.getArchiveSystemDir();
    archiveOnAppError = a.isArchiveOnAppError();
    parameterSet = new ParameterSet(a.getParameterSet());
    fileInputs = a.getFileInputs();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMb = a.getMemoryMb();
    maxMinutes = a.getMaxMinutes();
    subscriptions = a.getSubscriptions();
    tags = a.getJobTags();
  }
}
