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
package org.graylog2.database.aggregation;

import org.bson.Document;
import org.graylog2.database.aggregation.Grouping.Range;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BucketAggregationTest {
    @ParameterizedTest
    @ValueSource(strings = {"$where", "owner.$id", "a..b", ".a", "a.", "", "a b", "{\"$gt\": 1}"})
    void rejectsFieldPathsThatAreNotPlainPaths(final String path) {
        assertThatThrownBy(() -> Grouping.terms(path)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GroupedValueSource.related(path, "users", "_id", "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "event.timestamp", "assets.id", "_id", "gl2_source-input"})
    void acceptsPlainFieldPaths(final String path) {
        assertThat(Grouping.terms(path).source())
                .isEqualTo(new GroupedValueSource.Field(path));
    }

    @Test
    void rejectsSystemAndInvalidCollections() {
        assertThatThrownBy(() -> GroupedValueSource.related("owner", "system.users", "_id", "name"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GroupedValueSource.related("owner", "$users", "_id", "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aggregatesAllDocumentsWithoutAFilter() {
        final var request = BucketAggregation.of(null, Grouping.terms("owner"));

        assertThat(request.documentFilter().toBsonDocument()).isEmpty();
    }

    @Test
    void ordersByCountDescendingWithoutAnOrder() {
        final var request = BucketAggregation.of(null, Grouping.terms("owner"))
                .withOrder(BucketOrder.byKey(SortOrder.ASCENDING))
                .withOrder(null);

        assertThat(request.order()).isEqualTo(BucketOrder.byCount(SortOrder.DESCENDING));
    }

    @Test
    void validatesOrderAndLimit() {
        final var request = BucketAggregation.of(new Document(), Grouping.terms("owner"));

        assertThat(request.order()).isEqualTo(BucketOrder.byCount(SortOrder.DESCENDING));
        assertThat(request.limit()).isEqualTo(BucketAggregation.DEFAULT_LIMIT);
        assertThatThrownBy(() -> request.withLimit(0)).hasMessageContaining("Limit must be between");
        assertThatThrownBy(() -> request.withLimit(BucketAggregation.MAX_LIMIT + 1)).hasMessageContaining("Limit must be between");
    }

    @Test
    void validatesRanges() {
        final var score = GroupedValueSource.field("score");

        assertThatThrownBy(() -> Range.of("empty", (Number) null, null)).hasMessageContaining("at least one bound");
        assertThatThrownBy(() -> Range.of("backwards", 10, 5)).hasMessageContaining("start before it ends");
        assertThatThrownBy(() -> new Range("text", "a", "b")).hasMessageContaining("numbers or instants");
        assertThatThrownBy(() -> new Range("mixed", 1, Instant.EPOCH)).hasMessageContaining("mix numbers and instants");
        assertThatThrownBy(() -> Grouping.ranges(score, List.of())).hasMessageContaining("At least one range");
        assertThatThrownBy(() -> Grouping.ranges(score, List.of(Range.of("low", 0, 10), Range.of("low", 10, 20))))
                .hasMessageContaining("Duplicate range key <low>");
        assertThatThrownBy(() -> Grouping.ranges(score, List.of(Range.of("low", 0, 10), Range.of("recent", Instant.EPOCH, null))))
                .hasMessageContaining("must not mix");
    }
}
