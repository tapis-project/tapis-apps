package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.InputMode;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(ArgSpec.class);

  private final String arg;
  private final String name;
  private final String description;
  private final InputMode inputMode;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  public ArgSpec(String value1, String name1, String description1, InputMode mode1)
  {
    arg = value1;
    name = name1;
    description = description1;
    inputMode = mode1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getArg() { return arg; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public InputMode getInputMode() { return inputMode; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
