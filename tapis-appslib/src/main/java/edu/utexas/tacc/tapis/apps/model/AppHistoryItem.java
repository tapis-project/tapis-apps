package edu.utexas.tacc.tapis.apps.model;

import java.time.Instant;

import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.apps.model.App.AppOperation;


/*
 * Apps History
 *
 */
public final class AppHistoryItem
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Attribute names, also used as field names in Json
  public static final String APP_VERSION_FIELD = "appVersion";
  public static final String USER_TENANT_FIELD = "userTenant";
  public static final String USER_NAME_FIELD = "userName";
  public static final String OPERATION_FIELD = "operation";
  public static final String UPD_JSON_FIELD = "updJson";
  public static final String CREATED_FIELD = "created";

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  
  private final String appVersion;
  private final String userTenant;
  private final String userName;
  private final AppOperation operation;
  private final JsonElement updJson;
  private final Instant created; // UTC time for when record was created

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public AppHistoryItem(String appVersion1, String userTenant1, String userName1,
                      AppOperation operation1, JsonElement jsonElement, Instant created1)
  {
    appVersion = appVersion1;
    userTenant = userTenant1;
    userName = userName1;
    operation = operation1;
    updJson = jsonElement;
    created = created1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public String getAppVersion() { return appVersion; }
  public String getUserTenant() { return userTenant; }
  public String getUserName() { return userName; }
  public AppOperation getOperation() { return operation; }
  public JsonElement getUpdJson() { return updJson; }
  public Instant getCreated() { return created; }
}
