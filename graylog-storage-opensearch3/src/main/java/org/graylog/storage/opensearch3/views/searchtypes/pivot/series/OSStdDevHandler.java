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

import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.ExtendedStatsAggregateBase;

import java.util.Optional;

public class OSStdDevHandler extends OSBasicSeriesSpecHandler<StdDev> {

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final StdDev stddevSpec) {
        return SeriesAggregationBuilder.metric(new MutableNamedAggregationBuilder(name,
                Aggregation.builder().extendedStats(e -> e.field(stddevSpec.field()))));
    }

    @Override
    protected Object getValueFromAggregationResult(final Aggregate agg, final StdDev seriesSpec) {
        return Optional.ofNullable(agg)
                .filter(Aggregate::isExtendedStats)
                .map(Aggregate::extendedStats)
                .map(ExtendedStatsAggregateBase::stdDeviation)
                .orElse(null);
    }
}
