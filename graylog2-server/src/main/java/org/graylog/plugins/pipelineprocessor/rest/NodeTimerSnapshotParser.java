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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Translates a node's {@link MetricsSummaryResponse} into a {@link NodeTimerSnapshot}.
 * Non-timer or malformed entries are skipped silently so partial node responses still parse usefully.
 */
@Singleton
public final class NodeTimerSnapshotParser {

    private static final Logger LOG = LoggerFactory.getLogger(NodeTimerSnapshotParser.class);

    private final ObjectMapper objectMapper;

    @Inject
    NodeTimerSnapshotParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    NodeTimerSnapshot parse(MetricsSummaryResponse response) {
        final NodeTimerSnapshot.Builder builder = NodeTimerSnapshot.builder();
        for (Map<String, Object> entry : response.metrics()) {
            if (!(entry.get("full_name") instanceof String name)) {
                continue;
            }
            final Object metric = entry.get("metric");
            if (metric == null) {
                continue;
            }
            final TimerRateMetricsResponse timer;
            try {
                timer = objectMapper.convertValue(metric, TimerRateMetricsResponse.class);
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to parse metric '{}' as a timer. Skipping.", name, e);
                continue;
            }
            if (timer == null || timer.rate == null || timer.time == null) {
                continue;
            }
            builder.timer(name, timer.rate.fifteenMinute, timer.time.mean);
        }
        return builder.build();
    }
}
