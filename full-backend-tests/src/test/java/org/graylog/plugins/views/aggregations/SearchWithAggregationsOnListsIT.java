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
package org.graylog.plugins.views.aggregations;

import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Groupings over fields that hold several values in one message. The fixture holds six sign-ins:
 * <pre>
 * user_name            country     port
 * [admin, bob]         DE          [80, 443, 443]
 * [bob, admin]         DE          [443, 80]
 * [admin, bob, bob]    US          [80, 443]
 * admin                DE          443
 * bob                  [DE, US]    443
 * bob                  [US, DE]    80
 * </pre>
 */
@GraylogBackendConfiguration
public class SearchWithAggregationsOnListsIT {
    // Use this fixtureType in all documents of the fixture to keep this test apart from the others.
    private static final String FIXTURE_TYPE_FIELD_VALUE = "815302";

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
        api.backend().importElasticsearchFixture("messages-with-lists.json", SearchWithAggregationsOnListsIT.class);
    }

    @FullBackendTest
    void singleFieldSplitsListsByDefault() {
        execute(Values.builder().field("user_name").build())
                .body(".rows", hasSize(2))
                .body(countOf("admin"), equalTo(4))
                .body(countOf("bob"), equalTo(5));
    }

    @FullBackendTest
    void singleFieldKeepsListsWholeAsSortedSets() {
        execute(Values.builder().field("user_name").keepListsWhole().build())
                .body(".rows", hasSize(3))
                .body(countOf("[admin, bob]"), equalTo(3))
                .body(countOf("admin"), equalTo(1))
                .body(countOf("bob"), equalTo(2));
    }

    @FullBackendTest
    void singleValueFieldKeepsListsWhole() {
        execute(Values.builder().field("country").keepListsWhole().build())
                .body(".rows", hasSize(3))
                .body(countOf("DE"), equalTo(3))
                .body(countOf("US"), equalTo(1))
                .body(countOf("[DE, US]"), equalTo(2));
    }

    @FullBackendTest
    void numericFieldKeepsListsWholeWithRepeatedValues() {
        execute(Values.builder().field("port").keepListsWhole().build())
                .body(".rows", hasSize(4))
                .body(countOf("[80, 443, 443]"), equalTo(1))
                .body(countOf("[80, 443]"), equalTo(2))
                .body(countOf("443"), equalTo(2))
                .body(countOf("80"), equalTo(1));
    }

    @FullBackendTest
    void severalFieldsKeepListsWhole() {
        execute(Values.builder().fields(List.of("user_name", "country")).keepListsWhole().build())
                .body(".rows", hasSize(4))
                .body(countOf("[admin, bob]", "DE"), equalTo(2))
                .body(countOf("[admin, bob]", "US"), equalTo(1))
                .body(countOf("admin", "DE"), equalTo(1))
                .body(countOf("bob", "[DE, US]"), equalTo(2));
    }

    private ValidatableResponse execute(Values grouping) {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(grouping.withLimit(10))
                .series(Count.builder().build())
                .build();
        return api.search().executePivot(pivot, "fixtureType:" + FIXTURE_TYPE_FIELD_VALUE)
                .body(".total", equalTo(6));
    }

    private static String countOf(String... keys) {
        final String key = Arrays.stream(keys).map(k -> f("'%s'", k)).collect(Collectors.joining(", "));
        return f(".rows.find{ it.key == [%s] }.values[0].value", key);
    }
}
