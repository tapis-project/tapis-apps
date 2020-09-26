package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

public final class RespAppArray extends RespAbstract
{
  /**
   * Zero arg constructor needed to use jersey's SelectableEntityFilteringFeature
   */
  public RespAppArray() { }

  public RespAppArray(List<App> result) { this.result = result; }
  public List<App> result;
}