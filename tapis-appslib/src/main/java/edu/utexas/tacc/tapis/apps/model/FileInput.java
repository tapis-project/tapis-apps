package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class representing FileInputs contained in an App definition.

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
  private final int appSeqId;

  private final String sourceUrl;
  private final String targetPath;
  private final boolean inPlace;
  private final String metaName;
  private final String metaDescription;
  private final boolean metaRequired;
  private final String[] metaKeyValuePairs;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public FileInput(int seqId1, int appSeqId1, String sourceUrl1, String targetPath1, boolean inPlace1,
                     String metaName1, String metaDescription1, boolean metaRequired1, String[] metaKVPairs1)
  {
    seqId = seqId1;
    appSeqId = appSeqId1;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    inPlace = inPlace1;
    metaName = metaName1;
    metaDescription = metaDescription1;
    metaRequired = metaRequired1;
    metaKeyValuePairs = metaKVPairs1;
  }

  public FileInput(String sourceUrl1, String targetPath1, boolean inPlace1, String metaName1, String metaDescription1,
                   boolean metaRequired1, String[] metaKVPairs1)
  {
    seqId = -1;
    appSeqId = -1;
    sourceUrl = sourceUrl1;
    targetPath = targetPath1;
    inPlace = inPlace1;
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
  public String getSourceUrl() { return sourceUrl; }
  public String getTargetPath() { return targetPath; }
  public boolean isInPlace() { return inPlace; }
  public String getMetaName() { return metaName; }
  public String getMetaDescription() { return metaDescription; }
  public boolean isMetaRequired() { return metaRequired; }
  public String[] getMetaKeyValuePairs() { return metaKeyValuePairs; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
