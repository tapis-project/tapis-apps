package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Class representing job related attributes in a Tapis App.
 *
 */
public final class JobAttributes
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final String description;
  private final Boolean dynamicExecSystem;
  private final String[] execSystemConstraints;
  private final String execSystemId;
  private final String execSystemExecDir;
  private final String execSystemInputDir;
  private final String execSystemOutputDir;
  private final String execSystemLogicalQueue;
  private final String archiveSystemId;
  private final String archiveSystemDir;
  private final Boolean archiveOnAppError;
  private final ParameterSet parameterSet;
  private final List<FileInput> fileInputs;
  private final Integer nodeCount;
  private final Integer coresPerNode;
  private final Integer memoryMb;
  private final Integer maxMinutes;
  private final List<NotifSubscription> subscriptions;
  private final String[] tags;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all attributes.
   */
  public JobAttributes(String description1, Boolean dynamicExecSystem1, String[] execSystemConstraints1,
                       String execSystemId1, String execSystemExecDir1, String execSystemInputDir1, String execSystemOutputDir1,
                       String execSystemLogicalQueue1, String archiveSystemId1, String archiveSystemDir1,
                       Boolean archiveOnAppError1, ParameterSet parameterSet1, List<FileInput> fileInputs1,
                       Integer nodeCount1, Integer coresPerNode1, Integer memoryMb1, Integer maxMinutes1,
                       List<NotifSubscription> subscriptions1, String[] tags1)
  {
    description = description1;
    dynamicExecSystem = dynamicExecSystem1;
    execSystemConstraints = (execSystemConstraints1 == null) ? null : execSystemConstraints1.clone();
    execSystemId = execSystemId1;
    execSystemExecDir = execSystemExecDir1;
    execSystemInputDir = execSystemInputDir1;
    execSystemOutputDir = execSystemOutputDir1;
    execSystemLogicalQueue = execSystemLogicalQueue1;
    archiveSystemId = archiveSystemId1;
    archiveSystemDir = archiveSystemDir1;
    archiveOnAppError = archiveOnAppError1;
    parameterSet = (parameterSet1 == null) ? null : new ParameterSet(parameterSet1);
    fileInputs = (fileInputs1 == null) ? null: new ArrayList<>(fileInputs1);
    nodeCount = nodeCount1;
    coresPerNode = coresPerNode1;
    memoryMb = memoryMb1;
    maxMinutes = maxMinutes1;
    subscriptions = subscriptions1;
    tags = (tags1 == null) ? null : tags1.clone();
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getDescription() { return description; }
  public Boolean isDynamicExecSystem() { return dynamicExecSystem; }
  public String[] getExecSystemConstraints() { return execSystemConstraints; }
  public String getExecSystemId() { return execSystemId; }
  public String getExecSystemExecDir() { return execSystemExecDir; }
  public String getExecSystemInputDir() { return execSystemInputDir; }
  public String getExecSystemOutputDir() { return execSystemOutputDir; }
  public String getExecSystemLogicalQueue() { return execSystemLogicalQueue; }
  public String getArchiveSystemId() { return archiveSystemId; }
  public String getArchiveSystemDir() { return archiveSystemDir; }
  public Boolean getArchiveOnAppError() { return archiveOnAppError; }
  public ParameterSet getParameterSet() { return parameterSet; }
  public List<FileInput> getFileInputs() { return fileInputs; }
  public Integer getNodeCount() { return nodeCount; }
  public Integer getCoresPerNode() { return coresPerNode; }
  public Integer getMemoryMb() { return memoryMb; }
  public Integer getMaxMinutes() { return maxMinutes; }
  public List<NotifSubscription> getSubscriptions() { return subscriptions; }
  public String[] getTags() {
    return tags;
  }
}
