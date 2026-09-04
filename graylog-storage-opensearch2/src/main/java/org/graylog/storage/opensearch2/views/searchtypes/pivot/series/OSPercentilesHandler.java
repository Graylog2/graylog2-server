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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Percentiles;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.PercentilesAggregationBuilder;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;

public class OSPercentilesHandler extends OSBasicSeriesSpecHandler<Percentile, Percentiles> {

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Percentile percentileSpec) {
        final PercentilesAggregationBuilder percentiles = AggregationBuilders.percentiles(name).field(percentileSpec.field()).percentiles(percentileSpec.percentile());
        return SeriesAggregationBuilder.metric(percentiles);
    }

    @Override
    protected Object getValueFromAggregationResult(final Percentiles percentiles, final Percentile percentileSpec) {
        return percentiles.percentile(percentileSpec.percentile());
    }
}
