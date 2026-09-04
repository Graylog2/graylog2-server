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
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.RangeQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.FieldSortBuilder;
import org.graylog2.indexer.searches.Sorting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MoreSearchAdapterES7Test {
    private static final String FIELD = "field";
    private static final String VALUE = "100";
    private MoreSearchAdapterES7 adapter;

    @BeforeEach
    void setUp() {
        adapter = new MoreSearchAdapterES7(
                null,
                null,  // opensearchClient - not needed for sorting tests
                true,  // allowLeadingWildcard
                null  // multiChunkResultRetriever - not needed for sorting tests
        );
    }

    @Test
    void testBuildExtraFilter() {
        verifyFilter("<=100", QueryBuilders.rangeQuery(FIELD).lte(VALUE), RangeQueryBuilder.class);
        verifyFilter(">=100", QueryBuilders.rangeQuery(FIELD).gte(VALUE), RangeQueryBuilder.class);
        verifyFilter("<100", QueryBuilders.rangeQuery(FIELD).lt(VALUE), RangeQueryBuilder.class);
        verifyFilter(">100", QueryBuilders.rangeQuery(FIELD).gt(VALUE), RangeQueryBuilder.class);
        verifyFilter(VALUE, QueryBuilders.multiMatchQuery(VALUE, FIELD), MultiMatchQueryBuilder.class);
    }

    @Test
    void testSortingWithUnmappedTypeAscShouldUseMissingFirst() {
        // Given
        final Sorting sorting = new Sorting("some_field", Sorting.Direction.ASC, "date");

        // When
        final List<FieldSortBuilder> sortOptions = adapter.createSorting(sorting);

        // Then
        assertThat(sortOptions).hasSize(1);
        assertThat(sortOptions.get(0).missing()).isEqualTo("_first");
    }

    @Test
    void testSortingWithUnmappedTypeDescShouldUseMissingLast() {
        // Given
        final Sorting sorting = new Sorting("some_field", Sorting.Direction.DESC, "date");

        // When
        final List<FieldSortBuilder> sortOptions = adapter.createSorting(sorting);

        // Then
        assertThat(sortOptions).hasSize(1);
        assertThat(sortOptions.get(0).missing()).isEqualTo("_last");
    }

    @Test
    void testSortingWithoutUnmappedTypeShouldHaveNoMissingValue() {
        // Given
        final Sorting sorting = new Sorting("some_field", Sorting.Direction.ASC);

        // When
        final List<FieldSortBuilder> sortOptions = adapter.createSorting(sorting);

        // Then
        assertThat(sortOptions).hasSize(1);
        assertThat(sortOptions.get(0).missing()).isNull();
    }

    private static void verifyFilter(String value, QueryBuilder expectedFilter, Class<? extends QueryBuilder> expectedFilterClass) {
        QueryBuilder lessThanOrEqualFilter = MoreSearchAdapterES7.buildExtraFilter(FIELD, value);
        assertInstanceOf(expectedFilterClass, lessThanOrEqualFilter);
        assertEquals(expectedFilter, lessThanOrEqualFilter);
    }
}
