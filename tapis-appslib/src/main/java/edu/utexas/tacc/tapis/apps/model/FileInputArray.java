package edu.utexas.tacc.tapis.apps.model;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.App.FileInputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
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
  private final JsonObject notes; // metadata as json
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
    notes = DEFAULT_NOTES;
    sourceUrls = null;
    targetDir = null;
  }

  public FileInputArray(String name1, String description1, FileInputMode inputMode1, JsonObject notes1,
                        List<String> sourceUrls1, String targetDir1)
  {
    name = name1;
    description = description1;
    inputMode = (inputMode1 == null) ? DEFAULT_INPUT_MODE : inputMode1;
    notes = (notes1 == null) ? DEFAULT_NOTES : notes1;
    sourceUrls = sourceUrls1;
    targetDir = targetDir1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getName() { return name; }
  public String getDescription() { return description; }
  public FileInputMode getInputMode() { return inputMode; }
  public JsonObject getNotes() { return notes; }
  public List<String> getSourceUrls() { return (sourceUrls == null) ? null : new ArrayList<>(sourceUrls); }
  public String getTargetDir() { return targetDir; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
