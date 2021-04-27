package edu.utexas.tacc.tapis.apps.api.responses;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.apps.api.responses.results.TapisAppDTO;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.apps.model.App;

import java.util.List;

public final class RespApp extends RespAbstract
{
  public JsonObject result;

  public RespApp(App a, List<String> selectList)
  {
    result = new TapisAppDTO(a).getDisplayObject(selectList);
  }
}
