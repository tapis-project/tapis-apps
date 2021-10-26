package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.apps.model.App.ArgInputMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Argument with metadata
 *  - used for app arguments, container arguments, scheduler options
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class ArgSpec
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Default values
  public static final ArgInputMode DEFAULT_INPUT_MODE = ArgInputMode.INCLUDE_ON_DEMAND;
//  public static final Boolean DEFAULT_AUTO_MOUNT_LOCAL = true;


  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(ArgSpec.class);

  private final String arg;
  private final String name;
  private final String description;
  // For Gson serialization we do not make inputMode final.
  //   This is so we can fill in defaults if they are ever null.
  private ArgInputMode inputMode = DEFAULT_INPUT_MODE;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  public ArgSpec(String value1, String name1, String description1, ArgInputMode mode1)
  {
    arg = value1;
    name = name1;
    description = description1;
    inputMode = (mode1 == null) ? DEFAULT_INPUT_MODE : mode1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getArg() { return arg; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public ArgInputMode getInputMode() { return inputMode; }
  public void setInputMode(ArgInputMode im) { inputMode = (im == null) ? DEFAULT_INPUT_MODE : im; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
