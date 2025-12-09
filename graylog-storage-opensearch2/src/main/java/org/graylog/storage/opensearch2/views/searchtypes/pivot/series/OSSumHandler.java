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

import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;

public class OSSumHandler extends OSBasicSeriesSpecHandler<Sum, org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Sum> {

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Sum sumSpec) {
        final SumAggregationBuilder sum = AggregationBuilders.sum(name).field(sumSpec.field());
        return SeriesAggregationBuilder.metric(sum);
    }

    @Override
    protected Object getValueFromAggregationResult(final org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Sum sum,
                                                   final Sum seriesSpec) {
        return sum.getValue();
    }
}
