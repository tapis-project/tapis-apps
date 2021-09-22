package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.InputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/*
 * Class representing FileInputs contained in an App definition.

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

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(FileInput.class);

  private final String sourceUrl;
  private final String targetPath;
  private final boolean inPlace;
  private final String name;
  private final String description;
  private final InputMode inputMode;
  private final List<KeyValuePair> meta;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  public FileInput(String sourceUrl1, String targetPath1, boolean inPlace1, String name1, String description1,
                   InputMode mode, List<KeyValuePair> meta)
  {
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    inPlace = inPlace1;
    name = name1;
    description = description1;
    this.inputMode = mode;
    this.meta = meta;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }
  public boolean isInPlace() { return inPlace; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public InputMode getInputMode() { return inputMode; }
  public List<KeyValuePair> getMeta() { return meta; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
