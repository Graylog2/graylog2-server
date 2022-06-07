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
package org.graylog.testing.backenddriver;

import com.google.common.collect.ImmutableSet;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.testing.utils.JsonUtils;
import org.graylog.testing.utils.RangeUtils;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * WIP. This class illustrates how we could reuse common functionality for integration tests.
 * It might make sense to configure it with @link io.restassured.specification.RequestSpecification
 * and whatever else it might need in the future and make it injectable into the test class via
 *
 * @link org.graylog.testing.completebackend.GraylogBackendExtension
 * We should do that later, if we find that implementing more functionality here is useful and feasible.
 */
public class SearchDriver {

    private static final Logger LOG = LoggerFactory.getLogger(SearchDriver.class);

    /**
     * @param requestSpec @see io.restassured.specification.RequestSpecification
     * @return all messages' "message" field as List<String>
     */
    public static List<String> searchAllMessages(RequestSpecification requestSpec) {
        return searchAllMessagesInTimeRange(requestSpec, RangeUtils.allMessagesTimeRange());
    }

    public static List<String> searchAllMessagesInTimeRange(RequestSpecification requestSpec, TimeRange timeRange) {
        String queryId = "query-id";
        String messageListId = "message-list-id";

        String body = allMessagesJson(queryId, messageListId, timeRange);

        final JsonPath response = given()
                .spec(requestSpec)
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

    private static String allMessagesJson(String queryId, String messageListId, TimeRange timeRange) {
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

        return JsonUtils.toJsonString(s);
    }

    @SuppressWarnings("SameParameterValue")
    private static String allMessagesJsonPath(String queryId, String messageListId) {
        return "results." + queryId + ".search_types." + messageListId + ".messages.message.message";
    }
}
