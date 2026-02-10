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

import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AvgAggregate;

public class OSAverageHandler extends OSBasicSeriesSpecHandler<Average> {

    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Average avgSpec) {
        return SeriesAggregationBuilder.metric(new MutableNamedAggregationBuilder(name,
                Aggregation.builder().avg(a -> a.field(avgSpec.field()))));
    }

    @Override
    protected Object getValueFromAggregationResult(final Aggregate agg, final Average avgSpec) {
        AvgAggregate avg = (agg.isAvg()) ? agg.avg() : null;
        double value = (avg == null || avg.value() == null) ? 0 : avg.value();
        if (avgSpec.wholeNumber()) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                value = 0;
            } else {
                value = Math.round(value);
            }
        }
        return value;
    }
}
