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
package org.graylog.storage.opensearch3;

import org.assertj.core.api.Assertions;
import org.graylog.events.event.EventDto;
 import org.graylog2.indexer.searches.Sorting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.PlainJsonSerializable;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoreSearchAdapterOSTest {
    private static final String FIELD = "field";
    private static final JsonData VALUE = JsonData.of("100");
    private MoreSearchAdapterOS adapter;

    @BeforeEach
    void setUp() {
        adapter = new MoreSearchAdapterOS(
                null,  // opensearchClient - not needed for sorting tests
                true,  // allowLeadingWildcard
                null,  // multiChunkResultRetriever - not needed for sorting tests
                null   // messageFactory - not needed for sorting tests
        );
    }

    @Test
    void testBuildExtraFilter() {
        verifyFilter("<=100", Query.builder().range(range -> range.field(FIELD).lte(VALUE)).build());
        verifyFilter(">=100", Query.builder().range(range -> range.field(FIELD).gte(VALUE)).build());
        verifyFilter("<100", Query.builder().range(range -> range.field(FIELD).lt(VALUE)).build());
        verifyFilter(">100", Query.builder().range(range -> range.field(FIELD).gt(VALUE)).build());
        verifyFilter("100", Query.builder().multiMatch(mm -> mm.query("100").fields(FIELD)).build());
    }

    @Test
    void testNewCreateSortingShouldReturnTwoSortOptionsForTimerangeStart() {
        // Given
        final Sorting sorting = new Sorting(EventDto.FIELD_TIMERANGE_START, Sorting.Direction.ASC);

        // When
        final List<SortOptions> sortOptions = adapter.newCreateSorting(sorting);

        // Then
        assertThat(sortOptions).hasSize(2);
        assertThat(sortOptions.get(0).field().field()).isEqualTo(EventDto.FIELD_TIMERANGE_START);
        assertThat(sortOptions.get(1).field().field()).isEqualTo(EventDto.FIELD_TIMERANGE_END);
    }

    @Test
    void testNewCreateSortingShouldReturnOneSortOptionForRegularField() {
        // Given
        final Sorting sorting = new Sorting("custom_field", Sorting.Direction.DESC);

        // When
        final List<SortOptions> sortOptions = adapter.newCreateSorting(sorting);

        // Then
        assertThat(sortOptions).hasSize(1);
        assertThat(sortOptions.get(0).field().field()).isEqualTo("custom_field");
    }

    @Test
    void testNewCreateSortingShouldApplySortOrderCorrectly() {
        // Given
        final Sorting sortingAsc = new Sorting("test_field", Sorting.Direction.ASC);
        final Sorting sortingDesc = new Sorting("test_field", Sorting.Direction.DESC);

        // When
        final List<SortOptions> sortOptionsAsc = adapter.newCreateSorting(sortingAsc);
        final List<SortOptions> sortOptionsDesc = adapter.newCreateSorting(sortingDesc);

        // Then
        assertThat(sortOptionsAsc.get(0).field().order().jsonValue()).isEqualTo("asc");
        assertThat(sortOptionsDesc.get(0).field().order().jsonValue()).isEqualTo("desc");
    }

    private static void verifyFilter(String value, Query expected) {
        Query generatedQuery = MoreSearchAdapterOS.buildExtraFilter(FIELD, value);
        Assertions.assertThat(generatedQuery)
                .extracting(PlainJsonSerializable::toJsonString)
                .isEqualTo(expected.toJsonString());
    }
}
