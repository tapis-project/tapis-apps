package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.FileInputArray;
import edu.utexas.tacc.tapis.apps.model.ReqSubscribe;
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
  public String dtnSystemInputDir;
  public String dtnSystemOutputDir;
  public String execSystemLogicalQueue;
  public String archiveSystemId;
  public String archiveSystemDir;
  public boolean archiveOnAppError;
  public boolean isMpi;
  public String mpiCmd;
  public String cmdPrefix;
  public ParameterSet parameterSet;
  public List<FileInput> fileInputs;
  public List<FileInputArray> fileInputArrays;
  public int nodeCount = DEFAULT_NODE_COUNT;
  public int coresPerNode = DEFAULT_CORES_PER_NODE;
  public int memoryMB = DEFAULT_MEMORY_MB;
  public int maxMinutes = DEFAULT_MAX_MINUTES;
  public List<ReqSubscribe> subscriptions;
  public String[] tags;

  // Default constructor to ensure defaults are set during jax-rs processing
  // Make sure correct defaults are set for any fields that are not required.
  // NOTE: We set value = null to distinguish between not set and empty string for incoming jax-rs request.
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
    dtnSystemInputDir = a.getDtnSystemInputDir();
    dtnSystemOutputDir = a.getDtnSystemOutputDir();
    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
    archiveSystemId = a.getArchiveSystemId();
    archiveSystemDir = a.getArchiveSystemDir();
    archiveOnAppError = a.isArchiveOnAppError();
    isMpi = a.getIsMpi();
    mpiCmd = a.getMpiCmd();
    cmdPrefix = a.getCmdPrefix();
    parameterSet = new ParameterSet(a.getParameterSet());
    fileInputs = a.getFileInputs();
    fileInputArrays = a.getFileInputArrays();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMB();
    maxMinutes = a.getMaxMinutes();
    subscriptions = a.getSubscriptions();
    tags = a.getJobTags();
  }
}
