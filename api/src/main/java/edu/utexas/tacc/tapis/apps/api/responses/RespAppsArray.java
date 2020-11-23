package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

public final class RespAppsArray extends RespAbstract
{
  // Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
  public RespAppsArray() { }

  public RespAppsArray(List<App> result) { this.result = result; }
  public List<App> result;
}