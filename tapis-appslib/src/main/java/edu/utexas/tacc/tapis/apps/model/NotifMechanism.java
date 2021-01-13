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
public final class NotifMechanism
{
  // ============== Enums ==========================================
  public enum NotifMechanismType {WEBHOOK, EMAIL, QUEUE, ACTOR}

  // ============== Fields =========================================
  private static final Logger _log = LoggerFactory.getLogger(NotifMechanism.class);

  private final int seqId; // Unique database sequence number
  private final int subscriptionSeqId;
  private final NotifMechanismType mechanism;
  private final String webhookUrl;
  private final String emailAddress;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public NotifMechanism(int seqId1, int subscriptionSeqId1, NotifMechanismType mechanism1,
                        String webhookUrl1, String emailAddress1)
  {
    seqId = seqId1;
    subscriptionSeqId = subscriptionSeqId1;
    mechanism = mechanism1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
  }

  public NotifMechanism(NotifMechanismType mechanism1, String webhookUrl1, String emailAddress1)
  {
    seqId = -1;
    subscriptionSeqId = -1;
    mechanism = mechanism1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public int getSeqId() { return seqId; }
  public int getSubscriptionSeqId() { return subscriptionSeqId; }
  public NotifMechanismType getMechanism() { return mechanism; }
  public String getWebhookUrl() { return webhookUrl; }
  public String getEmailAddress() { return emailAddress; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
