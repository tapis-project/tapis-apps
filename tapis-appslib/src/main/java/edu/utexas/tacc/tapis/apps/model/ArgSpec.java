package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.apps.model.App.ArgInputMode;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o)
  {
    if (o == this) return true;
    // Note: no need to check for o==null since instanceof will handle that case
    if (!(o instanceof ArgSpec)) return false;
    var that = (ArgSpec) o;
    return (Objects.equals(this.arg, that.arg) && Objects.equals(this.name, that.name) &&
            Objects.equals(this.description, that.description) && Objects.equals(this.inputMode, that.inputMode));
  }

  @Override
  public int hashCode()
  {
    int retVal = (arg == null ? 1 : arg.hashCode());
    retVal = 31 * retVal + (name == null ? 0 : name.hashCode());
    retVal = 31 * retVal + (description == null ? 0 : description.hashCode());
    // By inspection of this class inputMode is not null
    retVal = 31 * retVal + inputMode.hashCode();
    return retVal;
  }
}
