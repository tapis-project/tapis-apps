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
 * Tapis Application
 * Each app is associated with a specific tenant.
 * Name of the app must be URI safe, see RFC 3986.
 *   Allowed characters: Alphanumeric  [0-9a-zA-Z] and special characters [-._~].
 * Each app has an owner and flag indicating if it is currently enabled.
 *
 * Tenant + id + version must be unique
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
  public enum ContainerRuntime {DOCKER, SINGULARITY}
  public enum Permission {ALL, READ, MODIFY, EXECUTE}
  public enum AppOperation {create, read, modify, execute, softDelete, hardDelete, changeOwner, getPerms,
                            grantPerms, revokePerms}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // NOTE: In order to use jersey's SelectableEntityFilteringFeature fields cannot be final.
  private int seqId;           // Unique database sequence number

  private String tenant;     // Name of the tenant for which the app is defined
  private String id;       // Name of the app
  private String version;    // Version of the app
  private String description; // Full description of the app
  private AppType appType; // Type of app, e.g. LINUX, OBJECT_STORE
  private String owner;      // User who owns the app and has full privileges
  private boolean enabled; // Indicates if app is currently enabled
  private boolean isInteractive;
  private boolean containerized;
  private ContainerRuntime containerRuntime;
  private String containerImage;
  private List<AppArg> containerArgs;
  private String command;
  private List<AppArg> commandArgs;
  private List<FileInput> execCodes;
  private String[] envVariables;
  private List<FileInput> fileInputs;
  private boolean dynamicExecSystem; // Indicates if constraints are to be used
  private String[] execSystemConstraints; // List of constraints
  private String execSystemId;
  private String execSystemExecDir;
  private String execSystemInputDir;
  private String execSystemOutputDir;
  private String execSystemLogicalQueue;
  private String archiveSystemId;
  private String archiveSystemDir;
  private boolean archiveOnAppError;
  private String jobDescription;
  private int maxJobs;
  private int maxJobsPerUser;
  private int nodeCount;
  private int coresPerNode;
  private int memoryMB;
  private int maxMinutes;
  private String[] archiveIncludes;
  private String[] archiveExcludes;
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
  public App(String id1, String version1, AppType appType1)
  {
    id = id1;
    version = version1;
    appType = appType1;
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public App(int seqId1, String tenant1, String id1, String version1, String description1, AppType appType1,
             String owner1, boolean enabled1, boolean isInteractive1, boolean containerized1, ContainerRuntime containerRuntime1,
             String containerImage1, String command1, boolean dynamicExecSystem1, String[] execSystemConstraints1,
             String execSystemId1, String execSystemExecDir1, String execSystemInputDir1, String execSystemOutputDir1,
             String execSystemLogicalQueue1, String archiveSystemId1, String archiveSystemDir1,
             boolean archiveOnAppError1, String jobDescription1, int maxJobs1,
             int maxJobsPerUser1, int nodeCount1, int coresPerNode1, int memoryMB1, int maxMinutes1,
             String[] archiveIncludes1, String[] archiveExcludes1,
             String[] tags1, Object notes1, String importRefId1, boolean deleted1,
             Instant created1, Instant updated1)
  {
    seqId = seqId1;
    tenant = tenant1;
    id = id1;
    version = version1;
    description = description1;
    appType = appType1;
    owner = owner1;
    enabled = enabled1;
    isInteractive = isInteractive1;
    containerized = containerized1;
    containerRuntime = containerRuntime1;
    containerImage = containerImage1;
    command = command1;
    dynamicExecSystem = dynamicExecSystem1;
    execSystemConstraints = (execSystemConstraints1 == null) ? null : execSystemConstraints1.clone();
    execSystemId = execSystemId1;
    execSystemExecDir = execSystemExecDir1;
    execSystemInputDir = execSystemInputDir1;
    execSystemOutputDir = execSystemOutputDir1;
    execSystemLogicalQueue = execSystemLogicalQueue1;
    archiveSystemId = archiveSystemId1;
    archiveSystemDir = archiveSystemDir1;
    archiveOnAppError = archiveOnAppError1;
    jobDescription = jobDescription1;
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    nodeCount = nodeCount1;
    coresPerNode = coresPerNode1;
    memoryMB = memoryMB1;
    maxMinutes = maxMinutes1;
    archiveIncludes = (archiveIncludes1 == null) ? null : archiveIncludes1.clone();
    archiveExcludes = (archiveExcludes1 == null) ? null : archiveExcludes1.clone();
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
    seqId = a.getSeqId();
    created = a.getCreated();
    updated = a.getUpdated();
    tenant = a.getTenant();
    id = a.getId();
    description = a.getDescription();
    appType = a.getAppType();
    owner = a.getOwner();
    enabled = a.isEnabled();
    containerized = a.isContainerized();
    containerRuntime = a.getContainerRuntime();
    containerImage = a.getContainerImage();
    command = a.getCommand();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = (a.getExecSystemConstraints() == null) ? null : a.getExecSystemConstraints().clone();
    execSystemId = a.getExecSystemId();
    execSystemExecDir = a.getExecSystemExecDir();
    execSystemInputDir = a.getExecSystemInputDir();
    execSystemOutputDir = a.getExecSystemOutputDir();
    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
    archiveSystemId = a.getArchiveSystemId();
    archiveSystemDir = a.getArchiveSystemDir();
    archiveOnAppError = a.isArchiveOnAppError();
    jobDescription = a.getJobDescription();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMB();
    maxMinutes = a.getMaxMinutes();
    archiveIncludes = (a.getArchiveIncludes() == null) ? null : a.getArchiveIncludes().clone();
    archiveExcludes = (a.getArchiveExcludes() == null) ? null : a.getArchiveExcludes().clone();
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

  public int getSeqId() { return seqId; }

  @Schema(type = "string")
  public Instant getCreated() { return created; }

  @Schema(type = "string")
  public Instant getUpdated() { return updated; }

  public String getTenant() { return tenant; }
  public App setTenant(String s) { tenant = s; return this; }

  public String getId() { return id; }
  public App setId(String s) { id = s; return this; }

  public String getVersion() { return version; }
  public App setVersion(String s) { version = s; return this; }

  public String getDescription() { return description; }
  public App setDescription(String d) { description = d; return this; }

  public AppType getAppType() { return appType; }

  public String getOwner() { return owner; }
  public App setOwner(String s) { owner = s;  return this;}

  public boolean isEnabled() { return enabled; }
  public App setEnabled(boolean b) { enabled = b;  return this; }

  public boolean isInteractive() { return isInteractive; }

  public boolean isContainerized() { return containerized; }

  public ContainerRuntime getContainerRuntime() { return containerRuntime; }
  void setContainerRuntime(ContainerRuntime r) { containerRuntime = r; }

  public String getContainerImage() { return containerImage; }
  public App setContainerImage(String s) { containerImage = s; return this; }

  public String getCommand() { return command; }
  public App setCommand(String s) { command = s; return this; }

  public List<FileInput> getExecCodes() {
    return (execCodes == null) ? null :  new ArrayList<>(execCodes);
  }
  public App setExecCodes(List<FileInput> fi) {
    execCodes = (fi == null) ? null : new ArrayList<>(fi);
    return this;
  }

  public boolean isDynamicExecSystem() { return dynamicExecSystem; }
  public App setDynamicExecSystem(boolean b) {dynamicExecSystem = b; return this; }

  public String[] getExecSystemConstraints() { return (execSystemConstraints == null) ? null : execSystemConstraints.clone(); }
  public App setExecSystemConstraints(String[] sa)
  {
    execSystemConstraints = (sa == null) ? null : sa.clone();
    return this;
  }

  public String getExecSystemId() { return execSystemId; }
  public App setExecSystemId(String s) { execSystemId = s; return this; }

  public String getExecSystemExecDir() { return execSystemExecDir; }
  public App setExecSystemExecDir(String s) { execSystemExecDir = s; return this; }

  public String getExecSystemInputDir() { return execSystemInputDir; }
  public App setExecSystemInputDir(String s) { execSystemInputDir = s; return this; }

  public String getExecSystemOutputDir() { return execSystemOutputDir; }
  public App setExecSystemOutputDir(String s) { execSystemOutputDir = s; return this; }

  public String getExecSystemLogicalQueue() { return execSystemLogicalQueue; }
  public App setExecSystemLogicalQueue(String s) { execSystemLogicalQueue = s; return this; }

  public String getArchiveSystemId() { return archiveSystemId; }
  public App setArchiveSystemId(String s) { archiveSystemId = s; return this; }

  public String getArchiveSystemDir() { return archiveSystemDir; }
  public App setArchiveSystemDir(String s) { archiveSystemDir = s; return this; }

  public boolean isArchiveOnAppError() { return archiveOnAppError; }
  public App setArchiveOnAppError(boolean b) { archiveOnAppError = b; return this; }

  public String[] getEnvVariables() { return (envVariables == null) ? null : envVariables.clone(); }
  public App setEnvVariables(String[] sa)
  {
    envVariables = (sa == null) ? null : sa.clone();
    return this;
  }

  public String getJobDescription() { return jobDescription; }
  public App setJobDescription(String s) { jobDescription = s; return this; }

  public int getMaxJobs()
  {
    return maxJobs;
  }
  public App setMaxJobs(int i) { maxJobs = i; return this; }

  public int getMaxJobsPerUser()
  {
    return maxJobsPerUser;
  }
  public App setMaxJobsPerUser(int i) { maxJobsPerUser = i; return this; }

  public int getNodeCount()
  {
    return nodeCount;
  }
  public App setNodeCount(int i) { nodeCount = i; return this; }

  public int getCoresPerNode()
  {
    return coresPerNode;
  }
  public App setCoresPerNode(int i) { coresPerNode = i; return this; }

  public int getMemoryMB()
  {
    return memoryMB;
  }
  public App setMemoryMB(int i) { memoryMB = i; return this; }

  public int getMaxMinutes() { return maxMinutes; }
  public App setMaxMinutes(int i) { maxMinutes = i; return this; }

  public List<FileInput> getFileInputs() {
    return (fileInputs == null) ? null : new ArrayList<>(fileInputs);
  }
  public App setFileInputs(List<FileInput> fi) {
    fileInputs = (fi == null) ? null : new ArrayList<>(fi);
    return this;
  }

  public String[] getArchiveIncludes() { return (archiveIncludes == null) ? null : archiveIncludes.clone(); }
  public App setArchiveIncludes(String[] sa)
  {
    archiveIncludes = (sa == null) ? null : sa.clone();
    return this;
  }

  public String[] getArchiveExcludes() { return (archiveExcludes == null) ? null : archiveExcludes.clone(); }
  public App setArchiveExcludes(String[] sa)
  {
    archiveExcludes = (sa == null) ? null : sa.clone();
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
}
