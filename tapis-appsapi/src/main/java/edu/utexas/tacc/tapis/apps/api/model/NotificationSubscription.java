package edu.utexas.tacc.tapis.apps.api.model;

import java.util.List;

/*
 * Class for a NotificationSubscription in an App definition contained in a request.
 */
public final class NotificationSubscription
{
  public String filter;
  public List<NotificationMechanism> notificationMechanisms;
}
