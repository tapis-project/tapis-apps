package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.security.client.gen.model.SkShare;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/*
 * Apps Share
 */
public final class AppShare
{

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final boolean publicShare; // Indicates if system is shared publicly with all users in tenant.
  private final Set<String> users; // Set of users that have the system shared with them.
  private final List<SkShare> skShares; // List of SkShare records. May be null if object created via Gson.fromJson().
  private final Set<String> publicGrantors; // Set of users who have granted "~public".


  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /*
   * Constructor taking all final arguments
   */
  public AppShare(boolean publicShare1, Set<String> userIDs1, List<SkShare> skShares1, Set<String> publicGrantors1)
  {
    publicShare = publicShare1;
    users = (userIDs1 == null) ? Collections.emptySet() : userIDs1;
    skShares = (skShares1 == null || skShares1.isEmpty()) ? Collections.emptyList() : skShares1;
    publicGrantors = (publicGrantors1 == null || publicGrantors1.isEmpty()) ? Collections.emptySet() : publicGrantors1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public boolean isPublic() { return publicShare; }
  public Set<String> getUserList() { return users; }
  public List<SkShare> getSkShares() { return skShares; }
  public Set<String> getPublicGrantors() { return publicGrantors; }
}
