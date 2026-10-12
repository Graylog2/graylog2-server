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
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs on every node and records which pipelines processed messages since the previous run.
 */
public class PipelineActivityPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineActivityPeriodical.class);

    private static final String PIPELINE_METER_PREFIX = Pipeline.class.getName() + ".";
    private static final String EXECUTED_SUFFIX = ".executed";
    private static final int PERIOD_SECONDS = (int) Duration.ofMinutes(15).toSeconds();

    private final MetricRegistry metricRegistry;
    private final PipelineActivityService activityService;
    private final UserPipelines userPipelines;
    private final Clock clock;
    private final boolean isEnabled;

    // Meters are node-local and start from zero on restart, so compare to this node's last run.
    private final Map<String, Long> previousCounts = new HashMap<>();

    @Inject
    public PipelineActivityPeriodical(MetricRegistry metricRegistry,
                                      PipelineActivityService activityService,
                                      UserPipelines userPipelines,
                                      Clock clock,
                                      TelemetryConfiguration telemetryConfiguration) {
        this.metricRegistry = metricRegistry;
        this.activityService = activityService;
        this.userPipelines = userPipelines;
        this.clock = clock;
        this.isEnabled = telemetryConfiguration.isTelemetryEnabled();
    }

    @Override
    public void doRun() {
        final Map<String, Long> currentCounts = readPipelineCounts(userPipelines.pipelineIds());
        final List<String> activePipelineIds = currentCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && !entry.getValue().equals(previousCounts.get(entry.getKey())))
                .map(Map.Entry::getKey)
                .toList();

        activityService.markActive(activePipelineIds, clock.instant());

        // Only advance after a successful write so a failed run is retried next time.
        previousCounts.clear();
        previousCounts.putAll(currentCounts);
    }

    private Map<String, Long> readPipelineCounts(Set<String> userPipelineIds) {
        final Map<String, Long> counts = new HashMap<>();
        metricRegistry.getMeters((name, metric) -> name.startsWith(PIPELINE_METER_PREFIX) && name.endsWith(EXECUTED_SUFFIX))
                .forEach((name, meter) -> {
                    final String pipelineId = name.substring(PIPELINE_METER_PREFIX.length(), name.length() - EXECUTED_SUFFIX.length());
                    // Illuminate pipelines share this meter prefix, and stage meter IDs keep extra name segments
                    // that never match a pipeline ID.
                    if (userPipelineIds.contains(pipelineId)) {
                        counts.put(pipelineId, meter.getCount());
                    }
                });
        return counts;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return isEnabled;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return PERIOD_SECONDS;
    }

    @Override
    public int getPeriodSeconds() {
        return PERIOD_SECONDS;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
