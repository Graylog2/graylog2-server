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
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ImmutableSystemScope;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private PipelineService pipelineService;
    @Captor
    private ArgumentCaptor<Collection<String>> activeIds;

    private MetricRegistry metricRegistry;
    private PipelineMetricRegistry pipelineMetrics;
    private PipelineActivityPeriodical periodical;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        pipelineMetrics = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        periodical = new PipelineActivityPeriodical(metricRegistry, activityService, pipelineService,
                Clock.fixed(NOW, ZoneOffset.UTC), new TelemetryConfiguration());

        // Build mocks before stubbing; nested stubbing inside thenReturn() is rejected by Mockito.
        final List<PipelineDao> pipelines = List.of(
                pipeline("p1", DefaultEntityScope.NAME),
                pipeline("idle", DefaultEntityScope.NAME),
                pipeline("system", ImmutableSystemScope.NAME));
        when(pipelineService.loadAll()).thenReturn(pipelines);
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
    void treatsResetMeterWithNewExecutionsAsActive() {
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark(5);
        periodical.doRun();

        // Editing a pipeline removes and re-registers its metrics.
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
    void ignoresPipelinesThatAreNotUserDefined() {
        // Illuminate registers its bundle pipelines under the same meter prefix, with IDs that aren't in MongoDB.
        pipelineMetrics.registerPipelineMeter("illuminate-pipeline", "executed").mark();
        pipelineMetrics.registerPipelineMeter("system", "executed").mark();
        pipelineMetrics.registerPipelineMeter("p1", "executed").mark();

        periodical.doRun();

        verify(activityService).markActive(activeIds.capture(), eq(NOW));
        assertThat(activeIds.getValue()).containsExactly("p1");
    }

    private static PipelineDao pipeline(String id, String scope) {
        final PipelineDao pipeline = mock(PipelineDao.class);
        // Lenient because the scope filter runs first, so non-default pipelines never have their ID read.
        lenient().when(pipeline.id()).thenReturn(id);
        when(pipeline.scope()).thenReturn(scope);
        return pipeline;
    }
}
