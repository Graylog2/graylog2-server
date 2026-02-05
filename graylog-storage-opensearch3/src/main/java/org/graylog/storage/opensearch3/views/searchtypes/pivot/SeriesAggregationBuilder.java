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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import org.opensearch.client.opensearch._types.aggregations.Aggregation;

enum Placement {
    ROOT,
    ROW,
    COLUMN,
    METRIC
}

public record SeriesAggregationBuilder(String aggregationName, Aggregation aggregationBuilder,
                                       Placement placement) {
    public static SeriesAggregationBuilder root(String aggregationName, Aggregation aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationName, aggregationBuilder, Placement.ROOT);
    }

    public static SeriesAggregationBuilder metric(String aggregationName, Aggregation aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationName, aggregationBuilder, Placement.METRIC);
    }

    public static SeriesAggregationBuilder row(String aggregationName, Aggregation aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationName, aggregationBuilder, Placement.ROW);
    }

    public static SeriesAggregationBuilder column(String aggregationName, Aggregation aggregationBuilder) {
        return new SeriesAggregationBuilder(aggregationName, aggregationBuilder, Placement.COLUMN);
    }
}
