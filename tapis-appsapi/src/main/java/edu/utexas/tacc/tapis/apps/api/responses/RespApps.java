package edu.utexas.tacc.tapis.apps.api.responses;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonArray;
import edu.utexas.tacc.tapis.apps.api.responses.results.ResultApp;
import edu.utexas.tacc.tapis.shared.utils.JsonArraySerializer;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultListMetadata;

import edu.utexas.tacc.tapis.apps.model.App;

import java.util.ArrayList;
import java.util.List;

/*
  Results from a retrieval of App resources.
 */
public final class RespApps extends RespAbstract
{
  // TODO Switch to JsonArray once DTO is implemented for select support
//  // Json objects require special serializer for Jackson to handle properly in outgoing response.
//  @JsonSerialize(using = JsonArraySerializer.class)
//  public JsonArray result;
  public List<ResultApp> result;

  public RespApps(List<App> appList, int limit, String orderBy, int skip, String startAfter, int totalCount)
  {
//    result = new JsonArray();
//    for (App app : appList)
//    {
//      result.add(new TapisAppDTO(app).getDisplayObject(selectList));
//    }
    result = new ArrayList<>();
    for (App app : appList) { result.add(new ResultApp(app)); }

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