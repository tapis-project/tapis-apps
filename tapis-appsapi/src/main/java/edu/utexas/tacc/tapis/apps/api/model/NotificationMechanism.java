package edu.utexas.tacc.tapis.apps.api.model;

import static edu.utexas.tacc.tapis.apps.model.NotifMechanism.NotifMechanismType;

/*
 * Class for a NotificationMechanism in an App definition contained in a request.
 */
public final class NotificationMechanism
{
  public NotifMechanismType mechanism;
  public String webhookURL;
  public String emailAddress;
}
