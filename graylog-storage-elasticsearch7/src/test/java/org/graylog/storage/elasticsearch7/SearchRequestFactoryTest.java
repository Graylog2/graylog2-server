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

import org.graylog.plugins.views.search.searchfilters.db.UsedSearchFiltersToQueryStringsMapper;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.utilities.AssertJsonPath.assertJsonPath;

class SearchRequestFactoryTest {
    private static final int BATCH_SIZE = 42;
    private static final AbsoluteRange RANGE = AbsoluteRange.create(
            DateTime.parse("2020-07-23T11:03:32.243Z"),
            DateTime.parse("2020-07-23T11:08:32.243Z")
    );
    private static final String TEST_SEARCH_FILTERS_STRING = "test-filters-string";
    private SearchRequestFactory searchRequestFactory;

    @BeforeEach
    void setUp() {
        this.searchRequestFactory = new SearchRequestFactory(new SortOrderMapper(), true, true,
                new TestSearchFilterMapper());
    }

    @Test
    void searchIncludesTimerange() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ChunkCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(RANGE)
                .build());

        assertJsonPath(search, request -> {
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.from", String.class)
                    .containsExactly("2020-07-23 11:03:32.243");
            request.jsonPathAsListOf("$.query.bool.filter..range.timestamp.to", String.class)
                    .containsExactly("2020-07-23 11:08:32.243");
        });
    }

    @Test
    void scrollSearchDoesNotHighlight() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ChunkCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(RANGE)
                .build());

        assertThat(search.toString()).doesNotContain("\"highlight\":");
    }

    @Test
    void searchIncludesProperSourceFields() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ChunkCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(RANGE)
                .fields(List.of("foo", "bar"))
                .build());

        assertJsonPath(search, request -> {
            request.jsonPathAsListOf("$._source.includes", String.class)
                    .containsExactly("foo", "bar");
            request.jsonPathAsListOf("$._source.excludes", String.class)
                    .isEmpty();
        });
    }

    @Test
    void searchIncludesSize() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ChunkCommand.builder()
                .indices(Collections.singleton("graylog_0"))
                .range(RANGE)
                .batchSize(BATCH_SIZE)
                .build());

        assertThat(search.toString()).contains("\"size\":42");
    }

    @Test
    void searchIncludesSearchFilters() {
        final SearchSourceBuilder search = this.searchRequestFactory.create(ChunkCommand.builder()
                .filters(Collections.singletonList(InlineQueryStringSearchFilter.builder()
                        .title("filter 1")
                        .queryString("test-filter-value")
                        .build()))
                .indices(Collections.singleton("graylog_0"))
                .range(RANGE)
                .batchSize(BATCH_SIZE)
                .build());

        assertThat(search.toString()).contains(TEST_SEARCH_FILTERS_STRING);
    }

    private static class TestSearchFilterMapper implements UsedSearchFiltersToQueryStringsMapper {
        @Override
        public Set<String> map(Collection<UsedSearchFilter> usedSearchFilters) {
            return Collections.singleton(TEST_SEARCH_FILTERS_STRING);
        }
    }
}
