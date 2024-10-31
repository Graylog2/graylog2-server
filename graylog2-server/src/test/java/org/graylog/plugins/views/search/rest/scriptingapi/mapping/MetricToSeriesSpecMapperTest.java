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
import org.graylog.plugins.views.search.rest.scriptingapi.validation.MetricValidator;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class MetricToSeriesSpecMapperTest {

    private MetricToSeriesSpecMapper toTest;
    private MetricValidator metricValidator;

    @BeforeEach
    void setUp() {
        metricValidator = mock(MetricValidator.class);
        toTest = new MetricToSeriesSpecMapper(metricValidator);
    }

    @Test
    void throwsExceptionWhenValidatorThrowsException() {
        doThrow(ValidationException.class).when(metricValidator).validate(any());
        assertThrows(ValidationException.class, () -> toTest.apply(new Metric("unknown", "http_method")));
    }

    @Test
    void constructsProperSeriesSpec() {
        final Metric metric = new Metric("avg", "took_ms");
        final SeriesSpec result = toTest.apply(metric);
        assertThat(result)
                .isNotNull()
                .isInstanceOf(Average.class)
                .satisfies(a -> assertEquals("took_ms", ((Average) a).field()))
                .satisfies(a -> assertEquals(Average.NAME, a.type()));
    }

}
