package edu.utexas.tacc.tapis.apps;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.apps.model.Capability;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.ArrayList;
import java.util.List;

/*
 * Utilities and data for integration testing
 */
public final class IntegrationUtils
{
  // Test data
  public static final String tenantName = "dev";
  public static final String ownerUser = "owner1";
  public static final String ownerUser2 = "owner2";
  public static final String apiUser = "testApiUser";
  public static final String appNamePrefix = "TestApp";
  public static final Gson gson =  TapisGsonUtils.getGson();
  public static final String[] tags = {"value1", "value2", "a",
    "a long tag with spaces and numbers (1 3 2) and special characters [_ $ - & * % @ + = ! ^ ? < > , . ( ) { } / \\ | ]. Backslashes must be escaped."};
  public static final Object notes = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final JsonObject notesObj = (JsonObject) notes;
  public static final String scrubbedJson = "{}";

  public static final Capability capA = new Capability(Capability.Category.SCHEDULER, "Type", "Slurm");
  public static final Capability capB = new Capability(Capability.Category.HARDWARE, "CoresPerNode", "4");
  public static final Capability capC = new Capability(Capability.Category.SOFTWARE, "OpenMP", "4.5");
  public static final Capability capD = new Capability(Capability.Category.CONTAINER, "Singularity", null);
  public static final List<Capability> capList = new ArrayList<>(List.of(capA, capB, capC, capD));

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
      String name = appNamePrefix + "_" + suffix;
      // Constructor initializes all attributes except for JobCapabilities
      apps[i] = new App(-1, tenantName, name, "description "+suffix, App.AppType.BATCH, ownerUser, true,
                            tags, notes, null, false, null, null);
      apps[i].setJobCapabilities(capList);
    }
    return apps;
  }
}
