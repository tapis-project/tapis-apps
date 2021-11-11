package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.apps.model.App.FileInputMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class representing a FileInput contained in an App definition.
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class FileInput
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Default values
  public static final FileInputMode DEFAULT_INPUT_MODE = FileInputMode.OPTIONAL;
  public static final Boolean DEFAULT_AUTO_MOUNT_LOCAL = true;

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(FileInput.class);

  private final String name;
  private final String description;
  // For Gson serialization we do not make inputMode or autoMountLocal final.
  //   This is so we can fill in defaults if they are ever null.
  private FileInputMode inputMode = DEFAULT_INPUT_MODE;
  private Boolean autoMountLocal = DEFAULT_AUTO_MOUNT_LOCAL;
  private final String sourceUrl;
  private final String targetPath;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  // Default constructor to set defaults. This appears to be needed for when object is created from json using gson.
  public FileInput()
  {
    name = null;
    description = null;
    setInputMode(null);
    setAutoMountLocal(null);
    sourceUrl = null;
    targetPath = null;
  }

  public FileInput(String name1, String description1, FileInputMode inputMode1, Boolean autoMountLocal1,
                   String sourceUrl1, String targetPath1)
  {
    name = name1;
    description = description1;
    setInputMode(inputMode1);
    setAutoMountLocal(autoMountLocal1);
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getName() { return name; }
  public String getDescription() { return description; }
  public FileInputMode getInputMode() { return inputMode; }
  public void setInputMode(FileInputMode im) { inputMode = (im == null) ? DEFAULT_INPUT_MODE : im;}
  public Boolean isAutoMountLocal() { return autoMountLocal; }
  public void setAutoMountLocal(Boolean b) { autoMountLocal = (b == null) ? DEFAULT_AUTO_MOUNT_LOCAL : b;}
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
