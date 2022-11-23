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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class MetricsToSeriesSpecMapperTest {

    private MetricsToSeriesSpecMapper toTest;
    private Map<String, SeriesDescription> availableFunctions;
    private MetricToSeriesSpecMapper metricToSeriesSpecMapper;

    @BeforeEach
    void setUp() {
        availableFunctions = Map.of("avg", SeriesDescription.create("avg", "Average"));
        metricToSeriesSpecMapper = mock(MetricToSeriesSpecMapper.class);
        toTest = new MetricsToSeriesSpecMapper(availableFunctions, metricToSeriesSpecMapper);
    }

    @Test
    void throwsBadRequestExceptionOnUnavailableFunction() {
        assertThrows(BadRequestException.class, () -> toTest.apply(new Metric("http_method", "unknown", null)));
    }

    @Test
    void constructsProperSeriesSpec() {
        final Metric metric = new Metric("took_ms", "avg", null);
        doReturn(Average.builder().field("took_ms").build()).when(metricToSeriesSpecMapper).apply(metric);
        final SeriesSpec result = toTest.apply(metric);
        assertThat(result)
                .isNotNull()
                .isInstanceOf(Average.class)
                .satisfies(a -> assertEquals("took_ms", a.field()))
                .satisfies(a -> assertEquals(Average.NAME, a.type()));
    }

}
