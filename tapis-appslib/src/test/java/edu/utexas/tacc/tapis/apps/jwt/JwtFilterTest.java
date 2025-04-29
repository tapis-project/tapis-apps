package edu.utexas.tacc.tapis.apps.jwt;

import edu.utexas.tacc.tapis.apps.config.RuntimeParameters;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.dao.AppsDaoImpl;
import edu.utexas.tacc.tapis.apps.service.AppsService;
import edu.utexas.tacc.tapis.apps.service.AppsServiceImpl;
import edu.utexas.tacc.tapis.apps.service.ServiceClientsFactory;
import edu.utexas.tacc.tapis.apps.service.ServiceContextFactory;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.systems.client.SystemsClient;
import edu.utexas.tacc.tapis.systems.client.gen.model.TapisSystem;
import edu.utexas.tacc.tapis.tokens.client.TokensClient;
import edu.utexas.tacc.tapis.tokens.client.gen.model.InlineObject1;
import edu.utexas.tacc.tapis.tokens.client.model.CreateTokenParms;
import edu.utexas.tacc.tapis.tokens.client.model.TokenResponsePackage;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jooq.tools.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import java.util.Base64;
import java.util.List;

/*
 * Test the JWT processing that happens in the filter() method of class JWTValidateRequestFilter in package
 *   edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.
 * Note that this test has the following dependencies running locally or in dev
 *    Admin tenant base URL - hard-coded to https://admin.develop.tapis.io
 *    Tokens service URL - hard-coded to https://dev.develop.tapis.io/v3/tokens
 *    Security Kernel service - obtained from tenants service
 *    Systems service URL hard-coded to https://dev.develop.tapis.io/v3/systems
 *          - used for testing calls against a Tapis java service that should validate JWTs
 *
 * Note that this test is for code in github repo: https://github.com/tapis-project/tapis-shared-java
 *      So, to test changes to tapis-shared-java:
 *         1. Publish tapis-shared-java to the TACC/Tapis nexus repo.
 *         2. Re-build and deploy the Systems service to DEV, assuming the test env is set up for running
 *            against Systems in DEV.
 *
 * All JWTs are tested by calling the getSystems endpoint for the Systems service
 * Tests: testValidJwt, testExpiredJwt, testModifiedJwt
 */
@Test(groups={"integration"})
public class JwtFilterTest
{
  private AppsServiceImpl svcImpl;
  private TokensClient tokensClientSys;
  private TokensClient tokensClientAuthSvc; // For Tokens client calls using basic auth
  private TokensClient tokensClientAuthOBO; // For Token client calls using a svc jwt with obo headers.
  private SystemsClient systemsClient;
  private String authSvcJwt;
  private String sysSvcJwt;

  private static final String ADMIN_TENANT_URL = "https://admin.develop.tapis.io";
  private static final String TOKENS_URL = "https://dev.develop.tapis.io/v3/tokens";
  private static final String DEV_TENANT_URL = "https://dev.develop.tapis.io";
  private static final String JWT_ISSUER = TOKENS_URL;
  private static final String JWT_TENANT = "dev";
  private static final String JWT_USER = "testuser2";
  private static final String JWT_USERA = "testuser2a";
  private static final String JWT_ALG_256 = "RS256";
  private static final String JWT_ALG_NONE = "none";
  private static final String JWT_SUBJ = String.format("%s@%s", JWT_USER, JWT_TENANT);
  private static final String SITE_ID = "tacc";
  private static final String ADMIN_TENANT_NAME = "admin";
  private static final String AUTH_SVC_NAME = "authenticator";
  private static final String SYS_SVC_NAME = "systems";

  @BeforeSuite
  public void setUp() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + JwtFilterTest.class.getSimpleName());
    // Make sure we have authenticator and systems service passwords
    String authSvcPassword = System.getenv("TAPIS_AUTHENTICATOR_PASSWORD");
    if (StringUtils.isBlank(authSvcPassword)) throw new Exception("Please set env var TAPIS_AUTHENTICATOR_PASSWORD");
    String sysSvcPassword = System.getenv("TAPIS_SYS_SVC_PASSWORD");
    if (StringUtils.isBlank(sysSvcPassword)) throw new Exception("Please set env var TAPIS_SYS_SVC_PASSWORD");

    // Setup for HK2 dependency injection
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure() {
        bind(AppsServiceImpl.class).to(AppsService.class);
        bind(AppsServiceImpl.class).to(AppsServiceImpl.class);
        bind(AppsDaoImpl.class).to(AppsDao.class);
        bindFactory(ServiceContextFactory.class).to(ServiceContext.class);
        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class);
      }
    });
    locator.inject(this);

    // Initialize TenantManager and services
    TenantManager.getInstance(ADMIN_TENANT_URL).getTenants();

    // Initialize services
    svcImpl = locator.getService(AppsServiceImpl.class);
    svcImpl.initService(SITE_ID, ADMIN_TENANT_NAME, RuntimeParameters.getInstance().getServicePassword());

    // Initialize the Systems client
    // First get a tokens client for Systems
    tokensClientSys = getTokensClient(SYS_SVC_NAME, sysSvcPassword);
    // Get a service JWT good for 5 minutes
    sysSvcJwt = getSvcJwt(tokensClientSys, ADMIN_TENANT_NAME, SYS_SVC_NAME, SITE_ID, 300);
    // Finally create the Systems service client
    systemsClient = new SystemsClient(DEV_TENANT_URL, sysSvcJwt);

    // Initialize the Auth service JWT
    // First get a tokens client for Authenticator
    tokensClientAuthSvc = getTokensClient(AUTH_SVC_NAME, authSvcPassword);
    // Get a service JWT good for 5 minutes
    authSvcJwt = getSvcJwt(tokensClientAuthSvc, ADMIN_TENANT_NAME, AUTH_SVC_NAME, SITE_ID, 300);
    // Get a tokens client that uses the auth svc jwt to call on behalf of a test user and tenant
    tokensClientAuthOBO = new TokensClient(ADMIN_TENANT_URL, authSvcJwt, JWT_TENANT, JWT_USER);
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + JwtFilterTest.class.getSimpleName());
  }

  // Create a valid JWT and make sure it works.
  @Test
  public void testValidJwt() throws Exception
  {
    System.out.println("Running test testValidJwt");
    // Create a valid JWT good for 10 seconds
    String encodedJwt = createValidJwt(10);
    // Attempt to list systems
    attemptSysList(encodedJwt);
  }

  // Create a very short-term JWT, let it expire and make sure expected exception is thrown.
  @Test
  public void testExpiredJwt() throws Exception
  {
    boolean pass = false;
    System.out.println("Running test testExpiredJwt");
    // Create a valid JWT good for 1 seconds
    String encodedJwt = createValidJwt(1);
    // Let it expire
    Thread.sleep(1500);
    // Attempt to list systems
    try { attemptSysList(encodedJwt); }
    catch (TapisClientException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("TAPIS_SECURITY_JWT_EXPIRED"),
                        "Unexpected exception: " + e.getMessage());
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  // Create a valid JWT, tamper with it and make sure expected exception is thrown.
  // Test: algorithm = none
  //       modified payload
  //
  @Test
  public void testModifiedJwt() throws Exception
  {
    Base64.Decoder base64Decoder =Base64.getDecoder();
    boolean pass = false;
    System.out.println("Running test testModifiedJwt");
    // Create a valid JWT good for 30 seconds
    String encodedJwt = createValidJwt(30);
    // Encoded jwt should have three parts separated by .
    String[] jwtParts = encodedJwt.split("\\.");
    Assert.assertEquals(jwtParts.length, 3, "JWT did not have 3 parts");
    String headerJson = new String(base64Decoder.decode(jwtParts[0]));
    String payloadJson = new String(base64Decoder.decode(jwtParts[1]));

    // Set alg to none
    String modifiedHeaderJson = headerJson.replaceAll(JWT_ALG_256, JWT_ALG_NONE);
    String modifiedHaderPayload = Base64.getEncoder().encodeToString(modifiedHeaderJson.getBytes());
    // Construct a new JWT that should fail validation
    String modifiedEncodedJwt = String.format("%s.%s.%s", modifiedHaderPayload, jwtParts[1], jwtParts[2]);

    // Attempt to list systems
    try { attemptSysList(modifiedEncodedJwt); }
    catch (TapisClientException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("TAPIS_SECURITY_JWT_INVALID_ALG"),
                        "Unexpected exception: " + e.getMessage());
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;

    // Tamper with the payload
    String modifiedPayloadJson = payloadJson.replaceAll(JWT_USER, JWT_USERA);
    String modifiedEncodedPayload = Base64.getEncoder().encodeToString(modifiedPayloadJson.getBytes());
    // Construct a new JWT that should fail signature validation
    modifiedEncodedJwt = String.format("%s.%s.%s", jwtParts[0], modifiedEncodedPayload, jwtParts[2]);

    // Attempt to list systems
    try { attemptSysList(modifiedEncodedJwt); }
    catch (TapisClientException e)
    {
      Assert.assertTrue(e.getMessage().startsWith("TAPIS_SECURITY_JWT_VERIFY_FAIL"),
                        "Unexpected exception: " + e.getMessage());
      pass = true;
    }
    Assert.assertTrue(pass);
    pass = false;
  }

  /*
   * Given a base64 encoded and signed jwt attempt to list systems
   */
  private void attemptSysList(String usrJwt) throws TapisClientException
  {
    // Update the systems client with the JWT.
    systemsClient.addDefaultHeader("X-Tapis-Token", usrJwt);
    // Call to list systems should succeed. Get 1 system with only the id attribute
    // getSystems(searchStr, limit, orderBy, skip, startAfter, selectStr, showDeleted)
    List<TapisSystem> sysList = systemsClient.getSystems("", 1, "", -1, "", "id", false);
    System.out.println("Number of systems found: " + sysList.size());
    if (!sysList.isEmpty())
      System.out.println("System 1 Id = " + sysList.getFirst().getId());
  }

  /*
   * Create a valid JWT for testuser2
   * @param expiry - expire time in seconds relative to now.
   // Decoded sample JWT:
     HEADER: { "alg": "RS256", "typ": "JWT" }
     BODY: {
      "jti": "84b33dfb-b838-47c1-b57d-cc57b062042c",
      "iss": "https://dev.develop.tapis.io/v3/tokens",
      "sub": "testuser2@dev",
      "tapis/tenant_id": "dev",
      "tapis/token_type": "access",
      "tapis/delegation": false,
      "tapis/delegation_sub": null,
      "tapis/username": "testuser2",
      "tapis/account_type": "user",
      "exp": 1866806 }
   */
  private String createValidJwt(int accessTtlSeconds) throws TapisException, TapisClientException
  {
    CreateTokenParms createParms = new CreateTokenParms();
    createParms.setTokenTenantId(JWT_TENANT);
    createParms.setTokenUsername(JWT_USER);
    createParms.setAccountType(InlineObject1.AccountTypeEnum.USER);
    createParms.setGenerateRefreshToken(false);
    createParms.setAccessTokenTtl(accessTtlSeconds);
    createParms.setTargetSiteId(SITE_ID);

    TokenResponsePackage tokPkg = tokensClientAuthOBO.createToken(createParms);
    if (!tokPkg.isValidAccessToken())
    {
      String msg = MsgUtils.getMsg("TAPIS_SECURITY_BAD_TOKEN_RESP", "access",
            createParms.getTokenUsername(), createParms.getTokenTenantId());
      System.out.println(msg);
      throw new TapisException(msg);
    }
    return tokPkg.getAccessToken().getAccessToken();
  }

  // Create a tokens client for given service
  private TokensClient getTokensClient(String serviceName, String servicePassword)
  {
    TokensClient client = new TokensClient(ADMIN_TENANT_URL);
    String authString = serviceName + ":" + servicePassword;
    String encodedString = Base64.getEncoder().encodeToString(authString.getBytes());
    client.addDefaultHeader("Authorization", "Basic " + encodedString);
    return client;
  }

  // Create a service jwt using the tokens client for the service
  private String getSvcJwt(TokensClient client, String tenant, String serviceName, String site, int accessTtlSeconds)
     throws TapisClientException, TapisException
  {
    CreateTokenParms createParms = new CreateTokenParms();
    createParms.setTokenTenantId(tenant);
    createParms.setTokenUsername(serviceName);
    createParms.setAccountType(InlineObject1.AccountTypeEnum.SERVICE);
    createParms.setGenerateRefreshToken(false);
    createParms.setAccessTokenTtl(accessTtlSeconds);
    createParms.setTargetSiteId(site);

    TokenResponsePackage tokPkg = client.createToken(createParms);
    if (!tokPkg.isValidAccessToken())
    {
      String msg = MsgUtils.getMsg("TAPIS_SECURITY_BAD_TOKEN_RESP", "access",
                                   createParms.getTokenUsername(), createParms.getTokenTenantId());
      System.out.println(msg);
      throw new TapisException(msg);
    }
    return tokPkg.getAccessToken().getAccessToken();
  }
}
