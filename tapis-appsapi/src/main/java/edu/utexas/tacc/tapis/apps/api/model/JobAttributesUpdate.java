package edu.utexas.tacc.tapis.apps.api.model;

import java.util.List;

/*
 * Class representing JobAttribute fields that can be set in an incoming patch request json body
 * Use classes for attribute types instead of primitives so that null can be use to indicate
 *   that the value has not been included in the update request.
 */
public final class JobAttributesUpdate
{
  public String description;
  public Boolean dynamicExecSystem;
  public String[] execSystemConstraints;
  public String execSystemId;
  public String execSystemExecDir;
  public String execSystemInputDir;
  public String execSystemOutputDir;
  public String execSystemLogicalQueue;
  public String archiveSystemId;
  public String archiveSystemDir;
  public Boolean archiveOnAppError;
  public ParameterSetUpdate parameterSet;
  public List<FileInputDefinition> fileInputDefinitions;
  public Integer nodeCount;
  public Integer coresPerNode;
  public Integer memoryMB;
  public Integer maxMinutes;
  public List<NotificationSubscription> subscriptions;
  public String[] tags;
}
