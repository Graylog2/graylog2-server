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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationRequestSpecTest {

    @Test
    void appliesSizeToGroupingsWithoutLimit() {
        final AggregationRequestSpec spec = new AggregationRequestSpec(
                "*",
                Set.of(),
                Set.of(),
                RelativeRange.create(3600),
                List.of(new Grouping("field1"), new Grouping("field2")),
                List.of(new Metric("count", null)),
                25
        );

        assertThat(spec.groupings()).hasSize(2);
        assertThat(spec.groupings().get(0).limit()).isEqualTo(Optional.of(25));
        assertThat(spec.groupings().get(1).limit()).isEqualTo(Optional.of(25));
    }

    @Test
    void overridesExistingLimitWhenSizeProvided() {
        final AggregationRequestSpec spec = new AggregationRequestSpec(
                "*",
                Set.of(),
                Set.of(),
                RelativeRange.create(3600),
                List.of(new Grouping("field1", 10), new Grouping("field2")),
                List.of(new Metric("count", null)),
                25
        );

        assertThat(spec.groupings()).hasSize(2);
        assertThat(spec.groupings().get(0).limit()).isEqualTo(Optional.of(25));
        assertThat(spec.groupings().get(1).limit()).isEqualTo(Optional.of(25));
    }

    @Test
    void doesNotModifyGroupingsWhenSizeIsNull() {
        final AggregationRequestSpec spec = new AggregationRequestSpec(
                "*",
                Set.of(),
                Set.of(),
                RelativeRange.create(3600),
                List.of(new Grouping("field1"), new Grouping("field2", 10)),
                List.of(new Metric("count", null)),
                null
        );

        assertThat(spec.groupings()).hasSize(2);
        assertThat(spec.groupings().get(0).limit()).isEqualTo(Optional.of(15)); // Default from Grouping constructor
        assertThat(spec.groupings().get(1).limit()).isEqualTo(Optional.of(10));
    }
}
