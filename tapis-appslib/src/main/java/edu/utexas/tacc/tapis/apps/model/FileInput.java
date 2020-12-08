package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/*
 * FileInput representing
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class FileInput
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(FileInput.class);

  private final int seqId; // Unique database sequence number
  private final int appId;

  private final String sourceUrl;
  private final String targetPath;
  private final String metaName;
  private final boolean metaRequired;
  private final String[] metaKeyValuePairs;
  private final Instant created; // UTC time for when record was created
  private final Instant updated; // UTC time for when record was last updated

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public FileInput(int seqId1, int appId1, String sourceUrl1, String targetPath1,
                     String metaName1, boolean metaRequired1, String[] metaKVPairs1,
                     Instant created1, Instant updated1)
  {
    seqId = seqId1;
    appId = appId1;
    created = created1;
    updated = updated1;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    metaName = metaName1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  public FileInput(String sourceUrl1, String targetPath1, String metaName1, boolean metaRequired1,
                   String[] metaKVPairs1)
  {
    seqId = -1;
    appId = -1;
    created = null;
    updated = null;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    metaName = metaName1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
