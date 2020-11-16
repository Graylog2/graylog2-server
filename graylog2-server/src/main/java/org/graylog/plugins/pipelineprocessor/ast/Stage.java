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
package org.graylog.plugins.pipelineprocessor.ast;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.auto.value.AutoValue;
import org.graylog2.shared.metrics.MetricUtils;

import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

@AutoValue
public abstract class Stage implements Comparable<Stage> {
    private List<Rule> rules;
    // not an autovalue property, because it introduces a cycle in hashCode() and we have no way of excluding it
    private transient Pipeline pipeline;
    private transient Meter executed;
    private transient String meterName;

    public abstract int stage();
    public abstract boolean matchAll();
    public abstract List<String> ruleReferences();

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public static Builder builder() {
        return new AutoValue_Stage.Builder();
    }

    public abstract Builder toBuilder();

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") Stage other) {
        return Integer.compare(stage(), other.stage());
    }

    /**
     * Register the metrics attached to this stage.
     *
     * @param metricRegistry the registry to add the metrics to
     */
    public void registerMetrics(MetricRegistry metricRegistry, String pipelineId) {
        meterName = name(Pipeline.class, pipelineId, "stage", String.valueOf(stage()), "executed");
        executed = metricRegistry.meter(meterName);
    }

    /**
     * The metric filter matching all metrics that have been registered by this pipeline.
     * Commonly used to remove the relevant metrics from the registry upon deletion of the pipeline.
     *
     * @return the filter matching this pipeline's metrics
     */
    public MetricFilter metricsFilter() {
        if (meterName == null) {
            return (name, metric) -> false;
        }
        return new MetricUtils.SingleMetricFilter(meterName);

    }
    public void markExecution() {
        if (executed != null) {
            executed.mark();
        }
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Stage build();

        public abstract Builder stage(int stageNumber);

        public abstract Builder matchAll(boolean mustMatchAll);

        public abstract Builder ruleReferences(List<String> ruleRefs);
    }

    @Override
    public String toString() {
        return "Stage " + stage();
    }
}
