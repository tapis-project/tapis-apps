package edu.utexas.tacc.tapis.apps.api.resources;

import static edu.utexas.tacc.tapis.apps.model.App.ID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.VERSION_FIELD;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import edu.utexas.tacc.tapis.apps.api.responses.RespAppsShare;
import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.AppShare;
import edu.utexas.tacc.tapis.apps.service.AppsService;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.RespResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;

/*
 * JAX-RS REST resource for Tapis App share
 *  NOTE: For OpenAPI spec please see repo openapi-apps file AppsAPI.yaml
 * Annotations map HTTP verb + endpoint to method invocation.
 * Permissions are stored in the Security Kernel
 *
 */
@Path("/v3/apps")
public class ShareResource {
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(ShareResource.class);

  private static final String APPLICATIONS_SVC = StringUtils.capitalize(TapisConstants.SERVICE_NAME_APPS);

  //Json schema resource files.
  private static final String SHARE_APPS_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/ShareAppsRequest.json";
  
  // Message keys
  private static final String TAPIS_FOUND = "TAPIS_FOUND";
  private static final String NOT_FOUND = "APPAPI_NOT_FOUND";
  private static final String INVALID_JSON_INPUT = "NET_INVALID_JSON_INPUT";
  private static final String JSON_VALIDATION_ERR = "TAPIS_JSON_VALIDATION_ERROR";
  private static final String UPDATED = "APPAPI_UPDATED";

  // Always return a nicely formatted response
  private static final boolean PRETTY = true;

  // Top level summary attributes to be included by default in some cases.
  public static final List<String> SUMMARY_ATTRS = new ArrayList<>(List.of(ID_FIELD, VERSION_FIELD, OWNER_FIELD));

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  @Context
  private HttpHeaders _httpHeaders;
  @Context
  private Application _application;
  @Context
  private UriInfo _uriInfo;
  @Context
  private SecurityContext _securityContext;
  @Context
  private ServletContext _servletContext;
  @Context
  private Request _request;

  // **************** Inject Services using HK2 ****************
  @Inject
  private AppsService appsService;

  
  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************
  /**
   * getShare
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return Response with share information object as the result
   */
  @GET
  @Path("/share/{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getShareApp(@PathParam("appId") String appId,
                            @Context SecurityContext securityContext)
  {
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    //RespAbstract resp1;
    AppShare appShare;
    
    try
    {
      // Retrieve App share information
      appShare = appsService.getAppShare(rUser, appId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_GET_SHR_ERR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // Resource not found
    if (appShare == null) {
     String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // ---------------------------- Success -------------------------------
    // Success means we retrieved the sharing information.
    RespAppsShare resp1 = new RespAppsShare(appShare);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "AppShare", appId), resp1);
  }
  
  /**
   * Create or update sharing information for an app
   * @param appId - name of the app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @POST
  @Path("/share/{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response shareApp(@PathParam("appId") String appId,
                              InputStream payloadStream,
                              @Context SecurityContext securityContext)
  {
    String opName = "createUpdateShare";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { 
      rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e) {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, SHARE_APPS_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e) {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    
    // ------------------------- Create a AppsShare from the json and validate constraints -------------------------
    AppShare appsShare;
    try { appsShare = TapisGsonUtils.getGson().fromJson(rawJson, AppShare.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PUT_TRACE", rUser, rawJson));
    
    try
    {
      // Retrieve App share information
      appsService.shareApp(rUser, appId, appsShare);
    } catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e) {
      msg = ApiUtils.getMsgAuth("APPAPI_UPD_SHR_ERR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, appId, opName), resp1);
  }
  
  /**
   * App unshare
   * @param appId - name of the app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @POST
  @Path("/unshare/{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response unshareApp(@PathParam("appId") String appId,
                              InputStream payloadStream,
                              @Context SecurityContext securityContext)
  {
    String opName = "unshare";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { 
      rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e) {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, SHARE_APPS_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e) {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    
    // ------------------------- Create a AppsShare from the json and validate constraints -------------------------
    AppShare appsShare;
    try { appsShare = TapisGsonUtils.getGson().fromJson(rawJson, AppShare.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PUT_TRACE", rUser, rawJson));
    
    try
    {
      // Unshare App
      appsService.unshareApp(rUser, appId, appsShare);
    } catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e) {
      msg = ApiUtils.getMsgAuth("APPAPI_UPD_SHR_ERR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, appId, opName), resp1);
  }
  
  /**
   * Sharing an app publicly
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @POST
  @Path("/share_public/{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response shareAppPublicly(@PathParam("appId") String appId,
                              @Context SecurityContext securityContext)
  {
    String opName = "sharePublicly";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PUT_TRACE", rUser));
    
    String msg;
    try
    {
      // Share App publicly
      appsService.shareAppPublicly(rUser, appId);
    } catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e) {
      msg = ApiUtils.getMsgAuth("APPAPI_UPD_SHR_ERR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, appId, opName), resp1);
  }
  
  /**
   * Sharing an app publicly
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @POST
  @Path("/unshare_public/{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response unshareAppPublicly(@PathParam("appId") String appId,
                              @Context SecurityContext securityContext)
  {
    String opName = "unsharePublicly";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PUT_TRACE", rUser));
    
    String msg;
    try
    {
      // Share App publicly
      appsService.unshareAppPublicly(rUser, appId);
    } catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e) {
      msg = ApiUtils.getMsgAuth("APPAPI_UPD_SHR_ERR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, appId, opName), resp1);
  }
  
  /**
   * Create an OK response given message and base response to put in result
   * @param msg - message for resp.message
   * @param resp - base response (the result)
   * @return - Final response to return to client
   */
  private static Response createSuccessResponse(Status status, String msg, RespAbstract resp)
  {
    return Response.status(status).entity(TapisRestUtils.createSuccessResponse(msg, PRETTY, resp)).build();
  }
}
