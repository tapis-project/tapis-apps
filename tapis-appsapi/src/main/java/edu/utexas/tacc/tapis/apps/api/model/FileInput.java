package edu.utexas.tacc.tapis.apps.api.model;

/*
 * Class for a FileInput contained in an App definition contained in a request.
 */
public final class FileInput
{
  public String sourceUrl;
  public String targetPath;
  public boolean inPlace;
  public ArgMetaSpec meta;
}
