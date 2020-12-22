package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.apps.model.App.NotificationMechanism;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * FileInput representing
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class NotificationSubscription
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Logging
  private static final Logger _log = LoggerFactory.getLogger(NotificationSubscription.class);

  private final int seqId; // Unique database sequence number
  private final int appSeqId;

  private final String filter;
  private final NotificationMechanism notificationMechanism;
  private final String webhookUrl;
  private final String emailAddress;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
    public NotificationSubscription(int seqId1, int appSeqId1, String filter1, String webhookUrl1,
                                    NotificationMechanism notificationMechanism1, String emailAddress1)
  {
    seqId = seqId1;
    appSeqId = appSeqId1;
    filter = filter1;
    notificationMechanism = notificationMechanism1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
  }

  public NotificationSubscription(String filter1, NotificationMechanism notificationMechanism1,
                                  String webhookUrl1, String emailAddress1)
  {
    seqId = -1;
    appSeqId = -1;
    filter = filter1;
    notificationMechanism = notificationMechanism1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getFilter() { return filter; }
  public NotificationMechanism getNotificationMechanism() { return notificationMechanism; }
  public String getWebhookUrl() { return webhookUrl; }
  public String getEmailAddress() { return emailAddress; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
