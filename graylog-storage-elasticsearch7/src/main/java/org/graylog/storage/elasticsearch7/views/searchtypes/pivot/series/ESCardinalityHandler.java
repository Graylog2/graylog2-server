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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;

public class ESCardinalityHandler extends ESBasicSeriesSpecHandler<Cardinality, org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality> {

    @Override
    protected SeriesAggregationBuilder createAggregationBuilder(final String name, final Cardinality cardinalitySpec) {
        final CardinalityAggregationBuilder card = AggregationBuilders.cardinality(name).field(cardinalitySpec.field());
        return SeriesAggregationBuilder.metric(card);
    }

    @Override
    protected Object getValueFromAggregationResult(final org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality cardinality,
                                                   final Cardinality cardinalitySpec) {
        return cardinality.getValue();
    }
}
