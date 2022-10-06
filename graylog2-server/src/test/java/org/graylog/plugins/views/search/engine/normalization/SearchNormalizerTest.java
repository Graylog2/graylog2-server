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
package org.graylog.plugins.views.search.engine.normalization;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class SearchNormalizerTest {

    @Test
    void normalizesAllQueriesInSearch() {
        SearchNormalizer exclamationAddingTestNormalizer = (query, p) -> query.toBuilder().id(query.id() + "!").build();

        Search toTest = Search.builder()
                .id("test_search")
                .queries(ImmutableSet.of(
                        Query.builder().id("Hey").build(),
                        Query.builder().id("Ho").build()
                ))
                .build();

        Search normalized = exclamationAddingTestNormalizer.normalize(toTest);

        assertThat(normalized.queries())
                .hasSize(2)
                .contains(Query.builder().id("Hey!").build())
                .contains(Query.builder().id("Ho!").build());
    }

}
