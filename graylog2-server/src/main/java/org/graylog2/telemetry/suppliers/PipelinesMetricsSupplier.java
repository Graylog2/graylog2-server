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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.telemetry.pipelines.PipelineActivityService;
import org.graylog2.telemetry.pipelines.UserPipelines;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PipelinesMetricsSupplier implements TelemetryMetricSupplier {
    private static final Duration ACTIVE_WINDOW = Duration.ofHours(24);

    private final PipelineService pipelineService;
    private final RuleService ruleService;
    private final MongoDbPipelineMetadataService metadataService;
    private final PipelineActivityService activityService;
    private final Clock clock;

    @Inject
    public PipelinesMetricsSupplier(PipelineService pipelineService,
                                    RuleService ruleService,
                                    MongoDbPipelineMetadataService metadataService,
                                    PipelineActivityService activityService,
                                    Clock clock) {
        this.pipelineService = pipelineService;
        this.ruleService = ruleService;
        this.metadataService = metadataService;
        this.activityService = activityService;
        this.clock = clock;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        final Set<String> userPipelineIds = UserPipelines.ids(pipelineService);

        final Set<String> activeUserPipelineIds = activityService.findActiveSince(clock.instant().minus(ACTIVE_WINDOW))
                .stream()
                .filter(userPipelineIds::contains)
                .collect(Collectors.toSet());

        final Map<String, Object> metrics = Map.of(
                "user_pipelines", (long) userPipelineIds.size(),
                "user_pipelines_active_24h", (long) activeUserPipelineIds.size(),
                "user_pipeline_rules", (long) ruleService.loadAllByScope(DefaultEntityScope.NAME).size(),
                "active_user_pipeline_functions", functionUsage(activeUserPipelineIds));

        return Optional.of(TelemetryEvent.of(metrics));
    }

    private List<FunctionUsage> functionUsage(Set<String> pipelineIds) {
        return metadataService.get(pipelineIds).values().stream()
                .flatMap(metadata -> metadata.functions().stream())
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new FunctionUsage(entry.getKey(), entry.getValue()))
                .toList();
    }

    record FunctionUsage(@JsonProperty("function") String function,
                         @JsonProperty("pipeline_count") long pipelineCount) {}
}
