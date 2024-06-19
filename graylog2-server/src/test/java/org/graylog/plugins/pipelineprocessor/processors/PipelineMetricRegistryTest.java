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
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineMetricRegistryTest {
    @Test
    void register() {
        final var metricRegistry = new MetricRegistry();
        final var registry = PipelineMetricRegistry.create(metricRegistry, "PIPELINE", "RULE");

        assertThat(registry.registerPipelineMeter("pipeline-1", "executed"))
                .isInstanceOf(Meter.class);
        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "PIPELINE.pipeline-1.executed"
        );

        assertThat(registry.registerStageMeter("pipeline-1", 3, "executed"))
                .isInstanceOf(Meter.class);
        assertThat(metricRegistry.getMetrics().keySet()).containsExactlyInAnyOrder(
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.3.executed"
        );

        assertThat(registry.registerLocalRuleMeter("pipeline-1", 3, "rule-1", "executed"))
                .isInstanceOf(Meter.class);
        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.3.executed",
                "RULE.rule-1.pipeline-1.3.executed"
        );

        assertThat(registry.registerGlobalRuleMeter("rule-1", "executed"))
                .isInstanceOf(Meter.class);
        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.3.executed",
                "RULE.rule-1.pipeline-1.3.executed",
                "RULE.rule-1.executed"
        );

        // The metric registry should only contain meters.
        assertThat(metricRegistry.getCounters()).isEmpty();
        assertThat(metricRegistry.getTimers()).isEmpty();
        assertThat(metricRegistry.getGauges()).isEmpty();
        assertThat(metricRegistry.getHistograms()).isEmpty();
    }

    @Test
    void removePipelineMetrics() {
        final var metricRegistry = new MetricRegistry();
        final var registry = PipelineMetricRegistry.create(metricRegistry, "PIPELINE", "RULE");

        registry.registerPipelineMeter("pipeline-1", "executed");
        registry.registerStageMeter("pipeline-1", 5, "executed");
        registry.registerLocalRuleMeter("pipeline-1", 5, "rule-1", "executed");
        registry.registerGlobalRuleMeter("rule-1", "executed");

        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "RULE.rule-1.executed",
                "RULE.rule-1.pipeline-1.5.executed",
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.5.executed"
        );

        registry.removePipelineMetrics("pipeline-1");

        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "RULE.rule-1.executed",
                "RULE.rule-1.pipeline-1.5.executed"
        );

        registry.removeRuleMetrics("rule-1");

        assertThat(metricRegistry.getMeters().keySet()).isEmpty();
    }

    @Test
    void removeRuleMetrics() {
        final var metricRegistry = new MetricRegistry();
        final var registry = PipelineMetricRegistry.create(metricRegistry, "PIPELINE", "RULE");

        registry.registerPipelineMeter("pipeline-1", "executed");
        registry.registerStageMeter("pipeline-1", 7, "executed");
        registry.registerLocalRuleMeter("pipeline-1", 7, "rule-1", "executed");
        registry.registerGlobalRuleMeter("rule-1", "executed");

        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "RULE.rule-1.executed",
                "RULE.rule-1.pipeline-1.7.executed",
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.7.executed"
        );

        registry.removeRuleMetrics("rule-1");

        assertThat(metricRegistry.getMeters().keySet()).containsExactlyInAnyOrder(
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.7.executed"
        );
    }

    @Test
    void validation() {
        final var metricRegistry = new MetricRegistry();

        assertThatThrownBy(() -> PipelineMetricRegistry.create(null, "PIPELINE", "RULE"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, null, "RULE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, "", "RULE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, "  ", "RULE"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, "PIPELINE", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, "PIPELINE", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PipelineMetricRegistry.create(metricRegistry, "PIPELINE", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
