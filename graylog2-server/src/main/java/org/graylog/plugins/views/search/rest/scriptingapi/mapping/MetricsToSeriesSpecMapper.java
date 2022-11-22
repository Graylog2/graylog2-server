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

import org.graylog.plugins.views.search.rest.SeriesDescription;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetricsToSeriesSpecMapper implements Function<Metric, SeriesSpec> {

    private final List<String> availableMetricTypes;
    private final MetricToSeriesSpecBuilderMapper seriesSpecBuilderCreator;

    @Inject
    public MetricsToSeriesSpecMapper(final Map<String, SeriesDescription> availableFunctions,
                                     final MetricToSeriesSpecBuilderMapper seriesSpecBuilderCreator) {
        this.availableMetricTypes = availableFunctions.values().stream().map(SeriesDescription::type).collect(Collectors.toList());
        this.seriesSpecBuilderCreator = seriesSpecBuilderCreator;
    }

    @Override
    public SeriesSpec apply(final Metric metric) {
        if (!availableMetricTypes.contains(metric.functionName())) {
            throw new BadRequestException("Unrecognized metric : " + metric.functionName() + ", valid metrics : " + availableMetricTypes);
        }
        return seriesSpecBuilderCreator.apply(metric).field(metric.fieldName()).build();
    }

}
