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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SeriesSpecBuilder;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;

import java.util.function.Function;

public class MetricToSeriesSpecBuilderMapper implements Function<Metric, SeriesSpecBuilder<? extends SeriesSpec, ? extends SeriesSpecBuilder<? extends SeriesSpec, ?>>> {

    @Override
    public SeriesSpecBuilder<? extends SeriesSpec, ? extends SeriesSpecBuilder<? extends SeriesSpec, ?>> apply(final Metric metric) {
        return switch (metric.functionName()) {
            case Average.NAME -> Average.builder();
            case Cardinality.NAME -> Cardinality.builder();
            case Count.NAME -> Count.builder();
            case Latest.NAME -> Latest.builder();
            case Max.NAME -> Max.builder();
            case Min.NAME -> Min.builder();
            case Percentile.NAME -> Percentile.builder();
            case StdDev.NAME -> StdDev.builder();
            case Sum.NAME -> Sum.builder();
            case SumOfSquares.NAME -> SumOfSquares.builder();
            case Variance.NAME -> Variance.builder();
            default -> Count.builder(); //TODO: do we want to have a default at all?

        };
    }
}
