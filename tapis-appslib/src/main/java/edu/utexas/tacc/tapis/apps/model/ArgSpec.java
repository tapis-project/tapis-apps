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


  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(ArgSpec.class);

  private final String arg;
  private final String name;
  private final String description;
  private final ArgInputMode inputMode;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  // Default constructor to set defaults. This appears to be needed for when object is created from json using gson.
  public ArgSpec()
  {
    name = null;
    description = null;
    arg = null;
    inputMode = DEFAULT_INPUT_MODE;
  }

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

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
