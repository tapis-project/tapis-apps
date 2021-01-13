package edu.utexas.tacc.tapis.apps.api.responses.results;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.api.model.KeyValuePair;
import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.NotificationSubscription;
import edu.utexas.tacc.tapis.shared.utils.JsonObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

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

//class JobAttributes
//{
//  public String description;
//  public boolean dynamicExecSystem;
//  public String[] execSystemConstraints;
//  public String execSystemId;
//  public String execSystemExecDir;
//  public String execSystemInputDir;
//  public String execSystemOutputDir;
//  public String execSystemLogicalQueue;
//  public String archiveSystemId;
//  public String archiveSystemDir;
//  public boolean archiveOnAppError;
//  public ParameterSet parameterSet;
//  public List<FileInputDefinition> fileInputDefinitions;
//  public int nodeCount;
//  public int coresPerNode;
//  public int memoryMB;
//  public int maxMinutes;
//  public List<NotificationSubscription> subscriptions;
//  public String[] tags;
//
//  public JobAttributes(App a)
//  {
//    description = a.getJobDescription();
//    dynamicExecSystem = a.isDynamicExecSystem();
//    execSystemConstraints = a.getExecSystemConstraints();
//    execSystemId = a.getExecSystemId();
//    execSystemExecDir = a.getExecSystemExecDir();
//    execSystemInputDir = a.getExecSystemInputDir();
//    execSystemOutputDir = a.getExecSystemOutputDir();
//    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
//    archiveSystemId = a.getArchiveSystemId();
//    archiveSystemDir = a.getArchiveSystemDir();
//    archiveOnAppError = a.isArchiveOnAppError();
//    parameterSet = new ParameterSet(a);
//    fileInputDefinitions = constructFileInputDefinitions(a.getFileInputs());
//    nodeCount = a.getNodeCount();
//    coresPerNode = a.getCoresPerNode();
//    memoryMB = a.getMemoryMb();
//    maxMinutes = a.getMaxMinutes();
//    subscriptions = a.getNotificationSubscriptions();
//    tags = a.getJobTags();
//  }
//  List<FileInputDefinition> constructFileInputDefinitions(List<edu.utexas.tacc.tapis.apps.api.model.FileInputDefinition> fileInputs)
//  {
//    var retList = new ArrayList<FileInputDefinition>();
//    if (fileInputs == null || fileInputs.isEmpty()) return retList;
//    for (edu.utexas.tacc.tapis.apps.api.model.FileInputDefinition fi : fileInputs)
//    {
//      ArgSpec argSpec = new ArgSpec();
//      ArgMetaSpec meta = new ArgMetaSpec();
//      argSpec.arg = appArg.getArgValue();
//      meta.name = appArg.getMetaName();
//      meta.description = appArg.getMetaDescription();
//      meta.required = appArg.isMetaRequired();
//      argSpec.meta = meta;
////      String[] kvPairs = ApiUtils.getKeyValuesAsArray(meta.keyValuePairs);
////      AppArg modelArg = new AppArg(apiArg.arg, meta.name, meta.description, meta.required, kvPairs);
////      retList.add(modelArg);
//      retList.add(fid);
//    }
//    return retList;
//  }
//}

//class FileInputDefinition
//{
//  public String sourceUrl;
//  public String targetPath;
//  public boolean inPlace;
//  public ArgMetaSpec meta;
//}

//class ParameterSet
//{
//  public List<ArgSpec> appArgs;
//  public List<ArgSpec> containerArgs;
//  public List<ArgSpec> schedulerOptions;
//  public List<KeyValuePair> envVariables;
//  public ArchiveFilter archiveFilter;
//
//  public ParameterSet(App a)
//  {
//    appArgs = constructArgSpecList(a.getAppArgs());
//    containerArgs = constructArgSpecList(a.getContainerArgs());
//    schedulerOptions = constructArgSpecList(a.getSchedulerOptions());
//    envVariables = ApiUtils.getKeyValuesAsList(a.getEnvVariables());
//    archiveFilter = new ArchiveFilter(a);
//  }
//
//  List<ArgSpec> constructArgSpecList(List<AppArg> appArgs)
//  {
//    var retList = new ArrayList<ArgSpec>();
//    if (appArgs == null || appArgs.isEmpty()) return retList;
//    for (AppArg appArg : appArgs)
//    {
//      ArgSpec argSpec = new ArgSpec();
//      ArgMetaSpec meta = new ArgMetaSpec();
//      argSpec.arg = appArg.getArgValue();
//      meta.name = appArg.getMetaName();
//      meta.description = appArg.getMetaDescription();
//      meta.required = appArg.isMetaRequired();
//      argSpec.meta = meta;
////      String[] kvPairs = ApiUtils.getKeyValuesAsArray(meta.keyValuePairs);
////      AppArg modelArg = new AppArg(apiArg.arg, meta.name, meta.description, meta.required, kvPairs);
////      retList.add(modelArg);
//    }
//    return retList;
//  }
//}

//class ArgSpec
//{
//  public String arg;
//  public ArgMetaSpec meta;
//}

//class ArgMetaSpec
//{
//  public String name;
//  public String description;
//  public boolean required;
//  public List<KeyValuePair> keyValuePairs;
//}

//class ArchiveFilter
//{
//  public String[] includes;
//  public String[] excludes;
//  ArchiveFilter(App a)
//  {
//    includes = a.getArchiveIncludes();
//    excludes = a.getArchiveExcludes();
//  }
//}
