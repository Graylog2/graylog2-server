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
package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;

enum Placement {
    ROOT,
    ROW,
    COLUMN,
    METRIC
}

public record SeriesAggregationBuilder(AggregationBuilder aggregationBuilder, Placement placement) {
    public static SeriesAggregationBuilder root(AggregationBuilder aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationBuilder, Placement.ROOT);
    }

    public static SeriesAggregationBuilder metric(AggregationBuilder aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationBuilder, Placement.METRIC);
    }

    public static SeriesAggregationBuilder row(AggregationBuilder aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationBuilder, Placement.ROW);
    }

    public static SeriesAggregationBuilder column(AggregationBuilder aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationBuilder, Placement.COLUMN);
    }
}
