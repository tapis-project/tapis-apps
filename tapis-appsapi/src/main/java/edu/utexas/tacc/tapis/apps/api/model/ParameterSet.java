package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.AppArg;

import java.util.ArrayList;
import java.util.List;

/*
 * Class for ParameterSet attributes in an App definition.
 */
public final class ParameterSet
{
  public List<ArgSpec> appArgs;
  public List<ArgSpec> containerArgs;
  public List<ArgSpec> schedulerOptions;
  public List<KeyValuePair> envVariables;
  public ArchiveFilter archiveFilter;

  public ParameterSet()
  {
    appArgs = new ArrayList<>();
    containerArgs = new ArrayList<>();
    schedulerOptions = new ArrayList<>();
    envVariables = new ArrayList<>();
    archiveFilter = new ArchiveFilter();
  }
  public ParameterSet(App a)
  {
    appArgs = buildArgSpecList(a.getAppArgs());
    containerArgs = buildArgSpecList(a.getContainerArgs());
    schedulerOptions = buildArgSpecList(a.getSchedulerOptions());
    envVariables = ApiUtils.getKeyValuesAsList(a.getEnvVariables());
    archiveFilter = new ArchiveFilter(a);
  }

  // Build a list of api model Args based on the lib model Args
  List<ArgSpec> buildArgSpecList(List<AppArg> appArgs)
  {
    var retList = new ArrayList<ArgSpec>();
    if (appArgs == null || appArgs.isEmpty()) return retList;
    for (AppArg appArg : appArgs)
    {
      ArgSpec argSpec = new ArgSpec();
      ArgMetaSpec meta = new ArgMetaSpec();
      argSpec.arg = appArg.getArgValue();
      meta.name = appArg.getMetaName();
      meta.description = appArg.getMetaDescription();
      meta.required = appArg.isMetaRequired();
      meta.keyValuePairs = ApiUtils.getKeyValuesAsList(appArg.getMetaKeyValuePairs());
      argSpec.meta = meta;
      retList.add(argSpec);
    }
    return retList;
  }
}
