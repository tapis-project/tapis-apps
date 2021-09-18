package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

/*
 * Class for appArg, containerArg and schedulerOption in an App definition contained in a request.
 */
public final class ArgSpec
{
  public String name;
  public String description;
  public App.InputMode mode;
  public List<KeyValuePair> meta;
  public String arg;
}
