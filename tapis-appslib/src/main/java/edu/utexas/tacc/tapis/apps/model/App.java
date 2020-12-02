package edu.utexas.tacc.tapis.apps.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.utils.JsonObjectSerializer;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

/*
  TODO
 * Tapis App representing TBD
 * Each app is associated with a specific tenant.
 * Name of the app must be URI safe, see RFC 3986.
 *   Allowed characters: Alphanumeric  [0-9a-zA-Z] and special characters [-._~].
 * Each app has an owner and flag indicating if it is currently enabled.
 *
 * Tenant + name must be unique
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class App
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  public static final String ROLE_READ_PREFIX = "Apps_R_";
  // Allowed substitution variables
  public static final String APIUSERID_VAR = "${apiUserId}";
  public static final String OWNER_VAR = "${owner}";
  public static final String TENANT_VAR = "${tenant}";
  public static final String PERMS_WILDCARD = "*";

  // Default values
  public static final String DEFAULT_OWNER = APIUSERID_VAR;
  public static final boolean DEFAULT_ENABLED = true;
  public static final JsonObject DEFAULT_NOTES = TapisGsonUtils.getGson().fromJson("{}", JsonObject.class);
  public static final String DEFAULT_TAGS_STR = "{}";
  public static final String[] DEFAULT_TAGS = new String[0];

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  public enum AppType {BATCH, INTERACTIVE}
  public enum Permission {ALL, READ, MODIFY, EXECUTE}
  public enum AppOperation {create, read, modify, execute, softDelete, hardDelete, changeOwner, getPerms,
                               grantPerms, revokePerms}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // NOTE: In order to use jersey's SelectableEntityFilteringFeature fields cannot be final.
  private int id;           // Unique database sequence number

  private String tenant;     // Name of the tenant for which the app is defined
  private String name;       // Name of the app
  private String version;    // Version of the app
  private String description; // Full description of the app
  private AppType appType; // Type of app, e.g. LINUX, OBJECT_STORE
  private String owner;      // User who owns the app and has full privileges
  private boolean enabled; // Indicates if app is currently enabled
  private List<Capability> jobCapabilities; // List of job related capabilities required by the app
  private String[] tags;       // List of arbitrary tags as strings

  @JsonSerialize(using = JsonObjectSerializer.class)
  private Object notes;      // Simple metadata as json

  private String importRefId; // Optional reference ID for object created via import
  private boolean deleted;

  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
   * NOTE: Adding a default constructor changes jOOQ behavior such that when Record.into() uses the default mapper
   *       the column names and POJO attribute names must match (with convention an_attr -> anAttr).
   */
  public App() { }

  /**
   * Constructor using only required attributes.
   */
  public App(String name1, String version1, AppType appType1)
  {
    name = name1;
    version = version1;
    appType = appType1;
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public App(int id1, String tenant1, String name1, String version1, String description1, AppType appType1,
             String owner1, boolean enabled1, String[] tags1, Object notes1, String importRefId1, boolean deleted1,
             Instant created1, Instant updated1)
  {
    id = id1;
    tenant = tenant1;
    name = name1;
    version = version1;
    description = description1;
    appType = appType1;
    owner = owner1;
    enabled = enabled1;
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
    importRefId = importRefId1;
    deleted = deleted1;
    created = created1;
    updated = updated1;
  }

  /**
   * Copy constructor. Returns a deep copy of an App object.
   * Make defensive copies as needed. Note Credential is immutable so no need for copy.
   */
  public App(App a)
  {
    if (a==null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    id = a.getId();
    created = a.getCreated();
    updated = a.getUpdated();
    tenant = a.getTenant();
    name = a.getName();
    description = a.getDescription();
    appType = a.getAppType();
    owner = a.getOwner();
    enabled = a.isEnabled();
    jobCapabilities = (a.getJobCapabilities() == null) ? null :  new ArrayList<>(a.getJobCapabilities());
    tags = (a.getTags() == null) ? null : a.getTags().clone();
    notes = a.getNotes();
    importRefId = a.getImportRefId();
    deleted = a.isDeleted();
  }

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************
  public static App checkAndSetDefaults(App app)
  {
    if (app==null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    if (StringUtils.isBlank(app.getOwner())) app.setOwner(DEFAULT_OWNER);
    if (app.getTags() == null) app.setTags(DEFAULT_TAGS);
    if (app.getNotes() == null) app.setNotes(DEFAULT_NOTES);
    return app;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  // NOTE: Setters that are not public are in place in order to use jersey's SelectableEntityFilteringFeature.

  public int getId() { return id; }
  void setId(int i) { id = i; };

  @Schema(type = "string")
  public Instant getCreated() { return created; }
  void setCreated(Instant i) { created = i; }

  @Schema(type = "string")
  public Instant getUpdated() { return updated; }
  void setUpdated(Instant i) { updated = i; }

  public String getTenant() { return tenant; }
  public App setTenant(String s) { tenant = s; return this; }

  public String getName() { return name; }
  public App setName(String s) { name = s; return this; }

  public String getVersion() { return version; }
  public App setVersion(String s) { version = s; return this; }

  public String getDescription() { return description; }
  public App setDescription(String d) { description = d; return this; }

  public AppType getAppType() { return appType; }
  void setAppType(AppType s) { appType = s; }

  public String getOwner() { return owner; }
  public App setOwner(String s) { owner = s;  return this;}

  public boolean isEnabled() { return enabled; }
  public App setEnabled(boolean b) { enabled = b;  return this; }

  public List<Capability> getJobCapabilities() {
    return (jobCapabilities == null) ? null : new ArrayList<>(jobCapabilities);
  }
  public App setJobCapabilities(List<Capability> c) {
    jobCapabilities = (c == null) ? null : new ArrayList<>(c);
    return this;
  }

  public String getImportRefId() { return importRefId; }
  public App setImportRefID(String s) { importRefId = s;  return this;}

  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }
  public App setTags(String[] t) {
    tags = (t == null) ? null : t.clone();
    return this;
  }

  public Object getNotes() { return notes; }
  public App setNotes(Object n) { notes = n; return this; }

  public boolean isDeleted() { return deleted; }
  void setDeleted(boolean b) { deleted = b; }
}
