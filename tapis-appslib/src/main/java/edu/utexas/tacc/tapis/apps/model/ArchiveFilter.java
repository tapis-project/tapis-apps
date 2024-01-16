package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

/*
 * Class for archive include and exclude lists in an App.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class ArchiveFilter
{
  private String[] includes;
  private String[] excludes;
  private Boolean includeLaunchFiles;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  public ArchiveFilter()
  {
    includes = App.EMPTY_STR_ARRAY;
    excludes = App.EMPTY_STR_ARRAY;
    includeLaunchFiles = true;
  }

  /**
   * Constructor setting all final attributes.
   */
  public ArchiveFilter(String[] includes1, String[] excludes1, boolean includeLaunchFiles1)
  {
    includes = (includes1 == null) ? null : includes1.clone();;
    excludes = (excludes1 == null) ? null : excludes1.clone();;
    includeLaunchFiles = includeLaunchFiles1;
  }

  /**
   * Copy Constructor
   */
  public ArchiveFilter(ArchiveFilter a)
  {
    includes = LibUtils.stripWhitespaceStrArray(a.getIncludes());
    excludes = LibUtils.stripWhitespaceStrArray(a.getExcludes());
    includeLaunchFiles = a.isIncludeLaunchFiles();
  }

  /* ********************************************************************** */
/*                               Accessors                                */
/* ********************************************************************** */
  public String[] getIncludes() { return (includes == null) ? null : includes.clone(); }
  public void setIncludes(String[] sa) { includes = LibUtils.stripWhitespaceStrArray(sa); }
  public String[] getExcludes() { return (excludes == null) ? null : excludes.clone(); }
  public void setExcludes(String[] sa) { excludes = LibUtils.stripWhitespaceStrArray(sa); }
  public Boolean isIncludeLaunchFiles() { return includeLaunchFiles; }
  public void setIncludeLaunchFiles(Boolean b) { includeLaunchFiles = b; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
