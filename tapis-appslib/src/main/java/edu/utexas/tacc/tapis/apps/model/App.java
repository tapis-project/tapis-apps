package edu.utexas.tacc.tapis.apps.model;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.utils.PathSanitizer;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;

import static edu.utexas.tacc.tapis.apps.model.KeyValuePair.KeyValueInputMode.FIXED;
import static edu.utexas.tacc.tapis.apps.model.KeyValuePair.RESERVED_PREFIX;
import static edu.utexas.tacc.tapis.apps.model.KeyValuePair.VALUE_NOT_SET;

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

  // Set of reserved application IDs and versions
  public static final Set<String> RESERVED_ID_SET
          = new HashSet<>(Set.of("HEALTHCHECK", "READYCHECK", "SEARCH", "SHARE", "UNSHARE", "SHARE_PUBLIC", "UNSHARE_PUBLIC"));
  public static final Set<String> RESERVED_VER_SET
          = new HashSet<>(Set.of("ISENABLED", "ENABLE", "DISABLE", "DELETE", "UNDELETE", "HISTORY", "CHANGEOWNER"));

  // Set of attributes (i.e. column names) not supported in searches
  public static final Set<String> SEARCH_ATTRS_UNSUPPORTED =
          new HashSet<>(Set.of("job_attributes", "runtime_options", "exec_system_constraints", "parameter_set",
                               "file_inputs", "file_input_arrays", "subscriptions", "job_tags", "notes"));

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
  public static final boolean DEFAULT_LOCKED = false;
  public static final boolean DEFAULT_CONTAINERIZED = true;
  public static final boolean DEFAULT_STRICT_FILE_INPUTS = false;
  public static final boolean DEFAULT_IS_PUBLIC = false;
  public static final Runtime DEFAULT_RUNTIME = Runtime.DOCKER;
  public static final JobType DEFAULT_JOB_TYPE = JobType.FORK;
  public static final JsonElement DEFAULT_PARAMETER_SET = TapisGsonUtils.getGson().fromJson("{}", JsonElement.class);
  public static final JsonElement DEFAULT_FILE_INPUTS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final JsonElement DEFAULT_FILE_INPUT_ARRAYS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final JsonElement DEFAULT_SUBSCRIPTIONS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final JsonObject DEFAULT_NOTES = TapisGsonUtils.getGson().fromJson("{}", JsonObject.class);
  public static final int DEFAULT_MAX_JOBS = -1;
  public static final int DEFAULT_MAX_JOBS_PER_USER = -1;
  public static final int DEFAULT_NODE_COUNT = 1;
  public static final int DEFAULT_CORES_PER_NODE = 1;
  public static final int DEFAULT_MEMORY_MB = 100;
  public static final int DEFAULT_MAX_MINUTES = 10;
  public static final String DEFAULT_DTNSYS_INDIR = TapisConstants.TAPIS_NOT_SET;
  public static final String DEFAULT_DTNSYS_OUTDIR = TapisConstants.TAPIS_NOT_SET;

  // Special select strings used for determining what attributes are returned in a response
  public static final String SEL_ALL_ATTRS = "allAttributes";
  public static final String SEL_SUMMARY_ATTRS = "summaryAttributes";

  // Attribute names, also used as field names in Json
  public static final String TENANT_FIELD = "tenant";
  public static final String ID_FIELD = "id";
  public static final String VERSION_FIELD = "version";
  public static final String DESCRIPTION_FIELD = "description";
  public static final String OWNER_FIELD = "owner";
  public static final String ENABLED_FIELD = "enabled";
  public static final String LOCKED_FIELD = "locked";
  public static final String CONTAINERIZED_FIELD = "containerized";
  public static final String RUNTIME_FIELD = "runtime";
  public static final String RUNTIMEVER_FIELD = "runtimeVersion";
  public static final String RUNTIMEOPTS_FIELD = "runtimeOptions";
  public static final String CONTAINERIMG_FIELD = "containerImage";
  public static final String JOB_TYPE_FIELD = "jobType";
  public static final String MAX_JOBS_FIELD = "maxJobs";
  public static final String MAX_JOBS_PER_USER_FIELD = "maxJobsPerUser";
  public static final String STRICT_FILE_INPUTS_FIELD = "strictFileInputs";
  public static final String JOB_ATTRS_FIELD = "jobAttributes";
  public static final String PARAM_SET_FIELD = "parameterSet";
  public static final String APP_ARGS_FIELD = "appArgs";
  public static final String CONTAINER_ARGS_FIELD = "containerArgs";
  public static final String SCHED_OPTS_FIELD = "schedulerOptions";
  public static final String ENV_VARS_FIELD = "envVariables";
  public static final String ARCHIVE_FILTER_FIELD = "archiveFilter";
  public static final String ARCHIVE_INCLUDES_FIELD = "includes";
  public static final String ARCHIVE_EXCLUDES_FIELD = "excludes";
  public static final String LOGCONFIG_FIELD = "logConfig";
  public static final String LOGCONFIG_OUT_FIELD = "stdoutFilename";
  public static final String LOGCONFIG_ERR_FIELD = "stderrFilename";
  public static final String FILE_INPUTS_FIELD = "fileInputs";
  public static final String FILE_INPUTS_SRC_FIELD = "sourceUrl";
  public static final String FILE_INPUTS_DST_FIELD = "targetPath";
  public static final String FILE_INPUT_ARRAYS_FIELD = "fileInputArrays";
  public static final String FILE_INPUT_ARRAYS_TARGET_FIELD = "targetDir";
  public static final String FILE_INPUT_ARRAYS_SRC_FIELD = "sourceUrls";
  public static final String SUBSCRIPTIONS_FIELD = "subscriptions";
  public static final String SUBSCRIPTIONS_DLVRY_ADDR_FIELD = "deliveryAddress";
  public static final String JOB_FIELD_PREFIX = "Job-";
  public static final String IS_DYNAMIC_EXEC_SYS_FIELD = "dynamicExecSystem";
  public static final String EXECSYSID_FIELD = "execSystemId";
  public static final String EXECSYS_CONSTRAINTS_FIELD = "execSystemConstraints";
  public static final String EXECSYSEXECDIR_FIELD = "execSystemExecDir";
  public static final String EXECSYSINDIR_FIELD = "execSystemInputDir";
  public static final String EXECSYSOUTDIR_FIELD = "execSystemOutputDir";
  public static final String DTNSYSINDIR_FIELD = "dtnSystemInputDir";
  public static final String DTNSYSOUTDIR_FIELD = "dtnSystemOutputDir";
  public static final String EXECSYSLOGICALQ_FIELD = "execSystemLogicalQueue";
  public static final String ARCHIVESYSID_FIELD = "archiveSystemId";
  public static final String ARCHIVESYSDIR_FIELD = "archiveSystemDir";
  public static final String ARCHIVE_ON_APP_ERROR_FIELD = "archiveOnAppError";
  public static final String ISMPI_FIELD = "isMpi";
  public static final String MPI_CMD_FIELD = "mpiCmd";
  public static final String CMD_PREFIX_FIELD = "cmdPrefix";
  public static final String NODE_COUNT_FIELD = "nodeCount";
  public static final String CORES_PER_NODE_FIELD = "coresPerNode";
  public static final String MEMORY_MB_FIELD = "memoryMB";
  public static final String MAX_MINUTES_FIELD = "maxMinutes";
  public static final String TAGS_FIELD = "tags";
  public static final String NOTES_FIELD = "notes";
  public static final String UUID_FIELD = "uuid";
  public static final String DELETED_FIELD = "deleted";
  public static final String CREATED_FIELD = "created";
  public static final String UPDATED_FIELD = "updated";
  public static final String SHARED_APP_CTX_FIELD = "sharedAppCtx";
  public static final String IS_PUBLIC_FIELD = "isPublic";
  public static final String SHARED_WITH_USERS_FIELD = "sharedWithUsers";

  // Private field names used only in this class for messages.
  private static final String NAME_FIELD = "name";
  private static final String VALUE_FIELD = "value";

  // Regex patterns for validating a file input sourceUrl.
  // Based on pattern from files service. edu.utexas.tacc.tapis.files.lib.models.TransferURI
  private static final Pattern SRC_URL_PATTERN = Pattern.compile("(http:\\/\\/|https:\\/\\/|tapis:\\/\\/)([\\w -\\.]+)\\/?(.*)");
  private static final Pattern SRC_URL_LOCAL_PATTERN = Pattern.compile("(tapislocal:\\/\\/)([\\w -\\.]+)\\/?(.*)");

  // Special containerArg value. This argument may only be used for an app of runtime type ZIP.
  private static final String ZIP_SAVE_CONTAINER_ARG = "--tapis-zip-save";

  // Message keys
  private static final String CREATE_MISSING_ATTR = "APPLIB_CREATE_MISSING_ATTR";
  private static final String INVALID_STR_ATTR = "APPLIB_INVALID_STR_ATTR";
  private static final String TOO_LONG_ATTR = "APPLIB_TOO_LONG_ATTR";
  private static final String CTL_CHR_ATTR = "APPLIB_CTL_CHR_ATTR";

  // Validation patterns
  //ID Must start alphanumeric and contain only alphanumeric and 4 special characters: - . _ ~
  private static final String PATTERN_VALID_ID = "^[a-zA-Z0-9]([a-zA-Z0-9]|[-\\._~])*";

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

  // Label to use when ArgSpec or FileInput name is missing.
  private static final String UNNAMED = "unnamed";

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  // NOTE: For jobType UNSET indicates it value was not specified in a patch request.
  //       UNSET should never be returned to caller.
  public enum JobType  {BATCH, FORK, UNSET}
  public enum AppOperation {create, read, modify, execute, delete, undelete, hardDelete, changeOwner,
                            enable, disable, getPerms, grantPerms, revokePerms, lock, unlock}
  public enum Permission {READ, MODIFY, EXECUTE}
  public enum Runtime {DOCKER, SINGULARITY, ZIP}
  // NOTE: RuntimeOption starts with NONE due to a bug in client code generation.
  //   Without an initial entry the prefix SINGULARITY_ gets stripped off the other 2 entries.
  //   See also https://github.com/tapis-project/openapi-apps/blob/dev/AppsAPI.yaml
  public enum RuntimeOption {NONE, SINGULARITY_START, SINGULARITY_RUN}
  public enum FileInputMode {OPTIONAL, REQUIRED, FIXED}
  public enum ArgInputMode {REQUIRED, FIXED, INCLUDE_ON_DEMAND, INCLUDE_BY_DEFAULT}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  private String sharedAppCtx; // App accessible due to having been shared with requesting user. Set to grantor.
  private boolean isPublic = DEFAULT_IS_PUBLIC;
  private Set<String> sharedWithUsers;

  // NOTE: In order to use jersey's SelectableEntityFilteringFeature fields cannot be final.
  // === Start fields in table apps =============================================
  private String tenant;     // Name of the tenant for which the app is defined
  private final String id;       // Name of the app
  private String owner;      // User who owns the app and has full privileges
  private boolean enabled; // Indicates if app is currently enabled
  private boolean containerized;
  private boolean deleted;
  // === End fields in table apps =============================================

  // === Start fields in table apps_versions =============================================
  private int seqId;           // Unique database sequence number
  private int verSeqId;
  private final String version;    // Version of the app
  private String description; // Full description of the app
  private boolean locked; // Indicates if app version is currently locked
  private Runtime runtime;
  private String runtimeVersion;
  private List<RuntimeOption> runtimeOptions;
  private String containerImage;
  private JobType jobType; // Type of app, e.g. BATCH, FORK
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
  private String dtnSystemInputDir = DEFAULT_DTNSYS_INDIR;
  private String dtnSystemOutputDir = DEFAULT_DTNSYS_OUTDIR;
  private String execSystemLogicalQueue;
  private String archiveSystemId;
  private String archiveSystemDir;
  private boolean archiveOnAppError;
  private boolean isMpi;
  private String mpiCmd;
  private String cmdPrefix;
  private ParameterSet parameterSet;
  private List<FileInput> fileInputs;
  private List<FileInputArray> fileInputArrays;
  private int nodeCount = DEFAULT_NODE_COUNT;
  private int coresPerNode = DEFAULT_CORES_PER_NODE;
  private int memoryMB = DEFAULT_MEMORY_MB;
  private int maxMinutes = DEFAULT_MAX_MINUTES;
  private List<ReqSubscribe> subscriptions;
  private String[] jobTags;
  // === End jobAttributes ==========
  private String[] tags;       // List of arbitrary tags as strings
  private JsonObject notes;      // Simple metadata as json
  private UUID uuid;
  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated
  // === End fields in table apps_versions =============================================

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor using only required attributes.
   */
  public App(String id1, String version1, String containerImage1)
  {
    id = LibUtils.stripStr(id1);
    version = LibUtils.stripStr(version1);
    containerImage = LibUtils.stripStr(containerImage1);
  }

  /**
   * Constructor using top level attributes
   */
  public App(int seqId1, String tenant1, String id1, String version1,
             String owner1, boolean enabled1, boolean containerized1, boolean deleted1)
  {
    seqId = seqId1;
    tenant = LibUtils.stripStr(tenant1);
    id = LibUtils.stripStr(id1);
    version = LibUtils.stripStr(version1);
    owner = LibUtils.stripStr(owner1);
    enabled = enabled1;
    containerized = containerized1;
    deleted = deleted1;
  }

  /**
   * Constructor using non-updatable attributes.
   * Rather than exposing otherwise unnecessary setters we use a special constructor.
   */
  public App(App a, String tenant1, String id1, String version1)
  {
    if (a==null || StringUtils.isBlank(tenant1) || StringUtils.isBlank(id1) || StringUtils.isBlank(version1))
      throw new IllegalArgumentException(LibUtils.getMsg("APPLIB_NULL_INPUT"));
    tenant = LibUtils.stripStr(tenant1);
    id = LibUtils.stripStr(id1);
    version = LibUtils.stripStr(version1);

    seqId = a.getSeqId();
    created = a.getCreated();
    updated = a.getUpdated();
    description = a.getDescription();
    owner = LibUtils.stripStr(a.getOwner());
    enabled = a.isEnabled();
    locked = a.isLocked();
    containerized = a.isContainerized();
    runtime = a.getRuntime();
    runtimeVersion = LibUtils.stripStr(a.getRuntimeVersion());
    runtimeOptions = a.getRuntimeOptions();
    containerImage = LibUtils.stripStr(a.getContainerImage());
    jobType = a.getJobType();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    strictFileInputs = a.isStrictFileInputs();
    jobDescription = a.getJobDescription();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = a.getExecSystemConstraints();
    execSystemId = LibUtils.stripStr(a.getExecSystemId());
    execSystemExecDir = LibUtils.stripStr(a.getExecSystemExecDir());
    execSystemInputDir = LibUtils.stripStr(a.getExecSystemInputDir());
    execSystemOutputDir = LibUtils.stripStr(a.getExecSystemOutputDir());
    dtnSystemInputDir = LibUtils.stripStr(a.getDtnSystemInputDir());
    dtnSystemOutputDir = LibUtils.stripStr(a.getDtnSystemOutputDir());
    execSystemLogicalQueue = LibUtils.stripStr(a.getExecSystemLogicalQueue());
    archiveSystemId = LibUtils.stripStr(a.getArchiveSystemId());
    archiveSystemDir = LibUtils.stripStr(a.getArchiveSystemDir());
    archiveOnAppError = a.isArchiveOnAppError();
    isMpi = a.getIsMpi();
    mpiCmd = LibUtils.stripStr(a.getMpiCmd());
    cmdPrefix = a.getCmdPrefix();
    parameterSet = a.getParameterSet();
    fileInputs = a.getFileInputs();
    fileInputArrays = a.getFileInputArrays();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMB();
    maxMinutes = a.getMaxMinutes();
    subscriptions = a.getSubscriptions();
    jobTags = a.getJobTags();
    tags = (a.getTags() == null) ? EMPTY_STR_ARRAY : a.getTags().clone();
    notes = a.getNotes();
    sharedAppCtx = LibUtils.stripStr(a.getSharedAppCtx());
    isPublic = a.isPublic();
    sharedWithUsers = a.getSharedWithUsers();
    uuid = a.getUuid();
    deleted = a.isDeleted();
    // Strip whitespace as appropriate for string attributes.
    stripWhitespace();
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public App(int seqId1, int verSeqId1, String tenant1, String id1, String version1, String description1,
             JobType jobType1, String owner1, boolean enabled1, boolean locked1, boolean containerized1,
             Runtime runtime1, String runtimeVersion1, List<RuntimeOption> runtimeOptions1, String containerImage1,
             int maxJobs1, int maxJobsPerUser1, boolean strictFileInputs1,
             // == Start jobAttributes
             String jobDescription1, boolean dynamicExecSystem1,
             String[] execSystemConstraints1, String execSystemId1, String execSystemExecDir1,
             String execSystemInputDir1, String execSystemOutputDir1,
             String dtnSystemInputDir1, String dtnSystemOutputDir1,
             String execSystemLogicalQueue1,
             String archiveSystemId1, String archiveSystemDir1, boolean archiveOnAppError1,
             boolean isMpi1, String mpiCmd1, String cmdPrefix1,
             ParameterSet parameterSet1, List<FileInput> fileInputs1, List<FileInputArray> fileInputArrays1,
             int nodeCount1, int coresPerNode1, int memoryMB1, int maxMinutes1,
             List<ReqSubscribe> subscriptions1, String[] jobTags1,
             // == End jobAttributes
             String[] tags1, JsonObject notes1, UUID uuid1, boolean deleted1, Instant created1, Instant updated1)
  {
    seqId = seqId1;
    verSeqId = verSeqId1;
    tenant = LibUtils.stripStr(tenant1);
    id = LibUtils.stripStr(id1);
    version = LibUtils.stripStr(version1);
    description = description1;
    jobType = (jobType1 == null) ? DEFAULT_JOB_TYPE : jobType1;
    owner = LibUtils.stripStr(owner1);
    enabled = enabled1;
    locked = locked1;
    containerized = containerized1;
    runtime = (runtime1 == null) ? DEFAULT_RUNTIME : runtime1;
    runtimeVersion = LibUtils.stripStr(runtimeVersion1);
    runtimeOptions = (runtimeOptions1 == null) ? null : new ArrayList<>(runtimeOptions1);
    containerImage = LibUtils.stripStr(containerImage1);
    maxJobs = maxJobs1;
    maxJobsPerUser = maxJobsPerUser1;
    strictFileInputs = strictFileInputs1;
    jobDescription = jobDescription1;
    dynamicExecSystem = dynamicExecSystem1;
    execSystemConstraints = (execSystemConstraints1 == null) ? null : execSystemConstraints1.clone();
    execSystemId = LibUtils.stripStr(execSystemId1);
    execSystemExecDir = LibUtils.stripStr(execSystemExecDir1);
    execSystemInputDir = LibUtils.stripStr(execSystemInputDir1);
    execSystemOutputDir = LibUtils.stripStr(execSystemOutputDir1);
    dtnSystemInputDir = (dtnSystemInputDir1 == null) ? DEFAULT_DTNSYS_INDIR : LibUtils.stripStr(dtnSystemInputDir1);
    dtnSystemOutputDir = (dtnSystemOutputDir1 == null) ? DEFAULT_DTNSYS_OUTDIR : LibUtils.stripStr(dtnSystemOutputDir1);
    execSystemLogicalQueue = LibUtils.stripStr(execSystemLogicalQueue1);
    archiveSystemId = LibUtils.stripStr(archiveSystemId1);
    archiveSystemDir = LibUtils.stripStr(archiveSystemDir1);
    archiveOnAppError = archiveOnAppError1;
    isMpi = isMpi1;
    mpiCmd = LibUtils.stripStr(mpiCmd1);
    cmdPrefix = cmdPrefix1;
    parameterSet = parameterSet1;
    fileInputs = (fileInputs1 == null) ? null : new ArrayList<>(fileInputs1);
    fileInputArrays = (fileInputArrays1 == null) ? null : new ArrayList<>(fileInputArrays1);
    nodeCount = nodeCount1;
    coresPerNode = coresPerNode1;
    memoryMB = memoryMB1;
    maxMinutes = maxMinutes1;
    subscriptions = (subscriptions1 == null) ? null : new ArrayList<>(subscriptions1);
    jobTags = (jobTags1 == null) ? null : jobTags1.clone();
    tags = (tags1 == null) ? null : tags1.clone();
    notes = notes1;
    uuid = uuid1;
    deleted = deleted1;
    created = created1;
    updated = updated1;
    // Strip whitespace as appropriate for string attributes.
    stripWhitespace();
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
    tenant = LibUtils.stripStr(a.getTenant());
    id = LibUtils.stripStr(a.getId());
    version = LibUtils.stripStr(a.getVersion());
    description = a.getDescription();
    owner = LibUtils.stripStr(a.getOwner());
    enabled = a.isEnabled();
    locked = a.isLocked();
    containerized = a.isContainerized();
    runtime = a.getRuntime();
    runtimeVersion = LibUtils.stripStr(a.getRuntimeVersion());
    runtimeOptions = a.getRuntimeOptions();
    containerImage = LibUtils.stripStr(a.getContainerImage());
    jobType = a.getJobType();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    strictFileInputs = a.isStrictFileInputs();
    jobDescription = a.getJobDescription();
    dynamicExecSystem = a.isDynamicExecSystem();
    execSystemConstraints = a.getExecSystemConstraints();
    execSystemId = LibUtils.stripStr(a.getExecSystemId());
    execSystemExecDir = LibUtils.stripStr(a.getExecSystemExecDir());
    execSystemInputDir = LibUtils.stripStr(a.getExecSystemInputDir());
    execSystemOutputDir = LibUtils.stripStr(a.getExecSystemOutputDir());
    dtnSystemInputDir = LibUtils.stripStr(a.getDtnSystemInputDir());
    dtnSystemOutputDir = LibUtils.stripStr(a.getDtnSystemOutputDir());
    execSystemLogicalQueue = LibUtils.stripStr(a.getExecSystemLogicalQueue());
    archiveSystemId = LibUtils.stripStr(a.getArchiveSystemId());
    archiveSystemDir = LibUtils.stripStr(a.getArchiveSystemDir());
    archiveOnAppError = a.isArchiveOnAppError();
    isMpi = a.getIsMpi();
    mpiCmd = LibUtils.stripStr(a.getMpiCmd());
    cmdPrefix = a.getCmdPrefix();
    parameterSet = a.getParameterSet();
    fileInputs = a.getFileInputs();
    fileInputArrays = a.getFileInputArrays();
    nodeCount = a.getNodeCount();
    coresPerNode = a.getCoresPerNode();
    memoryMB = a.getMemoryMB();
    maxMinutes = a.getMaxMinutes();
    subscriptions = a.getSubscriptions();
    jobTags = a.getJobTags();
    tags = a.getTags();
    notes = a.getNotes();
    sharedAppCtx = LibUtils.stripStr(a.getSharedAppCtx());
    isPublic = a.isPublic();
    sharedWithUsers = a.getSharedWithUsers();
    uuid = a.getUuid();
    deleted = a.isDeleted();
    created = a.getCreated();
    updated = a.getUpdated();
    // Strip whitespace as appropriate for string attributes.
    stripWhitespace();
  }

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************

  /**
   * Resolve variables for App attributes
   */
  public void resolveVariables(String oboUser)
  {
    // Resolve owner if necessary. If empty or "${apiUserId}" then fill in with oboUser.
    // Note that for a user request oboUser and jwtUser are the same and for a service request we want oboUser here.
    if (StringUtils.isBlank(owner) || owner.equalsIgnoreCase(APIUSERID_VAR)) setOwner(oboUser);
  }

  /**
   * Fill in defaults
   */
  public void setDefaults()
  {
    if (StringUtils.isBlank(owner)) setOwner(DEFAULT_OWNER);
    if (jobTags == null) setJobTags(EMPTY_STR_ARRAY);
    if (tags == null) setTags(EMPTY_STR_ARRAY);
    if (notes == null) setNotes(DEFAULT_NOTES);
    // Process request to create list of env variables with proper defaults.
    if (parameterSet != null && parameterSet.getEnvVariables() != null)
    {
      parameterSet.setEnvVariables(processEnvVariables(parameterSet.getEnvVariables()));
    }
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
    checkAttrControlCharacters(errMessages);
    return errMessages;
  }

  /*
   * Process envVariables from request to create list of job env variables with proper defaults.
   */
  public static List<KeyValuePair> processEnvVariables(List<KeyValuePair> requestEnvVars)
  {
    var envVars = new ArrayList<KeyValuePair>();
    // If no items return an empty list
    if (requestEnvVars == null || requestEnvVars.isEmpty()) return envVars;

    // Process each item. Constructor will set appropriate defaults
    for (KeyValuePair kv : requestEnvVars)
    {
      envVars.add(new KeyValuePair(kv.getKey(), kv.getValue(), kv.getDescription(), kv.getInputMode(), kv.getNotes()));
    }
    return envVars;
  }

  // ************************************************************************
  // ******************** Private methods ***********************************
  // ************************************************************************

  /**
   * Check for missing required attributes
   *   Id, version, containerImage
   */
  private void checkAttrRequired(List<String> errMessages)
  {
    if (StringUtils.isBlank(id)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, ID_FIELD));
    if (StringUtils.isBlank(version)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, VERSION_FIELD));
    if (StringUtils.isBlank(containerImage)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, CONTAINERIMG_FIELD));
  }

  /**
   * Check for invalid attributes
   *   id, runtimeVersion
   */
  private void checkAttrValidity(List<String> errMessages)
  {
    // Check that id is not empty and contains a valid pattern
    if (!StringUtils.isBlank(id) && !isValidId(id))
      errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, ID_FIELD, id));
    if (!StringUtils.isBlank(execSystemId) && !isValidId(execSystemId))
      errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, EXECSYSID_FIELD, execSystemId));
    if (!StringUtils.isBlank(archiveSystemId) && !isValidId(archiveSystemId))
      errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, ARCHIVESYSID_FIELD, archiveSystemId));

// TBD // Check that runtimeVersion is a single version, list of versions or range of versions
//    if (!StringUtils.isBlank(runtimeVersion))
//    {
////      DefaultArtifactVersion ver = new DefaultArtifactVersion(runtimeVersion);
//      try
//      {
//        VersionRange verRange = VersionRange.createFromVersionSpec(runtimeVersion);
//      }
//      catch (Exception e)
//      {
//        errMessages.add(LibUtils.getMsg("APPLIB_INVALID_RUNTIME_VER_RNG", runtimeVersion, e.getMessage()));
//      }
//    }
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

    if (!StringUtils.isBlank(dtnSystemInputDir) && dtnSystemInputDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + DTNSYSINDIR_FIELD, MAX_PATH_LEN));
    }

    if (!StringUtils.isBlank(dtnSystemOutputDir) && dtnSystemOutputDir.length() > MAX_PATH_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, JOB_FIELD_PREFIX + DTNSYSOUTDIR_FIELD, MAX_PATH_LEN));
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
   * Check for control characters for attributes that do not allow them.
   */
  private void checkAttrControlCharacters(List<String> errMessages)
  {
    checkForControlChars(errMessages, id, ID_FIELD);
    checkForControlChars(errMessages, version, VERSION_FIELD);
    checkForControlChars(errMessages, owner, OWNER_FIELD);
    checkForControlChars(errMessages, tenant, TENANT_FIELD);
    checkForControlChars(errMessages, runtimeVersion, RUNTIMEVER_FIELD);
    checkForControlChars(errMessages, containerImage, CONTAINERIMG_FIELD);
    // NOTE Use various checkForControlChars* methods to help code readability.
    checkForControlCharsStrArray(errMessages, tags, TAGS_FIELD, null);
    checkForControlCharsJobAttributes(errMessages);
  }

  /**
   * Check for control characters in jobAttributes
   */
  private void checkForControlCharsJobAttributes(List<String> errMessages)
  {
    checkForControlCharsStrArray(errMessages, execSystemConstraints, EXECSYS_CONSTRAINTS_FIELD, null);
    checkForControlChars(errMessages, execSystemId, EXECSYSID_FIELD);
    checkForControlChars(errMessages, execSystemExecDir, EXECSYSEXECDIR_FIELD);
    checkForControlChars(errMessages, execSystemInputDir, EXECSYSINDIR_FIELD);
    checkForControlChars(errMessages, execSystemOutputDir, EXECSYSOUTDIR_FIELD);
    checkForControlChars(errMessages, dtnSystemInputDir, DTNSYSINDIR_FIELD);
    checkForControlChars(errMessages, dtnSystemOutputDir, DTNSYSOUTDIR_FIELD);
    checkForControlChars(errMessages, archiveSystemId, ARCHIVESYSID_FIELD);
    checkForControlChars(errMessages, archiveSystemDir, ARCHIVESYSDIR_FIELD);
    checkForControlChars(errMessages, execSystemLogicalQueue, EXECSYSLOGICALQ_FIELD);
    checkForControlChars(errMessages, mpiCmd, MPI_CMD_FIELD);
    checkForControlCharsStrArray(errMessages, jobTags, JOB_ATTRS_FIELD, TAGS_FIELD);
    checkForControlCharsParameterSet(errMessages);
    checkForControlCharsFileInputs(errMessages);
    checkForControlCharsFileInputArrays(errMessages);
    checkForControlCharsSubscriptions(errMessages);
  }

  /**
   * Check for control characters in parameterSet
   */
  private void checkForControlCharsParameterSet(List<String> errMessages)
  {
    if (parameterSet == null) return;
    checkForControlCharsArgSpecList(errMessages, parameterSet.getAppArgs(), APP_ARGS_FIELD);
    checkForControlCharsArgSpecList(errMessages, parameterSet.getContainerArgs(), CONTAINER_ARGS_FIELD);
    checkForControlCharsArgSpecList(errMessages, parameterSet.getSchedulerOptions(), SCHED_OPTS_FIELD);
    checkForControlCharsEnvVariables(errMessages, parameterSet.getEnvVariables());
    checkForControlCharsArchiveFilter(errMessages, parameterSet.getArchiveFilter());
    checkForControlCharsLogConfig(errMessages, parameterSet.getLogConfig());
  }

  /**
   * Check for control characters in env variables
   */
  private void checkForControlCharsEnvVariables(List<String> errMessages, List<KeyValuePair> envVariables)
  {
    if (envVariables == null || envVariables.isEmpty()) return;
    for (var envVar : envVariables)
    {
      String name = envVar.getKey(); // NOTE: Previous check ensures name is not empty.
      String value = envVar.getValue();
      checkForControlChars(errMessages, name, ENV_VARS_FIELD, NAME_FIELD);
      checkForControlChars(errMessages, value, ENV_VARS_FIELD, VALUE_FIELD);
    }
  }

  /**
   * Check for control characters in archiveFilter
   */
  private void checkForControlCharsArchiveFilter(List<String> errMessages, ArchiveFilter archiveFilter) {
    if (archiveFilter == null) return;
    if (archiveFilter.getIncludes() != null)
    {
      for (var fileName : archiveFilter.getIncludes())
      {
        checkForControlChars(errMessages, fileName, ARCHIVE_FILTER_FIELD, ARCHIVE_INCLUDES_FIELD);
      }
    }
    if (archiveFilter.getExcludes() != null)
    {
      for (var fileName : archiveFilter.getExcludes())
      {
        checkForControlChars(errMessages, fileName, ARCHIVE_FILTER_FIELD, ARCHIVE_EXCLUDES_FIELD);
      }
    }
  }

  /**
   * Check for control characters in logConfig
   */
  private void checkForControlCharsLogConfig(List<String> errMessages, LogConfig logConfig)
  {
    if (logConfig == null) return;
    checkForControlChars(errMessages, logConfig.getStdoutFilename(), LOGCONFIG_FIELD, LOGCONFIG_OUT_FIELD);
    checkForControlChars(errMessages, logConfig.getStderrFilename(), LOGCONFIG_FIELD, LOGCONFIG_ERR_FIELD);
  }

  /**
   * Check for control characters in file inputs
   */
  private void checkForControlCharsFileInputs(List<String> errMessages)
  {
    if (fileInputs == null || fileInputs.isEmpty()) return;
    for (FileInput fi : fileInputs)
    {
      String name = StringUtils.isBlank(fi.getName()) ? UNNAMED : fi.getName();
      String src = fi.getSourceUrl();
      String dst = fi.getTargetPath();
      checkForControlChars(errMessages, name, FILE_INPUTS_FIELD, NAME_FIELD);
      checkForControlChars(errMessages, src, FILE_INPUTS_FIELD, name, FILE_INPUTS_SRC_FIELD);
      checkForControlChars(errMessages, dst, FILE_INPUTS_FIELD, name, FILE_INPUTS_DST_FIELD);
    }
  }

  /**
   * Check for control characters in file input arrays
   */
  private void checkForControlCharsFileInputArrays(List<String> errMessages)
  {
    if (fileInputArrays == null || fileInputArrays.isEmpty()) return;
    for (FileInputArray fia : fileInputArrays)
    {
      String name = StringUtils.isBlank(fia.getName()) ? UNNAMED : fia.getName();
      String targetDir = fia.getTargetDir();
      List<String> srcUrls = fia.getSourceUrls();
      checkForControlChars(errMessages, name, FILE_INPUT_ARRAYS_FIELD, NAME_FIELD);
      checkForControlChars(errMessages, targetDir, FILE_INPUT_ARRAYS_FIELD, name, FILE_INPUT_ARRAYS_TARGET_FIELD);

      if (srcUrls == null || srcUrls.isEmpty()) return;
      for (String srcUrl : srcUrls)
      {
        checkForControlChars(errMessages, srcUrl, FILE_INPUTS_FIELD, name, FILE_INPUT_ARRAYS_SRC_FIELD);
      }
    }
  }

  /**
   * Check for control characters in subscriptions
   */
  private void checkForControlCharsSubscriptions(List<String> errMessages)
  {
    if (subscriptions == null || subscriptions.isEmpty()) return;
    for (ReqSubscribe sub : subscriptions)
    {
      var deliveryTargets = sub.getDeliveryTargets();
      if (deliveryTargets == null || deliveryTargets.isEmpty()) continue;
      for (var deliveryTarget : deliveryTargets)
      {
        String deliveryAddress = deliveryTarget.getDeliveryAddress();
        checkForControlChars(errMessages, deliveryAddress, SUBSCRIPTIONS_FIELD, SUBSCRIPTIONS_DLVRY_ADDR_FIELD);
      }
    }
  }

  /**
   * Check for control characters for a list of ArgSpec entries
   */
  private void checkForControlCharsArgSpecList(List<String> errMessages, List<ArgSpec> argSpecs, String label1)
  {
    if (argSpecs == null) return;
    for (ArgSpec argSpec : argSpecs)
    {
      String name = StringUtils.isBlank(argSpec.getName()) ? UNNAMED : argSpec.getName();
      String value = argSpec.getArg();
      checkForControlChars(errMessages, name, label1, NAME_FIELD);
      // Since we use name as a label, we need to replace any control characters in it, so we do not
      // log any strings with control characters.
      name = PathSanitizer.replaceControlChars(name, '?');
      checkForControlChars(errMessages, value, label1, name, VALUE_FIELD);
    }
  }

  /*
   * Check for control characters in an array of strings.
   */
  private void checkForControlCharsStrArray(List<String> errMessages, String[] strArray, String label1, String label2)
  {
    if (strArray == null) return;
    for (String s : strArray)
    {
      checkForControlChars(errMessages, s, label1, label2);
    }
  }

  /**
   * Check for control characters in an attribute value. Use 3 part label.
   * Labels should never be null.
   */
  private void checkForControlChars(List<String> errMessages, String attrValue, String label1, String label2, String label3)
  {
    String label = String.format("%s.%s.%s", label1, label2, label3);
    checkForControlChars(errMessages, attrValue, label);
  }

  /**
   * Check for control characters in an attribute value. Use 1 or 2 part label for message.
   * label2 can be null.
   */
  private void checkForControlChars(List<String> errMessages, String attrValue, String label1, String label2)
  {
    String label = label1;
    if (!StringUtils.isBlank(label2)) label = String.format("%s.%s", label1, label2);
    checkForControlChars(errMessages, attrValue, label);
  }

  /**
   * Check for control characters in an attribute value.
   * If one is found add a message to the error list using provided label.
   */
  private void checkForControlChars(List<String> errMessages, String attrValue, String label)
  {

    try { PathSanitizer.detectControlChars(attrValue); }
    catch (TapisException e)
    {
      String logAttrValue = PathSanitizer.replaceControlChars(attrValue, '?');
      errMessages.add(LibUtils.getMsg(CTL_CHR_ATTR, label, logAttrValue, e.getMessage()));
    }
  }

  /**
   * Check misc attribute restrictions
   *  If containerized is true then containerImage must be set
   *  If containerized and SINGULARITY then RuntimeOptions must include one of SINGULARITY_START or SINGULARITY_RUN
   *  If containerized and ZIP then validate entry - must be absolute path or valid sourceUrl
   *  If app contains file inputs then validate sourceUrl attributes for each input.
   *  If dynamicExecSystem then execSystemConstraints must be given
   *  If archiveSystem given then archive dir must be given
   *  If parameterSet.envVariables set then check:
   *    - key is not empty.
   *    - if inputMode=FIXED then value != "!tapis_not_set"
   *  If runtime type is not ZIP then --tapis-zip-save is not allowed.
   *  If runtime type is ZIP then we only support containerArg == --tapis-zip-save
   */
  private void checkAttrMisc(List<String> errMessages)
  {
    // If containerized is true then containerImage must be set
    if (containerized && StringUtils.isBlank(containerImage))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_NOIMAGE"));
    }

    // If containerized and SINGULARITY then RuntimeOptions must be provided and include one and only one of
    //   SINGULARITY_START, SINGULARITY_RUN
    if (containerized && Runtime.SINGULARITY.equals(runtime))
    {
      // If options list contains both or neither of START and RUN then reject.
      if ( runtimeOptions == null ||
           (runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) && runtimeOptions.contains(RuntimeOption.SINGULARITY_START))
           ||
           !(runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) || runtimeOptions.contains(RuntimeOption.SINGULARITY_START)))
      {
        errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_SING_OPT", SING_OPT_LIST));
      }
    }

    // If containerized and ZIP then validate containerImage - must be absolute path or valid sourceUrl
    if (containerized && Runtime.ZIP.equals(runtime) && !StringUtils.isBlank(containerImage))
    {
      if (!isZipContainerImageValid(containerImage))
      {
        errMessages.add(LibUtils.getMsg("APPLIB_INVALID_CONTAINERIZED_ZIP_IMAGE", containerImage));
      }
    }

   // If app contains file inputs then validate sourceUrl attributes for each input.
    if (fileInputs != null) { for (FileInput fin : fileInputs) checkFileInputSourceUrl(errMessages, fin); }

    // If dynamicExecSystem then execSystemConstraints must be given
    if (dynamicExecSystem)
    {
      if (execSystemConstraints == null || execSystemConstraints.length == 0)
      {
        errMessages.add(LibUtils.getMsg("APPLIB_DYNAMIC_NOCONSTRAINTS"));
      }
    }

    // If archiveSystem given then archive dir must be given
    if (!StringUtils.isBlank(archiveSystemId) && StringUtils.isBlank(archiveSystemDir))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_ARCHIVE_NODIR"));
    }

    // Checks involving parameterSet
    if (parameterSet != null)
    {
      // Check env variables
      if (parameterSet.getEnvVariables() != null)
      {
        for (KeyValuePair kv : parameterSet.getEnvVariables())
        {
          // Name must not be empty
          if (StringUtils.isBlank(kv.getKey()))
          {
            errMessages.add(LibUtils.getMsg("APPLIB_ENV_VAR_NO_NAME", kv.getValue()));
          }
          // Check for inputMode=FIXED and value == "!tapis_not_set"
          if (FIXED.equals(kv.getInputMode()) && VALUE_NOT_SET.equals(kv.getValue()))
          {
            errMessages.add(LibUtils.getMsg("APPLIB_ENV_VAR_FIXED_UNSET", kv.getKey(), kv.getValue()));
          }
          // Check for variables that begin with "_tapis". This is not allowed. Jobs will not accept them.
          if (StringUtils.startsWith(kv.getKey(), RESERVED_PREFIX))
          {
            errMessages.add(LibUtils.getMsg("APPLIB_ENV_VAR_INVALID_PREFIX", kv.getKey(), kv.getValue()));
          }
        }
      }
      // Check container args
      var containerArgs = parameterSet.getContainerArgs();
      if (containerArgs != null)
      {
        for (ArgSpec argSpec : containerArgs)
        {
          // If runtime type is not ZIP then --tapis-zip-save is not allowed.
          if (runtime != Runtime.ZIP && ZIP_SAVE_CONTAINER_ARG.equals(argSpec.getArg()))
          {
            errMessages.add(LibUtils.getMsg("APPLIB_CON_ARG_NOT_SUPPORTED", runtime, argSpec.getArg()));
          }
          // If runtime type is ZIP then we only support containerArg == --tapis-zip-save
          if (runtime == Runtime.ZIP && !ZIP_SAVE_CONTAINER_ARG.equals(argSpec.getArg()))
          {
            errMessages.add(LibUtils.getMsg("APPLIB_CON_ARG_NOT_SUPPORTED", runtime, argSpec.getArg()));
          }
        }
      }
    }
  }

  /**
   * Strip whitespace as appropriate for string attributes.
   * For: tags, jobTags, execSystemConstraints
   * Stripping for other handled by the classes: parameterSet, fileInputs, fileInputArrays, subscriptions
   */
  private void stripWhitespace()
  {
    tags = LibUtils.stripWhitespaceStrArray(tags);
    jobTags = LibUtils.stripWhitespaceStrArray(jobTags);
    execSystemConstraints = LibUtils.stripWhitespaceStrArray(execSystemConstraints);
  }

  /**
   * Validate an ID string.
   * Must start alphabetic and contain only alphanumeric and 4 special characters: - . _ ~
   */
  private static boolean isValidId(String id) { return id.matches(PATTERN_VALID_ID); }

  /**
   * Validate a host string.
   * Check if a string is a valid hostname or IP address.
   * Use methods from org.apache.commons.validator.routines.
   */
  private static boolean isValidHost(String host)
  {
    // First check for valid IP address, then for valid domain name
    if (DomainValidator.getInstance().isValid(host) || InetAddressValidator.getInstance().isValid(host)) return true;
    else return false;
  }

  /**
   * Check to see if containerImage attribute for the case of a ZIP runtime app is valid.
   * Must be absolute path or sourceUrl valid for a file transfer (except tapislocal).
   */
  private static boolean isZipContainerImageValid(String containerImage)
  {
    if (containerImage!=null && containerImage.startsWith("/")) return true;
    Matcher matcher = SRC_URL_PATTERN.matcher(containerImage);
    if (matcher.find()) return true;
    return false;
  }

  /**
   * Check to see if fileInput sourceUrl is valid
   */
  private static void checkFileInputSourceUrl(List<String> errMessages, FileInput fileInput)
  {
    // sourceUrl is optional, so skip check if not set.
    if (StringUtils.isBlank(fileInput.getSourceUrl())) return;
    if (!validFileInputSourceUrl(fileInput.getSourceUrl()))
    {
      errMessages.add(LibUtils.getMsg("APPLIB_INVALID_FILEINPUT_SOURCEURL", fileInput.getName(), fileInput.getSourceUrl()));
    }
  }

  /**
   * Check fileInput sourceUrl
   */
  private static boolean validFileInputSourceUrl(String sourceUrl)
  {
    // Must be empty, http/s://, tapis:// or tapislocal://
    if (StringUtils.isBlank(sourceUrl)) return true;
    Matcher matcher = SRC_URL_PATTERN.matcher(sourceUrl);
    if (matcher.find()) return true;
    matcher = SRC_URL_LOCAL_PATTERN.matcher(sourceUrl);
    if (matcher.find()) return true;
    return false;
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

  public String getId() { return id; }

  public String getVersion() { return version; }

  public JobType getJobType() { return jobType; }
  public void setJobType(JobType jt) { jobType = jt;  }

  public String getDescription() { return description; }
  public void setDescription(String d) { description = d;  }

  public String getOwner() { return owner; }
  public void setOwner(String s) { owner = LibUtils.stripStr(s);  }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean b) { enabled = b;   }

  public boolean isLocked() { return locked; }
  public void setLocked(boolean b) { locked = b;   }

  public boolean isContainerized() { return containerized; }
  public void setContainerized(boolean b) { containerized = b;   }

  public Runtime getRuntime() { return runtime; }
  public void setRuntime(Runtime r) { runtime = r; }

  public String getRuntimeVersion() { return runtimeVersion; }
  public void setRuntimeVersion(String s) { runtimeVersion = LibUtils.stripStr(s);  }

  public List<RuntimeOption> getRuntimeOptions()
  {
    return (runtimeOptions == null) ? null : new ArrayList<>(runtimeOptions);
  }
  public void setRuntimeOptions(List<RuntimeOption> rol)
  {
    runtimeOptions = (rol == null) ? null : new ArrayList<>(rol);
  }

  public String getContainerImage() { return containerImage; }
  public void setContainerImage(String s) { containerImage = LibUtils.stripStr(s);  }

  public boolean isDynamicExecSystem() { return dynamicExecSystem; }
  public void setDynamicExecSystem(boolean b) {dynamicExecSystem = b;  }

  public String[] getExecSystemConstraints() { return (execSystemConstraints == null) ? null : execSystemConstraints.clone(); }
  public void setExecSystemConstraints(String[] sa)
  {
    execSystemConstraints = (sa == null) ? null : sa.clone();
  }

  public String getExecSystemId() { return execSystemId; }
  public void setExecSystemId(String s) { execSystemId = LibUtils.stripStr(s);  }

  public String getExecSystemExecDir() { return execSystemExecDir; }
  public void setExecSystemExecDir(String s) { execSystemExecDir = LibUtils.stripStr(s);  }

  public String getExecSystemInputDir() { return execSystemInputDir; }
  public void setExecSystemInputDir(String s) { execSystemInputDir = LibUtils.stripStr(s);  }

  public String getExecSystemOutputDir() { return execSystemOutputDir; }
  public void setExecSystemOutputDir(String s) { execSystemOutputDir = LibUtils.stripStr(s);  }

  public String getDtnSystemInputDir() { return dtnSystemInputDir; }
  public void setDtnSystemInputDir(String s) { dtnSystemInputDir = LibUtils.stripStr(s);  }

  public String getDtnSystemOutputDir() { return dtnSystemOutputDir; }
  public void setDtnSystemOutputDir(String s) { dtnSystemOutputDir = LibUtils.stripStr(s);  }

  public String getExecSystemLogicalQueue() { return execSystemLogicalQueue; }
  public void setExecSystemLogicalQueue(String s) { execSystemLogicalQueue = LibUtils.stripStr(s);  }

  public String getArchiveSystemId() { return archiveSystemId; }
  public void setArchiveSystemId(String s) { archiveSystemId = LibUtils.stripStr(s);  }

  public String getArchiveSystemDir() { return archiveSystemDir; }
  public void setArchiveSystemDir(String s) { archiveSystemDir = LibUtils.stripStr(s);  }

  public boolean isArchiveOnAppError() { return archiveOnAppError; }
  public void setArchiveOnAppError(boolean b) { archiveOnAppError = b;  }

  public boolean getIsMpi() { return isMpi; }
  public void setIsMpi(boolean b) { isMpi = b;  }

  public String getMpiCmd() { return mpiCmd; }
  public void setMpiCmd(String s) { mpiCmd = LibUtils.stripStr(s); }

  public String getCmdPrefix() { return cmdPrefix; }
  public void setCmdPrefix(String s) { cmdPrefix = s; }

  public String getJobDescription() { return jobDescription; }
  public void setJobDescription(String s) { jobDescription = s;  }

  public int getMaxJobs()
  {
    return maxJobs;
  }
  public void setMaxJobs(int i) { maxJobs = i;  }

  public int getMaxJobsPerUser()
  {
    return maxJobsPerUser;
  }
  public void setMaxJobsPerUser(int i) { maxJobsPerUser = i;  }

  public int getNodeCount()
  {
    return nodeCount;
  }
  public void setNodeCount(int i) { nodeCount = i;  }

  public int getCoresPerNode()
  {
    return coresPerNode;
  }
  public void setCoresPerNode(int i) { coresPerNode = i;  }

  public int getMemoryMB()
  {
    return memoryMB;
  }
  public void setMemoryMB(int i) { memoryMB = i;  }

  public int getMaxMinutes() { return maxMinutes; }
  public void setMaxMinutes(int i) { maxMinutes = i;  }

  public boolean isStrictFileInputs() { return strictFileInputs; }
  public void setStrictFileInputs(boolean b) { strictFileInputs = b;   }

  public List<FileInput> getFileInputs() {
    return (fileInputs == null) ? null : new ArrayList<>(fileInputs);
  }
  public void setFileInputs(List<FileInput> fi)
  {
    fileInputs = (fi == null) ? null : new ArrayList<>(fi);
  }

  public List<FileInputArray> getFileInputArrays() {
    return (fileInputArrays == null) ? null : new ArrayList<>(fileInputArrays);
  }
  public void setFileInputArrays(List<FileInputArray> fia) { fileInputArrays = (fia == null) ? null : new ArrayList<>(fia);
  }

  public List<ReqSubscribe> getSubscriptions()
  {
    return (subscriptions == null) ? null : new ArrayList<>(subscriptions);
  }
  public void setSubscriptions(List<ReqSubscribe> ns)
  {
    subscriptions = (ns == null) ? null : new ArrayList<>(ns);
  }

  public ParameterSet getParameterSet() { return parameterSet; }
  public void setParameterSet(ParameterSet ps) { parameterSet = ps; }

  public String[] getJobTags()
  {
    return (jobTags == null) ? null : jobTags.clone();
  }
  public void setJobTags(String[] jt)
  {
    jobTags = (jt == null) ? null : jt.clone();
  }

  public String[] getTags() {
    return (tags == null) ? null : tags.clone();
  }
  public void setTags(String[] t)
  {
    tags = (t == null) ? null : t.clone();
  }

  public JsonObject getNotes() { return notes; }
  public void setNotes(JsonObject n) { notes = n;  }

  public UUID getUuid() { return uuid; }
  public void setUuid(UUID u) { uuid = u;  }

  public boolean isDeleted() { return deleted; }

  public String getSharedAppCtx() { return sharedAppCtx; }
  public void setSharedAppCtx(String s) { sharedAppCtx = LibUtils.stripStr(s); }

  public boolean isPublic() { return isPublic; }
  public void setIsPublic(boolean b) { isPublic = b;  }

  public void setSharedWithUsers(Set<String> s) { sharedWithUsers = (s == null) ? null : new HashSet<>(s); }
  public Set<String> getSharedWithUsers() { return (sharedWithUsers == null) ? null : new HashSet<>(sharedWithUsers); }
}
