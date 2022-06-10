package edu.utexas.tacc.tapis.apps.model;

/*
 * Apps Share
 *
 */
public final class AppShareItem
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final Boolean publicApp;
  private final String[] userList;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public AppShareItem(Boolean publicApp1, String[] userList1)
  {
    publicApp = publicApp1;
    userList = userList1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public Boolean getPublicApp() { return publicApp; }
  public String[] getUserList() { return userList; }
}
