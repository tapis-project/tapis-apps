package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.api.responses.results.ResultApp;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.apps.model.App;

public final class RespApp extends RespAbstract
{
  public ResultApp result;

  public RespApp(App a) { result = new ResultApp(a); }
}
