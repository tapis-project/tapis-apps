package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.KeyValuePair;

import java.util.List;

/*
 * Class representing ArchiveFilter fields that can be set in an incoming patch request json body
 * Use classes for attribute types instead of primitives so that null can be used to indicate
 *   that the value has not been included in the update request.
 */
public final class ArchiveFilterUpdate
{
  public String[] includes;
  public String[] excludes;
  Boolean includeLaunchFiles;
}
