package edu.utexas.tacc.tapis.apps.api.utils;

import com.google.gson.JsonElement;
import edu.utexas.tacc.tapis.apps.api.model.ArgMetaSpec;
import edu.utexas.tacc.tapis.apps.api.model.ArgSpec;
import edu.utexas.tacc.tapis.apps.api.model.FileInput;
import edu.utexas.tacc.tapis.apps.api.model.KeyValuePair;
import edu.utexas.tacc.tapis.apps.api.model.NotificationMechanism;
import edu.utexas.tacc.tapis.apps.api.model.NotificationSubscription;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.AppArg;
import edu.utexas.tacc.tapis.apps.model.NotifMechanism;
import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
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
   * Return single json element as a string
   * @param jelem Json element
   * @param defaultVal string value to use as a default if element is null
   * @return json element as string
   */
  public static String getValS(JsonElement jelem, String defaultVal)
  {
    if (jelem == null) return defaultVal;
    else return jelem.getAsString();
  }

  /**
   * ThreadContext.validate checks for tenantId, user, accountType, etc.
   * If all OK return null, else return error response.
   *
   * @param threadContext - thread context to check
   * @param prettyPrint - flag for pretty print of response
   * @return null if OK, else error response
   */
  public static Response checkContext(TapisThreadContext threadContext, boolean prettyPrint)
  {
    if (threadContext.validate()) return null;
    String msg = MsgUtils.getMsg("TAPIS_INVALID_THREADLOCAL_VALUE", "validate");
    _log.error(msg);
    return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
  }

  /**
   * Check that app exists
   * @param rUser - principal user containing tenant and user info
   * @param appId - name of the app to check
   * @param prettyPrint - print flag used to construct response
   * @param opName - operation name, for constructing response msg
   * @return - null if all checks OK else Response containing info
   */
  public static Response checkAppExists(AppsService appsService, ResourceRequestUser rUser,
                                           String appId, boolean prettyPrint, String opName)
  {
    String msg;
    boolean appExists;
    try { appExists = appsService.checkForApp(rUser, appId); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_CHECK_ERROR", rUser, appId, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    if (!appExists)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOAPP", rUser, appId, opName);
      _log.error(msg);
      return Response.status(Response.Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    return null;
  }

  /**
   * Build a list of lib model AppArg objects given the request objects
   */
  public static List<AppArg> buildLibAppArgs(List<ArgSpec> argSpecs)
  {
    if (argSpecs == null) return null;
    var retList = new ArrayList<AppArg>();
    if (argSpecs.isEmpty()) return retList;
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
   * Build a list of lib model FileInput objects given the request objects
   */
  public static List<edu.utexas.tacc.tapis.apps.model.FileInput> buildLibFileInputs(List<FileInput> fileInputs)
  {
    if (fileInputs == null) return null;
    var retList = new ArrayList<edu.utexas.tacc.tapis.apps.model.FileInput>();
    if (fileInputs.isEmpty()) return retList;
    for (FileInput fid : fileInputs)
    {
      ArgMetaSpec meta = fid.meta;
      if (meta == null) meta = new ArgMetaSpec();
      String[] kvPairs = ApiUtils.getKeyValuesAsArray(meta.keyValuePairs);
      edu.utexas.tacc.tapis.apps.model.FileInput fileInput = new edu.utexas.tacc.tapis.apps.model.FileInput(fid.sourceUrl, fid.targetPath, fid.inPlace,
                                          meta.name, meta.description, meta.required, kvPairs);
      retList.add(fileInput);
    }
    return retList;
  }

  /**
   * Build a list of lib model subscription objects given the request api model objects
   */
  public static List<NotifSubscription> buildLibNotifSubscriptions(List<NotificationSubscription> apiSubscriptions)
  {
    if (apiSubscriptions == null) return null;
    var retList = new ArrayList<NotifSubscription>();
    if (apiSubscriptions.isEmpty()) return retList;
    for (NotificationSubscription apiSubscription : apiSubscriptions)
    {
      NotifSubscription libSubscription = new NotifSubscription(apiSubscription.filter);
      var apiMechanisms = apiSubscription.notificationMechanisms;
      if (apiMechanisms == null || apiMechanisms.isEmpty()) apiMechanisms = new ArrayList<>();
      var libMechanisms = new ArrayList<NotifMechanism>();
      for (NotificationMechanism apiMech : apiMechanisms)
      {
        var libMech = new NotifMechanism(apiMech.mechanism, apiMech.webhookURL, apiMech.emailAddress);
        libMechanisms.add(libMech);
      }
      libSubscription.setNotificationMechanisms(libMechanisms);
      retList.add(libSubscription);
    }
    return retList;
  }

  // Build a list of api model file inputs based on the lib model objects
  public static List<FileInput> buildApiFileInputs(List<edu.utexas.tacc.tapis.apps.model.FileInput> libFileInputs)
  {
    var retList = new ArrayList<FileInput>();
    if (libFileInputs == null || libFileInputs.isEmpty()) return retList;
    for (edu.utexas.tacc.tapis.apps.model.FileInput libFileInput : libFileInputs)
    {
      ArgMetaSpec meta = new ArgMetaSpec();
      meta.name = libFileInput.getMetaName();
      meta.description = libFileInput.getMetaDescription();
      meta.required = libFileInput.isMetaRequired();
      meta.keyValuePairs = ApiUtils.getKeyValuesAsList(libFileInput.getMetaKeyValuePairs());
      FileInput fid = new FileInput();
      fid.sourceUrl = libFileInput.getSourceUrl();
      fid.targetPath = libFileInput.getTargetPath();
      fid.inPlace = libFileInput.isInPlace();
      fid.meta = meta;
      retList.add(fid);
    }
    return retList;
  }

  // Build a list of api model subscriptions based on the lib model objects
  public static List<NotificationMechanism> buildApiNotifMechanisms(List<NotifMechanism> libMechanisms)
  {
    var retList = new ArrayList<NotificationMechanism>();
    if (libMechanisms == null || libMechanisms.isEmpty()) return retList;
    for (NotifMechanism libMechanism : libMechanisms)
    {
      NotificationMechanism apiMechanism = new NotificationMechanism();
      apiMechanism.mechanism = libMechanism.getMechanism();
      apiMechanism.webhookURL = libMechanism.getWebhookUrl();
      apiMechanism.emailAddress = libMechanism.getEmailAddress();
      retList.add(apiMechanism);
    }
    return retList;
  }

  // Build a list of api model notif mechanisms based on the lib model objects
  public static List<NotificationSubscription> buildApiNotifSubscriptions(List<NotifSubscription> libSubscriptions)
  {
    var retList = new ArrayList<NotificationSubscription>();
    if (libSubscriptions == null || libSubscriptions.isEmpty()) return retList;
    for (NotifSubscription libSubscription : libSubscriptions)
    {
      NotificationSubscription apiSubscription = new NotificationSubscription();
      apiSubscription.filter = libSubscription.getFilter();
      apiSubscription.notificationMechanisms = buildApiNotifMechanisms(libSubscription.getNotificationMechanisms());
      retList.add(apiSubscription);
    }
    return retList;
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
}
