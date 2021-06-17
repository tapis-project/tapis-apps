package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;

/*
 * Class representing tenant, user and request information associated with the Authenticated user or service making
 *   making the request.
 *
 * Immutable
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class ResourceRequestUser
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final String jwtTenantId; // Primary tenantId extracted from the JWT
  private final String jwtUserId; // Primary userId extracted from the JWT
  private final String oboTenantId; // On-behalf-of tenantId extracted from the JWT
  private final String oboUserId; // On-behalf-of userId extracted from the JWT
  // TODO/TBD: api values are always same as obo values because for a User request the jwt and obo values are always
  //           the same and for a Service requests the obo values are always (?) be used when operating on resources.
  private final String apiTenantId; // Effective tenantId to use when operating on resources
  private final String apiUserId; // Effective userId to use when operating on resources
  private final boolean isServiceRequest; // Indicates if it was a service request

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  /**
   * Simple constructor to populate all attributes
   */
  public ResourceRequestUser(AuthenticatedUser authenticatedUser)
  {
    jwtTenantId = authenticatedUser.getTenantId();
    jwtUserId = authenticatedUser.getName();
    oboTenantId = authenticatedUser.getOboTenantId();
    oboUserId = authenticatedUser.getOboUser();
    isServiceRequest = TapisThreadContext.AccountType.service.name().equals(authenticatedUser.getAccountType());
    apiTenantId = oboTenantId;
    apiUserId = oboUserId;
  }

  /* ********************************************************************** */
  /*                        Public methods                                  */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getJwtTenantId() { return jwtTenantId; }
  public String getJwtUserId() { return jwtUserId; }
  public String getOboTenantId() { return oboTenantId; }
  public String getOboUserId() { return oboUserId; }
  public String getApiTenantId() { return apiTenantId; }
  public String getApiUserId() { return apiUserId; }
  public boolean isServiceRequest() { return isServiceRequest; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
