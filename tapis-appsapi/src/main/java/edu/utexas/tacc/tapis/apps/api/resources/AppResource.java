package edu.utexas.tacc.tapis.apps.api.resources;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
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
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.dto.ResponseWrapper;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.apps.api.responses.RespAppsArray;
import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.utils.RestUtils;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.RespResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultResourceUrl;
import edu.utexas.tacc.tapis.apps.api.requests.ReqCreateApp;
import edu.utexas.tacc.tapis.apps.api.requests.ReqUpdateApp;
import edu.utexas.tacc.tapis.apps.api.responses.RespApp;
import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.service.AppsService;

/*
 * JAX-RS REST resource for a Tapis App (edu.utexas.tacc.tapis.apps.model.App)
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see file located in repo tapis-client-java at apps-client/AppsAPI.yaml
 *
 * NOTE: The "pretty" query parameter is available for all endpoints. It is processed in
 *       QueryParametersRequestFilter.java.
 */
@Path("/v3/apps")
public class AppResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(AppResource.class);

  // Json schema resource files.
  private static final String FILE_APP_CREATE_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppCreateRequest.json";
  private static final String FILE_APP_UPDATE_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppUpdateRequest.json";
  private static final String FILE_APP_SEARCH_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/AppSearchRequest.json";

  // Field names used in Json
  private static final String NAME_FIELD = "name";
  private static final String VERSION_FIELD = "version";
  private static final String NOTES_FIELD = "notes";
  private static final String APP_TYPE_FIELD = "appType";
  private static final String SEARCH_FIELD = "search";

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  /* Jax-RS context dependency injection allows implementations of these abstract
   * types to be injected (ch 9, jax-rs 2.0):
   *
   *      javax.ws.rs.container.ResourceContext
   *      javax.ws.rs.core.Application
   *      javax.ws.rs.core.HttpHeaders
   *      javax.ws.rs.core.Request
   *      javax.ws.rs.core.SecurityContext
   *      javax.ws.rs.core.UriInfo
   *      javax.ws.rs.core.Configuration
   *      javax.ws.rs.ext.Providers
   *
   * In a servlet environment, Jersey context dependency injection can also
   * initialize these concrete types (ch 3.6, jersey spec):
   *
   *      javax.servlet.HttpServletRequest
   *      javax.servlet.HttpServletResponse
   *      javax.servlet.ServletConfig
   *      javax.servlet.ServletContext
   *
   * Inject takes place after constructor invocation, so fields initialized in this
   * way can not be accessed in constructors.
   */
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
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson, msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_CREATE_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg("TAPIS_JSON_VALIDATION_ERROR", e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    ReqCreateApp req;
    // ------------------------- Create an app from the json and validate constraints -------------------------
    try {
      req = TapisGsonUtils.getGson().fromJson(rawJson, ReqCreateApp.class);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // Create an app from the request
    App app = createAppFromRequest(req);
    // Fill in defaults and check constraints on App attributes
    resp = validateApp(app, authenticatedUser, prettyPrint);
    if (resp != null) return resp;

    // Extract Notes from the raw json.
    Object notes = extractNotes(rawJson);
    app.setNotes(notes);

    // TODO: If needed scrub out any secretes
    String scrubbedJson = rawJson;
    // ---------------------------- Make service call to create the app -------------------------------
    // Update tenant name and pull out app name for convenience
    app.setTenant(authenticatedUser.getTenantId());
    String appName = app.getId();
    try
    {
      appsService.createApp(authenticatedUser, app, scrubbedJson);
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("APPLIB_APP_EXISTS"))
      {
        // IllegalStateException with msg containing APP_EXISTS indicates object exists - return 409 - Conflict
        msg = ApiUtils.getMsgAuth("APPAPI_APP_EXISTS", authenticatedUser, appName);
        _log.warn(msg);
        return Response.status(Status.CONFLICT).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
      else if (e.getMessage().contains("APPLIB_UNAUTH"))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth("APPAPI_APP_UNAUTH", authenticatedUser, appName, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid App was passed in
        msg = ApiUtils.getMsgAuth("APPAPI_CREATE_ERROR", authenticatedUser, appName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth("APPAPI_CREATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_CREATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success ------------------------------- 
    // Success means the object was created.
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString() + "/" + appName;
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return Response.status(Status.CREATED).entity(TapisRestUtils.createSuccessResponse(
      ApiUtils.getMsgAuth("APPAPI_CREATED", authenticatedUser, appName), prettyPrint, resp1)).build();
  }

  /**
   * Update an app
   * @param appName - name of the app
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PATCH
  @Path("{appName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateApp(@PathParam("appName") String appName,
                               InputStream payloadStream,
                               @Context SecurityContext securityContext)
  {
    String opName = "updateApp";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson, msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_UPDATE_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg("TAPIS_JSON_VALIDATION_ERROR", e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ------------------------- Create a PatchApp from the json and validate constraints -------------------------
    ReqUpdateApp req;
    try {
      req = TapisGsonUtils.getGson().fromJson(rawJson, ReqUpdateApp.class);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    PatchApp patchApp = createPatchAppFromRequest(req, authenticatedUser.getTenantId(), appName);

    // Extract Notes from the raw json.
    Object notes = extractNotes(rawJson);
    patchApp.setNotes(notes);

    // No attributes are required. Constraints validated and defaults filled in on server side.
    // No secrets in PatchApp so no need to scrub

    // ---------------------------- Make service call to update the app -------------------------------
    try
    {
      appsService.updateApp(authenticatedUser, patchApp, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", authenticatedUser, appName);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("APPLIB_UNAUTH"))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth("APPAPI_APP_UNAUTH", authenticatedUser, appName, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PatchApp was passed in
        msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(
            ApiUtils.getMsgAuth("APPAPI_UPDATED", authenticatedUser, appName), prettyPrint, resp1)).build();
  }

  /**
   * Change owner of an app
   * @param appName - name of the app
   * @param userName - name of the new owner
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @POST
  @Path("{appName}/changeOwner/{userName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeAppOwner(@PathParam("appName") String appName,
                                    @PathParam("userName") String userName,
                                    @Context SecurityContext securityContext)
  {
    String opName = "changeAppOwner";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ---------------------------- Make service call to update the app -------------------------------
    int changeCount;
    String msg;
    try
    {
      changeCount = appsService.changeAppOwner(authenticatedUser, appName, userName);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", authenticatedUser, appName);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("APPLIB_UNAUTH"))
      {
        // IllegalStateException with msg containing APP_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth("APPAPI_APP_UNAUTH", authenticatedUser, appName, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PatchApp was passed in
        msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_UPDATE_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(
            ApiUtils.getMsgAuth("APPAPI_UPDATED", authenticatedUser, appName), prettyPrint, resp1)).build();
  }

  /**
   * getApp
   * @param appName - name of the app
   * @param securityContext - user identity
   * @return Response with app object as the result
   */
  @GET
  @Path("{appName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApp(@PathParam("appName") String appName,
                         @Context SecurityContext securityContext)
  {
    String opName = "getApp";
    if (_log.isTraceEnabled()) logRequest(opName);

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    App app;
    try
    {
      app = appsService.getApp(authenticatedUser, appName, false);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_GET_NAME_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // Resource was not found.
    if (app == null)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_NOT_FOUND", authenticatedUser, appName);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the app information.
    RespApp resp1 = new RespApp(app);
    return createSuccessResponse(MsgUtils.getMsg("TAPIS_FOUND", "App", appName), resp1);
  }

  /**
   * getApps
   * Retrieve all apps accessible by requester and matching any search conditions provided as a single
   * search query parameter.
   * @param searchStr -  List of strings indicating search conditions to use when retrieving results
   * @param securityContext - user identity
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApps(@QueryParam("search") String searchStr,
                          @Context SecurityContext securityContext)
  {
    String opName = "getApps";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    List<String> searchList = null;
    try
    {
      // Extract the search conditions and validate their form. Back end will handle translating LIKE wildcard
      //   characters (* and !) and dealing with special characters in values.
      searchList = SearchUtils.extractAndValidateSearchList(searchStr);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_SEARCH_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    if (searchList != null && !searchList.isEmpty()) _log.debug("Using searchList. First value = " + searchList.get(0));

    // ------------------------- Retrieve all records -----------------------------
    List<App> apps;
    try { apps = appsService.getApps(authenticatedUser, searchList); }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_SELECT_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    if (apps == null) apps = Collections.emptyList();
    int cnt = apps.size();
    RespAppsArray resp1 = new RespAppsArray(apps);
    return createSuccessResponse(MsgUtils.getMsg("TAPIS_FOUND", "Apps", cnt + " items"), resp1);
  }

  /**
   * searchAppsQueryParameters
   * Dedicated search endpoint for App resource. Search conditions provided as query parameters.
   * @param securityContext - user identity
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @GET
  @Path("search/apps")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchAppsQueryParameters(@Context SecurityContext securityContext)
  {
    String opName = "searchAppsGet";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

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
      String msg = ApiUtils.getMsgAuth("APPAPI_SEARCH_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(Response.Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    if (searchList != null && !searchList.isEmpty()) _log.debug("Using searchList. First value = " + searchList.get(0));

    // ------------------------- Retrieve all records -----------------------------
    List<App> apps;
    try { apps = appsService.getApps(authenticatedUser, searchList); }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_SELECT_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    if (apps == null) apps = Collections.emptyList();
    int cnt = apps.size();
    RespAppsArray resp1 = new RespAppsArray(apps);
    return createSuccessResponse(MsgUtils.getMsg("TAPIS_FOUND", "Apps", cnt + " items"), resp1);
  }

  /**
   * searchAppsRequestBody
   * Dedicated search endpoint for App resource. Search conditions provided in a request body.
   * Request body contains an array of strings that are concatenated to form the full SQL-like search string.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return - list of apps accessible by requester and matching search conditions.
   */
  @POST
  @Path("search/apps")
//  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchAppsRequestBody(InputStream payloadStream,
                                           @Context SecurityContext securityContext)
  {
    String opName = "searchAppsPost";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson, msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_APP_SEARCH_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg("TAPIS_JSON_VALIDATION_ERROR", e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // Construct final SQL-like search string using the json
    // When put together full string must be a valid SQL-like where clause. This will be validated in the service call.
    // Not all SQL syntax is supported. See SqlParser.jj in tapis-shared-searchlib.
    String searchStr;
    try
    {
      searchStr = SearchUtils.getSearchFromRequestJson(rawJson);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg("NET_INVALID_JSON_INPUT", opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    _log.debug("Using search string: " + searchStr);

    // ------------------------- Retrieve all records -----------------------------
    List<App> apps;
    try { apps = appsService.getAppsUsingSqlSearchStr(authenticatedUser, searchStr); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_SELECT_ERROR", authenticatedUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    if (apps == null) apps = Collections.emptyList();
    int cnt = apps.size();
    RespAppsArray resp1 = new RespAppsArray(apps);
    return createSuccessResponse(MsgUtils.getMsg("TAPIS_FOUND", "Apps", cnt + " items"), resp1);
  }

  /**
   * deleteAppByName
   * @param appName - name of the app to delete
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("{appName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
// TODO Add query parameter "confirm" which must be set to true since this is an operation that cannot be undone by a user
  public Response deleteAppByName(@PathParam("appName") String appName,
                                     @Context SecurityContext securityContext)
  {
    String opName = "deleteAppByName";
    // Trace this request.
    if (_log.isTraceEnabled()) logRequest(opName);

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    int changeCount;
    try
    {
      changeCount = appsService.softDeleteAppByName(authenticatedUser, appName);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("APPAPI_DELETE_NAME_ERROR", authenticatedUser, appName, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we deleted the app.
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(
      MsgUtils.getMsg("TAPIS_DELETED", "App", appName), prettyPrint, resp1)).build();
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

  /**
   * Create an app from a ReqCreateApp
   */
  private static App createAppFromRequest(ReqCreateApp req)
  {
    var app = new App(-1, null, req.id, req.version, req.description, req.appType, req.owner, req.enabled,
                      req.isInteractive, req.containerized, req.containerRuntime, req.containerImage, req.command,
                      req.dynamicExecSystem, req.execSystemConstraints, req.execSystemId, req.execSystemExecDir,
                      req.execSystemInputDir, req.execSystemOutputDir, req.execSystemLogicalQueue,
                      req.archiveSystemId, req.archiveSystemDir,
                      req.archiveOnAppError, req.jobDescription, req.maxJobs, req.maxJobsPerUser, req.nodeCount,
                      req.coresPerNode, req.memoryMB, req.maxMinutes, req.archiveIncludes, req.archiveExcludes,
                      req.tags, req.notes, req.refImportId, false, null, null);
//    app.setJobCapabilities(req.jobCapabilities);
    return app;
  }

  /**
   * Create a PatchApp from a ReqUpdateApp
   */
  private static PatchApp createPatchAppFromRequest(ReqUpdateApp req, String tenantName, String appName)
  {
    PatchApp patchApp = new PatchApp(req.version, req.description, req.enabled,
//            req.jobCapabilities,
            req.tags, req.notes);
    // Update tenant name and app name
    patchApp.setTenant(tenantName);
    patchApp.setName(appName);
    return patchApp;
  }

  /**
   * Fill in defaults and check constraints on App attributes
   * Check values. name must be set.
   * Collect and report as many errors as possible so they can all be fixed before next attempt
   * NOTE: JsonSchema validation should handle some of these checks but we check here again just in case
   *
   * @return null if OK or error Response
   */
  private static Response validateApp(App app, AuthenticatedUser authenticatedUser, boolean prettyPrint)
  {
    // Make sure owner, notes and tags are all set
    App app1 = App.checkAndSetDefaults(app);

    String name = app1.getId();
    String msg;
    var errMessages = new ArrayList<String>();
    if (StringUtils.isBlank(app1.getId()))
    {
      msg = ApiUtils.getMsg("APPAPI_CREATE_MISSING_ATTR", NAME_FIELD);
      errMessages.add(msg);
    }
    if (StringUtils.isBlank(app1.getVersion()))
    {
      msg = ApiUtils.getMsg("APPAPI_CREATE_MISSING_ATTR", VERSION_FIELD);
      errMessages.add(msg);
    }
    if (app1.getAppType() == null)
    {
      msg = ApiUtils.getMsg("APPAPI_CREATE_MISSING_ATTR", APP_TYPE_FIELD);
      errMessages.add(msg);
    }

    // If validation failed log error message and return response
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(errMessages, authenticatedUser, name);
      _log.error(allErrors);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(allErrors, prettyPrint)).build();
    }
    return null;
  }

  /**
   * Extract notes from the incoming json
   */
  private static Object extractNotes(String rawJson)
  {
    Object notes = App.DEFAULT_NOTES;
    // Check inputs
    if (StringUtils.isBlank(rawJson)) return notes;
    // Turn the request string into a json object and extract the notes object
    JsonObject topObj = TapisGsonUtils.getGson().fromJson(rawJson, JsonObject.class);
    if (!topObj.has(NOTES_FIELD)) return notes;
    notes = topObj.getAsJsonObject(NOTES_FIELD);
    return notes;
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(List<String> msgList, AuthenticatedUser authenticatedUser, Object... parms) {
    if (msgList == null || msgList.isEmpty()) return "";
    var sb = new StringBuilder(ApiUtils.getMsgAuth("APPAPI_CREATE_INVALID_ERRORLIST", authenticatedUser, parms));
    sb.append(System.lineSeparator());
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  private void logRequest(String opName) {
    String msg = MsgUtils.getMsg("TAPIS_TRACE_REQUEST", getClass().getSimpleName(), opName,
            "  " + _request.getRequestURL());
    _log.trace(msg);
  }

  /**
   * Create an OK response given message and base response to put in result
   * @param msg - message for resp.message
   * @param resp - base response (the result)
   * @return - Final response to return to client
   */
  private static Response createSuccessResponse(String msg, RespAbstract resp)
  {
    resp.message = msg;
    resp.status = ResponseWrapper.RESPONSE_STATUS.success.name();
    resp.version = TapisUtils.getTapisVersion();
    return Response.ok(resp).build();
  }
}
