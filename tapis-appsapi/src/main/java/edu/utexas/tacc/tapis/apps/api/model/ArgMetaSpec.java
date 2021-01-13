package edu.utexas.tacc.tapis.apps.api.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Class for metadata about an appArg, containerArg, schedulerOption or fileInput in an App definition.
 */
public final class ArgMetaSpec
{
  public String name;
  public String description;
  public boolean required;
  public List<KeyValuePair> keyValuePairs;
  public ArgMetaSpec()
  {
    keyValuePairs = new ArrayList<>();
  }
}
