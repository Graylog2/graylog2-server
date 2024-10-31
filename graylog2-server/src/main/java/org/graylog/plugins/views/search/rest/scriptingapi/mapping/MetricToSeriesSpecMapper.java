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
import org.graylog.plugins.views.search.rest.scriptingapi.request.PercentageConfiguration;
import org.graylog.plugins.views.search.rest.scriptingapi.request.PercentileConfiguration;
import org.graylog.plugins.views.search.rest.scriptingapi.validation.MetricValidator;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;

import jakarta.inject.Inject;

import java.util.function.Function;

public class MetricToSeriesSpecMapper implements Function<Metric, SeriesSpec> {

    private final MetricValidator metricValidator;

    @Inject
    public MetricToSeriesSpecMapper(final MetricValidator metricValidator) {
        this.metricValidator = metricValidator;
    }

    @Override
    public SeriesSpec apply(final Metric metric) {
        metricValidator.validate(metric);

        return switch (metric.functionName()) {
            case Average.NAME -> Average.builder().field(metric.fieldName()).build();
            case Cardinality.NAME -> Cardinality.builder().field(metric.fieldName()).build();
            case Count.NAME -> Count.builder().field(metric.fieldName()).build();
            case Latest.NAME -> Latest.builder().field(metric.fieldName()).build();
            case Max.NAME -> Max.builder().field(metric.fieldName()).build();
            case Min.NAME -> Min.builder().field(metric.fieldName()).build();
            case Percentage.NAME -> Percentage.builder()
                    .field(metric.fieldName())
                    .strategy(metric.configuration() != null ? ((PercentageConfiguration) metric.configuration()).strategy() : null)
                    .build();
            case Percentile.NAME -> Percentile.builder()
                    .field(metric.fieldName())
                    .percentile(((PercentileConfiguration) metric.configuration()).percentile())
                    .build();
            case StdDev.NAME -> StdDev.builder().field(metric.fieldName()).build();
            case Sum.NAME -> Sum.builder().field(metric.fieldName()).build();
            case SumOfSquares.NAME -> SumOfSquares.builder().field(metric.fieldName()).build();
            case Variance.NAME -> Variance.builder().field(metric.fieldName()).build();
            default -> Count.builder().field(metric.fieldName()).build(); //TODO: do we want to have a default at all?
        };
    }


}
