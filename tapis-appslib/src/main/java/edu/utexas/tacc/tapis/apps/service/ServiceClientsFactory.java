package edu.utexas.tacc.tapis.apps.service;

import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import org.glassfish.hk2.api.Factory;

/**
 * HK2 Factory class providing a ServiceClients singleton instance for the service.
 * Binding happens in AppsApplication.java
 */
public class ServiceClientsFactory implements Factory<ServiceClients>
{
  @Override
  public ServiceClients provide() { return ServiceClients.getInstance(); }
  @Override
  public void dispose(ServiceClients c) {}
}
