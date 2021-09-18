package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

/*
 * Class for a FileInput contained in an App definition contained in a request.
 */
public final class FileInput
{
  public String name;
  public String description;
  public App.InputMode mode;
  public boolean inPlace;
  public String sourceUrl;
  public String targetPath;
  public List<KeyValuePair> meta;
}
