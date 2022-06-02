package edu.utexas.tacc.tapis.apps.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * Notification Subscription request in the context of a Job submission
 * Contains filter and list of notification delivery targets
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class ReqSubscribe
{
  // ============== Enums ==========================================
  public enum JobEventCategoryFilter {ALL, JOB_NEW_STATUS, JOB_INPUT_TRANSACTION_ID, JOB_ARCHIVE_TRANSACTION_ID,
                                      JOB_ERROR_MESSAGE, JOB_SUBSCRIPTION }

  // ============== Fields =========================================
  private static final Logger log = LoggerFactory.getLogger(ReqSubscribe.class);

  private final String description;
  private final boolean enabled;

  // Matching and delivery
  private final JobEventCategoryFilter jobEventCategoryFilter;
  private final List<DeliveryTarget> deliveryTargets;
  private final int ttlMinutes;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public ReqSubscribe(String description1, boolean enabled1, JobEventCategoryFilter jobEventCategoryFilter1,
                      List<DeliveryTarget> targets1, int ttlMinutes1)
  {
    description = description1;
    enabled = enabled1;
    jobEventCategoryFilter = jobEventCategoryFilter1;
    deliveryTargets = (targets1 == null) ? null : new ArrayList<>(targets1);
    ttlMinutes = ttlMinutes1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getDescription() { return description; }
  public boolean isEnabled() { return enabled; }
  public JobEventCategoryFilter getJobEventCategoryFilter() { return jobEventCategoryFilter; }
  public int getTtlMinutes() { return ttlMinutes; }

  public List<DeliveryTarget> getDeliveryTargets()
  {
    return (deliveryTargets == null) ? null : new ArrayList<>(deliveryTargets);
  }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
