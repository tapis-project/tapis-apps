package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;

import java.util.ArrayList;
import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.EMPTY_STR_ARRAY;

/*
 * Class for JobAttributes in an App definition.
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
  public List<FileInputDefinition> fileInputDefinitions;
  public int nodeCount;
  public int coresPerNode;
  public int memoryMB;
  public int maxMinutes;
  public List<NotificationSubscription> subscriptions;
  public String[] tags;

  // Default constructor
  public JobAttributes()
  {
    execSystemConstraints = EMPTY_STR_ARRAY;
    fileInputDefinitions = new ArrayList<>();
    subscriptions = new ArrayList<>();
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
    parameterSet = new ParameterSet(a);
    fileInputDefinitions = ApiUtils.buildApiFileInputDefinitions(a.getFileInputs());
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMb();
    maxMinutes = a.getMaxMinutes();
    subscriptions = ApiUtils.buildApiNotifSubscriptions(a.getNotificationSubscriptions());
    tags = a.getJobTags();
  }
}
