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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

public class PipelineMetricRegistry {
    private final MetricRegistry registry;
    private final String pipelinesPrefix;
    private final String rulesPrefix;

    public static PipelineMetricRegistry create(MetricRegistry metricRegistry,
                                                String pipelinesPrefix,
                                                String rulesPrefix) {
        return new PipelineMetricRegistry(metricRegistry, pipelinesPrefix, rulesPrefix);
    }

    private PipelineMetricRegistry(MetricRegistry metricRegistry, String pipelinesPrefix, String rulesPrefix) {
        this.registry = requireNonNull(metricRegistry, "metricRegistry is null");
        this.pipelinesPrefix = requireNonBlank(pipelinesPrefix, "pipelinesPrefix is blank");
        this.rulesPrefix = requireNonBlank(rulesPrefix, "rulesPrefix is blank");
    }

    public Meter registerPipelineMeter(String pipelineId, String name) {
        return registry.meter(name(pipelinesPrefix, pipelineId, name));
    }

    public Meter registerStageMeter(String pipelineId, int stage, String name) {
        return registry.meter(name(pipelinesPrefix, pipelineId, "stage", String.valueOf(stage), name));
    }

    public Meter registerLocalRuleMeter(String pipelineId, int stage, String ruleId, String name) {
        // The rule ID is the first name part after the prefix, so we can clean up metrics by rule ID.
        return registry.meter(name(rulesPrefix, ruleId, pipelineId, String.valueOf(stage), name));
    }

    public Meter registerGlobalRuleMeter(String ruleId, String name) {
        return registry.meter(name(rulesPrefix, ruleId, name));
    }

    public void removePipelineMetrics(String pipelineId) {
        registry.removeMatching(MetricFilter.startsWith(name(pipelinesPrefix, pipelineId)));
    }

    public void removeRuleMetrics(String ruleId) {
        registry.removeMatching(MetricFilter.startsWith(name(rulesPrefix, ruleId)));
    }
}
