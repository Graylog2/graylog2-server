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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.SingleBucketAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.TopHitsAggregate;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OSLatestHandler extends OSBasicSeriesSpecHandler<Latest> {
    private static final String AGG_NAME = "latest_aggregation";

    private final OSSerializationUtils serializationUtils;

    @Inject
    public OSLatestHandler(OSSerializationUtils serializationUtils) {
        this.serializationUtils = serializationUtils;
    }

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Latest latestSpec) {
        Aggregation topHitsSubAgg = Aggregation.of(a -> a
                .topHits(th -> th
                        .size(1)
                        .source(s -> s.filter(f -> f.includes(List.of(latestSpec.field()))))
                        .sort(sort -> sort
                                .field(f -> f.field("timestamp").order(SortOrder.Desc))
                        )
                )
        );
        Aggregation filterAgg = Aggregation.of(a -> a
                .filter(f -> f.exists(e -> e.field(latestSpec.field())))
                .aggregations(Map.of(AGG_NAME, topHitsSubAgg))
        );

        return SeriesAggregationBuilder.metric(name, filterAgg);
    }

    @Override
    protected Object getValueFromAggregationResult(final Aggregate agg, final Latest seriesSpec) {
        return Optional.ofNullable(agg.filter())
                .map(SingleBucketAggregateBase::aggregations)
                .map(a -> a.get(AGG_NAME))
                .map(Aggregate::topHits)
                .map(TopHitsAggregate::hits)
                .map(HitsMetadata::hits)
                .filter(hits -> !hits.isEmpty())
                .stream().findFirst()
                .map(List::getFirst)
                .map(hit -> {
                    try {
                        return serializationUtils.toMap(hit.source());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(source -> source.get(seriesSpec.field()))
                .orElse(null);
    }
}
