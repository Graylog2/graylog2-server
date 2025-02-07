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
package org.graylog2.inputs.diagnosis;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputDiagnosisMetricsTest {

    private static final String INPUT_FAILURE_1 = "input.failure1";
    private static final String INPUT_FAILURE_2 = "input.failure2";
    MetricRegistry metricRegistry;

    InputDiagnosisMetrics underTest;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        underTest = new InputDiagnosisMetrics(metricRegistry);
    }

    @Test
    void testIncreaseMetric() {
        underTest.incCount(INPUT_FAILURE_1);
        underTest.incCount(INPUT_FAILURE_1);

        underTest.update();

        assertThat(metricRegistry.getGauges().get(INPUT_FAILURE_1).getValue()).isEqualTo(2L);
    }

    @Test
    void testMetricsIncludesOnlyTheLast15Updates() {
        for (int i = 0; i < 16; i++) {
            underTest.incCount(INPUT_FAILURE_1);
            underTest.incCount(INPUT_FAILURE_1);
            underTest.incCount(INPUT_FAILURE_2);
            underTest.update();
        }
        assertThat(metricRegistry.getGauges().get(INPUT_FAILURE_1).getValue()).isEqualTo(30L);
        assertThat(metricRegistry.getGauges().get(INPUT_FAILURE_2).getValue()).isEqualTo(15L);
    }
}
