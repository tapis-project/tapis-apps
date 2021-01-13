package edu.utexas.tacc.tapis.apps.api.utils;

import com.google.gson.JsonElement;
import edu.utexas.tacc.tapis.apps.api.model.ArgMetaSpec;
import edu.utexas.tacc.tapis.apps.api.model.ArgSpec;
import edu.utexas.tacc.tapis.apps.api.model.FileInputDefinition;
import edu.utexas.tacc.tapis.apps.api.model.KeyValuePair;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.FileInput;
import edu.utexas.tacc.tapis.apps.model.NotificationSubscription;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.apps.service.AppsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/*
   Utility class containing general use static methods.
   This class is non-instantiable
 */
public class ApiUtils
{
  // Private constructor to make it non-instantiable
  private ApiUtils() { throw new AssertionError(); }

  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(ApiUtils.class);

  // Location of message bundle files
  private static final String MESSAGE_BUNDLE = "edu.utexas.tacc.tapis.apps.api.AppApiMessages";

  /* **************************************************************************** */
  /*                                Public Methods                                */
  /* **************************************************************************** */

  /**
   * Get a localized message using the specified key and parameters. Locale is null.
   * Fill in first 4 parameters with user and tenant info from AuthenticatedUser
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param key - Key used to lookup message in properties file.
   * @param parms - Parameters for template variables in message
   * @return Resulting message
   */
  public static String getMsgAuth(String key, AuthenticatedUser authUser, Object... parms)
  {
    // Construct new array of parms. This appears to be most straightforward approach to modify and pass on varargs.
    var newParms = new Object[4 + parms.length];
    newParms[0] = authUser.getTenantId();
    newParms[1] = authUser.getName();
    newParms[2] = authUser.getOboTenantId();
    newParms[3] = authUser.getOboUser();
    System.arraycopy(parms, 0, newParms, 4, parms.length);
    return getMsg(key, newParms);
  }

  /**
   * Get a localized message using the specified key and parameters. Locale is null.
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param key - Key used to lookup message in properties file.
   * @param parms - Parameters for template variables in message
   * @return Resulting message
   */
  public static String getMsg(String key, Object... parms)
  {
    return getMsg(key, null, parms);
  }

  /**
   * Get a localized message using the specified locale, key and parameters.
   * If there is a problem an error is logged and a special message is constructed with as much info as can be provided.
   * @param locale - Locale to use when building message. If null use default locale
   * @param key - Key used to lookup message in properties file.
   * @param parms - Parameters for template variables in message
   * @return Resulting message
   */
  public static String getMsg(String key, Locale locale, Object... parms)
  {
    // TODO: Pull tenant name from thread context and include it in the message
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

  public static String getValS(JsonElement jelem, String defaultVal)
  {
    if (jelem == null) return defaultVal;
    else return jelem.getAsString();
  }

  public static Response checkContext(TapisThreadContext threadContext, boolean prettyPrint)
  {
    // Validate call checks for tenantId, user and accountType
    // If all OK return null, else return error response.
    if (threadContext.validate()) return null;

    String msg = MsgUtils.getMsg("TAPIS_INVALID_THREADLOCAL_VALUE", "validate");
    _log.error(msg);
    return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
  }

  /**
   * Check that app exists
   * @param authenticatedUser - principal user containing tenant and user info
   * @param appName - name of the app to check
   * @param prettyPrint - print flag used to construct response
   * @param opName - operation name, for constructing response msg
   * @return - null if all checks OK else Response containing info
   */
  public static Response checkAppExists(AppsService appsService, AuthenticatedUser authenticatedUser,
                                           String appName, boolean prettyPrint, String opName)
  {
    String msg;
    boolean appExists;
    try { appExists = appsService.checkForApp(authenticatedUser, appName); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_CHECK_ERROR", authenticatedUser, appName, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    if (!appExists)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOAPP", authenticatedUser, appName, opName);
      _log.error(msg);
      return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    return null;
  }

  /**
   * Construct a list of lib model AppArg objects given the request objects
   */
  public static List<AppArg> constructAppArgs(List<ArgSpec> argSpecs)
  {
    var retList = new ArrayList<AppArg>();
    if (argSpecs == null || argSpecs.isEmpty()) return retList;
    for (ArgSpec argSpec : argSpecs)
    {
      ArgMetaSpec meta = argSpec.meta;
      if (meta == null) meta = new ArgMetaSpec();
      String[] kvPairs = ApiUtils.getKeyValuesAsArray(meta.keyValuePairs);
      AppArg appArg = new AppArg(argSpec.arg, meta.name, meta.description, meta.required, kvPairs);
      retList.add(appArg);
    }
    return retList;
  }

  /**
   * Construct a list of lib model FileInput objects given the request objects
   */
  public static List<FileInput> constructFileInputs(List<FileInputDefinition> fileInputDefinitions)
  {
    var retList = new ArrayList<FileInput>();
    if (fileInputDefinitions == null || fileInputDefinitions.isEmpty()) return retList;
    for (FileInputDefinition fid : fileInputDefinitions)
    {
      ArgMetaSpec meta = fid.meta;
      if (meta == null) meta = new ArgMetaSpec();
      String[] kvPairs = ApiUtils.getKeyValuesAsArray(meta.keyValuePairs);
      FileInput fileInput = new FileInput(fid.sourceUrl, fid.targetPath, fid.inPlace,
                                          meta.name, meta.description, meta.required, kvPairs);
      retList.add(fileInput);
    }
    return retList;
  }

  /**
   * Construct a list of model NotificationSubscription objects given the request objects
   */
  public static List<NotificationSubscription> constructNotificationSubscriptions(List<edu.utexas.tacc.tapis.apps.api.model.NotificationSubscription> apiSubscriptions)
  {
    var retList = new ArrayList<NotificationSubscription>();
    if (apiSubscriptions == null || apiSubscriptions.isEmpty()) return retList;
    for (edu.utexas.tacc.tapis.apps.api.model.NotificationSubscription subs : apiSubscriptions)
    {
//    TODO: Add notif mechanisms
      NotificationSubscription modelSubscription = new NotificationSubscription(subs.filter);
      retList.add(modelSubscription);
    }
    return retList;
  }

  /**
   * Return String[] array of key=value given list of KeyValuePair
   */
  public static String[] getKeyValuesAsArray(List<KeyValuePair> kvList)
  {
    if (kvList == null || kvList.size() == 0) return App.EMPTY_STR_ARRAY;
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
}
