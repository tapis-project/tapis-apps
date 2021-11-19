package edu.utexas.tacc.tapis.apps.api.responses.results;

import edu.utexas.tacc.tapis.apps.api.model.JobAttributes;
import edu.utexas.tacc.tapis.apps.model.App;
import edu.utexas.tacc.tapis.apps.model.App.JobType;
import edu.utexas.tacc.tapis.apps.model.App.Runtime;
import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;

import static edu.utexas.tacc.tapis.apps.api.resources.AppResource.SUMMARY_ATTRS;
import static edu.utexas.tacc.tapis.apps.model.App.DELETED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.TENANT_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.VERSION_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.JOB_TYPE_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.DESCRIPTION_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.OWNER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.ENABLED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.TAGS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.NOTES_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.UUID_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CONTAINERIMG_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.RUNTIME_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.RUNTIMEVER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.RUNTIMEOPTS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MAX_JOBS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.MAX_JOBS_PER_USER_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.STRICT_FILE_INPUTS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.JOB_ATTRS_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.CREATED_FIELD;
import static edu.utexas.tacc.tapis.apps.model.App.UPDATED_FIELD;

/*
  Classes representing an App result to be returned
 */
public final class TapisAppDTO
{
  private static final Gson gson = TapisGsonUtils.getGson();

  public String tenant;
  public String id;
  public String version;
  public String description;
  public String owner;
  public boolean enabled;
//  public boolean containerized;
  public Runtime runtime;
  public String runtimeVersion;
  public List<RuntimeOption> runtimeOptions;
  public String containerImage;
  public JobType jobType;
  public int maxJobs;
  public int maxJobsPerUser;
  public boolean strictFileInputs;
  public JobAttributes jobAttributes;
  public String[] tags;
  public Object notes;
  public UUID uuid;
  public boolean deleted;
  public Instant created;
  public Instant updated;

  public TapisAppDTO(App a)
  {
    tenant = a.getTenant();
    id = a.getId();
    version = a.getVersion();
    description = a.getDescription();
    owner = a.getOwner();
    enabled = a.isEnabled();
//    containerized = a.isContainerized();
    runtime = a.getRuntime();
    runtimeVersion = a.getRuntimeVersion();
    runtimeOptions = a.getRuntimeOptions();
    containerImage = a.getContainerImage();
    jobType = a.getJobType();
    maxJobs = a.getMaxJobs();
    maxJobsPerUser = a.getMaxJobsPerUser();
    strictFileInputs = a.isStrictFileInputs();
    jobAttributes = new JobAttributes(a);
    tags = a.getTags();
    notes = a.getNotes();
    uuid = a.getUuid();
    deleted = a.isDeleted();
    created = a.getCreated();
    updated = a.getUpdated();
    // Check for -1 in max values and return Integer.MAX_VALUE instead.
    //   As requested by Jobs service.
    if (maxJobs < 0) maxJobs = Integer.MAX_VALUE;
    if (maxJobsPerUser < 0) maxJobsPerUser = Integer.MAX_VALUE;
  }

  /**
   * Create a JsonObject containing the id attribute and any attribute in the selectSet that matches the name
   * of a public field in this class
   * If selectSet is null or empty then all attributes are included.
   * If selectSet contains "allAttributes" then all attributes are included regardless of other items in set
   * If selectSet contains "summaryAttributes" then summary attributes are included regardless of other items in set
   * @return JsonObject containing attributes in the select list.
   */
  public JsonObject getDisplayObject(List<String> selectList)
  {
    // Check for special case of returning all attributes
    if (selectList == null || selectList.isEmpty() || selectList.contains("allAttributes"))
    {
      return allAttrs();
    }

    var retObj = new JsonObject();

    // If summaryAttrs included then add them
    if (selectList.contains("summaryAttributes")) addSummaryAttrs(retObj);

    // Include specified list of attributes
    // If ID not in list we add it anyway.
    if (!selectList.contains(ID_FIELD)) addDisplayField(retObj, ID_FIELD);
    for (String attrName : selectList)
    {
      addDisplayField(retObj, attrName);
    }
    return retObj;
  }

  // Build a JsonObject with all displayable attributes
  private JsonObject allAttrs()
  {
    String jsonStr = gson.toJson(this);
    return gson.fromJson(jsonStr, JsonObject.class).getAsJsonObject();
  }

  // Add summary attributes to a json object
  private void addSummaryAttrs(JsonObject jsonObject)
  {
    for (String attrName: SUMMARY_ATTRS)
    {
      addDisplayField(jsonObject, attrName);
    }
  }

  /**
   * Add specified attribute name to the JsonObject that is to be returned as the displayable object.
   * If attribute does not exist in this class then it is a no-op.
   *
   * @param jsonObject Base JsonObject that will be returned.
   * @param attrName Attribute name to add to the JsonObject
   */
  private void addDisplayField(JsonObject jsonObject, String attrName)
  {
    String jsonStr;
    switch (attrName) {
      case TENANT_FIELD -> jsonObject.addProperty(TENANT_FIELD, tenant);
      case ID_FIELD -> jsonObject.addProperty(ID_FIELD, id);
      case VERSION_FIELD -> jsonObject.addProperty(VERSION_FIELD, version);
      case DESCRIPTION_FIELD ->jsonObject.addProperty(DESCRIPTION_FIELD, description);
      case OWNER_FIELD -> jsonObject.addProperty(OWNER_FIELD, owner);
      case ENABLED_FIELD -> jsonObject.addProperty(ENABLED_FIELD, Boolean.toString(enabled));
//      case CONTAINERIZED_FIELD -> jsonObject.addProperty(CONTAINERIZED_FIELD, Boolean.toString(containerized));
      case RUNTIME_FIELD -> jsonObject.addProperty(RUNTIME_FIELD, runtime.name());
      case RUNTIMEVER_FIELD -> jsonObject.addProperty(RUNTIMEVER_FIELD, runtimeVersion);
      case RUNTIMEOPTS_FIELD -> jsonObject.add(RUNTIMEOPTS_FIELD, gson.toJsonTree(runtimeOptions));
      case CONTAINERIMG_FIELD -> jsonObject.addProperty(CONTAINERIMG_FIELD, containerImage);
      case JOB_TYPE_FIELD -> jsonObject.addProperty(JOB_TYPE_FIELD, String.valueOf(jobType));
      case MAX_JOBS_FIELD -> jsonObject.addProperty(MAX_JOBS_FIELD, maxJobs);
      case MAX_JOBS_PER_USER_FIELD -> jsonObject.addProperty(MAX_JOBS_PER_USER_FIELD, maxJobsPerUser);
      case STRICT_FILE_INPUTS_FIELD -> jsonObject.addProperty(STRICT_FILE_INPUTS_FIELD, String.valueOf(strictFileInputs));
      case JOB_ATTRS_FIELD -> {
        jsonStr = gson.toJson(jobAttributes);
        jsonObject.add(JOB_ATTRS_FIELD, gson.fromJson(jsonStr, JsonObject.class));
      }
      case TAGS_FIELD -> jsonObject.add(TAGS_FIELD, gson.toJsonTree(tags));
      case NOTES_FIELD -> {
        jsonStr = gson.toJson(notes);
        jsonObject.add(NOTES_FIELD, gson.fromJson(jsonStr, JsonObject.class));
      }
      case UUID_FIELD -> jsonObject.addProperty(UUID_FIELD, uuid.toString());
      case DELETED_FIELD -> jsonObject.addProperty(DELETED_FIELD, Boolean.toString(deleted));
      case CREATED_FIELD -> jsonObject.addProperty(CREATED_FIELD, created.toString());
      case UPDATED_FIELD -> jsonObject.addProperty(UPDATED_FIELD, updated.toString());
    }
  }
}