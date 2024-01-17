package edu.utexas.tacc.tapis.apps.model;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.apps.model.App.FileInputMode;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;

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
  private final FileInputMode inputMode;
  private final Boolean autoMountLocal;
  private final JsonObject notes; // metadata as json
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
    inputMode = DEFAULT_INPUT_MODE;
    autoMountLocal = DEFAULT_AUTO_MOUNT_LOCAL;
    notes = DEFAULT_NOTES;
    sourceUrl = null;
    targetPath = null;
  }

  public FileInput(String name1, String description1, FileInputMode inputMode1, Boolean autoMountLocal1,
                   JsonObject notes1, String sourceUrl1, String targetPath1)
  {
    name = LibUtils.stripStr(name1);
    description = description1;
    inputMode = (inputMode1 == null) ? DEFAULT_INPUT_MODE : inputMode1;
    autoMountLocal = (autoMountLocal1 == null) ? DEFAULT_AUTO_MOUNT_LOCAL : autoMountLocal1;
    notes = (notes1 == null) ? DEFAULT_NOTES : notes1;
    sourceUrl = LibUtils.stripStr(sourceUrl1);
    targetPath = LibUtils.stripStr(targetPath1);
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getName() { return name; }
  public String getDescription() { return description; }
  public FileInputMode getInputMode() { return inputMode; }
  public Boolean isAutoMountLocal() { return autoMountLocal; }
  public JsonObject getNotes() { return notes; }
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
