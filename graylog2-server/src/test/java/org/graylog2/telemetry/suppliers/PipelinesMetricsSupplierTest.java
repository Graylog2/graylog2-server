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
package org.graylog2.telemetry.suppliers;

import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.telemetry.pipelines.PipelineActivityService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.suppliers.PipelinesMetricsSupplier.FunctionUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelinesMetricsSupplierTest {
    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    @Mock
    private PipelineService pipelineService;
    @Mock
    private RuleService ruleService;
    @Mock
    private MongoDbPipelineMetadataService metadataService;
    @Mock
    private PipelineActivityService activityService;

    private PipelinesMetricsSupplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new PipelinesMetricsSupplier(pipelineService, ruleService, metadataService, activityService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void reportsUserPipelineCountsAndFunctionUsage() {
        // Build mocks before stubbing the services; nested stubbing inside thenReturn() is rejected by Mockito.
        final List<PipelineDao> pipelines = List.of(
                pipeline("user-active-1", DefaultEntityScope.NAME),
                pipeline("user-active-2", DefaultEntityScope.NAME),
                pipeline("user-idle", DefaultEntityScope.NAME),
                pipeline("system", ImmutableSystemScope.NAME));
        final Map<String, PipelineRulesMetadataDao> metadataById = Map.of(
                "user-active-1", metadata(Set.of("set_field", "to_string")),
                "user-active-2", metadata(Set.of("set_field", "lookup_value")));

        when(pipelineService.loadAll()).thenReturn(pipelines);
        when(ruleService.loadAllByScope(DefaultEntityScope.NAME)).thenReturn(List.of(mock(RuleDao.class), mock(RuleDao.class)));
        // "system" and "deleted" are active but must not be counted as user pipelines.
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24))))
                .thenReturn(Set.of("user-active-1", "user-active-2", "system", "deleted"));
        when(metadataService.get(Set.of("user-active-1", "user-active-2"))).thenReturn(metadataById);

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("user_pipelines", 3L)
                .containsEntry("user_pipelines_active_24h", 2L)
                .containsEntry("user_pipeline_rules", 2L)
                .containsEntry("active_user_pipeline_functions", List.of(
                        new FunctionUsage("lookup_value", 1L),
                        new FunctionUsage("set_field", 2L),
                        new FunctionUsage("to_string", 1L)));
    }

    @Test
    void reportsZeroesWhenNothingIsActive() {
        final List<PipelineDao> pipelines = List.of(pipeline("user-idle", DefaultEntityScope.NAME));
        when(pipelineService.loadAll()).thenReturn(pipelines);
        when(ruleService.loadAllByScope(DefaultEntityScope.NAME)).thenReturn(List.of());
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24)))).thenReturn(Set.of());
        when(metadataService.get(Set.of())).thenReturn(Map.of());

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("user_pipelines", 1L)
                .containsEntry("user_pipelines_active_24h", 0L)
                .containsEntry("user_pipeline_rules", 0L)
                .containsEntry("active_user_pipeline_functions", List.of());
    }

    private static PipelineDao pipeline(String id, String scope) {
        final PipelineDao pipeline = mock(PipelineDao.class);
        // Lenient because the scope filter runs first, so non-default pipelines never have their ID read.
        lenient().when(pipeline.id()).thenReturn(id);
        when(pipeline.scope()).thenReturn(scope);
        return pipeline;
    }

    private static PipelineRulesMetadataDao metadata(Set<String> functions) {
        final PipelineRulesMetadataDao metadata = mock(PipelineRulesMetadataDao.class);
        when(metadata.functions()).thenReturn(functions);
        return metadata;
    }
}
