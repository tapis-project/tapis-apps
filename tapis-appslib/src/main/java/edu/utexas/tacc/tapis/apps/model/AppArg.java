package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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
  private final int appId;

  private final String value;
  private final String metaName;
  private final boolean metaRequired;
  private final String[] metaKeyValuePairs;
  private final Instant created; // UTC time for when record was created
  private final Instant updated; // UTC time for when record was last updated

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public AppArg(int seqId1, int appId1, String sourceUrl1, String targetPath1,
                  String metaName1, boolean metaRequired1, String[] metaKVPairs1,
                  Instant created1, Instant updated1)
  {
    seqId = seqId1;
    appId = appId1;
    created = created1;
    updated = updated1;
    value = sourceUrl1;
    metaName = metaName1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  public AppArg(String sourceUrl1, String targetPath1, String metaName1, boolean metaRequired1,
                String[] metaKVPairs1)
  {
    seqId = -1;
    appId = -1;
    created = null;
    updated = null;
    value = sourceUrl1;
    metaName = metaName1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getValue() { return value; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
