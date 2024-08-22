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

/**
 * A metric registry for processing pipelines, stages, and rules.
 * <p>
 * The registry simplifies metrics management by automatically applying metric name prefixes to metric registration
 * and removal. It also applies the correct name pattern for metrics.
 */
public class PipelineMetricRegistry {
    private final MetricRegistry registry;
    private final String pipelinesPrefix;
    private final String rulesPrefix;

    /**
     * Creates a new pipeline metric registry.
     *
     * @param metricRegistry  metric registry to register the metrics with
     * @param pipelinesPrefix metric name prefix for pipelines and stages
     * @param rulesPrefix     metric name prefix for rules
     * @return the new pipeline metric registry instance
     */
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

    /**
     * Registers a new pipeline meter for the given pipeline ID and name.
     *
     * @param pipelineId the pipeline ID
     * @param name       the meter name
     * @return the new pipeline meter
     * @throws IllegalArgumentException if pipelineId or name are blank
     */
    public Meter registerPipelineMeter(String pipelineId, String name) {
        // IMPORTANT: Changing the metric name pattern is a breaking API change!
        return registry.meter(name(
                pipelinesPrefix,
                requireNonBlank(pipelineId, "pipelineId is blank"),
                requireNonBlank(name, "name is blank")
        ));
    }

    /**
     * Registers a new stage meter for the given pipeline ID, stage, and name.
     *
     * @param pipelineId the pipeline ID
     * @param stage      the stage number
     * @param name       the meter name
     * @return the new stage meter
     * @throws IllegalArgumentException if pipelineId or name are blank
     */
    public Meter registerStageMeter(String pipelineId, int stage, String name) {
        // IMPORTANT: Changing the metric name pattern is a breaking API change!
        return registry.meter(name(
                pipelinesPrefix,
                requireNonBlank(pipelineId, "pipelineId is blank"),
                "stage",
                String.valueOf(stage),
                requireNonBlank(name, "name is blank")
        ));
    }

    /**
     * Registers a new local rule meter for the given pipeline ID, stage, rule ID, and name.
     * <p>
     * A local meter name includes the pipeline ID and stage number to track metrics for the rule in a specific
     * pipeline and stage.
     *
     * @param pipelineId the pipeline ID
     * @param stage      the stage number
     * @param ruleId     the rule ID
     * @param name       the meter name
     * @return the new local rule meter
     * @throws IllegalArgumentException if pipelineId, ruleId or name are blank
     */
    public Meter registerLocalRuleMeter(String pipelineId, int stage, String ruleId, String name) {
        // The rule ID is the first name part after the prefix, so we can clean up metrics by rule ID.
        // IMPORTANT: Changing the metric name pattern is a breaking API change!
        return registry.meter(name(
                rulesPrefix,
                requireNonBlank(ruleId, "ruleId is blank"),
                requireNonBlank(pipelineId, "pipelineId is blank"),
                String.valueOf(stage),
                requireNonBlank(name, "name is blank")
        ));
    }

    /**
     * Registers a new global rule meter for the given rule ID and name.
     * <p>
     * A global meter name only includes the rule ID to track metrics for the rule overall.
     *
     * @param ruleId the rule ID
     * @param name   the meter name
     * @return the new local rule meter
     * @throws IllegalArgumentException if ruleId or name are blank
     */
    public Meter registerGlobalRuleMeter(String ruleId, String name) {
        // IMPORTANT: Changing the metric name pattern is a breaking API change!
        return registry.meter(name(
                rulesPrefix,
                requireNonBlank(ruleId, "ruleId is blank"),
                requireNonBlank(name, " name is blank")
        ));
    }

    /**
     * Removes all pipeline metrics for the given pipeline ID from the registry.
     *
     * @param pipelineId the pipeline ID
     * @throws IllegalArgumentException if pipelineId is blank
     */
    public void removePipelineMetrics(String pipelineId) {
        registry.removeMatching(MetricFilter.startsWith(name(
                pipelinesPrefix,
                requireNonBlank(pipelineId, "pipelineId is blank")
        )));
    }

    /**
     * Removes all rule metrics for the given rule ID from the registry.
     *
     * @param ruleId the rule ID
     * @throws IllegalArgumentException if ruleId is blank
     */
    public void removeRuleMetrics(String ruleId) {
        registry.removeMatching(MetricFilter.startsWith(name(
                rulesPrefix,
                requireNonBlank(ruleId, "ruleId is blank")
        )));
    }
}
