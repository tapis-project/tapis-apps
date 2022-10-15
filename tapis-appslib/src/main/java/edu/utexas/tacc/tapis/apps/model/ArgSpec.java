package edu.utexas.tacc.tapis.apps.model;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.apps.model.App.ArgInputMode;

import java.util.Objects;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;

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
  private final JsonObject notes; // metadata as json

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
    notes = DEFAULT_NOTES;
  }

  // Constructer setting all attributes
  public ArgSpec(String arg1, String name1, String description1, ArgInputMode mode1, JsonObject notes1)
  {
    arg = arg1;
    name = name1;
    description = description1;
    inputMode = (mode1 == null) ? DEFAULT_INPUT_MODE : mode1;
    notes = (notes1 == null) ? DEFAULT_NOTES : notes1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getArg() { return arg; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public ArgInputMode getInputMode() { return inputMode; }
  public JsonObject getNotes() { return notes; }

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
            Objects.equals(this.description, that.description) && Objects.equals(this.inputMode, that.inputMode) &&
            // JsonObject overrides equals so following should be fine.
            Objects.equals(this.notes, that.notes));
  }

  @Override
  public int hashCode()
  {
    int retVal = (arg == null ? 1 : arg.hashCode());
    retVal = 31 * retVal + (name == null ? 0 : name.hashCode());
    retVal = 31 * retVal + (description == null ? 0 : description.hashCode());
    // By inspection of this class inputMode and notes are not null
    retVal = 31 * retVal + inputMode.hashCode();
    retVal = 31 * retVal + notes.hashCode();
    return retVal;
  }
}
