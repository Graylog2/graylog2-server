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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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

    @Test
    void unknownJsonSubtypeParsesAsNullConfiguration() throws JsonProcessingException {
        // This test ensures that we can parse a Metric where the "function" is not a known JsonSubType for
        // MetricConfiguration. The behavior for handling this changed in Jackson 2.14. We need to use a defaultImpl
        // of NoClass for the JsonTypeInfo since then. (see https://github.com/FasterXML/jackson-databind/issues/3533)
        final var json = """
                {
                  "function": "count",
                  "field": "test"
                }
                """;
        final var metric = new ObjectMapperProvider().get().readValue(json, Metric.class);

        assertThat(metric.functionName()).isEqualTo("count");
        assertThat(metric.fieldName()).isEqualTo("test");
        assertThat(metric.configuration()).isNull();
    }
}
