package edu.utexas.tacc.tapis.apps.api.responses;

import edu.utexas.tacc.tapis.apps.api.responses.results.TapisAppDTO;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultListMetadata;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;
import com.google.gson.JsonArray;

/*
  Results from a retrieval of App resources.
 */
public final class RespApps extends RespAbstract
{
  public JsonArray result;

  public RespApps(List<App> appList, int limit, String orderBy, int skip, String startAfter, int totalCount,
                  List<String> selectList)
  {
    result = new JsonArray();
    for (App app : appList)
    {
      result.add(new TapisAppDTO(app).getDisplayObject(selectList));
    }

    ResultListMetadata meta = new ResultListMetadata();
    meta.recordCount = result.size();
    meta.recordLimit = limit;
    meta.recordsSkipped = skip;
    meta.orderBy = orderBy;
    meta.startAfter = startAfter;
    meta.totalCount = totalCount;
    metadata = meta;
  }
}