package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.model.AppShareItem;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

/*
  Results from a retrieval of app share items resources.
 */
public final class RespAppShare extends RespAbstract
{
  public AppShareItem result;

  public RespAppShare(AppShareItem shItem)
  {
    result = shItem;
  }
}
