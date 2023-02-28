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
import com.google.common.collect.Sets;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.antlr.v4.runtime.CommonToken;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

@AutoValue
public abstract class Rule {
    private static final RateLimitedLog LOG = getRateLimitedLog(Rule.class);

    private transient Set<String> metricNames = Sets.newHashSet();

    private transient Meter globalExecuted;
    private transient Meter localExecuted;
    private transient Meter globalFailed;
    private transient Meter localFailed;
    private transient Meter globalMatched;
    private transient Meter localMatched;
    private transient Meter globalNotMatched;
    private transient Meter localNotMatched;

    @Nullable
    public abstract String id();

    public abstract String name();

    public abstract LogicalExpression when();

    public abstract Collection<Statement> then();

    public static Builder builder() {
        return new AutoValue_Rule.Builder();
    }

    public abstract Builder toBuilder();

    public Rule withId(String id) {
        return toBuilder().id(id).build();
    }

    public static Rule alwaysFalse(String name) {
        return builder().name(name).when(new BooleanExpression(new CommonToken(-1), false)).then(Collections.emptyList()).build();
    }

    /**
     * Register the metrics attached to this pipeline.
     *
     * @param metricRegistry the registry to add the metrics to
     */
    public void registerMetrics(MetricRegistry metricRegistry, String pipelineId, String stageId) {
        if (id() == null) {
            LOG.debug("Not registering metrics for unsaved rule {}", name());
            return;
        }
        if (id() != null) {
            globalExecuted = registerGlobalMeter(metricRegistry, "executed");
            localExecuted = registerLocalMeter(metricRegistry, pipelineId, stageId, "executed");

            globalFailed = registerGlobalMeter(metricRegistry, "failed");
            localFailed = registerLocalMeter(metricRegistry, pipelineId, stageId, "failed");

            globalMatched = registerGlobalMeter(metricRegistry, "matched");
            localMatched = registerLocalMeter(metricRegistry, pipelineId, stageId, "matched");

            globalNotMatched = registerGlobalMeter(metricRegistry, "not-matched");
            localNotMatched = registerLocalMeter(metricRegistry, pipelineId, stageId, "not-matched");

        }
    }

    private Meter registerGlobalMeter(MetricRegistry metricRegistry, String type) {
        final String name = MetricRegistry.name(Rule.class, id(), type);
        metricNames.add(name);
        return metricRegistry.meter(name);
    }

    private Meter registerLocalMeter(MetricRegistry metricRegistry,
                                     String pipelineId,
                                     String stageId, String type) {
        final String name = MetricRegistry.name(Rule.class, id(), pipelineId, stageId, type);
        metricNames.add(name);
        return metricRegistry.meter(name);
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
        return (name, metric) -> metricNames.contains(name);

    }

    public void markExecution() {
        if (id() != null) {
            globalExecuted.mark();
            localExecuted.mark();
        }
    }

    public void markMatch() {
        if (id() != null) {
            globalMatched.mark();
            localMatched.mark();
        }
    }

    public void markNonMatch() {
        if (id() != null) {
            globalNotMatched.mark();
            localNotMatched.mark();
        }
    }

    public void markFailure() {
        if (id() != null) {
            globalFailed.mark();
            localFailed.mark();
        }
    }

    /**
     * Creates a copy of this Rule.
     *
     * @return a copy of this rule
     */
    public Rule copy() {
        return toBuilder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder when(LogicalExpression condition);

        public abstract Builder then(Collection<Statement> actions);

        public abstract Rule build();
    }

    @Override
    public String toString() {
        return "Rule '" + name() + "' (" + id() + ")";
    }
}
