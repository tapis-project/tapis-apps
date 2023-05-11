package edu.utexas.tacc.tapis.apps.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Default values
  public static final Boolean DEFAULT_ENABLED = true;
  public static final int DEFAULT_TTL_MINUTES = 10080; // 60*24*7 = 1 week

  // ============== Enums ==========================================
  public enum JobEventCategoryFilter {ALL, JOB_NEW_STATUS, JOB_INPUT_TRANSACTION_ID, JOB_ARCHIVE_TRANSACTION_ID,
                                      JOB_ERROR_MESSAGE, JOB_SUBSCRIPTION }

  // ============== Fields =========================================
  private static final Logger log = LoggerFactory.getLogger(ReqSubscribe.class);

  private final String description;
  private final Boolean enabled;

  // Matching and delivery
  private final JobEventCategoryFilter jobEventCategoryFilter;
  private final List<DeliveryTarget> deliveryTargets;
  private final int ttlMinutes;

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */

  // Default constructor to set defaults. This appears to be needed for when object is created from json using gson.
  public ReqSubscribe()
  {
    description = null;
    enabled = DEFAULT_ENABLED;
    jobEventCategoryFilter = null;
    deliveryTargets = null;
    ttlMinutes = DEFAULT_TTL_MINUTES;
  }

  public ReqSubscribe(String description1, Boolean enabled1, JobEventCategoryFilter jobEventCategoryFilter1,
                      List<DeliveryTarget> targets1, int ttlMinutes1)
  {
    description = description1;
    enabled = (enabled1 == null) ? DEFAULT_ENABLED : enabled1;
    jobEventCategoryFilter = jobEventCategoryFilter1;
    deliveryTargets = (targets1 == null) ? null : new ArrayList<>(targets1);
    ttlMinutes = ttlMinutes1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getDescription() { return description; }
  public Boolean isEnabled() { return enabled; }
  public JobEventCategoryFilter getJobEventCategoryFilter() { return jobEventCategoryFilter; }
  public int getTtlMinutes() { return ttlMinutes; }

  public List<DeliveryTarget> getDeliveryTargets()
  {
    return (deliveryTargets == null) ? null : new ArrayList<>(deliveryTargets);
  }
}
