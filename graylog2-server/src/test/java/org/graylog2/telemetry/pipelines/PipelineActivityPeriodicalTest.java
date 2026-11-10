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
package org.graylog2.telemetry.pipelines;

import com.codahale.metrics.MetricRegistry;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog2.configuration.TelemetryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineActivityPeriodicalTest {
    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    @Mock
    private PipelineActivityService activityService;
    @Mock
    private UserPipelines userPipelines;
    @Captor
    private ArgumentCaptor<Collection<String>> activeIds;

    private MetricRegistry metricRegistry;
    private PipelineMetricRegistry pipelineMetrics;
    private PipelineActivityPeriodical periodical;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        pipelineMetrics = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        periodical = new PipelineActivityPeriodical(metricRegistry, activityService, userPipelines,
                Clock.fixed(NOW, ZoneOffset.UTC), new TelemetryConfiguration());

        // Lenient because not every test runs the periodical.
        lenient().when(userPipelines.pipelineIds()).thenReturn(Set.of("p1", "idle"));
    }

    @Test
    void marksPipelinesWithExecutionsOnFirstRun() {
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(3);
        pipelineMetrics.registerPipelineMeter("idle", "executed");

        periodical.doRun();

        verify(activityService).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getValue()).containsExactly("p1");
    }

    @Test
    void skipsPipelinesWhoseCountDidNotChange() {
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(3);
        periodical.doRun();

        periodical.doRun();

        verify(activityService, times(2)).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getAllValues().get(1)).isEmpty();
    }

    @Test
    void marksPipelineAgainWhenCountIncreases() {
        final var meter = pipelineMetrics.registerPipelineMeter("p1", "executed");
        meter.mark(3);
        periodical.doRun();

        meter.mark();
        periodical.doRun();

        verify(activityService, times(2)).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getAllValues().get(1)).containsExactly("p1");
    }

    @Test
    void treatsLowerCountWithNewExecutionsAsActive() {
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(5);
        periodical.doRun();

        // A count that goes down still means new executions, so it must not be skipped.
        pipelineMetrics.removePipelineMetrics("p1");
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(2);
        periodical.doRun();

        verify(activityService, times(2)).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getAllValues().get(1)).containsExactly("p1");
    }

    @Test
    void ignoresStageMeters() {
        pipelineMetrics.registerStageMeter("p1", 0, "executed").mark();

        periodical.doRun();

        verify(activityService).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getValue()).isEmpty();
    }

    @Test
    void retriesActivityAfterFailedWrite() {
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(3);
        doThrow(new RuntimeException("write failed")).doNothing().when(activityService).markActive(any(), any());

        assertThatThrownBy(() -> periodical.doRun()).isInstanceOf(RuntimeException.class);
        periodical.doRun();

        verify(activityService, times(2)).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getAllValues().get(1)).containsExactly("p1");
    }

    @Test
    void doesNotStartWhenTelemetryIsDisabled() {
        final TelemetryConfiguration disabled = mock(TelemetryConfiguration.class);
        when(disabled.isTelemetryEnabled()).thenReturn(false);

        final PipelineActivityPeriodical disabledPeriodical = new PipelineActivityPeriodical(metricRegistry,
                activityService, userPipelines, Clock.fixed(NOW, ZoneOffset.UTC), disabled);

        assertThat(disabledPeriodical.startOnThisNode()).isFalse();
        assertThat(periodical.startOnThisNode()).isTrue();
    }

    @Test
    void ignoresPipelinesThatAreNotUserDefined() {
        // Illuminate registers its bundle pipelines under the same meter prefix, with IDs that aren't in MongoDB.
        pipelineMetrics.registerPipelineMeter("illuminate-pipeline", "executed").mark();
        pipelineMetrics.registerPipelineMeter("system", "executed").mark();
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark();

        periodical.doRun();

        verify(activityService).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getValue()).containsExactly("p1");
    }
}
