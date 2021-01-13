package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.FileInput;

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
    fileInputDefinitions = buildFileInputDefinitions(a.getFileInputs());
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMb();
    maxMinutes = a.getMaxMinutes();
    subscriptions = buildSubscriptions(a.getNotificationSubscriptions());
    tags = a.getJobTags();
  }

  // Build a list of api model file inputs based on the lib model file inputs
  List<FileInputDefinition> buildFileInputDefinitions(List<FileInput> fileInputs)
  {
    var retList = new ArrayList<FileInputDefinition>();
    if (fileInputs == null || fileInputs.isEmpty()) return retList;
    for (FileInput fi : fileInputs)
    {
      ArgMetaSpec meta = new ArgMetaSpec();
      meta.name = fi.getMetaName();
      meta.description = fi.getMetaDescription();
      meta.required = fi.isMetaRequired();
      meta.keyValuePairs = ApiUtils.getKeyValuesAsList(fi.getMetaKeyValuePairs());
      FileInputDefinition fid = new FileInputDefinition();
      fid.sourceUrl = fi.getSourceUrl();
      fid.targetPath = fi.getTargetPath();
      fid.inPlace = fi.isInPlace();
      fid.meta = meta;
      retList.add(fid);
    }
    return retList;
  }

  // Build a list of api model subscriptions based on the lib model subscriptions
  List<NotificationSubscription> buildSubscriptions(List<edu.utexas.tacc.tapis.apps.model.NotificationSubscription> subscriptions)
  {
    var retList = new ArrayList<NotificationSubscription>();
    if (subscriptions == null || subscriptions.isEmpty()) return retList;
    for (edu.utexas.tacc.tapis.apps.model.NotificationSubscription subscription : subscriptions)
    {
      NotificationSubscription subscription1 = new NotificationSubscription();
      subscription1.filter = subscription.getFilter();
// TODO
//      subscription1.notificationMechanisms = ApiUtils.getNotificationMechanisms(subscription.getNotificationMechanisms());
      retList.add(subscription1);
    }
    return retList;
  }
}
