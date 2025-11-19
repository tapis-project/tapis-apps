package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

import static edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.getServiceTenantId;
import static edu.utexas.tacc.tapis.apps.service.AppsServiceImpl.getServiceUserId;

/*
   Utility class containing general use methods needed by the service implementation.
 */
public class AppUtils
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger log = LoggerFactory.getLogger(AppUtils.class);

  // Connection timeouts for SKClient
  static final int SK_READ_TIMEOUT_MS = 20000;
  static final int SK_CONN_TIMEOUT_MS = 20000;

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
  @Inject
  private AppsDao dao;
  @Inject
  private ServiceClients serviceClients;

  /* **************************************************************************** */
  /*                                Public Methods                                */
  /* **************************************************************************** */

  /* **************************************************************************** */
  /*                                Package-Private Methods                       */
  /* **************************************************************************** */

  /**
   * Get Security Kernel client with obo tenant and user set to the service tenant and user.
   * I.e. this is a client where the service calls SK as itself.
   * Note: Apps service always calls SK as itself.
   * Note: The ServiceClients class does caching
   * @return SK client
   * @throws TapisException - for Tapis related exceptions
   */
  SKClient getSKClient(ResourceRequestUser rUser) throws TapisException
  {
    SKClient skClient;
    String oboUser = getServiceUserId();
    String oboTenant = getServiceTenantId();
    try { skClient = serviceClients.getClient(oboUser, oboTenant, SKClient.class); }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SECURITY, oboTenant, oboUser);
      throw new TapisException(msg, e);
    }
    if (skClient == null)
    {
      String msg = LibUtils.getMsgAuth("APPLIB_SVC_CLIENT_NULL", rUser, TapisConstants.SERVICE_NAME_SECURITY, oboTenant, oboUser);
      throw new TapisException(msg);
    }
    skClient.setReadTimeout(SK_READ_TIMEOUT_MS);
    skClient.setConnectTimeout(SK_CONN_TIMEOUT_MS);
    return skClient;
  }

  /**
   * Construct message containing list of errors
   */
  static String getListOfErrors(ResourceRequestUser rUser, String appId, List<String> msgList)
  {
    var sb = new StringBuilder(LibUtils.getMsgAuth("APPLIB_CREATE_INVALID_ERRORLIST", rUser, appId));
    sb.append(System.lineSeparator());
    if (msgList == null || msgList.isEmpty()) return sb.toString();
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

}
