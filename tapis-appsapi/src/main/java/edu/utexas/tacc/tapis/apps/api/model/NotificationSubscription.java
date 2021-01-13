package edu.utexas.tacc.tapis.apps.api.model;

import static edu.utexas.tacc.tapis.apps.model.NotificationMechanism.NotificationMechanismType;
import java.util.List;

/*
 * Class for a NotificationSubscription in an App create request.
 */
public final class NotificationSubscription
{
  public String filter;
  public List<NotificationMechanism> notificationMechanisms;

  static class NotificationMechanism
  {
    public NotificationMechanismType mechanism;
    public String webhookURL;
    public String emailAddress;
  }
}
