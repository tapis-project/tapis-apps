package edu.utexas.tacc.tapis.apps.utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.model.ArgSpec;
import edu.utexas.tacc.tapis.apps.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.ParameterSet;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.KeyValuePair;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import static edu.utexas.tacc.tapis.apps.model.App.APP_ARGS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ARCHIVESYSDIR_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ARCHIVESYSID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ARCHIVE_FILTER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ARCHIVE_ON_APP_ERROR_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CMD_PREFIX_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CONTAINERIMG_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CONTAINERIZED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CONTAINER_ARGS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CORES_PER_NODE_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.DELETED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.DESCRIPTION_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ENABLED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ENV_VARS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYSEXECDIR_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYSID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYSINDIR_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYSLOGICALQ_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYSOUTDIR_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.EXECSYS_CONSTRAINTS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ISMPI_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.IS_DYNAMIC_EXEC_SYS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.JOB_ATTRS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.JOB_TYPE_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MAX_JOBS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MAX_JOBS_PER_USER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MAX_MINUTES_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MEMORY_MB_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MPI_CMD_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.NODE_COUNT_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.NOTES_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.PARAM_SET_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.RUNTIMEVER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.RUNTIME_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.SCHED_OPTS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.STRICT_FILE_INPUTS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.TAGS_FIELD;

/*
   Utility class containing general use static methods.
   This class is non-instantiable
 */
public class LibUtils
{
  // Private constructor to make it non-instantiable
  private LibUtils() { throw new AssertionError(); }

  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(LibUtils.class);

  // Location of message bundle files
  private static final String MESSAGE_BUNDLE = "edu.utexas.tacc.tapis.apps.AppLibMessages";

  /* **************************************************************************** */
  /*                                Public Methods                                */
  /* **************************************************************************** */

  /**
   * Get a localized message using the specified key and parameters. Locale is null.
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param key message key
   * @param parms message parameters
   * @return localized message
   */
  public static String getMsg(String key, Object... parms)
  {
    return getMsg(key, null, parms);
  }

  /**
   * Get a localized message using the specified key and parameters. Locale is null.
   * Fill in first 4 parameters with user and tenant info from AuthenticatedUser
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param key message key
   * @param parms message parameters
   * @return localized message
   */
  public static String getMsgAuth(String key, ResourceRequestUser rUser, Object... parms)
  {
    // Construct new array of parms. This appears to be most straightforward approach to modify and pass on varargs.
    var newParms = new Object[4 + parms.length];
    newParms[0] = rUser.getJwtTenantId();
    newParms[1] = rUser.getJwtUserId();
    newParms[2] = rUser.getOboTenantId();
    newParms[3] = rUser.getOboUserId();
    System.arraycopy(parms, 0, newParms, 4, parms.length);
    return getMsg(key, newParms);
  }

  /**
   * Get a localized message using the specified locale, key and parameters.
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param locale Locale for message
   * @param key message key
   * @param parms message parameters
   * @return localized message
   */
  public static String getMsg(String key, Locale locale, Object... parms)
  {
    String msgValue = null;

    if (locale == null) locale = Locale.getDefault();

    ResourceBundle bundle = null;
    try { bundle = ResourceBundle.getBundle(MESSAGE_BUNDLE, locale); }
    catch (Exception e)
    {
      _log.error("Unable to find resource message bundle: " + MESSAGE_BUNDLE, e);
    }
    if (bundle != null) try { msgValue = bundle.getString(key); }
    catch (Exception e)
    {
      _log.error("Unable to find key: " + key + " in resource message bundle: " + MESSAGE_BUNDLE, e);
    }

    if (msgValue != null)
    {
      // No problems. If needed fill in any placeholders in the message.
      if (parms != null && parms.length > 0) msgValue = MessageFormat.format(msgValue, parms);
    }
    else
    {
      // There was a problem. Build a message with as much info as we can give.
      StringBuilder sb = new StringBuilder("Key: ").append(key).append(" not found in bundle: ").append(MESSAGE_BUNDLE);
      if (parms != null && parms.length > 0)
      {
        sb.append("Parameters:[");
        for (Object parm : parms) {sb.append(parm.toString()).append(",");}
        sb.append("]");
      }
      msgValue = sb.toString();
    }
    return msgValue;
  }

  /**
   * Log a TAPIS_NULL_PARAMETER exception and throw a TapisException
   */
  public static void logAndThrowNullParmException(String opName, String parmName) throws TapisException
  {
    String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", opName, parmName);
    _log.error(msg);
    throw new TapisException(msg);
  }

  // =============== DB Transaction Management ============================
  /**
   * Close any DB connection related artifacts that are not null
   * @throws SQLException - on sql error
   */
  public static void closeAndCommitDB(Connection conn, PreparedStatement pstmt, ResultSet rs) throws SQLException
  {
    if (rs != null) rs.close();
    if (pstmt != null) pstmt.close();
    if (conn != null) conn.commit();
  }

  /**
   * Roll back a DB transaction and throw an exception
   * This method always throws an exception, either IllegalStateException or TapisException
   */
  public static void rollbackDB(Connection conn, Exception e, String msgKey, Object... parms) throws TapisException
  {
    try
    {
      if (conn != null) conn.rollback();
    }
    catch (Exception e1)
    {
      _log.error(MsgUtils.getMsg("DB_FAILED_ROLLBACK"), e1);
    }

    // If IllegalStateException or TapisException pass it back up
    if (e instanceof IllegalStateException) throw (IllegalStateException) e;
    if (e instanceof TapisException) throw (TapisException) e;

    // Log the exception.
    String msg = MsgUtils.getMsg(msgKey, parms);
    _log.error(msg, e);
    throw new TapisException(msg, e);
  }

  /**
   * Close DB connection, typically called from finally block
   */
  public static void finalCloseDB(Connection conn)
  {
    // Always return the connection back to the connection pool.
    try
    {
      if (conn != null) conn.close();
    }
    catch (Exception e)
    {
      // If commit worked, we can swallow the exception.
      // If not, the commit exception will have been thrown.
      String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION_CLOSE");
      _log.error(msg, e);
    }
  }

  /**
   * Return String[] array of key=value given list of KeyValuePair
   */
  public static String[] getKeyValuesAsArray(List<KeyValuePair> kvList)
  {
    if (kvList == null) return null;
    if (kvList.size() == 0) return App.EMPTY_STR_ARRAY;
    return kvList.stream().map(KeyValuePair::toString).toArray(String[]::new);
  }

  /**
   * Return list of KeyValuePair given String[] array of key=value
   */
  public static List<KeyValuePair> getKeyValuesAsList(String[] kvArray)
  {
    if (kvArray == null || kvArray.length == 0) return Collections.emptyList();
    List<KeyValuePair> kvList = Arrays.stream(kvArray).map(KeyValuePair::fromString).collect(Collectors.toList());
    return kvList;
  }

  /**
   * Compare original and modified Apps to detect changes and produce a complete and succinct description of the changes.
   * If no changes then return null.
   * NOTE that although some attributes should never change in this code path we include them here in case there is
   *   a bug or the design changes and this code path does include them.
   * Attributes that should not change: isEnabled, owner, containerized, isDeleted
   *
   * @param o - original App
   * @param n - new App
   * @param p - incoming PatchApp if this was a PATCH operation
   * @return Description of the changes or null if no changes detected.
   */
  public static String getChangeDescriptionAppUpdate(App o, App n, PatchApp p)
  {
    // For now log entire definitions
    var joOld = TapisGsonUtils.getGson().toJson(o);
    var joFinal = new JSONObject();
    joFinal.put("AppId", o.getId());
    joFinal.put("AppVersion", o.getVersion());
    joFinal.put("Old", joOld);
    joFinal.put("New", joNew);
    return joFinal.toString();
//TODO    boolean changeFound = false;
//    boolean notPatch = (p == null);
//    var jo = new JSONObject();
//    if (!Objects.equals(o.getDescription(), n.getDescription()))
//    {changeFound=true;addChange(jo, DESCRIPTION_FIELD, o.getDescription(), n.getDescription());}
//    if (!Objects.equals(o.getOwner(),n.getOwner()))
//    {changeFound=true;addChange(jo, OWNER_FIELD, o.getOwner(), n.getOwner());}
//    if (!(o.isEnabled() == n.isEnabled()))
//    {changeFound=true;addChange(jo, ENABLED_FIELD, o.isEnabled(), n.isEnabled());}
//    if (!(o.isContainerized() == n.isContainerized()))
//    {changeFound=true;addChange(jo, CONTAINERIZED_FIELD, o.isContainerized(), n.isContainerized());}
//    if (!Objects.equals(o.getContainerImage(),n.getContainerImage()))
//    {changeFound=true;addChange(jo, CONTAINERIMG_FIELD, o.getContainerImage(), n.getContainerImage());}
//    if (!Objects.equals(o.getRuntime(),n.getRuntime()))
//    {changeFound=true;addChange(jo, RUNTIME_FIELD, o.getRuntime().name(), n.getRuntime().name());}
//    if (!Objects.equals(o.getRuntimeVersion(),n.getRuntimeVersion()))
//    {changeFound=true;addChange(jo, RUNTIMEVER_FIELD, o.getRuntimeVersion(), n.getRuntimeVersion());}
//    if (!Objects.equals(o.getJobType(),n.getJobType()))
//    {changeFound=true;addChange(jo, JOB_TYPE_FIELD, o.getJobType().name(), n.getJobType().name());}
//    if (!Objects.equals(o.getMaxJobs(),n.getMaxJobs()))
//    {changeFound=true;addChange(jo, MAX_JOBS_FIELD, o.getMaxJobs(), n.getMaxJobs());}
//    if (!Objects.equals(o.getMaxJobsPerUser(),n.getMaxJobsPerUser()))
//    {changeFound=true;addChange(jo, MAX_JOBS_PER_USER_FIELD, o.getMaxJobsPerUser(), n.getMaxJobsPerUser());}
//    if (!(o.isStrictFileInputs() == n.isStrictFileInputs()))
//    {changeFound=true;addChange(jo, STRICT_FILE_INPUTS_FIELD, o.isStrictFileInputs(), n.isStrictFileInputs());}
//    if (!(o.isDeleted() == n.isDeleted()))
//    {changeFound=true;addChange(jo, DELETED_FIELD, o.isDeleted(), n.isDeleted());}
//
//    // ------------------------------------------------------
//    // Following attributes require more complex handling
//    // ------------------------------------------------------
//    // JOB_ATTRIBUTES - If not a patch or patch value not null then need to compare
//    JobAttributes pJobAttrs = (notPatch ? null : p.getJobAttributes());
//    if (notPatch || pJobAttrs != null)
//    {
//      // Go through job attributes and add changes. If any changes found the method returns true so we only update
//      //    changeFound if it reports changes were found.
//      if (compareJobAttributesAndAddChanges(jo, o, n, notPatch, pJobAttrs)) changeFound = true;
//    }
//    // TAGS - If it is a patch and the patch value was null then no need to compare
//    //   i.e. if not a patch or patch value was not null then do need to compare.
//    // Since TAGS are just strings we can use Objects.equals()
//    if (notPatch || p.getTags() != null)
//    {
//      // Sort so it does not matter if order is different
//      List<String> oldSortedTags = Arrays.asList(Objects.requireNonNull(o.getTags()));
//      List<String> newSortedTags = Arrays.asList(Objects.requireNonNull(n.getTags()));
//      Collections.sort(oldSortedTags);
//      Collections.sort(newSortedTags);
//      if (!Objects.equals(oldSortedTags, newSortedTags))
//      {
//        changeFound = true;
//        addChange(jo, TAGS_FIELD, oldSortedTags, newSortedTags);
//      }
//    }
//
//    // NOTES - If not a patch or patch value not null then need to compare
//    Object pNotes = (notPatch ? null : p.getNotes());
//    if (notPatch || pNotes != null)
//    {
//      if (!compareNotes(o.getNotes(), n.getNotes()))
//      {
//        changeFound = true;
//        addChange(jo, NOTES_FIELD, (JsonObject) o.getNotes(), (JsonObject) n.getNotes());
//      }
//    }
//
//    // If nothing has changed we are done.
//    if (!changeFound) return null;
//
//    var joFinal = new JSONObject();
//    joFinal.put("AppId", o.getId());
//    joFinal.put("AppVersion", o.getVersion());
//    joFinal.put("AttributeChanges", jo);
//    return joFinal.toString();
  }

  /**
   * Create a change description for a permissions grant or revoke.
   */
  public static String getChangeDescriptionPermsUpdate(String appId, String user, Set<App.Permission> permissions)
  {
    var o = new JSONObject();
    o.put("AppId", appId);
    o.put("TargetUser", user);
    var perms = new JSONArray();
    for (App.Permission p : permissions) { perms.put(p.toString()); }
    o.put("Permissions", perms);
    return o.toString();
  }

  /**
   * Create a change description for update of owner.
   */
  public static String getChangeDescriptionUpdateOwner(String appId, String oldOwner, String newOwner)
  {
    var o = new JSONObject();
    o.put("AppId", appId);
    addChange(o, OWNER_FIELD, oldOwner, newOwner);
    return o.toString();
  }
  /*
   * Methods to add change entries for TSystem updates.
   */
  public static void addChange(JSONObject jo, String field, String o, String n)
  {
    var jo1 = new JSONObject();
    jo1.put("oldValue", o);
    jo1.put("newValue", n);
    jo.put(field, jo1);
  }
  public static void addChange(JSONObject jo, String field, boolean o, boolean n)
  {
    var jo1 = new JSONObject();
    jo1.put("oldValue", o);
    jo1.put("newValue", n);
    jo.put(field, jo1);
  }
  public static void addChange(JSONObject jo, String field, int o, int n)
  {
    var jo1 = new JSONObject();
    jo1.put("oldValue", o);
    jo1.put("newValue", n);
    jo.put(field, jo1);
  }
  public static void addChange(JSONObject jo, String field, String[] o, String[] n)
  {
    var jo1 = new JSONObject();
    // TODO/TBD how does this look?
    jo1.put("oldValue", o);
    jo1.put("newValue", n);
    jo.put(field, jo1);
  }
  public static void addChange(JSONObject jo, String field, JsonObject o, JsonObject n)
  {
    var jo1 = new JSONObject();
    // Convert gson.JsonObject to org.json.JSONObject
    var oj = new JSONObject(o.toString());
    var nj = new JSONObject(n.toString());
    jo1.put("oldValue", oj);
    jo1.put("newValue", nj);
    jo.put(field, jo1);
  }
  public static void addChange(JSONObject jo, String field, List<?> o, List<?> n)
  {
    var jo1 = new JSONObject();
    // TODO/TBD how does this look?
    jo1.put("oldValue", o);
    jo1.put("newValue", n);
    jo.put(field, jo1);
  }

  /*
   * To compare notes cast the Objects to gson's JsonObject and let gson do the compare
   */
  private static boolean compareNotes(Object o, Object n)
  {
    JsonObject oj = (JsonObject) o;
    JsonObject nj = (JsonObject) n;
    return Objects.equals(oj, nj);
  }

  /**
   * Go through job attributes and add changes. If nothing actually changed the method returns false
   *
   * @param jo JSONObject for collecting change descriptions
   * @param o Original App
   * @param n Update App
   * @param notPatch if a patch operation
   * @param pJobAttrs jobAttrs from patch or null if not a patch
   * @return true if changes found, else false
   */
  private static boolean compareJobAttributesAndAddChanges(JSONObject jo, App o, App n, boolean notPatch,
                                                           JobAttributes pJobAttrs)
  {
    boolean changeFound = false;
    ParameterSet pParamSet = null;
    if (pJobAttrs != null) pParamSet = pJobAttrs.getParameterSet();

    var joJobAttrs = new JSONObject();

    // Go through all job attributes in the App.
    // If not a patch or patch value not null then need to compare
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getDescription()!=null))
    {
      if (!Objects.equals(o.getJobDescription(), n.getJobDescription()))
      {changeFound=true;addChange(joJobAttrs, DESCRIPTION_FIELD, o.getJobDescription(), n.getJobDescription());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.isDynamicExecSystem()!=null))
    {
      if (!(o.isDynamicExecSystem() == n.isDynamicExecSystem()))
      {changeFound=true;addChange(jo, IS_DYNAMIC_EXEC_SYS_FIELD, o.isDynamicExecSystem(), n.isDynamicExecSystem());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemConstraints()!=null))
    {
      if (!Arrays.equals(o.getExecSystemConstraints(), n.getExecSystemConstraints()))
      {
        changeFound = true;
        addChange(jo, EXECSYS_CONSTRAINTS_FIELD, o.getExecSystemConstraints(), n.getExecSystemConstraints());
      }
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemId()!=null))
    {
      if (!Objects.equals(o.getExecSystemId(), n.getExecSystemId()))
      {changeFound=true;addChange(joJobAttrs, EXECSYSID_FIELD, o.getExecSystemId(), n.getExecSystemId());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemExecDir()!=null))
    {
      if (!Objects.equals(o.getExecSystemExecDir(), n.getExecSystemExecDir()))
      {changeFound=true;addChange(joJobAttrs, EXECSYSEXECDIR_FIELD, o.getExecSystemExecDir(), n.getExecSystemExecDir());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemInputDir()!=null))
    {
      if (!Objects.equals(o.getExecSystemInputDir(), n.getExecSystemInputDir()))
      {changeFound=true;addChange(joJobAttrs, EXECSYSINDIR_FIELD, o.getExecSystemInputDir(), n.getExecSystemInputDir());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemOutputDir()!=null))
    {
      if (!Objects.equals(o.getExecSystemOutputDir(), n.getExecSystemOutputDir()))
      {changeFound=true;addChange(joJobAttrs, EXECSYSOUTDIR_FIELD, o.getExecSystemOutputDir(), n.getExecSystemOutputDir());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getExecSystemLogicalQueue()!=null))
    {
      if (!Objects.equals(o.getExecSystemLogicalQueue(), n.getExecSystemLogicalQueue()))
      {changeFound=true;addChange(joJobAttrs, EXECSYSLOGICALQ_FIELD, o.getExecSystemLogicalQueue(), n.getExecSystemLogicalQueue());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getArchiveSystemId()!=null))
    {
      if (!Objects.equals(o.getArchiveSystemId(), n.getArchiveSystemId()))
      {changeFound=true;addChange(joJobAttrs, ARCHIVESYSID_FIELD, o.getArchiveSystemId(), n.getArchiveSystemId());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getArchiveSystemDir()!=null))
    {
      if (!Objects.equals(o.getArchiveSystemDir(), n.getArchiveSystemDir()))
      {changeFound=true;addChange(joJobAttrs, ARCHIVESYSDIR_FIELD, o.getArchiveSystemDir(), n.getArchiveSystemDir());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getArchiveOnAppError()!=null))
    {
      if (!(o.isArchiveOnAppError() == n.isArchiveOnAppError()))
      {changeFound=true;addChange(jo, ARCHIVE_ON_APP_ERROR_FIELD, o.isArchiveOnAppError(), n.isArchiveOnAppError());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getIsMpi()!=null))
    {
      if (!(o.getIsMpi() == n.getIsMpi()))
      {changeFound=true;addChange(jo, ISMPI_FIELD, o.getIsMpi(), n.getIsMpi());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getMpiCmd()!=null))
    {
      if (!Objects.equals(o.getMpiCmd(), n.getMpiCmd()))
      {changeFound=true;addChange(joJobAttrs, MPI_CMD_FIELD, o.getMpiCmd(), n.getMpiCmd());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getCmdPrefix()!=null))
    {
      if (!Objects.equals(o.getCmdPrefix(), n.getCmdPrefix()))
      {changeFound=true;addChange(joJobAttrs, CMD_PREFIX_FIELD, o.getCmdPrefix(), n.getCmdPrefix());}
    }
    // Job parameterSet
    // If not a patch or patch value not null then need to compare
    if (notPatch || pParamSet != null)
    {
      // Go through job attributes parameter set and add changes. If any changes found the method returns true so we only update
      //    changeFound if it reports changes were found.
      if (compareParameterSetAndAddChanges(joJobAttrs, o.getParameterSet(), n.getParameterSet(), notPatch, pParamSet)) changeFound = true;
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getFileInputs()!=null))
    {
//TODO      if (compareFileInputsAndAddChanges(joJobAttrs, o.getFileInputs(), n.getFileInputs(), p)) changeFound = true;
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getFileInputArrays()!=null))
    {
//TODO       if (compareFileInputArraysAndAddChanges(joJobAttrs, o.getFileInputArrays(), n.getFileInputArrays(), p)) changeFound = true;
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getNodeCount()!=null))
    {
      if (!Objects.equals(o.getNodeCount(),n.getNodeCount()))
      {changeFound=true;addChange(jo, NODE_COUNT_FIELD, o.getNodeCount(), n.getNodeCount());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getCoresPerNode()!=null))
    {
      if (!Objects.equals(o.getCoresPerNode(),n.getCoresPerNode()))
      {changeFound=true;addChange(jo, CORES_PER_NODE_FIELD, o.getCoresPerNode(), n.getCoresPerNode());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getMemoryMB()!=null))
    {
      if (!Objects.equals(o.getMemoryMB(),n.getMemoryMB()))
      {changeFound=true;addChange(jo, MEMORY_MB_FIELD, o.getMemoryMB(), n.getMemoryMB());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getMaxMinutes()!=null))
    {
      if (!Objects.equals(o.getMaxMinutes(),n.getMaxMinutes()))
      {changeFound=true;addChange(jo, MAX_MINUTES_FIELD, o.getMaxMinutes(), n.getMaxMinutes());}
    }
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getSubscriptions()!=null))
    {
//TODO       if (compareSubscriptionsAndAddChanges(joJobAttrs, o.getSubscriptions(), n.getSubscriptions(), p)) changeFound = true;
    }

    // Job tags
    if (notPatch || (pJobAttrs!=null && pJobAttrs.getTags()!=null))
    {
      // Sort so it does not matter if order is different
      List<String> oldSortedTags = Arrays.asList(o.getJobTags());
      List<String> newSortedTags = Arrays.asList(n.getJobTags());
      Collections.sort(oldSortedTags);
      Collections.sort(newSortedTags);
      if (!Objects.equals(oldSortedTags, newSortedTags))
      {
        changeFound = true;
        addChange(jo, TAGS_FIELD, oldSortedTags, newSortedTags);
      }
    }

    // If any changes found add JobAttributes change descriptions under the JobAttributes object
    if (changeFound) jo.put(JOB_ATTRS_FIELD, joJobAttrs);
    return changeFound;
  }

  /**
   * Go through parameterSet of job attributes and add changes. If nothing actually changed the method returns false
   *
   * @param jo JSONObject for collecting change descriptions
   * @param o Original ParameterSet
   * @param n Updated ParameterSet
   * @param notPatch - flag for patch or not
   * @param patchParamSet - parameterSet from patch
   * @return true if changes found, else false
   */
  private static boolean compareParameterSetAndAddChanges(JSONObject jo, ParameterSet o, ParameterSet n,
                                                          boolean notPatch, ParameterSet patchParamSet)
  {
    boolean changeFound = false;
    var joParamSet = new JSONObject();

    // Go through all parameter set attributes in the job attributes.
    // If not a patch or patch value not null then need to compare
    if (notPatch || (patchParamSet!=null && patchParamSet.getAppArgs()!=null))
    {
      // ArgSpec supports equals so we can use Objects.equals()
      if (!Objects.equals(o.getAppArgs(),n.getAppArgs()))
        {changeFound=true;addChange(jo, APP_ARGS_FIELD, o.getAppArgs(), n.getAppArgs());}
    }
    if (notPatch || (patchParamSet!=null && patchParamSet.getContainerArgs()!=null))
    {
      if (!Objects.equals(o.getContainerArgs(),n.getContainerArgs()))
        {changeFound=true;addChange(jo, CONTAINER_ARGS_FIELD, o.getContainerArgs(), n.getContainerArgs());}
    }
    if (notPatch || (patchParamSet!=null && patchParamSet.getSchedulerOptions()!=null))
    {
      if (!Objects.equals(o.getSchedulerOptions(),n.getSchedulerOptions()))
        {changeFound=true;addChange(jo, SCHED_OPTS_FIELD, o.getSchedulerOptions(), n.getSchedulerOptions());}
    }
    if (notPatch || (patchParamSet!=null && patchParamSet.getEnvVariables()!=null))
    {
      if (!Objects.equals(o.getEnvVariables(), n.getEnvVariables()))
        {changeFound = true; addChange(jo, ENV_VARS_FIELD, o.getEnvVariables(), n.getEnvVariables()); }
    }
    if (notPatch || (patchParamSet!=null && patchParamSet.getArchiveFilter()!=null))
    {
//TODO      if (compareArchiveFilter(joParamSet, ARCHIVE_FILTER_FIELD, o.getArchiveFilter(), n.getArchiveFilter())) changeFound = true;
    }
    // If any changes found add descriptions under the JobAttributes object
    if (changeFound) jo.put(PARAM_SET_FIELD, joParamSet);
    return changeFound;
  }
}
