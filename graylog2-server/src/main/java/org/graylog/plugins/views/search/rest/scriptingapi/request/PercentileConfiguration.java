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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;

import java.util.Optional;

public record PercentileConfiguration(@JsonProperty("percentile") Double percentile) implements MetricConfiguration {

    @Override
    public Optional<String> columnName(final Metric metric) {
        return Optional.of(Percentile.builder()
                .field(metric.fieldName())
                .percentile(percentile())
                .build()
                .literal());
    }
}
