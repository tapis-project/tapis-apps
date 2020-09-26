package edu.utexas.tacc.tapis.apps.api;

import javax.ws.rs.ApplicationPath;

import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.providers.TapisExceptionMapper;
import edu.utexas.tacc.tapis.sharedapi.providers.ValidationExceptionMapper;
import edu.utexas.tacc.tapis.sharedapi.security.ServiceJWT;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import edu.utexas.tacc.tapis.apps.config.RuntimeParameters;
import edu.utexas.tacc.tapis.apps.dao.AppsDao;
import edu.utexas.tacc.tapis.apps.dao.AppsDaoImpl;
import edu.utexas.tacc.tapis.apps.service.AppsService;
import edu.utexas.tacc.tapis.apps.service.AppsServiceImpl;

import edu.utexas.tacc.tapis.apps.service.AppsServiceJWTFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.message.filtering.SelectableEntityFilteringFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.jooq.tools.StringUtils;

import java.net.URI;

// The path here is appended to the context root and is configured to work when invoked in a standalone
// container (command line) and in an IDE (eclipse).
// NOTE: When running using tomcat this path should match the war file name (v3#apps.war) for running
//       in IntelliJ IDE as well as from a docker container.
// NOTE: When running using tomcat in IntelliJ IDE the live openapi docs contain /v3/apps in the URL
//       but when running from a docker container they do not.
// NOTE: When running using grizzly in IntelliJ IDE or from docker container the live openapi docs do not
//       contain /v3/apps in the URL.
// NOTE: When running from IntelliJ IDE the live openapi docs do not contain the top level paths
//       GET /v3/apps, POST /v3/apps, GET /v3/apps/{sysName} and POST /v3/apps/{sysName}
//       but the file on disk (tapis-appsapi/src/main/resources/openapi.json) does contains the paths.
// NOTE: All the paths in the openapi file on disk (tapis-appsapi/src/main/resources/openapi.json) are
//       missing the prefix /v3/apps
// NOTE: ApplicationPath changed from "v3/apps" to "/" since each resource class includes "/v3/apps" in the
//       path set at the class level. See AppResource.java, PermsResource.java, etc.
@ApplicationPath("/")
public class AppsApplication extends ResourceConfig
{
  // For all logging use println or similar so we do not have a dependency on a logging subsystem.
  public AppsApplication()
  {
    // Log our existence.
    // Output version information on startup
    System.out.println("**** Starting tapis-apps. Version: " + TapisUtils.getTapisFullVersion() + " ****");

    // Register the swagger resources that allow the
    // documentation endpoints to be automatically generated.
    register(OpenApiResource.class);
    register(AcceptHeaderOpenApiResource.class);

    // Setup and register Jersey's dynamic filtering
    // This allows for returning selected attributes in a return result
    //   using the query parameter select, e.g.
    //   /v3/apps?select=result.id,result.name,result.host,result.enabled
    property(SelectableEntityFilteringFeature.QUERY_PARAM_NAME, "select");
    register(SelectableEntityFilteringFeature.class);
    // Register either Jackson or Moxy for SelectableEntityFiltering
    // NOTE: Using shaded jar and Moxy works when running from Intellij IDE but breaks things when running in docker.
    // NOTE: Using Jackson results in following TApp attributes not being returned: notes, created, updated.
    // NOTE: Using unshaded jar and Moxy appears to resolve all issues.
    register(new MoxyJsonConfig().resolver());
//    register(JacksonFeature.class);

    // Register classes needed for returning a standard Tapis response for non-Tapis exceptions.
    register(TapisExceptionMapper.class);
    register(ValidationExceptionMapper.class);
    // Register service class for calling init method during application startup
    register(AppsServiceImpl.class);

    // We specify what packages JAX-RS should recursively scan
    // to find annotations.  By setting the value to the top-level
    // tapis directory in all projects, we can use JAX-RS annotations
    // in any tapis class.  In particular, the filter classes in
    // tapis-sharedapi will be discovered whenever that project is
    // included as a maven dependency.
    packages("edu.utexas.tacc.tapis");
    packages("edu.utexas.tacc.tapis2");

    // Set the application name.
    // Note that this has no impact on base URL
    setApplicationName("apps");

    // Perform remaining init steps in try block so we can print a fatal error message if something goes wrong.
    try {

      // Initialize tenant manager singleton. This can be used by all subsequent application code, including filters.
      // The base url of the tenants service is a required input parameter.
      // Retrieve the tenant list from the tenant service now to fail fast if we can't access the list.
      String url = RuntimeParameters.getInstance().getTenantsSvcURL();
      TenantManager.getInstance(url).getTenants();

      // Initialize bindings for HK2 dependency injection
      register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(AppsServiceImpl.class).to(AppsService.class); // Used in Resource classes for most service calls
          bind(AppsServiceImpl.class).to(AppsServiceImpl.class); // Used in AppsResource for checkDB
          bind(AppsDaoImpl.class).to(AppsDao.class); // Used in service impl
          bindFactory(AppsServiceJWTFactory.class).to(ServiceJWT.class); // Used in service impl and AppsResource
          bind(SKClient.class).to(SKClient.class); // Used in service impl
        }
      });

    } catch (Exception e) {
      // This is a fatal error
      System.out.println("**** FAILURE TO INITIALIZE: tapis-appsapi ****");
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Embedded Grizzly HTTP server
   */
  public static void main(String[] args) throws Exception
  {
    // If TAPIS_SERVICE_PORT set in env then use it.
    // Useful for starting service locally on a busy system where 8080 may not be available.
    String servicePort = System.getenv("TAPIS_SERVICE_PORT");
    if (StringUtils.isBlank(servicePort)) servicePort = "8080";

    // Set base protocol and port. If mainly running in k8s this may not need to be configurable.
    final URI BASE_URI = URI.create("http://0.0.0.0:" + servicePort + "/");
    // Initialize the application container
    ResourceConfig config = new AppsApplication();
    // Initialize the service
    // In order to instantiate our service class using HK2 we need to create an application handler
    //   which allows us to get an injection manager which is used to get a locator.
    //   The locator allows us to get classes that have been registered using AbstractBinder.
    // NOTE: As of Jersey 2.26 dependency injection was abstracted out to make it easier to use DI frameworks
    //       other than HK2, although finding docs and examples on how to do so seems difficult.
    ApplicationHandler handler = new ApplicationHandler(config);
    InjectionManager im = handler.getInjectionManager();
    ServiceLocator locator = im.getInstance(ServiceLocator.class);
    AppsServiceImpl svcImpl = locator.getService(AppsServiceImpl.class);
    svcImpl.initService();
    // Create and start the server
    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);
    server.start();
  }
}
