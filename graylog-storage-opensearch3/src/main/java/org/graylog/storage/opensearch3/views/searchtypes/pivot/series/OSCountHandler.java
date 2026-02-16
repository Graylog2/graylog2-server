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
package org.graylog.storage.opensearch3.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.ValueCountAggregate;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OSCountHandler extends OSPivotSeriesSpecHandler<Count> {
    private static final Logger LOG = LoggerFactory.getLogger(OSCountHandler.class);

    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Count count, OSGeneratedQueryContext queryContext) {
        return count.field()
                .map(field -> {
                    // the request was for a field count, we have to add a value_count sub aggregation
                    queryContext.recordNameForPivotSpec(pivot, count, name);
                    return List.of(SeriesAggregationBuilder.metric(new MutableNamedAggregationBuilder(name,
                            Aggregation.builder().valueCount(a -> a.field(field)))));
                })
                // doc_count is always present in elasticsearch's bucket aggregations, no need to add it
                .orElse(List.of());
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Count count,
                                        MultiSearchItem<JsonData> searchResult,
                                        Aggregate agg,
                                        OSGeneratedQueryContext esGeneratedQueryContext) {
        final Object value;
        if (agg == null) {
            LOG.error("Unexpected null aggregation result, returning 0 for the count. This is a bug.");
            value = 0;
        } else if (agg.isMultiTerms()) {
            value = agg.multiTerms().buckets().array().getFirst().docCount();
        } else if (agg.isValueCount()) {
            value = Optional.ofNullable(agg.valueCount())
                    .map(ValueCountAggregate::value)
                    .map(Double::longValue)
                    .orElse(0L);
        } else {
            value = null;
        }
        return Stream.of(Value.create(count.id(), Count.NAME, value));
    }

    @Override
    public Aggregate extractAggregationFromResult(Pivot pivot, PivotSpec spec, MultiBucketBase aggregations, IndexerGeneratedQueryContext<?> queryContext) {
        final String agg = queryContext.getAggNameForPivotSpecFromContext(pivot, spec);
        if (agg == null) {
            return ValueCountAggregate.of(v -> v
                    .value((double) aggregations.docCount())
            ).toAggregate();
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            return aggregations.aggregations().get(agg);
        }
    }
}
