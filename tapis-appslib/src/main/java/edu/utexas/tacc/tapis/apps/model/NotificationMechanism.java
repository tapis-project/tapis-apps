package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Delivery mechanism for a notification
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class NotificationMechanism
{
  // ============== Enums ==========================================
  public enum NotifMechanismType {WEBHOOK, EMAIL, QUEUE, ACTOR}

  // ============== Fields =========================================
  private static final Logger _log = LoggerFactory.getLogger(NotificationMechanism.class);

  private final NotifMechanismType mechanism;
  private final String webhookUrl;
  private final String emailAddress;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public NotificationMechanism(NotifMechanismType mechanism1, String webhookUrl1, String emailAddress1)
  {
    mechanism = mechanism1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public NotifMechanismType getMechanism() { return mechanism; }
  public String getWebhookUrl() { return webhookUrl; }
  public String getEmailAddress() { return emailAddress; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
