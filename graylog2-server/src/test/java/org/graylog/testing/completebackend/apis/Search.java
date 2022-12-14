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
import io.restassured.specification.RequestSpecification;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;


public class Search implements GraylogRestApi {

    public List<String> searchForAllMessages() {
        List<String> messages = new ArrayList<>();

        waitFor(() -> captureMessages(messages::addAll), "Timed out waiting for messages to be present");

        return messages;
    }

    public Search waitForMessage(String message) {
        waitFor(() -> captureMessage(message), "Timed out waiting for message to be present");
        return this;
    }


    public Search waitForMessages(String... messages) {
        waitFor(() -> searchAllMessages().containsAll(Arrays.asList(messages)), "Timed out waiting for message to be present");
        return this;
    }

    private boolean captureMessage(String message) {
        return searchAllMessages().contains(message);
    }



    private boolean captureMessages(Consumer<List<String>> messagesCaptor) {
        List<String> messages = searchAllMessages();
        if (!messages.isEmpty()) {
            messagesCaptor.accept(messages);
            return true;
        }
        return false;
    }

    public void waitForMessagesCount(int count) {
        waitFor(() -> searchForAllMessages().size() >= count, "Failed to wait for messages count:" + count);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Search.class);
    private final RequestSpecification requestSpecification;

    public Search(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    /**
     * @return all messages' "message" field as List<String>
     */
    public List<String> searchAllMessages() {
        return searchAllMessagesInTimeRange(allMessagesTimeRange());
    }

    public List<String> searchAllMessagesInTimeRange(TimeRange timeRange) {
        String queryId = "query-id";
        String messageListId = "message-list-id";

        String body = allMessagesJson(queryId, messageListId, timeRange);

        final JsonPath response = given()
                .spec(this.requestSpecification)
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

    private String allMessagesJson(String queryId, String messageListId, TimeRange timeRange) {
        MessageList messageList = MessageList.builder().id(messageListId).build();
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


    public AbsoluteRange allMessagesTimeRange() {
        return AbsoluteRange.create("2010-01-01T00:00:00.0Z", "2050-01-01T00:00:00.0Z");
    }

    public String toJsonString(Object s) {
        try {
            return new ObjectMapperProvider().get().writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Search", e);
        }
    }
}
