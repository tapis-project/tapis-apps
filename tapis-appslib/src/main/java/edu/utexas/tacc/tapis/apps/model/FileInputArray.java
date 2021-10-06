package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.InputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * Class representing FileInputArrays contained in an App definition.

 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class FileInputArray
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(FileInputArray.class);

  private final String name;
  private final String description;
  private final InputMode inputMode;
  private final List<String> sourceUrls;
  private final String targetDir;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  public FileInputArray(String name1, String description1, InputMode inputMode1, List<String> sourceUrls1,
                        String targetDir1)
  {
    name = name1;
    description = description1;
    inputMode = inputMode1;
// TODO/TBD do we need to preserve null?    sourceUrls = (sourceUrls1 == null) ? null : new ArrayList<>(sourceUrls1);
    sourceUrls = sourceUrls1;
    targetDir = targetDir1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getName() { return name; }
  public String getDescription() { return description; }
  public InputMode getInputMode() { return inputMode; }
  public List<String> getSourceUrls() { return (sourceUrls == null) ? null : new ArrayList<>(sourceUrls); }
  public String getTargetDir() { return targetDir; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
