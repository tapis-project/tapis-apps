package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.api.responses.results.ResultApp;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.ArrayList;
import java.util.List;

public final class RespAppsArray extends RespAbstract
{
  public List<ResultApp> result;

  // Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
  public RespAppsArray() { }

  public RespAppsArray(List<App> appList)
  {
    result = new ArrayList<>();
    for (App app : appList) { result.add(new ResultApp(app)); }
  }
}