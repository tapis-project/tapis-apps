package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.AppType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;

/*
 * Utilities and data for integration testing
 */
public final class IntegrationUtils
{
  public static final Gson gson =  TapisGsonUtils.getGson();
  // Test data
  public static final String tenantName = "dev";
  public static final String ownerUser = "owner1";
  public static final String ownerUser2 = "owner2";
  public static final String apiUser = "testApiUser";
  public static final String appIdPrefix = "TestApp";
  public static final String appVersion = "0.0.1";
  public static final boolean enabledTrue = true;
  public static final boolean enabledFalse = false;
  public static final Runtime runtime = Runtime.DOCKER;
  public static final String runtimeVersion = "0.0.1";
  public static final String containerImage = "containerImage1";
  public static final boolean dynamicExecSystem = true;
  public static final String[] execSystemConstraints = {"Constraint1 AND", "Constraint2"};
  public static final String execSystemId = "execSystem1";
  public static final String execSystemExecDir = "execSystemExecDir1";
  public static final String execSystemInputDir = "execSystemInputDir1";
  public static final String execSystemOutputDir = "execSystemOutputDir1";
  public static final String execSystemLogicalQueue = "execSystemLogicalQueue1";
  public static final String archiveSystemId = "archiveSystem1";
  public static final String archiveSystemDir = "archiveSystemDir1";
  public static final boolean archiveOnAppError = true;
  public static final String jobDescription = "job description 1";
  public static final int maxJobs = 1;
  public static final int maxJobsPerUser = 1;
  public static final int nodeCount = 1;
  public static final int coresPerNode = 1;
  public static final int memoryMb = 1;
  public static final int maxMinutes = 1;


  public static final String[] envVariables = {"key1=val1", "key2=val2"};
  public static final String[] archiveIncludes = {"/include1", "/include2"};
  public static final String[] archiveExcludes = {"/exclude1", "/exclude2"};

  public static final String[] jobTags = {"jobtag1", "jobtag2"};
  public static final String[] tags = {"value1", "value2", "a",
    "a long tag with spaces and numbers (1 3 2) and special characters [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final Object notes = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final JsonObject notesObj = (JsonObject) notes;
  public static final String scrubbedJson = "{}";

//  public static final Capability capA = new Capability(Capability.Category.SCHEDULER, "Type", "Slurm");
//  public static final Capability capB = new Capability(Capability.Category.HARDWARE, "CoresPerNode", "4");
//  public static final Capability capC = new Capability(Capability.Category.SOFTWARE, "OpenMP", "4.5");
//  public static final Capability capD = new Capability(Capability.Category.CONTAINER, "Singularity", null);
//  public static final List<Capability> capList = new ArrayList<>(List.of(capA, capB, capC, capD));

  /**
   * Create an array of App objects in memory
   * Names will be of format TestApp_K_NNN where K is the key and NNN runs from 000 to 999
   * We need a key because maven runs the tests in parallel so each set of apps created by an integration
   *   test will need its own namespace.
   * @param n number of apps to create
   * @return array of App objects
   */
  public static App[] makeApps(int n, String key)
  {
    App[] apps = new App[n];
    for (int i = 0; i < n; i++)
    {
      // Suffix which should be unique for each app within each integration test
      String suffix = key + "_" + String.format("%03d", i+1);
      String appId = appIdPrefix + "_" + suffix;
      // Constructor initializes all attributes except for JobCapabilities
      apps[i] = new App(-1, tenantName, appId, appVersion, "description "+suffix, AppType.BATCH, ownerUser, enabledTrue,
                        runtime, runtimeVersion, containerImage, maxJobs, maxJobsPerUser, jobDescription, dynamicExecSystem,
                        execSystemConstraints, execSystemId, execSystemExecDir, execSystemInputDir, execSystemOutputDir,
                        execSystemLogicalQueue, archiveSystemId, archiveSystemDir, archiveOnAppError,
                        nodeCount, coresPerNode, memoryMb, maxMinutes, envVariables, archiveIncludes, archiveExcludes, jobTags,
                        tags, notes, null, false, null, null);
      // Aux table data
      //      apps[i].setFileInputs(????);
    }
    return apps;
  }
}
