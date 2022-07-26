package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.model.AppsShare;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

/*
  Results from a retrieval of app share items resources.
 */
public final class RespAppsShare extends RespAbstract
{
  public AppsShare result;

  public RespAppsShare(AppsShare appsShare)
  {
    result = appsShare;
  }
}
