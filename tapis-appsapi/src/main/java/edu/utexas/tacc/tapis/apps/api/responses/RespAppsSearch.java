package edu.utexas.tacc.tapis.apps.api.responses;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.shared.utils.JsonArraySerializer;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultListMetadata;

import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

/*
  Results from a retrieval of App resources.
 */
public final class RespAppsSearch extends RespAbstract
{
  // Json objects require special serializer for Jackson to handle properly in outgoing response.
  @JsonSerialize(using = JsonArraySerializer.class)
  public JsonArray result;

  public RespAppsSearch(List<App> appList, int limit, String orderBy, int skip, String startAfter, int totalCount)
  {
    result = new JsonArray();
    for (App app : appList)
    {
      result.add(TapisGsonUtils.getGson().toJson(app));
    }

    ResultListMetadata meta = new ResultListMetadata();
    meta.recordCount = result.size();
    meta.recordLimit = limit;
    meta.recordsSkipped = skip;
    meta.orderBy = orderBy;
    meta.startAfter = startAfter;
    meta.totalCount = totalCount;
    String metaJsonStr = TapisGsonUtils.getGson().toJson(meta);
    metadata = TapisGsonUtils.getGson().fromJson(metaJsonStr, JsonObject.class);
  }
}