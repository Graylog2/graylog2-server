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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MetricTest {

    @Test
    void parsesCountMetricCorrectlyEvenWithoutTheFieldName() {
        Optional<Metric> count = Metric.fromStringRepresentation("count");
        assertThat(count)
                .hasValue(new Metric("count", null));

        count = Metric.fromStringRepresentation("count:");
        assertThat(count)
                .hasValue(new Metric("count", null));
    }

    @Test
    void parsesMetricsCorrectly() {
        Optional<Metric> count = Metric.fromStringRepresentation("count:stars");
        assertThat(count)
                .hasValue(new Metric("count", "stars"));

        count = Metric.fromStringRepresentation("avg:salary");
        assertThat(count)
                .hasValue(new Metric("avg", "salary"));
    }

}
