package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.ArchiveFilter;
import edu.utexas.tacc.tapis.apps.model.KeyValuePair;

import java.util.List;

/*
 * Class representing ParameterSet fields that can be set in an incoming patch request json body
 * Use classes for attribute types instead of primitives so that null can be used to indicate
 *   that the value has not been included in the update request.
 * NOTE: Currently there are no attributes that might be primitives.
 */
public final class ParameterSetUpdate
{
  public List<ArgSpec> appArgs;
  public List<ArgSpec> containerArgs;
  public List<ArgSpec> schedulerOptions;
  public List<KeyValuePair> envVariables;
  public ArchiveFilterUpdate archiveFilter;
}
