/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster.jest.tasks;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import io.searchbox.core.Index.Builder;
import io.searchbox.indices.reindex.Reindex;
import org.assertj.core.api.ObjectAssert;
import org.graylog2.ElasticsearchBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class GetTasksTestIT extends ElasticsearchBase {

    private static final Logger LOG = LoggerFactory.getLogger(GetTasksTestIT.class);
    private static final MissingNode MISSING_NODE = MissingNode.getInstance();

    private final static ObjectMapper om = new ObjectMapper();

    private static void dump(JestResult result, String title) {
        dump(result.getJsonObject(), title);
    }

    private static void dump(JsonNode node, String title) {
        try {
            LOG.info("{}:\n{}", title, om.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        } catch (JsonProcessingException e) {
            LOG.error("Couldn't dump json!", e);
        }
    }

    // A helper to avoid assertThat() creating IterableAssert objects: JsonNode is also Iterable<JsonNode>!
    private static ObjectAssert<JsonNode> assertThatJsonNode(JsonNode node) {
        return new ObjectAssert<>(node);
    }

    @Test
    public void getTasks() throws IOException {
        final GetTasks getTasks = new GetTasks.Builder().build();

        final JestResult result = client().execute(getTasks);
        assertThat(result.isSucceeded()).isTrue();

        final JsonNode json = result.getJsonObject();
        assertThat(json.path("nodes")).isNotEqualTo(MISSING_NODE);
        for (JsonNode node : json.path("nodes")) {
            assertThat(node.path("name")).isNotEqualTo(MISSING_NODE);
            assertThat(node.path("tasks")).isNotEqualTo(MISSING_NODE);
        }
    }

    @Test
    public void findLongRunningTask() throws IOException {
        final String indexName = createRandomIndex("gettasktest");
        final String reindexName = createRandomIndex("reindex");
        String reindexTaskId;
        try {
            prepareSourceIndex(indexName);

            reindexTaskId = longRunningReindex(indexName, reindexName);
            assertThat(reindexTaskId).isNotBlank();

            final GetTasks getTasks = new GetTasks.Builder().detailed(true).build();
            final JestResult tasksResult = client().execute(getTasks);
            dump(tasksResult, "tasks");
            assertThat(tasksResult.getJsonObject().findValuesAsText("action"))
                    .contains("indices:data/write/reindex");

            final JsonNode reindexTaskNode = tasksResult.getJsonObject().findPath(reindexTaskId);
            assertThat(reindexTaskNode.isObject()).isTrue();
            assertThat(reindexTaskNode.path("cancellable").isBoolean()).isTrue();

            final JestResult filteredTasksResult = client()
                    .execute(new GetTasks.Builder().detailed(true).actions("*reindex").build());
            dump(filteredTasksResult, "filtered tasks");
            assertThat(filteredTasksResult.isSucceeded()).isTrue();
            final JsonNode filteredTasksJson = filteredTasksResult.getJsonObject();
            // we should only have one task overall because we filtered on the only reindex task
            assertThat(filteredTasksJson.findValues(reindexTaskId)).hasSize(1);
            // repeat some generic checks as above when we manually peeled out the task result
            final JsonNode ourReindexTask = filteredTasksJson.findValue(reindexTaskId);
            assertThat(ourReindexTask.isObject()).isTrue();
            assertThat(ourReindexTask.path("cancellable").isBoolean()).isTrue();

//    this is unfortunate, because if there aren't any child tasks running _right_ now,
//    then the response is empty. Not much to check for other than the success of the call itself

            // get all child tasks of the reindexing task
            final JestResult ofParentResult = client()
                    .execute(new GetTasks.Builder().ofParentTaskId(reindexTaskId).build());
            assertThat(ofParentResult.isSucceeded()).isTrue();
            final JsonNode parentJson = ofParentResult.getJsonObject();
            dump(parentJson, "using parent_task_id (can be empty)");

            // cancel the long running task
            // we assume the task takes more than 1 second to complete
            try {
                final JestResult waitForCompleteTimeout = client().execute(
                        new GetTasks.Builder(reindexTaskId)
                                .waitForCompletion(true)
                                .timeout("250ms")
                                .build());
                dump(waitForCompleteTimeout, "wait for completion with timeout");
                assertThat(waitForCompleteTimeout.isSucceeded()).isFalse();
            } catch (SocketTimeoutException e) {
                // we expect a timeout here, all fine
                System.out.println("Reindex still running, canceling it.");
            } catch (Exception e) {
                fail("Unexcepted exception", e);
            }

            final CancelTask cancelTask = new CancelTask.Builder(reindexTaskId).build();
            final JestResult cancelResult = client().execute(cancelTask);
            dump(cancelResult, "canceled task");
            assertThat(cancelResult.isSucceeded()).isTrue();

            // request the canceled task again and check that it is "done"
            final GetTasks getCanceledTask = new GetTasks.Builder(reindexTaskId).build();
            final JestResult canceledTaskResult = client().execute(getCanceledTask);
            dump(canceledTaskResult, "after cancellation");
            assertThat(canceledTaskResult.isSucceeded()).isTrue();
            final JsonNode canceledJson = canceledTaskResult.getJsonObject();
            assertThatJsonNode(canceledJson.path("completed"))
                    .matches(JsonNode::isBoolean)
                    .matches(completed -> !completed.asBoolean(), "task is not completed");
            assertThatJsonNode(canceledJson.at("/task/status/canceled"))
                    .isNotEqualTo(MISSING_NODE)
                    .matches(JsonNode::isTextual)
                    .matches(status -> status.asText().equalsIgnoreCase("by user request"));
        } finally {
            deleteIndex(indexName);
            deleteIndex(reindexName);
        }
    }

    private void prepareSourceIndex(String indexName) throws IOException {
        // we don't actually care about the document content, we just need some of them for scrolling
        final Index doc = new Builder(of("message", "something")).build();

        final Bulk.Builder bulkBuilder = new Bulk.Builder();
        for (int i = 0; i < 10; i++) {
            bulkBuilder.addAction(Collections.nCopies(10000, doc));
            final BulkResult bulkResult = client().execute(
                    bulkBuilder
                            .defaultIndex(indexName)
                            .defaultType("test")
                            .build()
            );
            assertThat(bulkResult.isSucceeded()).isTrue();
        }
    }

    private String longRunningReindex(String indexName, String reindexName)
            throws IOException {
        String reindexTaskId;
        final Reindex reindex = new Reindex.Builder(
                // use very small batch sizes
                of("index", indexName, "size", 1),
                of("index", reindexName))
                // mas despacio, por favor
                .requestsPerSecond(1)
                .waitForCompletion(false)
                .build();
        final JestResult reindexResult = client().execute(reindex);
        dump(reindexResult, "reindex response");
        assertThat(reindexResult.isSucceeded()).isTrue();
        reindexTaskId = reindexResult.getJsonObject().findPath("task").asText();
        return reindexTaskId;
    }
}