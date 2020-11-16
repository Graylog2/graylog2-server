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
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog2.plugin.Message;
import org.graylog2.shared.metrics.MetricUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This interpreter listener maintains timer metrics for rules.
 */
public class RuleMetricsListener implements InterpreterListener {
    public enum Type {
        EXECUTE, EVALUATE
    }

    private final MetricRegistry metricRegistry;
    private final Map<TimerMapKey, Timer.Context> evaluateTimers = new HashMap<>();
    private final Map<TimerMapKey, Timer.Context> executeTimers = new HashMap<>();

    public RuleMetricsListener(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public static String getMetricName(String name, Type type) {
        return name(Rule.class, name, "trace", type.toString().toLowerCase(Locale.US), "duration");
    }

    private void forEachStage(Rule rule, Pipeline pipeline, Consumer<Stage> consumer) {
        pipeline.stages().forEach(stage -> {
            stage.getRules().stream()
                    .filter(stageRule -> Objects.equals(stageRule.id(), rule.id()))
                    .forEach(stageRule -> consumer.accept(stage));
        });
    }

    private void startTimerForKey(TimerMapKey key, String metricName, Map<TimerMapKey, Timer.Context> timers) {
        final Timer timer = MetricUtils.getOrRegister(metricRegistry, metricName, new Timer());
        timers.put(key, timer.time());
    }

    private void startTimer(Rule rule, Pipeline pipeline, Type type, Map<TimerMapKey, Timer.Context> timers) {
        if (rule.id() != null && pipeline.id() != null) {
            forEachStage(rule, pipeline, stage -> {
                final String name = name(rule.id(), pipeline.id(), String.valueOf(stage.stage()));
                startTimerForKey(new TimerMapKey(rule, pipeline, stage), getMetricName(name, type), timers);
            });
            startTimerForKey(new TimerMapKey(rule), getMetricName(rule.id(), type), timers);
        }
    }

    private void stopTimerForKey(TimerMapKey key, Map<TimerMapKey, Timer.Context> timers) {
        final Timer.Context timer = timers.get(key);
        if (timer != null) {
            timer.stop();
        }
    }

    private void stopTimer(Rule rule, Pipeline pipeline, Map<TimerMapKey, Timer.Context> timers) {
        if (rule.id() != null && pipeline.id() != null) {
            forEachStage(rule, pipeline, stage -> stopTimerForKey(new TimerMapKey(rule, pipeline, stage), timers));
            stopTimerForKey(new TimerMapKey(rule), timers);
        }
    }

    @Override
    public void startProcessing() {
    }

    @Override
    public void finishProcessing() {
    }

    @Override
    public void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams) {
    }

    @Override
    public void enterStage(Stage stage) {
    }

    @Override
    public void exitStage(Stage stage) {
    }

    @Override
    public void evaluateRule(Rule rule, Pipeline pipeline) {
        startTimer(rule, pipeline, Type.EVALUATE, evaluateTimers);
    }

    @Override
    public void failEvaluateRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, pipeline, evaluateTimers);
    }

    @Override
    public void satisfyRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, pipeline, evaluateTimers);
    }

    @Override
    public void dissatisfyRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, pipeline, evaluateTimers);
    }

    @Override
    public void executeRule(Rule rule, Pipeline pipeline) {
        startTimer(rule, pipeline, Type.EXECUTE, executeTimers);
    }

    @Override
    public void finishExecuteRule(Rule rule, Pipeline pipeline) {
        stopTimer(rule, pipeline, executeTimers);
    }

    @Override
    public void failExecuteRule(Rule rule, Pipeline pipeline) {
    }

    @Override
    public void continuePipelineExecution(Pipeline pipeline, Stage stage) {
    }

    @Override
    public void stopPipelineExecution(Pipeline pipeline, Stage stage) {
    }

    /**
     * Helper class to simplify timer map key handling.
     */
    private static class TimerMapKey {
        private final String rule;
        private final String pipeline;
        private final int stage;

        TimerMapKey(Rule rule) {
            this(rule, null, null);
        }

        TimerMapKey(Rule rule, @Nullable Pipeline pipeline, @Nullable Stage stage) {
            this.rule = rule.id();
            this.pipeline = pipeline != null ? pipeline.id() : null;
            this.stage = stage != null ? stage.stage() : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimerMapKey that = (TimerMapKey) o;
            return stage == that.stage &&
                    rule.equals(that.rule) &&
                    Objects.equals(pipeline, that.pipeline);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule, pipeline, stage);
        }
    }
}
