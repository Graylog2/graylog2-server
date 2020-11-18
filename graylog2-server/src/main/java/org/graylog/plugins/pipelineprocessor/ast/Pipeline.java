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
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.Sets;
import org.graylog2.shared.metrics.MetricUtils;

import javax.annotation.Nullable;
import java.util.SortedSet;

@AutoValue
public abstract class Pipeline {

    private String metricName;
    private transient Meter executed;

    @Nullable
    public abstract String id();
    public abstract String name();
    public abstract SortedSet<Stage> stages();

    public static Builder builder() {
        return new AutoValue_Pipeline.Builder();
    }

    public static Pipeline empty(String name) {
        return builder().name(name).stages(Sets.<Stage>newTreeSet()).build();
    }

    public abstract Builder toBuilder();

    public Pipeline withId(String id) {
        return toBuilder().id(id).build();
    }

    @Override
    @Memoized
    public abstract int hashCode();

    /**
     * Register the metrics attached to this pipeline.
     *
     * @param metricRegistry the registry to add the metrics to
     */
    public void registerMetrics(MetricRegistry metricRegistry) {
        if (id() != null) {
            metricName = MetricRegistry.name(Pipeline.class, id(), "executed");
            executed = metricRegistry.meter(metricName);
        }
    }

    /**
     * The metric filter matching all metrics that have been registered by this pipeline.
     * Commonly used to remove the relevant metrics from the registry upon deletion of the pipeline.
     *
     * @return the filter matching this pipeline's metrics
     */
    public MetricFilter metricsFilter() {
        if (id() == null) {
            return (name, metric) -> false;
        }
        return new MetricUtils.SingleMetricFilter(metricName);

    }
    public void markExecution() {
        if (executed != null) {
            executed.mark();
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Pipeline build();

        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder stages(SortedSet<Stage> stages);
    }

    @Override
    public String toString() {
        return "Pipeline '" + name() + "' (" + id() + ")";
    }
}
