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
package org.graylog.events.rest;

import com.github.rholder.retry.RetryException;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@GraylogBackendConfiguration(serverLifecycle = CLASS)
public class EventsResourceSlicesIT {
    private static final String SLICES_URL = "/events/slices";
    private static GraylogApis api;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) throws ExecutionException, RetryException {
        api = graylogApis;
        api.streams().createStream("stream-001", api.indices().defaultIndexSetId(), true);
        api.indices().waitForIndex("gl-events_0");

        api.backend().importElasticsearchFixture("events-slices-base.json", EventsResourceSlicesIT.class);
        api.backend().importElasticsearchFixture("events-slices-keys.json", EventsResourceSlicesIT.class);
        api.backend().importElasticsearchFixture("events-slices-filters.json", EventsResourceSlicesIT.class);
    }

    // --- Unfiltered slice tests ---

    @FullBackendTest
    void testSlicesByPriority() {
        final var slices = requestSlices("priority", "", Map.of(), false);
        assertSlice(slices, "1", 6);
        assertSlice(slices, "2", 7);
        assertSlice(slices, "3", 8);
        assertSlice(slices, "4", 5);
    }

    @FullBackendTest
    void testSlicesByAlert() {
        final var slices = requestSlices("alert", "", Map.of(), false);
        assertSlice(slices, "true", 14);
        assertSlice(slices, "false", 12);
    }

    // --- Filter combination tests ---

    @FullBackendTest
    void testSlicesByPriorityWithDefinitionFilter() {
        final var slices = requestSlices("priority", "",
                Map.of("event_definitions", Set.of("def-filter-001")), false);
        assertSlice(slices, "1", 2);
        assertSlice(slices, "2", 1);
    }

    @FullBackendTest
    void testSlicesByAlertWithPriorityFilter() {
        final var slices = requestSlices("alert", "",
                Map.of("priority", Set.of("1")), false);
        assertSlice(slices, "true", 5);
        assertSlice(slices, "false", 1);
    }

    @FullBackendTest
    void testSlicesWithQuery() {
        final var slicesAll = requestSlices("priority", "", Map.of(), false);
        final var slicesFiltered = requestSlices("priority", "message:\"does-not-exist-xyz\"", Map.of(), false);

        final int totalAll = slicesAll.stream()
                .mapToInt(s -> ((Number) ((Map<?, ?>) s).get("count")).intValue())
                .sum();
        final int totalFiltered = slicesFiltered.stream()
                .mapToInt(s -> ((Number) ((Map<?, ?>) s).get("count")).intValue())
                .sum();
        assertThat(totalFiltered).isLessThan(totalAll);
    }

    // --- Helper methods ---

    private List<Object> requestSlices(String sliceColumn, String query, Map<String, Object> filter, boolean includeAll) {
        final var requestBody = createRequest(sliceColumn, query, filter, includeAll);
        return given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .body(requestBody)
                .post(SLICES_URL)
                .then()
                .log()
                .ifStatusCodeMatches(Matchers.not(HTTP_OK))
                .statusCode(HTTP_OK)
                .extract()
                .jsonPath()
                .getList("slices");
    }

    private Map<String, Object> createRequest(String sliceColumn, String query, Map<String, Object> filter, boolean includeAll) {
        final var request = new HashMap<String, Object>();
        request.put("query", query);
        request.put("slice_column", sliceColumn);
        request.put("include_all", includeAll);
        request.put("timerange", Map.of(
                "type", "absolute",
                "from", "2025-01-15T00:00:00.000Z",
                "to", "2025-01-16T00:00:00.000Z"
        ));
        request.put("filter", filter);
        return request;
    }

    @SuppressWarnings("unchecked")
    private void assertSlice(List<Object> slices, String value, int expectedCount) {
        assertThat(slices)
                .anySatisfy(s -> {
                    final var slice = (Map<String, Object>) s;
                    assertThat(slice.get("value")).isEqualTo(value);
                    assertThat(((Number) slice.get("count")).intValue()).isEqualTo(expectedCount);
                });
    }
}
