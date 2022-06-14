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
package org.graylog2.contentpacks.model.entities;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEntityTest {

    @Test
    public void testLoadsEmptySearchFiltersCollectionFromContentPack() {
        QueryEntity noFiltersQuery = QueryEntity.Builder
                .createWithDefaults()
                .id("nvmd")
                .timerange(RelativeRange.allTime())
                .query(ElasticsearchQueryString.empty())
                .build();
        assertThat(noFiltersQuery.toNativeEntity(Collections.emptyMap(), Collections.emptyMap()).filters())
                .isNotNull()
                .isEmpty();

    }

    @Test
    public void testLoadsSearchFiltersCollectionFromContentPack() {

        final ImmutableList<UsedSearchFilter> originalSearchFilters = ImmutableList.of(
                InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").disabled(true).build(),
                ReferencedQueryStringSearchFilter.create("42")
        );
        QueryEntity queryWithFilters = QueryEntity.Builder
                .createWithDefaults()
                .id("nvmd")
                .timerange(RelativeRange.allTime())
                .query(ElasticsearchQueryString.empty())
                .filters(originalSearchFilters)
                .build();
        assertThat(queryWithFilters.toNativeEntity(Collections.emptyMap(), Collections.emptyMap()).filters())
                .isNotNull()
                .isEqualTo(originalSearchFilters);
    }
}
