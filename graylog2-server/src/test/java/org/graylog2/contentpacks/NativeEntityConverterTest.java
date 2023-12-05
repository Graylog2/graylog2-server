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
package org.graylog2.contentpacks;

import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class NativeEntityConverterTest {

    private NativeEntityConverter<?> toTest;

    @BeforeEach
    void setUp() {
        //only default methods will be tested, implementation of others does not matter
        toTest = (parameters, nativeEntities) -> null;
    }

    @Test
    void testSearchFilterConversionReturnsEmptyListOnNullInput() {
        assertThat(toTest.convertSearchFilters(null)).isEmpty();
    }

    @Test
    void testSearchFilterConversionReturnsEmptyListOnEmptyInput() {
        assertThat(toTest.convertSearchFilters(List.of())).isEmpty();
    }

    @Test
    void testSearchFilterConversionKeepsOriginalFiltersAndInlinesReferencedOnes() {
        final ReferencedQueryStringSearchFilter shouldBeInlined = ReferencedQueryStringSearchFilter.builder()
                .id("Referenced")
                .queryString("smth:nice")
                .description("Will be inlined")
                .build();
        final InlineQueryStringSearchFilter shouldStayUnchanged = InlineQueryStringSearchFilter.builder()
                .queryString("smth:normal")
                .description("Will stay unchanged")
                .build();

        final List<UsedSearchFilter> convertedSearchFilters = toTest.convertSearchFilters(List.of(
                shouldBeInlined,
                shouldStayUnchanged
        ));

        assertThat(convertedSearchFilters)
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        InlineQueryStringSearchFilter.builder()
                                .queryString("smth:nice")
                                .description("Will be inlined")
                                .build(),
                        shouldStayUnchanged
                );
    }
}
