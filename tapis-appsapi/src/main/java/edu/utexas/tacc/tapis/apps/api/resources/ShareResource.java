package edu.utexas.tacc.tapis.apps.api.resources;

/*
 * JAX-RS REST resource for Tapis App share
 *  NOTE: For OpenAPI spec please see repo openapi-apps file AppsAPI.yaml
 * Annotations map HTTP verb + endpoint to method invocation.
 * Permissions are stored in the Security Kernel
 *
 */
@Path("/v3/apps/share")
public class ShareResource {
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(ShareResource.class);

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

  private final String className = getClass().getSimpleName();

  /*
   * Tapis Apps share resource endpoints.
   *
   * NOTE: For OpenAPI spec please see file AppsApi.yaml located in repo
   * openapi-apps.
   */
 @Path("/v3/share")
 public class ShareResource
 {
  
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
  @Path("{appId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getShare(@PathParam("appId") String appId,
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
    AppShareItem appShare;
    
    try
    {
      // Retrieve App share information
      appShare = appsService.getAppShare(rUser, appId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("SYSAPI_SYS_GET_ERROR", rUser, appId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // System not found
    if (appShare == null) {
     String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, appId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    
    // ---------------------------- Success -------------------------------
    // Success means we retrieved the system history information.
    RespAppShare resp1 = new RespAppShare(appShare);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "AppShare", appId), resp1);
  }
}
