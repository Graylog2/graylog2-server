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
package org.graylog.plugins.pipelineprocessor.processors.listeners;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSortedSet;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RuleMetricsListenerTest {

    private static final String RULE_ID = "r1";
    private static final String PIPELINE_ID = "p1";

    @Mock
    private Rule rule;
    @Mock
    private Pipeline pipeline;
    @Mock
    private Stage stage;

    private MetricRegistry metricRegistry;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        lenient().when(rule.id()).thenReturn(RULE_ID);
        lenient().when(pipeline.id()).thenReturn(PIPELINE_ID);
        lenient().when(pipeline.stages()).thenReturn(ImmutableSortedSet.of(stage));
        lenient().when(stage.stage()).thenReturn(0);
        lenient().when(stage.getRules()).thenReturn(List.of(rule));
    }

    @Test
    void recordsAllInvocationsAtRateOne() {
        final RuleMetricsListener listener = new RuleMetricsListener(metricRegistry, 1, n -> {
            throw new AssertionError("sampler must not be consulted at sampleRate=1");
        });

        for (int i = 0; i < 5; i++) {
            listener.evaluateRule(rule, pipeline);
            listener.satisfyRule(rule, pipeline);
        }

        final String ruleOnlyTimer = RuleMetricsListener.getMetricName(RULE_ID, RuleMetricsListener.Type.EVALUATE);
        assertThat(metricRegistry.getTimers().get(ruleOnlyTimer).getCount()).isEqualTo(5L);
    }

    @Test
    void recordsOnlySampledInvocations() {
        // 4 cycles with sampler decisions in/out/in/out → only the 2 sampled-in cycles record
        final boolean[] decisions = {true, false, true, false};
        final AtomicInteger idx = new AtomicInteger();
        final RuleMetricsListener listener = new RuleMetricsListener(
                metricRegistry, 2, n -> decisions[idx.getAndIncrement()]);

        for (int i = 0; i < 4; i++) {
            listener.evaluateRule(rule, pipeline);
            listener.satisfyRule(rule, pipeline);
        }

        final String ruleOnlyTimer = RuleMetricsListener.getMetricName(RULE_ID, RuleMetricsListener.Type.EVALUATE);
        assertThat(metricRegistry.getTimers().get(ruleOnlyTimer).getCount()).isEqualTo(2L);
    }

    @Test
    void noTimerWhenAllSampledOut() {
        final RuleMetricsListener listener = new RuleMetricsListener(metricRegistry, 10, n -> false);

        for (int i = 0; i < 5; i++) {
            listener.evaluateRule(rule, pipeline);
            listener.satisfyRule(rule, pipeline);
            listener.executeRule(rule, pipeline);
            listener.finishExecuteRule(rule, pipeline);
        }

        final SortedMap<String, Timer> timers = metricRegistry.getTimers();
        final String evaluateRuleOnly = RuleMetricsListener.getMetricName(RULE_ID, RuleMetricsListener.Type.EVALUATE);
        final String executeRuleOnly = RuleMetricsListener.getMetricName(RULE_ID, RuleMetricsListener.Type.EXECUTE);
        assertThat(timers).doesNotContainKeys(evaluateRuleOnly, executeRuleOnly);
    }

    @Test
    void rejectsNonPositiveSampleRate() {
        assertThatThrownBy(() -> new RuleMetricsListener(metricRegistry, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleMetricsListener(metricRegistry, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
