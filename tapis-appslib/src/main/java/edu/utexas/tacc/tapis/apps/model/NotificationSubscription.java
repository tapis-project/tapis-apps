package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * Notification Subscription consisting of filters and list of notification targets
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

  private final String typeFilter;
  private final String subjectFilter;
  private List<DeliveryTarget> deliveryTargets;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public NotificationSubscription(String typeFilter1, String subjectFilter1)
  {
    typeFilter = typeFilter1;
    subjectFilter = subjectFilter1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getTypeFilter() { return typeFilter; }
  public String getSubjectFilter() { return subjectFilter; }

  public List<DeliveryTarget> getDeliveryTargets()
  {
    return (deliveryTargets == null) ? null : new ArrayList<>(deliveryTargets);
  }
  public void setDeliveryTargets(List<DeliveryTarget> nmList)
  {
    deliveryTargets = (nmList == null) ? null : new ArrayList<>(nmList);
  }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
