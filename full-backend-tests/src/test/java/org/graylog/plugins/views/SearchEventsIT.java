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
package org.graylog.plugins.views;

import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog2.database.filtering.AttributeFilter;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@GraylogBackendConfiguration
public class SearchEventsIT {
    private static final String SEARCH_TYPE_NAME = "eventlist";
    private static final String SEARCH_TYPE_PATH = "results.query1.search_types." + SEARCH_TYPE_NAME;

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
        api.backend().importElasticsearchFixture("SearchEventsIT-events.json", SearchEventsIT.class);
    }

    @FullBackendTest
    void testReturnsAllEvents() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(5))
                .body("total_results", equalTo(5));
    }

    @FullBackendTest
    void testEventsContainExpectedFields() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .sort(new EventList.SortConfig("timestamp", EventList.Direction.ASC))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        // First event (oldest): "High CPU usage detected", non-alert, priority 2
        response.rootPath(SEARCH_TYPE_PATH + ".events[0]")
                .body("id", equalTo("event-001"))
                .body("message", equalTo("High CPU usage detected"))
                .body("timestamp", equalTo("2024-01-15T10:00:00.000Z"))
                .body("alert", is(false))
                .body("event_definition_id", equalTo("eventdef-001"))
                .body("priority", equalTo(2));

        // Second event: "High memory usage detected", alert, priority 3
        response.rootPath(SEARCH_TYPE_PATH + ".events[1]")
                .body("id", equalTo("event-002"))
                .body("message", equalTo("High memory usage detected"))
                .body("timestamp", equalTo("2024-01-15T10:05:00.000Z"))
                .body("alert", is(true))
                .body("event_definition_id", equalTo("eventdef-001"))
                .body("priority", equalTo(3));
    }

    @FullBackendTest
    void testDefaultSortIsTimestampDescending() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        final List<String> messages = response.extract()
                .jsonPath()
                .getList(SEARCH_TYPE_PATH + ".events.message", String.class);

        // Events should be ordered by timestamp descending (newest first)
        assertThat(messages).containsExactly(
                "Authentication failures exceeded threshold",
                "Network latency spike",
                "Disk space running low",
                "High memory usage detected",
                "High CPU usage detected"
        );
    }

    @FullBackendTest
    void testSortByTimestampAscending() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .sort(new EventList.SortConfig("timestamp", EventList.Direction.ASC))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        final List<String> messages = response.extract()
                .jsonPath()
                .getList(SEARCH_TYPE_PATH + ".events.message", String.class);

        // Events should be ordered by timestamp ascending (oldest first)
        assertThat(messages).containsExactly(
                "High CPU usage detected",
                "High memory usage detected",
                "Disk space running low",
                "Network latency spike",
                "Authentication failures exceeded threshold"
        );
    }

    @FullBackendTest
    void testSortByPriorityDescending() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .sort(new EventList.SortConfig("priority", EventList.Direction.DESC))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        final List<Long> priorities = response.extract()
                .jsonPath()
                .getList(SEARCH_TYPE_PATH + ".events.priority", Long.class);

        assertThat(priorities).isSortedAccordingTo(
                (a, b) -> Long.compare(b, a)
        );
    }

    @FullBackendTest
    void testPaginationFirstPage() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(2)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(2))
                .body("total_results", equalTo(5));
    }

    @FullBackendTest
    void testPaginationSecondPage() {
        final EventList eventList = EventList.builder()
                .page(2)
                .perPage(2)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(2))
                .body("total_results", equalTo(5));
    }

    @FullBackendTest
    void testPaginationLastPage() {
        final EventList eventList = EventList.builder()
                .page(3)
                .perPage(2)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(1))
                .body("total_results", equalTo(5));
    }

    @FullBackendTest
    void testFilterByAlert() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .attributes(List.of(AttributeFilter.create("alert", List.of("true"))))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(3))
                .body("events.alert", everyItem(is(true)));
    }

    @FullBackendTest
    void testFilterByEventDefinitionId() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .attributes(List.of(AttributeFilter.create("event_definition_id", List.of("eventdef-001"))))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(2))
                .body("events.event_definition_id", everyItem(equalTo("eventdef-001")));
    }

    @FullBackendTest
    void testFilterByPriority() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .attributes(List.of(AttributeFilter.create("priority", List.of("3"))))
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("events", hasSize(2))
                .body("events.priority", everyItem(equalTo(3)));
    }

    @FullBackendTest
    void testEventResultType() {
        final EventList eventList = EventList.builder()
                .page(1)
                .perPage(10)
                .build();

        final ValidatableResponse response = executeEventList(eventList);

        response.rootPath(SEARCH_TYPE_PATH)
                .body("type", equalTo("events"));
    }

    private ValidatableResponse executeEventList(EventList eventList) {
        final EventList eventListWithId = eventList.toBuilder()
                .id(SEARCH_TYPE_NAME)
                .build();

        final SearchDTO search = SearchDTO.builder()
                .queries(QueryDTO.builder()
                        .timerange(RelativeRange.allTime())
                        .id("query1")
                        .query(ElasticsearchQueryString.of(""))
                        .searchTypes(Set.of(eventListWithId))
                        .build())
                .build();

        return given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .body(api.search().toJsonString(search))
                .post("/views/search/sync")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .body("execution.completed_exceptionally", equalTo(false))
                .body("execution.done", equalTo(true));
    }
}
