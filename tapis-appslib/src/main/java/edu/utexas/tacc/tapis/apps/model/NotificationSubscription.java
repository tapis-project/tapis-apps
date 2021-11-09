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

  private final String filter;
  private List<NotificationMechanism> notificationMechanisms;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public NotificationSubscription(String filter1)
  {
    filter = filter1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
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
