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

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;

import java.util.Map;
import java.util.Optional;

public class OSPercentilesHandler extends OSBasicSeriesSpecHandler<Percentile> {

    private final OSSerializationUtils serializationUtils;

    @Inject
    public OSPercentilesHandler(OSSerializationUtils serializationUtils) {
        this.serializationUtils = serializationUtils;
    }

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Percentile percentileSpec) {
        return SeriesAggregationBuilder.metric(new MutableNamedAggregationBuilder(name,
                Aggregation.builder().percentiles(p -> p
                        .field(percentileSpec.field())
                        .percents(percentileSpec.percentile()))));
    }

    @Override
    protected Object getValueFromAggregationResult(final Aggregate agg, final Percentile percentileSpec) {
        return Optional.ofNullable(agg)
                .filter(Aggregate::isTdigestPercentiles)
                .map(Aggregate::tdigestPercentiles)
                .flatMap(v -> v.values().keyed().entrySet()
                        .stream()
                        .filter(e -> Double.parseDouble(e.getKey()) == percentileSpec.percentile())
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .map(serializationUtils::toObject)
                );
    }
}
