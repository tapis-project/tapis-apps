package edu.utexas.tacc.tapis.apps.api.model;

import java.util.List;

/*
 * Class representing ParameterSet fields that can be set in an incoming patch request json body
 * Use classes for attribute types instead of primitives so that null can be use to indicate
 *   that the value has not been included in the update request.
 */
public final class ParameterSetUpdate
{
  public List<ArgSpec> appArgs;
  public List<ArgSpec> containerArgs;
  public List<ArgSpec> schedulerOptions;
  public List<KeyValuePair> envVariables;
  public ArchiveFilter archiveFilter;
}
