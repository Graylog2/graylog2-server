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
import org.graylog.plugins.views.search.searchfilters.model.DBSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

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
        final String originalId = "42";
        final String expectedId = "43";

        final ImmutableList<UsedSearchFilter> originalSearchFilters = ImmutableList.of(
                InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").disabled(true).build(),
                ReferencedQueryStringSearchFilter.create(originalId).withQueryString("method:GET")
        );

        final ImmutableList<UsedSearchFilter> expectedSearchFilters = ImmutableList.of(
                InlineQueryStringSearchFilter.builder().title("title").description("descr").queryString("*").disabled(true).build(),
                ReferencedQueryStringSearchFilter.create(expectedId).withQueryString("method:GET")
        );

        final Map<EntityDescriptor, Object> nativeEntities = Map.of(EntityDescriptor.create(originalId, ModelTypes.SEARCH_FILTER_V1), new TestDBSearchFilter(expectedId));

        QueryEntity queryWithFilters = QueryEntity.Builder
                .createWithDefaults()
                .id("nvmd")
                .timerange(RelativeRange.allTime())
                .query(ElasticsearchQueryString.empty())
                .filters(originalSearchFilters)
                .build();
        assertThat(queryWithFilters.toNativeEntity(Collections.emptyMap(), nativeEntities).filters())
                .isNotNull()
                .isEqualTo(expectedSearchFilters);
    }

    private class TestDBSearchFilter implements DBSearchFilter {
        String id;

        TestDBSearchFilter(String id) {
            this.id = id;
        }
        @Override
        public String id() {
            return id;
        }
    }
}
