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
package org.graylog.storage.opensearch3.views;

import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MutableSearchRequestBuilderTest {

    @Test
    void copyPreservesAllFields() {
        final var query = QueryBuilders.matchAll().build().toQuery();
        final var trackHits = TrackHits.of(t -> t.enabled(true));
        final var indices = List.of("index-1", "index-2");
        final var cancelAfter = Time.of(t -> t.time("30s"));
        final var source = SourceConfig.of(s -> s.fetch(true));
        final var searchAfter = List.of(FieldValue.of("value1"));
        final var sortOption = SortOptions.of(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)));

        final var original = new MutableSearchRequestBuilder()
                .query(query)
                .from(10)
                .size(25)
                .trackTotalHits(trackHits)
                .index(indices)
                .allowNoIndices(true)
                .ignoreUnavailable(true)
                .expandWildcards(ExpandWildcard.Open)
                .cancelAfterTimeInterval(cancelAfter)
                .preference("_local")
                .sort(sortOption)
                .source(source)
                .searchAfter(searchAfter);

        final var copy = original.copy();

        assertThat(copy.query).isEqualTo(query);
        assertThat(copy.from).isEqualTo(10);
        assertThat(copy.size).isEqualTo(25);
        assertThat(copy.trackTotalHits).isEqualTo(trackHits);
        assertThat(copy.indices).isEqualTo(indices);
        assertThat(copy.allowNoIndices).isTrue();
        assertThat(copy.ignoreUnavailable).isTrue();
        assertThat(copy.expandWildcards).containsExactly(ExpandWildcard.Open);
        assertThat(copy.cancelAfterTimeInterval).isEqualTo(cancelAfter);
        assertThat(copy.preference).isEqualTo("_local");
        assertThat(copy.sort).containsExactly(sortOption);
        assertThat(copy.source).isEqualTo(source);
        assertThat(copy.searchAfter).isEqualTo(searchAfter);
    }

    @Test
    void addingAggregationToCopyDoesNotAffectOriginal() {
        final var agg = new MutableNamedAggregationBuilder("original_agg",
                new Aggregation.Builder().valueCount(v -> v.field("field1")));

        final var original = new MutableSearchRequestBuilder()
                .aggregation(agg);

        final var copy = original.copy();

        final var newAgg = new MutableNamedAggregationBuilder("copy_agg",
                new Aggregation.Builder().avg(a -> a.field("field2")));
        copy.aggregation(newAgg);

        assertThat(original.aggregations).hasSize(1);
        assertThat(original.aggregations).extracting(MutableNamedAggregationBuilder::getName)
                .containsExactly("original_agg");

        assertThat(copy.aggregations).hasSize(2);
        assertThat(copy.aggregations).extracting(MutableNamedAggregationBuilder::getName)
                .containsExactly("original_agg", "copy_agg");
    }

    @Test
    void addingSortToCopyDoesNotAffectOriginal() {
        final var originalSort = SortOptions.of(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)));

        final var original = new MutableSearchRequestBuilder()
                .sort(originalSort);

        final var copy = original.copy();

        final var newSort = SortOptions.of(s -> s.field(f -> f.field("source").order(SortOrder.Asc)));
        copy.sort(newSort);

        assertThat(original.sort).hasSize(1);
        assertThat(copy.sort).hasSize(2);
    }

    @Test
    void copyWithEmptyAggregationsProducesIndependentList() {
        final var original = new MutableSearchRequestBuilder();

        final var copy = original.copy();

        final var agg = new MutableNamedAggregationBuilder("new_agg",
                new Aggregation.Builder().valueCount(v -> v.field("field1")));
        copy.aggregation(agg);

        assertThat(original.aggregations).isEmpty();
        assertThat(copy.aggregations).hasSize(1);
    }

    @Test
    void copyWithNullSortAllowsAddingSort() {
        final var original = new MutableSearchRequestBuilder();
        assertThat(original.sort).isNull();

        final var copy = original.copy();
        assertThat(copy.sort).isNull();

        final var sortOption = SortOptions.of(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)));
        copy.sort(sortOption);

        assertThat(original.sort).isNull();
        assertThat(copy.sort).hasSize(1);
    }
}
