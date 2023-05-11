package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Class for ParameterSet attributes in an App.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class ParameterSet
{
  private List<ArgSpec> appArgs;
  private List<ArgSpec> containerArgs;
  private List<ArgSpec> schedulerOptions;
  private List<KeyValuePair> envVariables;
  private ArchiveFilter archiveFilter;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  public ParameterSet()
  {
    appArgs = new ArrayList<>();
    containerArgs = new ArrayList<>();
    schedulerOptions = new ArrayList<>();
    envVariables = new ArrayList<>();
    archiveFilter = new ArchiveFilter();
  }

  /**
   * Constructor setting all final attributes.
   */
  public ParameterSet(List<ArgSpec> appArgs1, List<ArgSpec> containerArgs1, List<ArgSpec> schedulerOptions1,
                      List<KeyValuePair> envVariables1, ArchiveFilter archiveFilter1)
  {
    appArgs = (appArgs1 == null) ? null: new ArrayList<>(appArgs1);
    containerArgs = (containerArgs1 == null) ? null: new ArrayList<>(containerArgs1);
    schedulerOptions = (schedulerOptions1 == null) ? null: new ArrayList<>(schedulerOptions1);
    envVariables = (envVariables1 == null) ? null: new ArrayList<>(envVariables1);
    archiveFilter = (archiveFilter1 == null) ? null: new ArchiveFilter(archiveFilter1);
  }

  public ParameterSet(ParameterSet p)
  {
    appArgs = p.getAppArgs();
    containerArgs = p.getContainerArgs();
    schedulerOptions = p.getSchedulerOptions();
    envVariables = p.getEnvVariables();
    archiveFilter = p.getArchiveFilter();
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public List<ArgSpec> getAppArgs() { return (appArgs == null) ? null : new ArrayList<>(appArgs);  }
  public void setAppArgs(List<ArgSpec> aa) { appArgs = aa; }
  public List<ArgSpec> getContainerArgs() { return (containerArgs == null) ? null : new ArrayList<>(containerArgs); }
  public void setContainerArgs(List<ArgSpec> aa) { containerArgs = aa; }
  public List<ArgSpec> getSchedulerOptions() { return (schedulerOptions == null) ? null : new ArrayList<>(schedulerOptions); }
  public void setSchedulerOptions(List<ArgSpec> aa) { schedulerOptions = aa; }
  public List<KeyValuePair> getEnvVariables() { return (envVariables == null) ? null : new ArrayList<>(envVariables); }
  public void setEnvVariables(List<KeyValuePair> kv) { envVariables = kv; }
  // When patching we update archiveFilter attributes via setter, so do not return a new instance of ArchiveFilter.
  public ArchiveFilter getArchiveFilter() { return archiveFilter; }
  public void setArchiveFilter(ArchiveFilter af) { archiveFilter = af; }
}
