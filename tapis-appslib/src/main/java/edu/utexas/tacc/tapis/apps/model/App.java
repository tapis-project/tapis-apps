package edu.utexas.tacc.tapis.apps.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;


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

  // Set of reserved application names
  public static final Set<String> RESERVED_ID_SET = new HashSet<>(Set.of("HEALTHCHECK", "READYCHECK", "SEARCH"));

  // Constants indicating app version, uuid or seq_id is not relevant.
  public static final int INVALID_SEQ_ID = -1;
  public static final UUID INVALID_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  public static final String NO_APP_VERSION = null;

  public static final String PERMISSION_WILDCARD = "*";
  // Allowed substitution variables
  public static final String APIUSERID_VAR = "${apiUserId}";

  // Default values
  public static final String[] EMPTY_STR_ARRAY = new String[0];
  public static final String DEFAULT_OWNER = APIUSERID_VAR;
  public static final boolean DEFAULT_ENABLED = true;
  public static final boolean DEFAULT_CONTAINERIZED = true;
  public static final boolean DEFAULT_STRICT_FILE_INPUTS = false;
  public static final Runtime DEFAULT_RUNTIME = Runtime.DOCKER;
  public static final JsonObject DEFAULT_NOTES = TapisGsonUtils.getGson().fromJson("{}", JsonObject.class);
  public static final int DEFAULT_NODE_COUNT = 1;
  public static final int DEFAULT_CORES_PER_NODE = 1;
  public static final int DEFAULT_MEMORY_MB = 100;
  public static final int DEFAULT_MAX_MINUTES = 10;

  // Attribute names, also used as field names in Json
  public static final String ID_FIELD = "id";
  public static final String VERSION_FIELD = "version";
  public static final String APP_TYPE_FIELD = "appType";
  public static final String DESCRIPTION_FIELD = "description";
  public static final String OWNER_FIELD = "owner";
  public static final String RUNTIMEVER_FIELD = "runtimeVersion";
  public static final String CONTAINERIMG_FIELD = "containerImage";
  public static final String JOB_FIELD_PREFIX = "Job-";
  public static final String EXECSYSID_FIELD = "execSystemId";
  public static final String EXECSYSEXECDIR_FIELD = "execSystemExecDir";
  public static final String EXECSYSINDIR_FIELD = "execSystemInputDir";
  public static final String EXECSYSOUTDIR_FIELD = "execSystemOutputDir";
  public static final String ARCHIVESYSID_FIELD = "archiveSystemId";
  public static final String ARCHIVESYSDIR_FIELD = "archiveSystemDir";
  public static final String NOTES_FIELD = "notes";

  // Message keys
  private static final String CREATE_MISSING_ATTR = "APPLIB_CREATE_MISSING_ATTR";
  private static final String INVALID_STR_ATTR = "APPLIB_INVALID_STR_ATTR";
  private static final String TOO_LONG_ATTR = "APPLIB_TOO_LONG_ATTR";

  // Validation patterns
  //ID Must start alphabetic and contain only alphanumeric and 4 special characters: - . _ ~
  private static final String PATTERN_VALID_ID = "^[a-zA-Z]([a-zA-Z0-9]|[-\\._~])*";

  private static final String SING_OPT_LIST = String.format("%s,%s", RuntimeOption.SINGULARITY_RUN,
                                                                     RuntimeOption.SINGULARITY_START);

  // Validation constants
  private static final Integer MAX_ID_LEN = 80;
  private static final Integer MAX_VERSION_LEN = 64;
  private static final Integer MAX_DESCRIPTION_LEN = 2048;
  private static final Integer MAX_PATH_LEN = 4096;
  private static final Integer MAX_USERNAME_LEN = 60;
  private static final Integer MAX_RUNTIME_VER_LEN = 128;
  private static final Integer MAX_CONTAINER_IMAGE_LEN = 128;
  private static final Integer MAX_SCHEDULERNAME_LEN = 64;
  private static final Integer MAX_QUEUENAME_LEN = 128;
  private static final Integer MAX_HPCQUEUENAME_LEN = 128;
  private static final Integer MAX_TAG_LEN = 128;

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  public enum AppType {BATCH, FORK}
  public enum AppOperation {create, read, modify, execute, softDelete, hardDelete, changeOwner,
                            enable, disable, getPerms, grantPerms, revokePerms}
  public enum Permission {READ, MODIFY, EXECUTE}
  public enum Runtime {DOCKER, SINGULARITY}
  public enum RuntimeOption {SINGULARITY_START, SINGULARITY_RUN}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // NOTE: In order to use jersey's SelectableEntityFilteringFeature fields cannot be final.
  // === Start fields in main table =============================================
  private int seqId;           // Unique database sequence number
  private int verSeqId;
  private String tenant;     // Name of the tenant for which the app is defined
  private String id;       // Name of the app
  private String version;    // Version of the app
  private String description; // Full description of the app
  private AppType appType; // Type of app, e.g. BATCH, DIRECT
  private String owner;      // User who owns the app and has full privileges
  private boolean enabled; // Indicates if app is currently enabled
  private boolean containerized;
  private Runtime runtime;
  private String runtimeVersion;
  private List<RuntimeOption> runtimeOptions;
  private String containerImage;
  private int maxJobs;
  private int maxJobsPerUser;
  private boolean strictFileInputs;
  // === Start jobAttributes ==========
  private String jobDescription;
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
  private String[] envVariables;
  private String[] archiveIncludes;
  private String[] archiveExcludes;
  private boolean archiveIncludeLaunchFiles;
  private int nodeCount = DEFAULT_NODE_COUNT;
  private int coresPerNode = DEFAULT_CORES_PER_NODE;
  private int memoryMb = DEFAULT_MEMORY_MB;
  private int maxMinutes = DEFAULT_MAX_MINUTES;
  private String[] jobTags;
  // === End jobAttributes ==========
  // === End fields in main table =============================================

  // Aux tables
  private List<FileInput> fileInputs;
  private List<NotifSubscription> notificationSubscriptions;
  private List<AppArg> appArgs;  // parameterSet
  private List<AppArg> containerArgs; // parameterSet
  private List<AppArg> schedulerOptions; // parameterSet

  private String[] tags;       // List of arbitrary tags as strings
  private Object notes;      // Simple metadata as json
  private UUID uuid;
  private boolean deleted;
  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

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
   * Constructor using top level attributes
   */
  public App(int seqId1, String tenant1, String id1, String version1, AppType appType1,
             String owner1, boolean enabled1, boolean containerized1, boolean deleted1)
  {
    seqId = seqId1;
    tenant = tenant1;
    id = id1;
    version = version1;
    appType = appType1;
    owner = owner1;
    enabled = enabled1;
    containerized = containerized1;
    deleted = deleted1;
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   * Note that FileInputs, AppArgs, ContainerArgs, SchedulerOptions and Subscriptions must be set separately.
   */
  public App(int seqId1, int verSeqId1, String tenant1, String id1, String version1, String description1,
             AppType appType1, String owner1, boolean enabled1, boolean containerized1,
             Runtime runtime1, String runtimeVersion1, List<RuntimeOption> runtimeOptions1, String containerImage1,
             int maxJobs1, int maxJobsPerUser1, boolean strictFileInputs1,
             // == Start jobAttributes
             String jobDescription1, boolean dynamicExecSystem1,
             String[] execSystemConstraints1, String execSystemId1, String execSystemExecDir1,
             String execSystemInputDir1, String execSystemOutputDir1, String execSystemLogicalQueue1,
             String archiveSystemId1, String archiveSystemDir1, boolean archiveOnAppError1, String[] envVariables1,
             String[] archiveIncludes1, String[] archiveExcludes1, boolean archiveIncludeLaunchFiles1,
             int nodeCount1, int coresPerNode1, int memoryMb1, int maxMinutes1,
             String[] jobTags1,
             // == End jobAttributes
             String[] tags1, Object notes1, UUID uuid1, boolean deleted1, Instant created1, Instant updated1)
  {
    seqId = seqId1;
    verSeqId = verSeqId1;
    tenant = tenant1;
    id = id1;
    version = version1;
    description = description1;
    appType = appType1;
    owner = owner1;
    enabled = enabled1;
    containerized = containerized1;
    runtime = runtime1;
    runtimeVersion = runtimeVersion1;
    runtimeOptions = (runtimeOptions1 == null) ? null : new ArrayList<>(runtimeOptions1);
    containerImage = containerImage1;
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    strictFileInputs = strictFileInputs1;
    jobDescription = jobDescription1;
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
    nodeCount = nodeCount1;
    coresPerNode = coresPerNode1;
    memoryMb = memoryMb1;
    maxMinutes = maxMinutes1;
    envVariables = (envVariables1 == null) ? null : envVariables1.clone();
    archiveIncludes = (archiveIncludes1 == null) ? null : archiveIncludes1.clone();
    archiveExcludes = (archiveExcludes1 == null) ? null : archiveExcludes1.clone();
    archiveIncludeLaunchFiles = archiveIncludeLaunchFiles1;
    jobTags = (jobTags1 == null) ? null : jobTags1.clone();
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
    uuid = uuid1;
    deleted = deleted1;
    created = created1;
    updated = updated1;
  }

  /**
   * Copy constructor. Returns a deep copy of an App object.
   * The getters make defensive copies as needed.
   */
  public App(App a)
  {
    if (a==null) throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    seqId = a.getSeqId();
    verSeqId = a.getVerSeqId();
    tenant = a.getTenant();
    id = a.getId();
    version = a.getVersion();
    description = a.getDescription();
    appType = a.getAppType();
    owner = a.getOwner();
    enabled = a.isEnabled();
    containerized = a.isContainerized();
    runtime = a.getRuntime();
    runtimeVersion = a.getRuntimeVersion();
    runtimeOptions = a.getRuntimeOptions();
    containerImage = a.getContainerImage();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    strictFileInputs = a.isStrictFileInputs();
    jobDescription = a.getJobDescription();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = a.getExecSystemConstraints();
    execSystemId = a.getExecSystemId();
    execSystemExecDir = a.getExecSystemExecDir();
    execSystemInputDir = a.getExecSystemInputDir();
    execSystemOutputDir = a.getExecSystemOutputDir();
    execSystemLogicalQueue = a.getExecSystemLogicalQueue();
    archiveSystemId = a.getArchiveSystemId();
    archiveSystemDir = a.getArchiveSystemDir();
    archiveOnAppError = a.isArchiveOnAppError();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMb = a.getMemoryMb();
    maxMinutes = a.getMaxMinutes();
    envVariables = a.getEnvVariables();
    archiveIncludes = a.getArchiveIncludes();
    archiveExcludes = a.getArchiveExcludes();
    archiveIncludeLaunchFiles = a.getArchiveIncludeLaunchFiles();
    jobTags = a.getJobTags();
    fileInputs = a.getFileInputs();
    notificationSubscriptions = a.getNotificationSubscriptions();
    appArgs = a.getAppArgs();
    containerArgs = a.getContainerArgs();
    schedulerOptions = a.getSchedulerOptions();
    tags = a.getTags();
    notes = a.getNotes();
    uuid = a.getUuid();
    deleted = a.isDeleted();
    created = a.getCreated();
    updated = a.getUpdated();
  }

  /**
   * Resolve variables for App attributes
   */
  public void resolveVariables(String oboUser)
  {
    // Resolve owner if necessary. If empty or "${apiUserId}" then fill in oboUser.
    // Note that for a user request oboUser and apiUserId are the same and for a service request we want oboUser here.
    if (StringUtils.isBlank(owner) || owner.equalsIgnoreCase(APIUSERID_VAR)) setOwner(oboUser);
  }

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************
  public void setDefaults()
  {
    if (StringUtils.isBlank(owner)) setOwner(DEFAULT_OWNER);
    if (jobTags == null) setJobTags(EMPTY_STR_ARRAY);
    if (tags == null) setTags(EMPTY_STR_ARRAY);
    if (notes == null) setNotes(DEFAULT_NOTES);
  }

  /**
   * Check constraints on App attributes.
   * Make checks that do not require a dao or service call.
   * Check only internal consistency and restrictions.
   *
   * @return  list of error messages, empty list if no errors
   */
  public List<String> checkAttributeRestrictions()
  {
    var errMessages = new ArrayList<String>();
    checkAttrRequired(errMessages);
    checkAttrValidity(errMessages);
    checkAttrStringLengths(errMessages);
    checkAttrMisc(errMessages);
    return errMessages;
  }

  // ************************************************************************
  // *********************** Private methods *********************************
  // ************************************************************************

  /**
   * Check for missing required attributes
   *   Id, version, appType
   */
  private void checkAttrRequired(List<String> errMessages)
  {
    if (StringUtils.isBlank(id)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, ID_FIELD));
    if (StringUtils.isBlank(version)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, VERSION_FIELD));
    if (appType == null) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, APP_TYPE_FIELD));
  }

  /**
   * Check for invalid attributes
   *   id
   */
  private void checkAttrValidity(List<String> errMessages)
  {
    if (!StringUtils.isBlank(id) && !isValidId(id)) errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, ID_FIELD, id));
  }

  /**
   * Check for attribute strings that exceed limits
   *   id, version, description, owner, runtimeVersion, containerImage, jobDescription,
   *   execSystemId, execSystemExecDir, execSystemInputDir, execSystemOutputDir,
   *   archiveSystemId, archiveSystemDir
   */
  private void checkAttrStringLengths(List<String> errMessages)
  {
    if (!StringUtils.isBlank(id) && id.length() > MAX_ID_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, ID_FIELD, MAX_ID_LEN));
    }

    if (!StringUtils.isBlank(version) && version.length() > MAX_VERSION_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, VERSION_FIELD, MAX_VERSION_LEN));
    }

    if (!StringUtils.isBlank(description) && description.length() > MAX_DESCRIPTION_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, DESCRIPTION_FIELD, MAX_DESCRIPTION_LEN));
    }

    if (!StringUtils.isBlank(owner) && owner.length() > MAX_USERNAME_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, OWNER_FIELD, MAX_USERNAME_LEN));
    }

    if (!StringUtils.isBlank(runtimeVersion) && runtimeVersion.length() > MAX_RUNTIME_VER_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, RUNTIMEVER_FIELD, MAX_RUNTIME_VER_LEN));
    }

    if (!StringUtils.isBlank(containerImage) && containerImage.length() > MAX_CONTAINER_IMAGE_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, CONTAINERIMG_FIELD, MAX_CONTAINER_IMAGE_LEN));
    }

    if (!StringUtils.isBlank(jobDescription) && jobDescription.length() > MAX_DESCRIPTION_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + DESCRIPTION_FIELD, MAX_DESCRIPTION_LEN));
    }

    if (!StringUtils.isBlank(execSystemId) && execSystemId.length() > MAX_ID_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + EXECSYSID_FIELD, MAX_ID_LEN));
    }

    if (!StringUtils.isBlank(execSystemExecDir) && execSystemExecDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + EXECSYSEXECDIR_FIELD, MAX_PATH_LEN));
    }

    if (!StringUtils.isBlank(execSystemInputDir) && execSystemInputDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + EXECSYSINDIR_FIELD, MAX_PATH_LEN));
    }

    if (!StringUtils.isBlank(execSystemOutputDir) && execSystemOutputDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + EXECSYSOUTDIR_FIELD, MAX_PATH_LEN));
    }

    if (!StringUtils.isBlank(archiveSystemId) && archiveSystemId.length() > MAX_ID_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + ARCHIVESYSID_FIELD, MAX_ID_LEN));
    }

    if (!StringUtils.isBlank(archiveSystemDir) && archiveSystemDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + ARCHIVESYSDIR_FIELD, MAX_PATH_LEN));
    }
  }

  /**
   * Check misc attribute restrictions
   *  If containerized is true then containerImage must be set
   *  If containerized and SINGULARITY then RuntimeOptions must include one of SINGULARITY_START or SINGULARITY_RUN
   *  If dynamicExecSystem then execSystemConstraints must be given
   *  If not dynamicExecSystem then execSystemId must be given
   *  If archiveSystem given then archive dir must be given
   */
  private void checkAttrMisc(List<String> errMessages)
  {
    // If containerized is true then containerImage must be set
    if (containerized && StringUtils.isBlank(containerImage))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_NOIMAGE"));
    }

    // If containerized and SINGULARITY then RuntimeOptions must be include one and only one of
    //   SINGULARITY_START and SINGULARITY_RUN
    if (containerized && Runtime.SINGULARITY.equals(runtime))
    {
      // If options list contains both or neither of START and RUN then reject.
      if ((runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) && runtimeOptions.contains(RuntimeOption.SINGULARITY_START))
           ||
           !(runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) || runtimeOptions.contains(RuntimeOption.SINGULARITY_START)))
      {
        errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_SING_OPT", SING_OPT_LIST));
      }
    }

    // If dynamicExecSystem then execSystemConstraints must be given
    if (dynamicExecSystem)
    {
      if (execSystemConstraints == null || execSystemConstraints.length == 0)
      {
        errMessages.add(LibUtils.getMsg("APPLIB_DYNAMIC_NOCONSTRAINTS"));
      }
    }

    // If not dynamicExecSystem then execSystemId must be given
    if (!dynamicExecSystem && StringUtils.isBlank(execSystemId))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_NOTDYNAMIC_NOSYSTEMID"));
    }

    // If archiveSystem given then archive dir must be given
    if (!StringUtils.isBlank(archiveSystemId) && StringUtils.isBlank(archiveSystemDir))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_ARCHIVE_NODIR"));
    }
  }

  /**
   * Validate an ID string.
   * Must start alphabetic and contain only alphanumeric and 4 special characters: - . _ ~
   */
  private boolean isValidId(String id) { return id.matches(PATTERN_VALID_ID); }

  /**
   * Validate a host string.
   * Check if a string is a valid hostname or IP address.
   * Use methods from org.apache.commons.validator.routines.
   */
  private boolean isValidHost(String host)
  {
    // First check for valid IP address, then for valid domain name
    if (DomainValidator.getInstance().isValid(host) || InetAddressValidator.getInstance().isValid(host)) return true;
    else return false;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public int getSeqId() { return seqId; }

  public int getVerSeqId() { return verSeqId; }

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

  public boolean isContainerized() { return containerized; }
  public App setContainerized(boolean b) { containerized = b;  return this; }

  public Runtime getRuntime() { return runtime; }
  public void setRuntime(Runtime r) { runtime = r; }

  public String getRuntimeVersion() { return runtimeVersion; }
  public App setRuntimeVersion(String s) { runtimeVersion = s; return this; }

  public List<RuntimeOption> getRuntimeOptions()
  {
    return (runtimeOptions == null) ? null : new ArrayList<>(runtimeOptions);
  }
  public App setRuntimeOptions(List<RuntimeOption> rol)
  {
    runtimeOptions = (rol == null) ? null : new ArrayList<>(rol);
    return this;
  }


  public String getContainerImage() { return containerImage; }
  public App setContainerImage(String s) { containerImage = s; return this; }

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

  public int getMemoryMb()
  {
    return memoryMb;
  }
  public App setMemoryMb(int i) { memoryMb = i; return this; }

  public int getMaxMinutes() { return maxMinutes; }
  public App setMaxMinutes(int i) { maxMinutes = i; return this; }

  public boolean isStrictFileInputs() { return strictFileInputs; }
  public App setStrictFileInputs(boolean b) { strictFileInputs = b;  return this; }

  public List<FileInput> getFileInputs() {
    return (fileInputs == null) ? null : new ArrayList<>(fileInputs);
  }
  public App setFileInputs(List<FileInput> fi) {
    fileInputs = (fi == null) ? null : new ArrayList<>(fi);
    return this;
  }

  public List<NotifSubscription> getNotificationSubscriptions()
  {
    return (notificationSubscriptions == null) ? null : new ArrayList<>(notificationSubscriptions);
  }
  public App setNotificationSubscriptions(List<NotifSubscription> ns) {
    notificationSubscriptions = (ns == null) ? null : new ArrayList<>(ns);
    return this;
  }

  public List<AppArg> getAppArgs() {
    return (appArgs == null) ? null : new ArrayList<>(appArgs);
  }
  public App setAppArgs(List<AppArg> al) {
    appArgs = (al == null) ? null : new ArrayList<>(al);
    return this;
  }

  public List<AppArg> getContainerArgs() {
    return (containerArgs == null) ? null : new ArrayList<>(containerArgs);
  }
  public App setContainerArgs(List<AppArg> al) {
    containerArgs = (al == null) ? null : new ArrayList<>(al);
    return this;
  }

  public List<AppArg> getSchedulerOptions()
  {
    return (schedulerOptions == null) ? null : new ArrayList<>(schedulerOptions);
  }
  public App setSchedulerOptions(List<AppArg> al) {
    schedulerOptions = (al == null) ? null : new ArrayList<>(al);
    return this;
  }

  public String[] getEnvVariables() { return (envVariables == null) ? null : envVariables.clone(); }
  public App setEnvVariables(String[] sa)
  {
    envVariables = (sa == null) ? null : sa.clone();
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

  public boolean getArchiveIncludeLaunchFiles() { return archiveIncludeLaunchFiles; }
  public App setArchiveIncludeLaunchFiles(boolean b) { archiveIncludeLaunchFiles = b; return this; }

  public String[] getJobTags() {
    return (jobTags == null) ? null : jobTags.clone();
  }
  public App setJobTags(String[] t) {
    tags = (t == null) ? null : t.clone();
    return this;
  }

  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }
  public App setTags(String[] t) {
    tags = (t == null) ? null : t.clone();
    return this;
  }

  public Object getNotes() { return notes; }
  public App setNotes(Object n) { notes = n; return this; }

  public UUID getUuid() { return uuid; }
  public App setUuid(UUID u) { uuid = u; return this; }

  public boolean isDeleted() { return deleted; }
}
