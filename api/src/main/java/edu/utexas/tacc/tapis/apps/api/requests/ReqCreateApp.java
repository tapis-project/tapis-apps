package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.model.Capability;
import edu.utexas.tacc.tapis.apps.model.App.AppType;

import java.util.List;

import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.apps.model.App.DEFAULT_TAGS;

/*
 * Class representing all app attributes that can be set in an incoming create request json body
 */
public final class ReqCreateApp
{
  public String name;       // Name of the app
  public String description; // Full description of the app
  public AppType appType; // Type of app, e.g.
  public String owner = DEFAULT_OWNER;      // User who owns the app and has full privileges
  public boolean enabled = DEFAULT_ENABLED; // Indicates if the app is currently enabled
  public List<Capability> jobCapabilities; // List of job related capabilities required by the app
  public String[] tags = DEFAULT_TAGS;       // List of arbitrary tags as strings
  public Object notes = DEFAULT_NOTES;      // Simple metadata as json
  public String refImportId; // Optional reference ID for an app created via import
}
