package edu.utexas.tacc.tapis.apps.api.requests;

import edu.utexas.tacc.tapis.apps.model.Capability;

import java.util.List;

/*
 * Class representing all app attributes that can be set in an incoming patch request json body
 */
public final class ReqUpdateApp
{
  public String description; // Full description of the app
  public Boolean enabled; // Indicates if apps is currently enabled
  public List<Capability> jobCapabilities; // List of job related capabilities supported by the system
  public String[] tags;       // List of arbitrary tags as strings
  public Object notes;      // Simple metadata as json
}
