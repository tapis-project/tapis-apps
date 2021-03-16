package edu.utexas.tacc.tapis.apps.api.model;

import org.jooq.tools.StringUtils;

/*
 * Class for KeyValuePair in an App definition contained in a request.
 * Key should not contain the character "="
 */
public final class KeyValuePair
{
  private String key;
  private String value;
  private String keyValueStr;

  public KeyValuePair(String key1, String value1)
  {
    key = key1;
    value = value1;
    keyValueStr = key1 + "=" + value1;
  }

  public String getKey() { return key; }
  public void setKey(String s) { key = s; }
  public String getValue() { return value; }
  public void setValue(String s) { value = s; }

  @Override
  public String toString()
  {
    if (keyValueStr == null) keyValueStr = key + "=" + value;
    return keyValueStr;
  }

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
}
