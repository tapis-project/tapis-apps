package edu.utexas.tacc.tapis.apps.model;

import java.util.Set;

/*
 * Apps Share
 *
 */
public final class AppShare
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final boolean publicShare;
  private final Set<String> users;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public AppShare(Boolean publicApp1, Set<String> userList1)
  {
    publicShare = publicApp1;
    users = userList1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public boolean isPublic() { return publicShare; }
  public Set<String> getUserList() { return users; }
}
