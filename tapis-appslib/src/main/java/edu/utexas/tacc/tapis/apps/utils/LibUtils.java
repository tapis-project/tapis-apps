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

import static edu.utexas.tacc.tapis.apps.model.App.DELETED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.DESCRIPTION_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ENABLED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.NOTES_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_FIELD;
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

  // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
  /**
   * Compare original and modified Apps to detect changes and produce a complete and succinct description of the changes.
   * If no changes then return null.
   * NOTE that although some attributes should never change in this code path we include them here in case there is
   *   a bug or the design changes and this code path does include them.
   * Attributes that should not change: TODO/TBD isEnabled, owner, containerized, isDeleted, isInteractive
   *
   * @param o - original App
   * @param n - new App
   * @param p - incoming PatchApp if this was a PATCH operation
   * @return Description of the changes or null if no changes detected.
   */
  public static String getChangeDescriptionAppUpdate(App o, App n, PatchApp p)
  {
    boolean noChanges = true;
    boolean isPatch = (p != null);
    var jo = new JSONObject();
    if (!Objects.equals(o.getDescription(), n.getDescription()))
    {noChanges=false;addChange(jo, DESCRIPTION_FIELD, o.getDescription(), n.getDescription());}
    if (!Objects.equals(o.getOwner(),n.getOwner()))
    {noChanges=false;addChange(jo, OWNER_FIELD, o.getOwner(), n.getOwner());}
    if (!(o.isEnabled() == n.isEnabled()))
    {noChanges=false;addChange(jo, ENABLED_FIELD, o.isEnabled(), n.isEnabled());}
//    if (!Objects.equals(o.getEffectiveUserId(),n.getEffectiveUserId()))
//    {noChanges=false;addChange(jo, EFFECTIVE_USER_ID_FIELD, o.getEffectiveUserId(), n.getEffectiveUserId());}
//    if (!Objects.equals(o.getDefaultAuthnMethod(),n.getDefaultAuthnMethod()))
//    {noChanges=false;addChange(jo, DEFAULT_AUTHN_METHOD_FIELD, o.getDefaultAuthnMethod().name(), n.getDefaultAuthnMethod().name());}
//    if (!Objects.equals(o.getBucketName(),n.getBucketName()))
//    {noChanges=false;addChange(jo, BUCKET_NAME_FIELD, o.getBucketName(), n.getBucketName());}
//    if (!Objects.equals(o.getRootDir(),n.getRootDir()))
//    {noChanges=false;addChange(jo, ROOT_DIR_FIELD, o.getRootDir(), n.getRootDir());}
//    if (!Objects.equals(o.getPort(),n.getPort()))
//    {noChanges=false;addChange(jo, PORT_FIELD, o.getPort(), n.getPort());}
//    if (!Objects.equals(o.isUseProxy(),n.isUseProxy()))
//    {noChanges=false;addChange(jo, USE_PROXY_FIELD, o.isUseProxy(), n.isUseProxy());}
//    if (!Objects.equals(o.getProxyHost(),n.getProxyHost()))
//    {noChanges=false;addChange(jo, PROXY_HOST_FIELD, o.getProxyHost(), n.getProxyHost());}
//    if (!Objects.equals(o.getProxyHost(),n.getProxyHost()))
//    {noChanges=false;addChange(jo, PROXY_PORT_FIELD, o.getProxyPort(), n.getProxyPort());}
//    if (!Objects.equals(o.getProxyPort(),n.getProxyPort()))
//    {noChanges=false;addChange(jo, DTN_MOUNT_POINT_FIELD, o.getDtnMountPoint(), n.getDtnMountPoint());}
//    if (!Objects.equals(o.getDtnMountPoint(),n.getDtnMountPoint()))
//    {noChanges=false;addChange(jo, DTN_MOUNT_SOURCE_PATH_FIELD, o.getDtnMountSourcePath(), n.getDtnMountSourcePath());}
//    if (!Objects.equals(o.getDtnMountSourcePath(),n.getDtnMountSourcePath()))
//    {noChanges=false;addChange(jo, DTN_MOUNT_SOURCE_PATH_FIELD, o.getDtnMountSourcePath(), n.getDtnMountSourcePath());}
//    if (!Objects.equals(o.getDtnSystemId(),n.getDtnSystemId()))
//    {noChanges=false;addChange(jo, DTN_SYSTEM_ID_FIELD, o.getDtnSystemId(), n.getDtnSystemId());}
//    if (!Objects.equals(o.isDtn(),n.isDtn()))
//    {noChanges=false;addChange(jo, IS_DTN_FIELD, o.isDtn(), n.isDtn());}
//    if (!Objects.equals(o.getCanExec(),n.getCanExec()))
//    {noChanges=false;addChange(jo, CAN_EXEC_FIELD, o.getCanExec(), n.getCanExec());}
//    if (!Objects.equals(o.getCanRunBatch(),n.getCanRunBatch()))
//    {noChanges=false;addChange(jo, CAN_RUN_BATCH_FIELD, o.getCanRunBatch(), n.getCanRunBatch());}
//    if (!Objects.equals(o.getMpiCmd(),n.getMpiCmd()))
//    {noChanges=false;addChange(jo, MPI_CMD_FIELD, o.getMpiCmd(), n.getMpiCmd());}
//    if (!Objects.equals(o.getJobWorkingDir(),n.getJobWorkingDir()))
//    {noChanges=false;addChange(jo, JOB_WORKING_DIR_FIELD, o.getJobWorkingDir(), n.getJobWorkingDir());}
//    if (!Objects.equals(o.getJobMaxJobs(),n.getJobMaxJobs()))
//    {noChanges=false;addChange(jo, JOB_MAX_JOBS_FIELD, o.getJobMaxJobs(), n.getJobMaxJobs());}
//    if (!Objects.equals(o.getJobMaxJobsPerUser(),n.getJobMaxJobsPerUser()))
//    {noChanges=false;addChange(jo, JOB_MAX_JOBS_PER_USER_FIELD, o.getJobMaxJobsPerUser(), n.getJobMaxJobsPerUser());}
//    if (!Objects.equals(o.getBatchScheduler(),n.getBatchScheduler()))
//    {noChanges=false;addChange(jo, BATCH_SCHEDULER_FIELD, o.getBatchScheduler().name(), n.getBatchScheduler().name());}
//    if (!Objects.equals(o.getBatchDefaultLogicalQueue(),n.getBatchDefaultLogicalQueue()))
//    {noChanges=false;addChange(jo, BATCH_DEFAULT_LOGICAL_QUEUE_FIELD, o.getBatchDefaultLogicalQueue(), n.getBatchDefaultLogicalQueue());}
//    if (!Objects.equals(o.getBatchSchedulerProfile(),n.getBatchSchedulerProfile()))
//    {noChanges=false;addChange(jo, BATCH_SCHEDULER_PROFILE_FIELD, o.getBatchSchedulerProfile(), n.getBatchSchedulerProfile());}
    if (!(o.isDeleted() == n.isDeleted()))
    {noChanges=false;addChange(jo, DELETED_FIELD, o.isDeleted(), n.isDeleted());}

    // ------------------------------------------------------
    // Following attributes require more complex handling
    // ------------------------------------------------------
//    // TODO JOB_RUNTIMES - JobRuntime supports equals so Objects.equals should do something sensible,
//    //  but order will be important.
//    if (!Objects.equals(o.getJobRuntimes(),n.getJobRuntimes()))
//    {noChanges=false;addChange(jo, JOB_RUNTIMES_FIELD, o.getJobRuntimes(), n.getJobRuntimes());}
////    // JOB_RUNTIMES If it is a patch and the patch value was null then no need to compare
////    if (!isPatch || p.getJobRuntimes() != null)
////    {
////      if (!compareRuntimes(o.getJobRuntimes(), n.getJobRuntimes()))
////      {
////        noChanges = false;
////        addChange(jo, JOB_RUNTIMES_FIELD, o.getJobRuntimes(), n.getJobRuntimes());
////      }
////    }
//    // TODO JOB_ENV_VARIABLES
//    if (!Objects.equals(o.getJobEnvVariables(),n.getJobEnvVariables()))
//    {noChanges=false;addChange(jo, JOB_ENV_VARIABLES_FIELD, o.getJobEnvVariables(), n.getJobEnvVariables());}
//// TODO   // JOB_ENV_VARIABLES If it is a patch and the patch value was null then no need to compare
////    if (!isPatch || p.getJobEnvVariables() != null)
////    {
////      if (!compareKeyPairs(o.getJobEnvVariables(), n.getJobEnvVariables()))
////      {
////        noChanges = false;
////        addChange(jo, JOB_ENV_VARIABLES_FIELD, o.getJobEnvVariables(), n.getJobEnvVariables());
////      }
////    }
//
//    // TODO BATCH_LOGICAL_QUEUES
//    if (!Objects.equals(o.getBatchLogicalQueues(),n.getBatchLogicalQueues()))
//    {noChanges=false;addChange(jo, BATCH_LOGICAL_QUEUES_FIELD, o.getBatchLogicalQueues(), n.getBatchLogicalQueues());}
//
//    // TODO JOB_CAPABILITIES
//    if (!Objects.equals(o.getJobCapabilities(),n.getJobCapabilities()))
//    {noChanges=false;addChange(jo, JOB_CAPABILITIES_FIELD, o.getJobCapabilities(), n.getJobCapabilities());}
//
    // TAGS - If it is a patch and the patch value was null then no need to compare
    //   i.e. if not a patch or patch value was not null then do need to compare.
    // Since TAGS are just strings we can use Objects.equals()
    if (!isPatch || p.getTags() != null)
    {
      // Sort so it does not matter if order is different
      List<String> oldSortedTags = Arrays.asList(o.getTags());
      List<String> newSortedTags = Arrays.asList(n.getTags());
      Collections.sort(oldSortedTags);
      Collections.sort(newSortedTags);
      if (!Objects.equals(oldSortedTags, newSortedTags))
      {
        noChanges = false;
        addChange(jo, TAGS_FIELD, oldSortedTags, newSortedTags);
      }
    }

    // NOTES - If it is a patch and the patch value was null then no need to compare
    if (!isPatch || p.getNotes() != null)
    {
      if (!compareNotes(o.getNotes(), n.getNotes()))
      {
        noChanges=false;
        addChange(jo, NOTES_FIELD, (JsonObject) o.getNotes(), (JsonObject) n.getNotes());
      }
    }

    // If nothing has changed we are done.
    if (noChanges) return null;

    var joFinal = new JSONObject();
    joFinal.put("AppId", o.getId());
    joFinal.put("AppVersion", o.getVersion());
    joFinal.put("AttributeChanges", jo);
    return joFinal.toString();
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
}
