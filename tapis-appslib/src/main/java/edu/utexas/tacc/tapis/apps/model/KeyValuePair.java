package edu.utexas.tacc.tapis.apps.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/*
 * Class for KeyValuePair in an App definition.
 * Key should not contain the character "=" because it would change the value
 * TODO/TBD: Override equals/hashCode so List.equals can work. For change history checking
 *           Make comparable?
 *           Use google's autoValue annotation for equals/hashCode?
 *           Better yet use java's new record keyword?
 *           Unfortunately both autoValue and record keyword break gson.
 */
public final class KeyValuePair
{
  private final String key;
  private final String value;

  public KeyValuePair(String key1, String value1)
  {
    key = key1;
    value = StringUtils.isBlank(value1) ? "" : value1;
  }

  public String getKey() { return key; }
  public String getValue() { return value; }

  public static KeyValuePair fromString(String s)
  {
    if (StringUtils.isBlank(s)) return new KeyValuePair("","");
    int e1 = s.indexOf('=');
    String k = s.substring(0, e1);
    String v = "";
    // Everything after "=" is the value
    if (e1 > 0) v = s.substring(e1+1);
    return new KeyValuePair(k, v);
  }

  @Override
  public String toString() { return String.format("%s=%s", key, value); }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) return true;
    // Note: no need to check for o==null since instanceof will handle that case
    if (!(o instanceof KeyValuePair)) return false;
    var that = (KeyValuePair) o;
    return (Objects.equals(this.key, that.key) && Objects.equals(this.value, that.value));
  }

  @Override
  public int hashCode()
  {
    int retVal = (key == null ? 1 : key.hashCode());
    // By inspection of this class value is not null
    retVal = 31 * retVal + value.hashCode();
    return retVal;
  }
}