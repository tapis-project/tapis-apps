package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.InputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final int seqId; // Unique database sequence number
  private final int appSeqId;

  private final String sourceUrl;
  private final String targetPath;
  private final boolean inPlace;
  private final String name;
  private final String description;
  private final InputMode mode;
  private final String[] meta;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public FileInput(int seqId1, int appSeqId1, String sourceUrl1, String targetPath1, boolean inPlace1,
                     String name1, String description1, InputMode mode, String[] meta)
  {
    seqId = seqId1;
    appSeqId = appSeqId1;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    inPlace = inPlace1;
    name = name1;
    description = description1;
    this.mode = mode;
    this.meta = meta;
  }

  public FileInput(String sourceUrl1, String targetPath1, boolean inPlace1, String name1, String description1,
                   InputMode mode, String[] meta)
  {
    seqId = -1;
    appSeqId = -1;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    inPlace = inPlace1;
    name = name1;
    description = description1;
    this.mode = mode;
    this.meta = meta;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public int getSeqId() { return seqId; }
  public int getAppSeqId() { return appSeqId; }
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }
  public boolean isInPlace() { return inPlace; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public InputMode getMode() { return mode; }
  public String[] getMeta() { return meta; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
