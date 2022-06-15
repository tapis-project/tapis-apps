package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Delivery target for a notification
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class DeliveryTarget
{
  // ============== Enums ==========================================
  public enum NotifDeliveryMethod {WEBHOOK, EMAIL}

  // ============== Fields =========================================
  private static final Logger _log = LoggerFactory.getLogger(DeliveryTarget.class);

  private final NotifDeliveryMethod deliveryType;
  private final String deliveryAddress;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public DeliveryTarget(NotifDeliveryMethod method1, String deliveryAddress1)
  {
    deliveryType = method1;
    deliveryAddress = deliveryAddress1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public NotifDeliveryMethod getDeliveryType() { return deliveryType; }
  public String getDeliveryAddress() { return deliveryAddress; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}