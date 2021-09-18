package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.InputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Argument with metadata
 *  - used for container arguments, command arguments, scheduler options
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class AppArg
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(AppArg.class);

  private final int seqId; // Unique database sequence number
  private final int appSeqId;
  private final String argValue;
  private final String name;
  private final String description;
  private final InputMode mode;
  private final String[] meta;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public AppArg(int seqId1, int appId1, String value1, String name1,
                  String description1, InputMode mode1, String[] meta1)
  {
    seqId = seqId1;
    appSeqId = appId1;
    argValue = value1;
    name = name1;
    description = description1;
    mode = mode1;
    meta = meta1;
  }

  public AppArg(String value1, String name1, String description1, InputMode mode1, String[] meta1)
  {
    seqId = -1;
    appSeqId = -1;
    argValue = value1;
    name = name1;
    description = description1;
    mode = mode1;
    meta = meta1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public int getSeqId() { return seqId; }
  public int getAppSeqId() { return appSeqId; }
  public String getArgValue() { return argValue; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public InputMode getMode() { return mode; }
  public String[] getMeta() { return meta; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
