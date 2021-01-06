package edu.utexas.tacc.tapis.apps.model;

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
  private String id;       // Name of the app
  private String version;    // Version of the app
  private final String description; // Full description of the app
  private final Boolean enabled; // Indicates if app is currently enabled
  private final String[] tags;       // List of arbitrary tags as strings
  private Object notes;      // Simple metadata as json

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all final attributes.
   */
  public PatchApp(String description1, Boolean enabled1,
                  String[] tags1, Object notes1)
  {
    description = description1;
    enabled = enabled1;
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getTenant() { return tenant; }
  public void setTenant(String s) { tenant = s; }

  public String getId() { return id; }
  public void setId(String s) { id = s; }

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
