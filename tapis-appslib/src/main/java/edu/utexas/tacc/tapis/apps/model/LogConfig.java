package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.apache.commons.lang3.StringUtils;

/*
 * Class for logConfig in App to allow for specifying stdout and stderr when running a job.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class LogConfig
{
  private String stdoutFilename = "";
  private String stderrFilename = "";

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  public LogConfig()
  {
    stdoutFilename = "";
    stderrFilename = "";
  }

  /**
   * Constructor setting all final attributes.
   */
  public LogConfig(String stdout1, String stderr1)
  {
    stdoutFilename = (StringUtils.isBlank(stdout1)) ? "" : stdout1;
    stderrFilename = (StringUtils.isBlank(stderr1)) ? "" : stderr1;
  }

  /**
   * Copy Constructor
   */
  public LogConfig(LogConfig lc)
  {
    if (lc != null)
    {
      stdoutFilename = lc.getStdoutFilename();
      stderrFilename = lc.getStderrFilename();
    }
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getStdoutFilename() { return stdoutFilename; }
  public void setStdoutFilename(String s) { stdoutFilename = s; }
  public String getStderrFilename() { return stderrFilename; }
  public void setStderrFilename(String s) { stderrFilename = s; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
