package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Argument with metadata
 *  - used for container arguments, command arguments, scheduler options
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class AppArg
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(AppArg.class);

  private final int seqId; // Unique database sequence number
  private final int appSeqId;
  private final String value;
  private final String metaName;
  private final String metaDescription;
  private final boolean metaRequired;
  private final String[] metaKeyValuePairs;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public AppArg(int seqId1, int appId1, String value1, String metaName1,
                  String metaDescription1, boolean metaRequired1, String[] metaKVPairs1)
  {
    seqId = seqId1;
    appSeqId = appId1;
    value = value1;
    metaName = metaName1;
    metaDescription = metaDescription1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  public AppArg(String value1, String metaName1, String metaDescription1, boolean metaRequired1, String[] metaKVPairs1)
  {
    seqId = -1;
    appSeqId = -1;
    value = value1;
    metaName = metaName1;
    metaDescription = metaDescription1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public int getSeqId() { return seqId; }
  public int getAppSeqId() { return appSeqId; }
  public String getValue() { return value; }
  public String getMetaName() { return metaName; }
  public String getMetaDescription() { return metaDescription; }
  public boolean isMetaRequired() { return metaRequired; }
  public String[] getMetaKeyValuePairs() { return metaKeyValuePairs; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
