package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.FileInputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.FileInput.DEFAULT_INPUT_MODE;

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
  private FileInputMode inputMode;
  private final List<String> sourceUrls;
  private final String targetDir;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  // Default constructor to set defaults. This appears to be needed for when object is created from json using gson.
  public FileInputArray()
  {
    name = null;
    description = null;
    inputMode = DEFAULT_INPUT_MODE;
    sourceUrls = null;
    targetDir = null;
  }

  public FileInputArray(String name1, String description1, FileInputMode inputMode1, List<String> sourceUrls1,
                        String targetDir1)
  {
    name = name1;
    description = description1;
    inputMode = (inputMode1 == null) ? DEFAULT_INPUT_MODE : inputMode1;
    sourceUrls = sourceUrls1;
    targetDir = targetDir1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getName() { return name; }
  public String getDescription() { return description; }
  public FileInputMode getInputMode() { return inputMode; }
  public List<String> getSourceUrls() { return (sourceUrls == null) ? null : new ArrayList<>(sourceUrls); }
  public String getTargetDir() { return targetDir; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
