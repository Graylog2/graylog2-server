/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.completebackend.apis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


public class Search implements GraylogRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(Search.class);
    private static final Duration WAIT_FOR_MESSAGES_TIMEOUT = Duration.ofSeconds(300);

    private final GraylogApis api;

    public Search(GraylogApis api) {
        this.api = api;
    }

    public Search waitForMessage(String message) {
        return waitForMessages(message);
    }

    public Search waitForMessages(String... messages) {
        return waitForMessages(Arrays.asList(messages));
    }

    public Search waitForMessages(Collection<String> messages) {
        return waitForMessages(messages, RelativeRange.allTime(), Set.of());
    }

    public Search waitForMessages(Collection<String> messages, TimeRange timeRange, Set<String> streams) {
        try {
          waitFor(() -> searchMessages(timeRange, streams).containsAll(messages), "Timed out waiting for messages to be present", WAIT_FOR_MESSAGES_TIMEOUT);
       } catch (AssertionError error) {
            final var results = searchMessages(timeRange, streams);
            LOG.error("Messages we're waiting for: {}", String.join(", ", messages));
            LOG.error("Messages found: {}", String.join(", ", results));
            api.log();
            throw error;
        }
        return this;
    }

    public void waitForMessagesCount(int count) {
        waitFor(() -> searchMessages().size() >= count, "Failed to wait for messages count:" + count);
    }

   /**
     * @return all messages' "message" field as List<String>
     */
    public List<String> searchMessages() {
        return searchMessages(RelativeRange.allTime());
    }

    public List<String> searchMessages(TimeRange timeRange) {
        return searchMessages(timeRange, Set.of());
    }

    public List<String> searchMessages(TimeRange timeRange, Set<String> streams) {
        String queryId = "query-id";
        String messageListId = "message-list-id";

        String body = allMessagesJson(queryId, messageListId, timeRange, streams);

        final JsonPath response = given()
                .spec(api.requestSpecification())
                .when()
                .body(body)
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat().body("execution.completed_exceptionally", notNullValue())
                .extract().body().jsonPath();

        if (response.get("execution.completed_exceptionally")) {
            final Object errors = response.getString("errors");
            LOG.warn("Failed to obtain messages: {}", errors);
        }

        return response.getList(allMessagesJsonPath(queryId, messageListId), String.class);
    }

    private String allMessagesJson(String queryId, String messageListId, TimeRange timeRange, Set<String> streams) {
        MessageList messageList = MessageList.builder().streams(streams).id(messageListId).build();
        QueryDTO q = QueryDTO.builder()
                .id(queryId)
                .query(ElasticsearchQueryString.of(""))
                .timerange(timeRange)
                .searchTypes(ImmutableSet.of(messageList))
                .build();
        SearchDTO s = SearchDTO.builder()
                .id(new ObjectId().toHexString())
                .queries(q)
                .build();

        return toJsonString(s);
    }

    @SuppressWarnings("SameParameterValue")
    private static String allMessagesJsonPath(String queryId, String messageListId) {
        return "results." + queryId + ".search_types." + messageListId + ".messages.message.message";
    }

    public String toJsonString(Object s) {
        try {
            return new ObjectMapperProvider().get().writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Search", e);
        }
    }

    public ValidatableResponse executePivot(Pivot pivot) {
        return executePivot(pivot, "source:pivot-fixtures");
    }

    public ValidatableResponse executePivot(Pivot pivot, String queryString) {
        return executePivot(pivot, queryString, Set.of());
    }

    public ValidatableResponse executePivot(Pivot pivot, String queryString, Set<String> streams) {
        final var pivotName = "pivotaggregation";
        final var pivotPath = "results.query1.search_types." + pivotName;
        final Pivot pivotWithId = pivot.toBuilder()
                .id(pivotName)
                .build();

        final SearchDTO search = SearchDTO.builder()
                .queries(QueryDTO.builder()
                        .timerange(RelativeRange.allTime())
                        .id("query1")
                        .query(ElasticsearchQueryString.of(queryString))
                        .filter(StreamFilter.anyIdOf(streams.toArray(new String[0])))
                        .searchTypes(Set.of(pivotWithId))
                        .build())
                .build();

        return given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .body(toJsonString(search))
                .post("/views/search/sync")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .body("execution.completed_exceptionally", equalTo(false))
                .body("execution.done", equalTo(true))
                .rootPath(pivotPath);
    }
}
