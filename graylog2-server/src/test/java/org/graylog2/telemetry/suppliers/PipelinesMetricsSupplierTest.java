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

import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog2.telemetry.pipelines.PipelineActivityService;
import org.graylog2.telemetry.pipelines.UserPipelines;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.telemetry.suppliers.PipelinesMetricsSupplier.OTHER_FUNCTIONS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelinesMetricsSupplierTest {
    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    @Mock
    private UserPipelines userPipelines;
    @Mock
    private MongoDbPipelineMetadataService metadataService;
    @Mock
    private PipelineActivityService activityService;

    private PipelinesMetricsSupplier supplier;

    @BeforeEach
    void setUp() {
        final FunctionRegistry functionRegistry = new FunctionRegistry(Map.of(
                SetField.NAME, new SetField(),
                StringConversion.NAME, new StringConversion(),
                Lowercase.NAME, new Lowercase(),
                "acme_hr_lookup", pluginFunction(),
                "acme_geo_lookup", pluginFunction()));
        supplier = new PipelinesMetricsSupplier(userPipelines, metadataService, activityService, functionRegistry,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void reportsUserPipelineCountsAndFunctionUsage() {
        // Build mocks before stubbing the services; nested stubbing inside thenReturn() is rejected by Mockito.
        final Map<String, PipelineRulesMetadataDao> metadataById = Map.of(
                "user-active-1", metadata(Set.of("set_field", "to_string")),
                "user-active-2", metadata(Set.of("set_field", "lowercase")));

        when(userPipelines.pipelineIds()).thenReturn(Set.of("user-active-1", "user-active-2", "user-idle"));
        when(userPipelines.ruleCount()).thenReturn(2L);
        // "system" and "deleted" are active but must not be counted as user pipelines.
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24))))
                .thenReturn(Set.of("user-active-1", "user-active-2", "system", "deleted"));
        when(metadataService.get(Set.of("user-active-1", "user-active-2"))).thenReturn(metadataById);

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("user_pipelines", 3L)
                .containsEntry("user_pipelines_active_24h", 2L)
                .containsEntry("user_pipeline_rules", 2L)
                .containsEntry("active_user_pipeline_functions", Map.of(
                        "lowercase", 1L,
                        "set_field", 2L,
                        "to_string", 1L));
    }

    @Test
    void reportsFunctionsNotShippedWithGraylogAsOther() {
        final Map<String, PipelineRulesMetadataDao> metadataById = Map.of(
                // Two plugin functions in one pipeline count that pipeline once.
                "plugin-user", metadata(Set.of("set_field", "acme_hr_lookup", "acme_geo_lookup")),
                // Functions from a removed plugin no longer resolve.
                "unresolved-user", metadata(Set.of("removed_plugin_function")));

        when(userPipelines.pipelineIds()).thenReturn(Set.of("plugin-user", "unresolved-user"));
        when(userPipelines.ruleCount()).thenReturn(2L);
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24))))
                .thenReturn(Set.of("plugin-user", "unresolved-user"));
        when(metadataService.get(Set.of("plugin-user", "unresolved-user"))).thenReturn(metadataById);

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("active_user_pipeline_functions", Map.of(
                OTHER_FUNCTIONS, 2L,
                "set_field", 1L));
    }

    @Test
    void activePipelineWithoutMetadataContributesNoFunctions() {
        final Map<String, PipelineRulesMetadataDao> metadataById = Map.of(
                "with-metadata", metadata(Set.of("set_field")));

        when(userPipelines.pipelineIds()).thenReturn(Set.of("with-metadata", "without-metadata"));
        when(userPipelines.ruleCount()).thenReturn(1L);
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24))))
                .thenReturn(Set.of("with-metadata", "without-metadata"));
        when(metadataService.get(Set.of("with-metadata", "without-metadata"))).thenReturn(metadataById);

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("user_pipelines_active_24h", 2L)
                .containsEntry("active_user_pipeline_functions", Map.of("set_field", 1L));
    }

    @Test
    void reportsZeroesWhenNothingIsActive() {
        when(userPipelines.pipelineIds()).thenReturn(Set.of("user-idle"));
        when(userPipelines.ruleCount()).thenReturn(0L);
        when(activityService.findActiveSince(NOW.minus(Duration.ofHours(24)))).thenReturn(Set.of());
        when(metadataService.get(Set.of())).thenReturn(Map.of());

        final TelemetryEvent event = supplier.get().orElseThrow();

        assertThat(event.metrics()).containsEntry("user_pipelines", 1L)
                .containsEntry("user_pipelines_active_24h", 0L)
                .containsEntry("user_pipeline_rules", 0L)
                .containsEntry("active_user_pipeline_functions", Map.of());
    }

    // A JDK proxy class lives outside Graylog's packages, like a function registered by a customer plugin.
    private static Function<?> pluginFunction() {
        return (Function<?>) Proxy.newProxyInstance(PipelinesMetricsSupplierTest.class.getClassLoader(),
                new Class<?>[]{Function.class}, (proxy, method, args) -> {
                    throw new UnsupportedOperationException();
                });
    }

    private static PipelineRulesMetadataDao metadata(Set<String> functions) {
        final PipelineRulesMetadataDao metadata = mock(PipelineRulesMetadataDao.class);
        when(metadata.functions()).thenReturn(functions);
        return metadata;
    }
}
