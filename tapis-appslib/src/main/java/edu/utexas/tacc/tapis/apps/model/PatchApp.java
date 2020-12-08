package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Class representing an update to a Tapis App.
 * Fields set to null indicate attribute not updated.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class PatchApp
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private String tenant;     // Name of the tenant for which the app is defined
  private String name;       // Name of the app
  private String version;    // Version of the app
  private final String description; // Full description of the app
  private final Boolean enabled; // Indicates if app is currently enabled
//  private final List<Capability> jobCapabilities; // List of job related capabilities required by the app
  private final String[] tags;       // List of arbitrary tags as strings
  private Object notes;      // Simple metadata as json

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all final attributes.
   */
  public PatchApp(String version1, String description1, Boolean enabled1,
//                  List<Capability> jobCapabilities1,
                  String[] tags1, Object notes1)
  {
    version = version1;
    description = description1;
    enabled = enabled1;
//    jobCapabilities = (jobCapabilities1 == null) ? null : new ArrayList<>(jobCapabilities1);
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getTenant() { return tenant; }
  public void setTenant(String s) { tenant = s; }

  public String getName() { return name; }
  public void setName(String s) { name = s; }

  public String getVersion() { return version; }
  public void setVersion(String s) { version = s; }

  public String getDescription() { return description; }

  public Boolean isEnabled() { return enabled; }

//  public List<Capability> getJobCapabilities() {
//    return (jobCapabilities == null) ? null : new ArrayList<>(jobCapabilities);
//  }

  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }

  public Object getNotes() {
    return notes;
  }
  public void setNotes(Object n) { notes = n; }
}
