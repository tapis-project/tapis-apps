package edu.utexas.tacc.tapis.apps.api.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.glassfish.grizzly.http.server.Request;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;
import edu.utexas.tacc.tapis.sharedapi.responses.RespNameArray;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultNameArray;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.utils.RestUtils;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App.Permission;
import edu.utexas.tacc.tapis.apps.service.AppsService;

/*
 * JAX-RS REST resource for Tapis App permissions
 *  NOTE: For OpenAPI spec please see repo openapi-apps file AppsAPI.yaml
 * Annotations map HTTP verb + endpoint to method invocation.
 * Permissions are stored in the Security Kernel
 *
 */
@Path("/v3/apps/perms")
public class PermsResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(PermsResource.class);

  // Json schema resource files.
  private static final String FILE_PERMS_REQUEST = "/edu/utexas/tacc/tapis/apps/api/jsonschema/PermsRequest.json";

  // Field names used in Json
  private static final String PERMISSIONS_FIELD = "permissions";

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
   * Assign specified permissions for given app and user.
   * @param payloadStream - request body
   * @return basic response
   */
  @POST
  @Path("/{appName}/user/{userName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response grantUserPerms(@PathParam("appName") String appName,
                                 @PathParam("userName") String userName,
                                 InputStream payloadStream,
                                 @Context SecurityContext securityContext)
  {
    String msg;
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();

    // Trace this request.
    if (_log.isTraceEnabled())
    {
      msg = MsgUtils.getMsg("TAPIS_TRACE_REQUEST", getClass().getSimpleName(), "grantUserPerms",
                                   "  " + _request.getRequestURL());
      _log.trace(msg);
    }

    // Check that we have all we need from the context, tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Check prerequisites -------------------------
    // Check that the app exists
    resp = ApiUtils.checkAppExists(appsService, authenticatedUser, appName, prettyPrint, "grantUserPerms");
    if (resp != null) return resp;

    // Read the payload into a string.
    String json;
    try { json = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_JSON_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ------------------------- Extract and validate payload -------------------------
    var permsList = new HashSet<Permission>();
    resp = checkAndExtractPayload(authenticatedUser, appName, userName, prettyPrint, json, permsList);
    if (resp != null) return resp;

    // ------------------------- Perform the operation -------------------------
    // Make the service call to assign the permissions
    try
    {
      appsService.grantUserPermissions(authenticatedUser, appName, userName, permsList, json);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    String permsListStr = permsList.stream().map(Enum::name).collect(Collectors.joining(","));
    RespBasic resp1 = new RespBasic();
    return Response.status(Status.CREATED)
      .entity(TapisRestUtils.createSuccessResponse(ApiUtils.getMsgAuth("APPAPI_PERMS_GRANTED", authenticatedUser, appName,
                                                                       userName, permsListStr),
                                                   prettyPrint, resp1))
      .build();
  }

  /**
   * getUserPerms
   * @return Response with list of permissions
   */
  @GET
  @Path("/{appName}/user/{userName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserPerms(@PathParam("appName") String appName,
                               @PathParam("userName") String userName,
                               @Context SecurityContext securityContext)
  {
    String msg;
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();

    // Trace this request.
    if (_log.isTraceEnabled())
    {
      msg = MsgUtils.getMsg("TAPIS_TRACE_REQUEST", getClass().getSimpleName(), "getUserPerms",
                                   "  " + _request.getRequestURL());
      _log.trace(msg);
    }

    // Check that we have all we need from the context, the tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Check prerequisites -------------------------
    // Check that the app exists
    resp = ApiUtils.checkAppExists(appsService, authenticatedUser, appName, prettyPrint, "getUserPerms");
    if (resp != null) return resp;

    // ------------------------- Perform the operation -------------------------
    // Make the service call to get the permissions
    Set<Permission> perms;
    try { perms = appsService.getUserPermissions(authenticatedUser, appName, userName); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(RestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    if (perms == null) perms = new HashSet<>();
    ResultNameArray names = new ResultNameArray();
    List<String> permNames = new ArrayList<>();
    for (Permission perm : perms) { permNames.add(perm.name()); }
    names.names = permNames.toArray(new String[0]);
    RespNameArray resp1 = new RespNameArray(names);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(
      MsgUtils.getMsg("TAPIS_FOUND", "App permissions", perms.size() + " items"), prettyPrint, resp1)).build();
  }

  /**
   * Revoke permission for given app and user.
   * @return basic response
   */
  @DELETE
  @Path("/{appName}/user/{userName}/{permission}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeUserPerm(@PathParam("appName") String appName,
                                 @PathParam("userName") String userName,
                                 @PathParam("permission") String permissionStr,
                                 @Context SecurityContext securityContext)
  {
    String msg;
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();

    // Trace this request.
    if (_log.isTraceEnabled())
    {
      msg = MsgUtils.getMsg("TAPIS_TRACE_REQUEST", getClass().getSimpleName(), "revokeUserPerm",
                            "  " + _request.getRequestURL());
      _log.trace(msg);
    }

    // Check that we have all we need from the context, tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Check prerequisites -------------------------
    // Check that the app exists
    resp = ApiUtils.checkAppExists(appsService, authenticatedUser, appName, prettyPrint, "revokeUserPerm");
    if (resp != null) return resp;

    // ------------------------- Perform the operation -------------------------
    // Make the service call to revoke the permissions
    var permsList = new HashSet<Permission>();
    try
    {
      Permission perm = Permission.valueOf(permissionStr);
      permsList.add(perm);
      appsService.revokeUserPermissions(authenticatedUser, appName, userName, permsList, null);
    }
    catch (IllegalArgumentException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ENUM_ERROR", authenticatedUser, appName, userName, permissionStr, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    RespBasic resp1 = new RespBasic();
    return Response.status(Status.CREATED)
      .entity(TapisRestUtils.createSuccessResponse(ApiUtils.getMsgAuth("APPAPI_PERMS_REVOKED", authenticatedUser, appName,
                                                                       userName, permissionStr),
                                                   prettyPrint, resp1))
      .build();
  }

  /**
   * Revoke permissions for given app and user.
   * @param payloadStream - request body
   * @return basic response
   */
  @POST
  @Path("/{appName}/user/{userName}/revoke")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeUserPerms(@PathParam("appName") String appName,
                                  @PathParam("userName") String userName,
                                  InputStream payloadStream,
                                  @Context SecurityContext securityContext)
  {
    String msg;
    // Trace this request.
    if (_log.isTraceEnabled())
    {
      msg = MsgUtils.getMsg("TAPIS_TRACE_REQUEST", getClass().getSimpleName(), "revokeUserPerms",
                            "  " + _request.getRequestURL());
      _log.trace(msg);
    }

    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    boolean prettyPrint = threadContext.getPrettyPrint();
    // Check that we have all we need from the context, tenant name and apiUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, prettyPrint);
    if (resp != null) return resp;

    // Get AuthenticatedUser which contains jwtTenant, jwtUser, oboTenant, oboUser, etc.
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) securityContext.getUserPrincipal();

    // ------------------------- Check prerequisites -------------------------
    // Check that the app exists
    resp = ApiUtils.checkAppExists(appsService, authenticatedUser, appName, prettyPrint, "revokeUserPerms");
    if (resp != null) return resp;

    // Read the payload into a string.
    String json;
    try { json = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_JSON_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ------------------------- Extract and validate payload -------------------------
    var permsList = new HashSet<Permission>();
    resp = checkAndExtractPayload(authenticatedUser, appName, userName, prettyPrint, json, permsList);
    if (resp != null) return resp;

    // ------------------------- Perform the operation -------------------------
    // Make the service call to revoke the permissions
    try
    {
      appsService.revokeUserPermissions(authenticatedUser, appName, userName, permsList, json);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ERROR", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    // ---------------------------- Success -------------------------------
    String permsListStr = permsList.stream().map(Enum::name).collect(Collectors.joining(","));
    RespBasic resp1 = new RespBasic();
    return Response.status(Status.CREATED)
      .entity(TapisRestUtils.createSuccessResponse(ApiUtils.getMsgAuth("APPAPI_PERMS_REVOKED", authenticatedUser, appName,
                                                                       userName, permsListStr),
                                                   prettyPrint, resp1))
      .build();
  }


  // ************************************************************************
  // *********************** Private Methods ********************************
  // ************************************************************************

  /**
   * Check json payload and extract permissions list.
   * @param appName - name of the app, for constructing response msg
   * @param userName - name of user associated with the perms request, for constructing response msg
   * @param prettyPrint - print flag used to construct response
   * @param json - Request json extracted from payloadStream
   * @param permsList - List for resulting permissions extracted from payload
   * @return - null if all checks OK else Response containing info
   */
  private Response checkAndExtractPayload(AuthenticatedUser authenticatedUser, String appName, String userName, boolean prettyPrint,
                                          String json, Set<Permission> permsList)
  {
    String msg;
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(json, FILE_PERMS_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_JSON_INVALID", authenticatedUser, appName, userName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }

    JsonObject obj = TapisGsonUtils.getGson().fromJson(json, JsonObject.class);

    // Extract permissions from the request body
    JsonArray perms = null;
    if (obj.has(PERMISSIONS_FIELD)) perms = obj.getAsJsonArray(PERMISSIONS_FIELD);
    if (perms != null && perms.size() > 0)
    {
      for (int i = 0; i < perms.size(); i++)
      {
        // Remove quotes from around incoming string
        String permStr = StringUtils.remove(perms.get(i).toString(),'"');
        // Convert the string to an enum and add it to the list
        try {permsList.add(Permission.valueOf(permStr)); }
        catch (IllegalArgumentException e)
        {
          msg = ApiUtils.getMsgAuth("APPAPI_PERMS_ENUM_ERROR", authenticatedUser, appName, userName, permStr, e.getMessage());
          _log.error(msg, e);
          return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
        }
      }
    }

    msg = null;
    // Check values. We should have at least one permission
    if (perms == null || perms.size() <= 0)
    {
      msg = ApiUtils.getMsgAuth("APPAPI_PERMS_NOPERMS", authenticatedUser, appName, userName);
    }

    // If validation failed log error message and return response
    if (msg != null)
    {
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, prettyPrint)).build();
    }
    else return null;
  }
}
