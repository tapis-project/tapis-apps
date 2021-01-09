package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * Notification Subscription consisting of a filter and list of notification mechanisms
 *
 * This class is intended to be as immutable as possible.
 * Please try to keep it that way.
 *
 */
public final class NotificationSubscription
{
  // ============== Enums ==========================================

  // ============== Fields =========================================
  private static final Logger _log = LoggerFactory.getLogger(NotificationSubscription.class);

  private final int seqId; // Unique database sequence number
  private final int appSeqId;

  private final String filter;
  private List<NotificationMechanism> notificationMechanisms;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public NotificationSubscription(int seqId1, int appSeqId1, String filter1)
  {
    seqId = seqId1;
    appSeqId = appSeqId1;
    filter = filter1;
  }

  public NotificationSubscription(String filter1)
  {
    seqId = -1;
    appSeqId = -1;
    filter = filter1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public int getSeqId() { return seqId; }
  public int getAppSeqId() { return appSeqId; }
  public String getFilter() { return filter; }

  public List<NotificationMechanism> getNotificationMechanisms()
  {
    return (notificationMechanisms == null) ? null : new ArrayList<>(notificationMechanisms);
  }
  public void setNotificationMechanisms(List<NotificationMechanism> nmList)
  {
    notificationMechanisms = (nmList == null) ? null : new ArrayList<>(nmList);
  }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
