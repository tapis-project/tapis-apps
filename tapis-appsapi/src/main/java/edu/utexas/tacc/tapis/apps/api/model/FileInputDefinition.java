package edu.utexas.tacc.tapis.apps.api.model;

/*
 * Class for a FileInput contained in an App create request.
 */
public final class FileInputDefinition
{
  public String sourceUrl;
  public String targetPath;
  public boolean inPlace;
  public ArgMetaSpec meta;
}
