package edu.utexas.tacc.tapis.apps.model;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import java.util.Objects;
import edu.utexas.tacc.tapis.apps.model.App.ArgInputMode;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;

/*
 * Class for KeyValuePair in an App definition.
 * Key should not contain the character "=" because it would change the value
 * NOTE: Might want to override equals/hashCode so List.equals can work. For change history checking
 *       Make comparable?
 *       Use Google's autoValue annotation for equals/hashCode?
 *       Better yet use java's new record keyword?
 *       Unfortunately both autoValue and record keyword break gson.
 */
public final class KeyValuePair
{
  public static final ArgInputMode DEFAULT_INPUT_MODE = ArgInputMode.INCLUDE_BY_DEFAULT;

  private final String key;
  private final String value;
  private final String description;
  private final ArgInputMode inputMode;
  private final JsonObject notes; // metadata as json

  public KeyValuePair(String key1, String value1, String description1, ArgInputMode inputMode1, JsonObject notes1)
  {
    key = key1;
    value = StringUtils.isBlank(value1) ? "" : value1;
    description = StringUtils.isBlank(description1) ? "" : description1;
    inputMode = (inputMode1 == null) ? DEFAULT_INPUT_MODE : inputMode1;
    notes = (notes1 == null) ? DEFAULT_NOTES : notes1;
  }

  public KeyValuePair(String key1, String value1, String description1)
  {
    key = key1;
    value = StringUtils.isBlank(value1) ? "" : value1;
    description = StringUtils.isBlank(description1) ? "" : description1;
    inputMode = DEFAULT_INPUT_MODE;
    notes = DEFAULT_NOTES;
  }

  public KeyValuePair(String key1, String value1)
  {
    key = key1;
    value = StringUtils.isBlank(value1) ? "" : value1;
    description = "";
    inputMode = DEFAULT_INPUT_MODE;
    notes = DEFAULT_NOTES;
  }

  public String getKey() { return key; }
  public String getValue() { return value; }
  public String getDescription() { return description; }
  public ArgInputMode getInputMode() { return inputMode; }
  public JsonObject getNotes() { return notes; }

// NOTE: If this is ever used it will strip off the description
//  public static KeyValuePair fromString(String s)
//  {
//    if (StringUtils.isBlank(s)) return new KeyValuePair("","");
//    int e1 = s.indexOf('=');
//    String k = s.substring(0, e1);
//    String v = "";
//    // Everything after "=" is the value
//    if (e1 > 0) v = s.substring(e1+1);
//    return new KeyValuePair(k, v);
//  }

  @Override
  public String toString() { return String.format("%s=%s;description:%s,inputMode:%s,notes:%s", key, value, description, inputMode, notes); }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) return true;
    // Note: no need to check for o==null since instanceof will handle that case
    if (!(o instanceof KeyValuePair)) return false;
    var that = (KeyValuePair) o;
    return (Objects.equals(this.key, that.key) && Objects.equals(this.value, that.value) &&
            Objects.equals(this.description, that.description)  && Objects.equals(this.inputMode, that.inputMode) &&
            // JsonObject overrides equals so following should be fine.
            Objects.equals(this.notes, that.notes));
  }

  @Override
  public int hashCode()
  {
    int retVal = (key == null ? 1 : key.hashCode());
    // By inspection of this class value, description, inputMode and notes cannot be null
    retVal = 31 * retVal + value.hashCode();
    retVal = 31 * retVal + description.hashCode();
    retVal = 31 * retVal + inputMode.hashCode();
    retVal = 31 * retVal + notes.hashCode();
    return retVal;
  }
}
