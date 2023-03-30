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
package org.graylog.plugins.views.search.errors;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.EmptyTimeRange;
import org.graylog2.indexer.ElasticsearchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SearchTypeErrorParserTest {

    private Query query;

    @BeforeEach
    void setUp() {
        query = Query.builder()
                .id("test_query")
                .timerange(EmptyTimeRange.emptyTimeRange())
                .query(ElasticsearchQueryString.empty())
                .filter(null)
                .build();
    }

    @Test
    void returnsResultWindowLimitError() {
        final ElasticsearchException elasticsearchException = new ElasticsearchException("Result window is too large, [from + size] must be less than or equal to: [42]");
        final SearchTypeError error = SearchTypeErrorParser.parse(query, "searchTypeId", elasticsearchException);

        assertThat(error).isInstanceOf(ResultWindowLimitError.class);

        assertThat((ResultWindowLimitError) error)
                .satisfies(e -> assertEquals(42, e.getResultWindowLimit()))
                .satisfies(e -> assertEquals("searchTypeId", e.searchTypeId()))
                .satisfies(e -> assertEquals("test_query", e.queryId()));
    }

    @Test
    void returnsResultWindowLimitErrorIfPresentInTheExceptionsCauseChain() {
        final ElasticsearchException elasticsearchException = new ElasticsearchException(
                "Something is wrong!",
                new IllegalStateException(
                        "Run for your lives!!!",
                        new ElasticsearchException("Result window is too large, [from + size] must be less than or equal to: [42]")
                ));
        final SearchTypeError error = SearchTypeErrorParser.parse(query, "searchTypeId", elasticsearchException);

        assertThat(error).isInstanceOf(ResultWindowLimitError.class);

        assertThat((ResultWindowLimitError) error)
                .satisfies(e -> assertEquals(42, e.getResultWindowLimit()))
                .satisfies(e -> assertEquals("searchTypeId", e.searchTypeId()))
                .satisfies(e -> assertEquals("test_query", e.queryId()));
    }

    @Test
    void returnsSearchTypeErrorIfNoResultWindowLimitErrorPresent() {
        final ElasticsearchException elasticsearchException = new ElasticsearchException(
                "Something is wrong!",
                new IllegalStateException(
                        "Oh my!!!",
                        new ElasticsearchException("Your Elasticsearch is on a sick leave. If you want your data, ask Opensearch instead.")
                ));
        final SearchTypeError error = SearchTypeErrorParser.parse(query, "searchTypeId", elasticsearchException);

        assertThat(error)
                .satisfies(e -> assertEquals("searchTypeId", e.searchTypeId()))
                .satisfies(e -> assertEquals("test_query", e.queryId()));
    }
}
