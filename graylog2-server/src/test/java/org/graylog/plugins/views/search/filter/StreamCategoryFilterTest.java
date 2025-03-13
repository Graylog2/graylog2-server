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
package org.graylog.plugins.views.search.filter;

import org.graylog.plugins.views.search.Filter;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamCategoryFilterTest {

    @Test
    void testToStreamFilter() {
        Filter filter = StreamCategoryFilter.ofCategory("colors");
        filter = ((StreamCategoryFilter) filter).toStreamFilter(this::categoryMapping, streamId -> true);

        assertThat(filter).isInstanceOf(OrFilter.class);
        assertThat(filter.filters()).isNotNull();
        assertThat(filter.filters()).hasSize(3);
        assertThat(filter.filters().stream()).allSatisfy(f -> {
            assertThat(f).isInstanceOf(StreamFilter.class);
            assertThat(f.filters()).isNull();
        });
    }

    @Test
    void testToStreamFilterWithPermissions() {
        Filter filter = StreamCategoryFilter.ofCategory("colors");
        filter = ((StreamCategoryFilter) filter).toStreamFilter(this::categoryMapping,
                (streamId) -> List.of("blue", "red", "one", "two").contains(streamId));

        assertThat(filter).isInstanceOf(OrFilter.class);
        assertThat(filter.filters()).isNotNull();
        assertThat(filter.filters()).hasSize(2);
        assertThat(filter.filters().stream()).allSatisfy(f -> {
            assertThat(f).isInstanceOf(StreamFilter.class);
            assertThat(f.filters()).isNull();
            assertThat(List.of("blue", "red")).contains(((StreamFilter)f).streamId());
        });
    }

    @Test
    void testToStreamFilterReturnsNull() {
        Filter filter = StreamCategoryFilter.ofCategory("colors");
        filter = ((StreamCategoryFilter) filter).toStreamFilter(this::categoryMapping, (streamId) -> false);

        assertThat(filter).isNull();
    }

    private Stream<String> categoryMapping(Collection<String> categories) {
        Set<String> streams = new HashSet<>();
        if (categories.contains("colors")) {
            streams.addAll(List.of("red", "yellow", "blue"));
        }
        return streams.stream();
    }
}
