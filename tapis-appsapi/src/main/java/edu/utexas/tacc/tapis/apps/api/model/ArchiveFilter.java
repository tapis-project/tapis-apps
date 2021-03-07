package edu.utexas.tacc.tapis.apps.api.model;

import edu.utexas.tacc.tapis.apps.model.App;

/*
 * Class for archive include and exclude lists in an App definition.
 */
public final class ArchiveFilter
{
  public String[] includes;
  public String[] excludes;

  public ArchiveFilter()
  {
    includes = App.EMPTY_STR_ARRAY;
    excludes = App.EMPTY_STR_ARRAY;
  }
  ArchiveFilter(App a)
  {
    includes = a.getArchiveIncludes();
    excludes = a.getArchiveExcludes();
  }
}
