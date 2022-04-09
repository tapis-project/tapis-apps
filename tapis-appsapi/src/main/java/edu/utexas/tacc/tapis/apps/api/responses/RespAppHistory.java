package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.model.AppHistoryItem;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

import java.util.List;

/*
  Results from a retrieval of SystemHistory items resources.
 */
public final class RespAppHistory extends RespAbstract
{
  public List<AppHistoryItem> result;

  public RespAppHistory(List<AppHistoryItem> shList)
  {
    result = shList;
  }
}
