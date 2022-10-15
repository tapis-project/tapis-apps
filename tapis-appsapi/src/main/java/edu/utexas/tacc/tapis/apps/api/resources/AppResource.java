package edu.utexas.tacc.tapis.apps.api.resources;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultBoolean;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;
import edu.utexas.tacc.tapis.apps.model.App.JobType;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.api.requests.ReqPostApp;
import edu.utexas.tacc.tapis.apps.api.requests.ReqPutApp;
import edu.utexas.tacc.tapis.apps.api.responses.RespApp;
import edu.utexas.tacc.tapis.apps.api.responses.RespAppHistory;
import edu.utexas.tacc.tapis.apps.api.responses.RespApps;
import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.service.AppsService;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBoolean;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.RespResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultResourceUrl;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_CONTAINERIZED;
import static edu.utexas.tacc.tapis.apps.model.App.ID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.VERSION_FIELD;

/*
 * JAX-RS REST resource for a Tapis App (edu.utexas.tacc.tapis.apps.model.App)
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see repo openapi-apps, file AppsAPI.yaml
 */
@Path("/v3/apps")
public class AppResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(AppResource.class);

  private static final String APPLICATIONS_SVC = StringUtils.capitalize(TapisConstants.SERVICE_NAME_APPS);

  // Json schema resource files.
  private static final String FILE_APP_CREATE_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppPostRequest.json";
  private static final String FILE_APP_PUT_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppPutRequest.json";
  private static final String FILE_APP_UPDATE_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppPatchRequest.json";
  private static final String FILE_APP_SEARCH_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppSearchRequest.json";

  // Message keys
  private static final String INVALID_JSON_INPUT = "NET_INVALID_JSON_INPUT";
  private static final String JSON_VALIDATION_ERR = "TAPIS_JSON_VALIDATION_ERROR";
  private static final String UPDATE_ERR = "APPAPI_UPDATE_ERROR";
  private static final String CREATE_ERR = "APPAPI_CREATE_ERROR";
  private static final String SELECT_ERR = "APPAPI_SELECT_ERROR";
  private static final String LIB_UNAUTH = "APPLIB_UNAUTH";
  private static final String API_UNAUTH = "APPAPI_APP_UNAUTH";
  private static final String TAPIS_FOUND = "TAPIS_FOUND";
  private static final String NOT_FOUND = "APPAPI_NOT_FOUND";
  private static final String UPDATED = "APPAPI_UPDATED";

  // Format strings
  private static final String APPS_CNT_STR = "%d applications";

  // Operation names
  private static final String OP_ENABLE = "enableApp";
  private static final String OP_DISABLE = "disableApp";
  private static final String OP_CHANGEOWNER = "changeAppOwner";
  private static final String OP_DELETE = "deleteApp";
  private static final String OP_UNDELETE = "undeleteApp";

  // Always return a nicely formatted response
  private static final boolean PRETTY = true;

  // Top level summary attributes to be included by default in some cases.
  public static final List<String> SUMMARY_ATTRS =
          new ArrayList<>(List.of(ID_FIELD, VERSION_FIELD, OWNER_FIELD));


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

  private final String className = getClass().getSimpleName();

  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  /**
   * Create an app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createApp(InputStream payloadStream,
                            @Context SecurityContext securityContext)
  {
    String opName = "createApp";
    // Note that although the following approximately 30 lines of code is very similar for many endpoints the slight
    //   variations and use of fetched data makes it difficult to refactor into common routines. Common routines
    //   might make the code even more complex and difficult to follow.
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_CREATE_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    ReqPostApp req;
    // ------------------------- Create an app from the json and validate constraints -------------------------
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPostApp.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, "N/A", "ReqPostApp == null");
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create an app from the request
    App app = createAppFromPostRequest(rUser.getOboTenantId(), req, rawJson);

    // So far no need to scrub out secrets, so scrubbed and raw are the same.
    String scrubbedJson = rawJson;
    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_CREATE_TRACE", rUser, scrubbedJson));

    // Fill in defaults and check constraints on App attributes
    app.setDefaults();
    resp = validateApp(app, rUser);
    if (resp != null) return resp;

    // ---------------------------- Make service call to create the app -------------------------------
    String appId = app.getId();
    try
    {
      appsService.createApp(rUser, app, scrubbedJson);
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("APPLIB_APP_EXISTS"))
      {
        // IllegalStateException with msg containing APP_EXISTS indicates object exists - return 409 - Conflict
        msg = ApiUtils.getMsgAuth("APPAPI_APP_EXISTS", rUser, appId, app.getVersion());
        _log.warn(msg);
        return Response.status(Status.CONFLICT).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, appId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid App was passed in
        msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, appId, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, appId, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success ------------------------------- 
    // Success means the object was created.
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString() + "/" + appId;
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.CREATED, ApiUtils.getMsgAuth("APPAPI_CREATED", rUser, appId), resp1);
  }

  /**
   * Update selected attributes of an app
   * @param appId - id of the app
   * @param appVersion - version of the app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PATCH
  @Path("{appId}/{appVersion}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response patchApp(@PathParam("appId") String appId,
                           @PathParam("appVersion") String appVersion,
                           InputStream payloadStream,
                           @Context SecurityContext securityContext)
  {
    String opName = "patchApp";
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId,"appVersion="+appVersion);

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_UPDATE_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ------------------------- Create a PatchApp from the json -------------------------
    PatchApp patchApp;
    try
    {
      patchApp = TapisGsonUtils.getGson().fromJson(rawJson, PatchApp.class);
      // If json does not contain jobType then set jobType to a special value to indicate it was not present.
      // Turn the request string into a json object and check
      JsonObject topObj = TapisGsonUtils.getGson().fromJson(rawJson, JsonObject.class);
      if (!topObj.has(App.JOB_TYPE_FIELD)) patchApp.setJobType(JobType.UNSET);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PATCH_TRACE", rUser, rawJson));

    // Notes require special handling. Else they end up as a LinkedTreeMap which causes trouble when attempting to
    // convert to a JsonObject.
    patchApp.setNotes(extractNotes(rawJson));

    // No attributes are required. Constraints validated and defaults filled in on server side.
    // No secrets in PatchApp so no need to scrub

    // ---------------------------- Make service call to update the app -------------------------------
    try
    {
      appsService.patchApp(rUser, appId, appVersion, patchApp, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, appId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PatchApp was passed in
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
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
   * Update all updatable attributes of an app
   * @param appId - id of the app
   * @param appVersion - version of the app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PUT
  @Path("{appId}/{appVersion}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response putApp(@PathParam("appId") String appId,
                         @PathParam("appVersion") String appVersion,
                         InputStream payloadStream,
                         @Context SecurityContext securityContext)
  {
    String opName = "putApp";
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId,"appVersion="+appVersion);

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    // NOTE that CREATE and PUT are very similar schemas.
    // Only difference should be for PUT there are no required properties.
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_PUT_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ------------------------- Create an App from the json and validate constraints -------------------------
    ReqPutApp req;
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPutApp.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", rUser, appId, opName, "ReqPutApp == null");
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create an App from the request
    App putApp = createAppFromPutRequest(rUser.getOboTenantId(), appId, appVersion, req, rawJson);

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("APPAPI_PUT_TRACE", rUser, rawJson));

    // Fill in defaults and check constraints on App attributes
    // NOTE: We do not have all the Tapis App attributes yet so we cannot validate it
    putApp.setDefaults();

    // ---------------------------- Make service call to update the app -------------------------------
    try
    {
       appsService.putApp(rUser, putApp, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, appId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PutApp was passed in
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
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
   * Enable an app
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{appId}/enable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response enableApp(@PathParam("appId") String appId,
                            @Context SecurityContext securityContext)
  {
    return postAppSingleUpdate(OP_ENABLE, appId, null, securityContext);
  }

  /**
   * Disable an app
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{appId}/disable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response disableApp(@PathParam("appId") String appId,
                             @Context SecurityContext securityContext)
  {
    return postAppSingleUpdate(OP_DISABLE, appId, null, securityContext);
  }

  /**
   * Delete an app
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{appId}/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteApp(@PathParam("appId") String appId,
                            @Context SecurityContext securityContext)
  {
    return postAppSingleUpdate(OP_DELETE, appId, null, securityContext);
  }

  /**
   * Undelete an app
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{appId}/undelete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response undeleteApp(@PathParam("appId") String appId,
                              @Context SecurityContext securityContext)
  {
    return postAppSingleUpdate(OP_UNDELETE, appId, null, securityContext);
  }

  /**
   * Change owner of an app
   * @param appId - name of the app
   * @param userName - name of the new owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{appId}/changeOwner/{userName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeAppOwner(@PathParam("appId") String appId,
                                 @PathParam("userName") String userName,
                                 @Context SecurityContext securityContext)
  {
    return postAppSingleUpdate(OP_CHANGEOWNER, appId, userName, securityContext);
  }

  /**
   * getAppLatestVersion
   * Retrieve most recently created version of an application.
   * @param appId - name of the app
   * @param requireExecPerm - check for EXECUTE permission as well as READ permission
   * @param securityContext - user identity
   * @return Response with app object as the result
   */
  @GET
  @Path("{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAppLatestVersion(@PathParam("appId") String appId,
                         @QueryParam("requireExecPerm") @DefaultValue("false") boolean requireExecPerm,
                         @Context SecurityContext securityContext)
  {
    String opName = "getAppLatestVersion";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId,"requireExecPerm="+requireExecPerm);

    List<String> selectList = threadContext.getSearchParameters().getSelectList();

    App app;
    try
    {
      app = appsService.getApp(rUser, appId, null, requireExecPerm, null);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_GET_NAME_ERROR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (app == null)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the app information.
    RespApp resp1 = new RespApp(app, selectList);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "App", appId), resp1);
  }

  /**
   * getApp
   * Retrieve specified version of an application.
   * @param appId - name of the app
   * @param appVersion - version of the app
   * @param requireExecPerm - check for EXECUTE permission as well as READ permission
   * @param impersonationId - use provided Tapis username instead of oboUser when checking auth
   * @param securityContext - user identity
   * @return Response with app object as the result
   */
  @GET
  @Path("{appId}/{appVersion}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApp(@PathParam("appId") String appId,
                         @PathParam("appVersion") String appVersion,
                         @QueryParam("requireExecPerm") @DefaultValue("false") boolean requireExecPerm,
                         @QueryParam("impersonationId") String impersonationId,
                         @Context SecurityContext securityContext)
  {
    String opName = "getApp";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(),
                          "appId="+appId, "appVersion="+appVersion,
                          "requireExecPerm="+requireExecPerm, "impersonationId="+impersonationId);

    List<String> selectList = threadContext.getSearchParameters().getSelectList();

    App app;
    try
    {
      app = appsService.getApp(rUser, appId, appVersion, requireExecPerm, impersonationId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_GET_NAME_ERROR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (app == null)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the app information.
    RespApp resp1 = new RespApp(app, selectList);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "App", appId), resp1);
  }

  /**
   * getApps
   * Retrieve all apps accessible by requester and matching any search conditions provided.
   * NOTE: The query parameters search, limit, orderBy, skip, startAfter are all handled in the filter
   *       QueryParametersRequestFilter. No need to use @QueryParam here.
   * @param securityContext - user identity
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApps(@Context SecurityContext securityContext,
                          @QueryParam("showDeleted") @DefaultValue("false") boolean showDeleted)
  {
    String opName = "getApps";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "showDeleted="+showDeleted);

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();

    // ------------------------- Retrieve records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, null, srchParms, showDeleted);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    return successResponse;
  }

  /**
   * searchAppsQueryParameters
   * Dedicated search endpoint for App resource. Search conditions provided as query parameters.
   * @param securityContext - user identity
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @GET
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchAppsQueryParameters(@Context SecurityContext securityContext,
                                            @QueryParam("showDeleted") @DefaultValue("false") boolean showDeleted)
  {
    String opName = "searchAppsGet";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "showDeleted="+showDeleted);

    // Create search list based on query parameters
    // Note that some validation is done for each condition but the back end will handle translating LIKE wildcard
    //   characters (* and !) and deal with escaped characters.
    List<String> searchList;
    try
    {
      searchList = SearchUtils.buildListFromQueryParms(_uriInfo.getQueryParameters());
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_SEARCH_ERROR", rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();
    srchParms.setSearchList(searchList);

    // ------------------------- Retrieve all records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, null, srchParms, showDeleted);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    return successResponse;
  }

  /**
   * searchAppsRequestBody
   * Dedicated search endpoint for App resource. Search conditions provided in a request body.
   * Request body contains an array of strings that are concatenated to form the full SQL-like search string.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @param showDeleted - whether or not to included resources that have been marked as deleted.
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @POST
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchAppsRequestBody(InputStream payloadStream,
                                        @Context SecurityContext securityContext,
                                        @QueryParam("showDeleted") @DefaultValue("false") boolean showDeleted)
  {
    String opName = "searchAppsPost";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "showDeleted="+showDeleted);

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_SEARCH_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Construct final SQL-like search string using the json
    // When put together full string must be a valid SQL-like where clause. This will be validated in the service call.
    // Not all SQL syntax is supported. See SqlParser.jj in tapis-shared-searchlib.
    String sqlSearchStr;
    try
    {
      sqlSearchStr = SearchUtils.getSearchFromRequestJson(rawJson);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();

    // ------------------------- Retrieve all records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, sqlSearchStr, srchParms, showDeleted);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    return successResponse;
  }

  /**
   * isEnabled
   * Check if application is enabled.
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return Response with boolean result
   */
  @GET
  @Path("{appId}/isEnabled")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response isEnabled(@PathParam("appId") String appId,
                         @Context SecurityContext securityContext)
  {
    String opName = "isEnabled";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId);

    boolean isEnabled;
    try
    {
      isEnabled = appsService.isEnabled(rUser, appId);
    }
    catch (NotFoundException e)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_GET_NAME_ERROR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we made the check
    ResultBoolean respResult = new ResultBoolean();
    respResult.aBool = isEnabled;
    RespBoolean resp1 = new RespBoolean(respResult);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "App", appId), resp1);
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

  /**
   * changeOwner, enable, disable, delete and undelete follow same pattern
   * Note that userName only used for changeOwner
   * @param opName Name of operation.
   * @param appId Id of app to update
   * @param userName new owner name for op changeOwner
   * @param securityContext Security context from client call
   * @return Response to be returned to the client.
   */
  private Response postAppSingleUpdate(String opName, String appId, String userName, SecurityContext securityContext)
  {
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
    {
      // NOTE: We deliberately do not check for blank. If empty string passed in we want to record it here.
      if (userName!=null)
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId, "userName="+userName);
      else
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "appId="+appId);
    }

    // ---------------------------- Make service call to update the app -------------------------------
    int changeCount;
    String msg;
    try
    {
      if (OP_ENABLE.equals(opName))
        changeCount = appsService.enableApp(rUser, appId);
      else if (OP_DISABLE.equals(opName))
        changeCount = appsService.disableApp(rUser, appId);
      else if (OP_DELETE.equals(opName))
        changeCount = appsService.deleteApp(rUser, appId);
      else if (OP_UNDELETE.equals(opName))
        changeCount = appsService.undeleteApp(rUser, appId);
      else
        changeCount = appsService.changeAppOwner(rUser, appId, userName);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, appId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates resulting app would be invalid
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, appId, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, appId, opName), resp1);
  }

  /**
   * Create an app from a ReqPostApp
   * Check for req == null should have already been done
   */
  private static App createAppFromPostRequest(String tenantId, ReqPostApp req, String rawJson)
  {
    var jobAttrs = req.jobAttributes;
    if (jobAttrs == null) jobAttrs = new JobAttributes();
    // Extract Notes from the raw json.
    JsonObject notes = extractNotes(rawJson);
    // Create App
    var app = new App(-1, -1, tenantId, req.id, req.version, req.description, req.jobType, req.owner, req.enabled,
          DEFAULT_CONTAINERIZED,  req.runtime, req.runtimeVersion, req.runtimeOptions, req.containerImage,
          req.maxJobs, req.maxJobsPerUser, req.strictFileInputs,
          jobAttrs.description, jobAttrs.dynamicExecSystem, jobAttrs.execSystemConstraints, jobAttrs.execSystemId,
          jobAttrs.execSystemExecDir, jobAttrs.execSystemInputDir, jobAttrs.execSystemOutputDir,
          jobAttrs.execSystemLogicalQueue, jobAttrs.archiveSystemId, jobAttrs.archiveSystemDir, jobAttrs.archiveOnAppError,
          jobAttrs.isMpi, jobAttrs.mpiCmd, jobAttrs.cmdPrefix,
          jobAttrs.parameterSet, jobAttrs.fileInputs, jobAttrs.fileInputArrays, jobAttrs.nodeCount, jobAttrs.coresPerNode,
          jobAttrs.memoryMB, jobAttrs.maxMinutes, jobAttrs.subscriptions, jobAttrs.tags,
          req.tags, notes, null, false, null, null);
    return app;
  }

  /**
   * Create an App from a ReqPutApp
   */
  private static App createAppFromPutRequest(String tenantId, String id, String version, ReqPutApp req, String rawJson)
  {
    var jobAttrs = req.jobAttributes;
    if (jobAttrs == null) jobAttrs = new JobAttributes();
    // Extract Notes from the raw json.
    JsonObject notes = extractNotes(rawJson);

    // NOTE: Following attributes are not updatable and must be filled in on service side.
    String owner = null;
    boolean enabled = true;
    var app = new App(-1, -1, tenantId, id, version, req.description, req.jobType, owner, enabled,
          DEFAULT_CONTAINERIZED,  req.runtime, req.runtimeVersion, req.runtimeOptions, req.containerImage,
          req.maxJobs, req.maxJobsPerUser, req.strictFileInputs,
          jobAttrs.description, jobAttrs.dynamicExecSystem, jobAttrs.execSystemConstraints, jobAttrs.execSystemId,
          jobAttrs.execSystemExecDir, jobAttrs.execSystemInputDir, jobAttrs.execSystemOutputDir,
          jobAttrs.execSystemLogicalQueue, jobAttrs.archiveSystemId, jobAttrs.archiveSystemDir, jobAttrs.archiveOnAppError,
          jobAttrs.isMpi, jobAttrs.mpiCmd, jobAttrs.cmdPrefix,
          jobAttrs.parameterSet, jobAttrs.fileInputs, jobAttrs.fileInputArrays, jobAttrs.nodeCount, jobAttrs.coresPerNode,
          jobAttrs.memoryMB, jobAttrs.maxMinutes, jobAttrs.subscriptions, jobAttrs.tags,
          req.tags, notes, null, false, null, null);
    return app;
  }

  /**
   * Fill in defaults and check constraints on App attributes
   * Check values. Id and version must be set.
   * Collect and report as many errors as possible so they can all be fixed before next attempt
   * NOTE: JsonSchema validation should handle some of these checks but we check here again just in case
   *
   * @return null if OK or error Response
   */
  private static Response validateApp(App app1, ResourceRequestUser rUser)
  {
    // Make call for lib level validation
    List<String> errMessages = app1.checkAttributeRestrictions();

    // Now validate attributes that have special handling at API level.
    // Currently, no additional checks.

    // If validation failed log error message and return response
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(errMessages, rUser, app1.getId());
      _log.error(allErrors);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(allErrors, PRETTY)).build();
    }
    return null;
  }

  /**
   * Extract notes from the incoming json
   * This explicit method to extract is needed because notes is an unstructured object and other seemingly simpler
   * approaches caused problems with the json marshalling. This method ensures notes end up as a JsonObject rather
   * than a LinkedTreeMap.
   */
  private static JsonObject extractNotes(String rawJson)
  {
    JsonObject notes = null;
    // Check inputs
    if (StringUtils.isBlank(rawJson)) return notes;
    // Turn the request string into a json object and extract the notes object
    JsonObject topObj = TapisGsonUtils.getGson().fromJson(rawJson, JsonObject.class);
    if (!topObj.has(App.NOTES_FIELD)) return notes;
    notes = topObj.getAsJsonObject(App.NOTES_FIELD);
    return notes;
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(List<String> msgList, ResourceRequestUser rUser, Object... parms) {
    if (msgList == null || msgList.isEmpty()) return "";
    var sb = new StringBuilder(ApiUtils.getMsgAuth("APPAPI_CREATE_INVALID_ERRORLIST", rUser, parms));
    sb.append(System.lineSeparator());
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   *  Common method to return a list of applications given a search list and search parameters.
   *  srchParms must be non-null
   *  One of srchParms.searchList or sqlSearchStr must be non-null
   */
  private Response getSearchResponse(ResourceRequestUser rUser, String sqlSearchStr,
                                     SearchParameters srchParms, boolean showDeleted)
          throws Exception
  {
    RespAbstract resp1;
    List<App> apps;
    int totalCount = -1;
    String itemCountStr;

    List<String> searchList = srchParms.getSearchList();
    List<String> selectList = srchParms.getSelectList();
    if (selectList == null || selectList.isEmpty()) selectList = SUMMARY_ATTRS;

    // If limit was not specified then use the default
    int limit = (srchParms.getLimit() == null) ? SearchParameters.DEFAULT_LIMIT : srchParms.getLimit();
    // Set some variables to make code easier to read
    int skip = srchParms.getSkip();
    String startAfter = srchParms.getStartAfter();
    boolean computeTotal = srchParms.getComputeTotal();
    String orderBy = srchParms.getOrderBy();
    List<OrderBy> orderByList = srchParms.getOrderByList();

    if (StringUtils.isBlank(sqlSearchStr))
      apps = appsService.getApps(rUser, searchList, limit, orderByList, skip, startAfter, showDeleted);
    else
      apps = appsService.getAppsUsingSqlSearchStr(rUser, sqlSearchStr, limit, orderByList, skip,
                                                  startAfter, showDeleted);
    if (apps == null) apps = Collections.emptyList();
    itemCountStr = String.format(APPS_CNT_STR, apps.size());
    if (computeTotal && limit <= 0) totalCount = apps.size();

    // If we need the count and there was a limit then we need to make a call
    if (computeTotal && limit > 0)
    {
      totalCount = appsService.getAppsTotalCount(rUser, searchList, orderByList, startAfter, showDeleted);
    }

    // ---------------------------- Success -------------------------------
    resp1 = new RespApps(apps, limit, orderBy, skip, startAfter, totalCount, selectList);

    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, APPLICATIONS_SVC, itemCountStr), resp1);
  }
  
  /**
   * getHistory
   * @param appId - name of the app
   * @param securityContext - user identity
   * @return Response with system history object as the result
   */
  @GET
  @Path("{appId}/history")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHistory(@PathParam("appId") String appId,
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
    List<AppHistoryItem> appHistory;
    
    try
    {
      // Retrieve system history List
      appHistory = appsService.getAppHistory(rUser, appId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("SYSAPI_SYS_GET_ERROR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // System not found
    if (appHistory == null || appHistory.size()==0) {
     String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // ---------------------------- Success -------------------------------
    // Success means we retrieved the system history information.
    RespAppHistory resp1 = new RespAppHistory(appHistory);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "AppHistory", appId), resp1);
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
