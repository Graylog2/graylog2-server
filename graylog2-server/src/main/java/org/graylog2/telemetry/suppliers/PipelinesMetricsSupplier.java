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

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog2.telemetry.pipelines.PipelineActivityService;
import org.graylog2.telemetry.pipelines.UserPipelines;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PipelinesMetricsSupplier implements TelemetryMetricSupplier {
    private static final Duration ACTIVE_WINDOW = Duration.ofHours(24);
    static final String OTHER_FUNCTIONS = "other";

    private final UserPipelines userPipelines;
    private final MongoDbPipelineMetadataService metadataService;
    private final PipelineActivityService activityService;
    private final FunctionRegistry functionRegistry;
    private final Clock clock;

    @Inject
    public PipelinesMetricsSupplier(UserPipelines userPipelines,
                                    MongoDbPipelineMetadataService metadataService,
                                    PipelineActivityService activityService,
                                    FunctionRegistry functionRegistry,
                                    Clock clock) {
        this.userPipelines = userPipelines;
        this.metadataService = metadataService;
        this.activityService = activityService;
        this.functionRegistry = functionRegistry;
        this.clock = clock;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        final Set<String> userPipelineIds = userPipelines.pipelineIds();

        final Set<String> activeUserPipelineIds = activityService.findActiveSince(clock.instant().minus(ACTIVE_WINDOW))
                .stream()
                .filter(userPipelineIds::contains)
                .collect(Collectors.toSet());

        final Map<String, Object> metrics = Map.of(
                "user_pipelines", (long) userPipelineIds.size(),
                "user_pipelines_active_24h", (long) activeUserPipelineIds.size(),
                "user_pipeline_rules", userPipelines.ruleCount(),
                "active_user_pipeline_functions", functionUsage(activeUserPipelineIds));

        return Optional.of(TelemetryEvent.of(metrics));
    }

    // Number of active user pipelines using each function, keyed by function name.
    private Map<String, Long> functionUsage(Set<String> pipelineIds) {
        return metadataService.get(pipelineIds).values().stream()
                // Collect per pipeline so a pipeline using several unreported functions counts once as "other".
                .flatMap(metadata -> metadata.functions().stream()
                        .map(this::reportedFunctionName)
                        .collect(Collectors.toSet())
                        .stream())
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    }

    // Plugins can register functions under customer-chosen names, so only functions shipped with Graylog are named.
    private String reportedFunctionName(String functionName) {
        final var function = functionRegistry.resolve(functionName);
        if (function == null) {
            return OTHER_FUNCTIONS;
        }
        final String className = function.getClass().getName();
        return className.startsWith("org.graylog.") || className.startsWith("org.graylog2.") ? functionName : OTHER_FUNCTIONS;
    }
}
