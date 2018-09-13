/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.indices;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import java.util.Optional;
import javax.annotation.Nullable;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.cluster.jest.tasks.CancelTask.Builder;
import org.graylog2.indexer.cluster.jest.tasks.GetTasks;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an async task we have started, or are tracking, in Elasticsearch. Use this class to
 * interact with the task, such as stopping it, getting progress information and details.
 * <p>
 * Each task has some static metadata, such as the action performed, description, start time, id,
 * node, whether it can be cancelled, type and the parent task id if applicable, and two variable
 * parts:
 * <ul>
 * <li>elapsed time, every task has this</li>
 * <li>details about the action specific progress, not all tasks have this</li>
 * </ul>
 * </p>
 * <p>
 * The task details are polymorphic and depend on the action the task is associated with.
 * </p>
 */
public class ElasticsearchTaskView {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTaskView.class);

  private final JestClient jestClient;
  private final ObjectMapper objectMapper;
  private final JsonNode rawJson;
  private final StaticData staticData;
  private final boolean completed;
  private final JsonNode response;

  @AssistedInject
  ElasticsearchTaskView(JestClient jestClient, ObjectMapper objectMapper,
      @Assisted JsonNode rawJson) {
    this.jestClient = jestClient;
    this.objectMapper = objectMapper;
    this.rawJson = rawJson;

    StaticData staticData1 = null;
    boolean completed1 = false;
    JsonNode response = MissingNode.getInstance();
    try {
      staticData1 = objectMapper.treeToValue(rawJson.path("task"), StaticData.class);
      completed1 = rawJson.path("completed").asBoolean();
      response = rawJson.path("response");
    } catch (JsonProcessingException e) {
      LOG.error("Unable to parse task data", ExceptionUtils.getRootCause(e));
    }
    this.staticData = staticData1;
    this.completed = completed1;
    this.response = response;
  }

  public long id() {
    return staticData.id();
  }

  /**
   * Elasticsearch uses this id for uniquely identifying tasks across the cluster.
   * @return the clusterwide unique task id
   */
  public String taskId() {
    return nodeId() + ":" + id();
  }

  public String action() {
    return staticData.action();
  }

  public boolean cancellable() {
    return staticData.cancellable();
  }

  public String description() {
    return staticData.description();
  }

  public String nodeId() {
    return staticData.nodeId();
  }

  public Optional<String> parentTaskId() {
    return staticData.parentTaskId();
  }

  public String type() {
    return staticData.type();
  }

  public DateTime startTime() {
    return staticData.startTime();
  }

  public boolean isCompleted () {
    return completed;
  }

  public JsonNode responseAsJson() {
    return response;
  }

  public <T> Optional<T> responseAs(Class<T> responseKlass) {
    try {
      return Optional.of(objectMapper.treeToValue(response, responseKlass));
    } catch (JsonProcessingException e) {
      LOG.error("Expected response type {}, cannot decode task response.", responseKlass, ExceptionUtils.getRootCause(e));
      return Optional.empty();
    }
  }

  public boolean cancel() {
    if (!cancellable()) {
      LOG.error("The task " + taskId() + "/" + action() + " is not cancellable.");
      return false;
    }
    final JestResult cancelResponse = JestUtils.execute(jestClient,
        new Builder(taskId()).build(),
        () -> "Unable to cancel task " + id());
    return cancelResponse.isSucceeded();
  }

  /**
   * Blocks until this task has been completed.
   *
   * @return true if the task completed, false otherwise (which likely throw an exception)
   */
  public boolean waitForCompletion() {
    if (isCompleted()) {
      return true;
    }
    final JestResult completion = JestUtils
        .execute(jestClient, new GetTasks.Builder(taskId()).waitForCompletion(true).build(),
            () -> "Unable to wait for task " + taskId() + " to complete.");
    return completion.isSucceeded();
  }

  public interface Factory {

    /**
     * Create a new task from the given data
     *
     * @param responseJsonNode the json returned from elasticsearch, contains static data, response
     * and completion information
     * @return the current view of the task
     */
    ElasticsearchTaskView create(JsonNode responseJsonNode);
  }

  // we are not interested in all properties just yet
  @JsonIgnoreProperties(ignoreUnknown = true)
  @AutoValue
  public abstract static class StaticData {

    @JsonCreator
    public static StaticData create(
        @JsonProperty("id") long id,
        @JsonProperty("action") String action,
        @JsonProperty("cancellable") boolean cancellable,
        @JsonProperty("description") String description,
        @JsonProperty("node") String nodeId,
        @JsonProperty("parent_task_id") @Nullable String parentTaskId,
        @JsonProperty("type") String type,
        @JsonProperty("start_time_in_millis") long startTime) {
      return new AutoValue_ElasticsearchTaskView_StaticData(id, action, cancellable, description,
          nodeId, Optional.ofNullable(parentTaskId), type,
          new DateTime(startTime, DateTimeZone.UTC));
    }

    abstract long id();

    abstract String action();

    abstract boolean cancellable();

    abstract String description();

    abstract String nodeId();

    abstract Optional<String> parentTaskId();

    abstract String type();

    abstract DateTime startTime();
  }

  @AutoValue
  public abstract static class Status {

    public abstract boolean completed();

    public abstract long elapsedNanos();

  }
}
