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
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreterStateUpdater;
import org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener;
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadService.ActiveCombination;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EVALUATE;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EXECUTE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessingLoadBuilderTest {

    private static final String RULE_A = "rule-A";
    private static final String PIPELINE_P = "pipeline-P";
    private static final int STAGE_0 = 0;

    private PipelineInterpreterStateUpdater stateUpdater;
    private RuleMetricsConfigService ruleMetricsConfigService;
    private ProcessingLoadBuilder processingLoadBuilder;

    @BeforeEach
    void setUp() {
        stateUpdater = mock(PipelineInterpreterStateUpdater.class);
        ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        final NodeTimerSnapshotParser snapshotParser = new NodeTimerSnapshotParser(new ObjectMapper());
        processingLoadBuilder = new ProcessingLoadBuilder(stateUpdater, ruleMetricsConfigService, new ProcessingLoadService(), snapshotParser);
    }

    @Test
    void unavailableWhenDebugMetricsOff() {
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(false));

        final AtomicBoolean fetcherInvoked = new AtomicBoolean(false);
        final ProcessingLoadResponse response = processingLoadBuilder.buildUnfiltered(timerNames -> {
            fetcherInvoked.set(true);
            return Map.of();
        });

        assertThat(response.available()).isFalse();
        assertThat(fetcherInvoked.get()).isFalse();
    }

    @Test
    void unavailableWhenNoActiveCombinations() {
        final PipelineInterpreter.State state = stateWith();
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        when(stateUpdater.getLatestState()).thenReturn(state);

        final AtomicBoolean fetcherInvoked = new AtomicBoolean(false);
        final ProcessingLoadResponse response = processingLoadBuilder.buildUnfiltered(timerNames -> {
            fetcherInvoked.set(true);
            return Map.of();
        });

        assertThat(response.available()).isFalse();
        assertThat(fetcherInvoked.get()).isFalse();
    }

    @Test
    void buildsResponseWhenMetricsEnabledAndDataPresent() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        when(stateUpdater.getLatestState()).thenReturn(state);

        final ProcessingLoadResponse response = processingLoadBuilder.buildUnfiltered(timerNames -> {
            assertThat(timerNames).containsExactlyInAnyOrder(
                    timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE),
                    timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE)
            );
            return Map.of("node-1", metricsResponse(List.of(
                    timerEntry(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 100.0d, 10.0d),
                    timerEntry(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 100.0d, 90.0d)
            )));
        });

        assertThat(response.available()).isTrue();
        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(10_000.0d);
        assertThat(response.rules()).extracting(RuleLoad::loadPercent).containsExactly(100.00d);
    }

    @Test
    void survivesNodeWithoutResponse() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        when(stateUpdater.getLatestState()).thenReturn(state);

        final ProcessingLoadResponse response = processingLoadBuilder.buildUnfiltered(timerNames -> Map.of(
                "node-1", metricsResponse(List.of(
                        timerEntry(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 100.0d, 10.0d),
                        timerEntry(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 100.0d, 90.0d)
                ))
        ));

        assertThat(response.available()).isTrue();
        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(10_000.0d);
    }

    @Test
    void metricsEnabledReflectsConfig() {
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        assertThat(processingLoadBuilder.metricsEnabled()).isTrue();

        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(false));
        assertThat(processingLoadBuilder.metricsEnabled()).isFalse();
    }

    @Test
    void activeCombinationsEmptyWhenMetricsOff() {
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(false));
        assertThat(processingLoadBuilder.activeCombinations()).isEmpty();
    }

    @Test
    void activeCombinationsEmptyWhenNoCombinations() {
        final PipelineInterpreter.State state = stateWith();
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        when(stateUpdater.getLatestState()).thenReturn(state);

        assertThat(processingLoadBuilder.activeCombinations()).isEmpty();
    }

    @Test
    void activeCombinationsReturnsRuleAndPipelineIds() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        when(ruleMetricsConfigService.get()).thenReturn(metricsConfig(true));
        when(stateUpdater.getLatestState()).thenReturn(state);

        final List<ActiveCombination> combinations = processingLoadBuilder.activeCombinations();
        assertThat(combinations).hasSize(1);
        assertThat(combinations.getFirst().ruleId()).isEqualTo(RULE_A);
        assertThat(combinations.getFirst().pipelineId()).isEqualTo(PIPELINE_P);
    }

    private static RuleMetricsConfigDto metricsConfig(boolean enabled) {
        return RuleMetricsConfigDto.builder().metricsEnabled(enabled).build();
    }

    private static MetricsSummaryResponse metricsResponse(List<Map<String, Object>> entries) {
        return MetricsSummaryResponse.create(entries.size(), new ArrayList<>(entries));
    }

    private static Map<String, Object> timerEntry(String name, double fifteenMinuteRate, double meanMicroseconds) {
        return Map.of(
                "full_name", name,
                "metric", Map.of(
                        "rate", Map.of("fifteen_minute", fifteenMinuteRate),
                        "time", Map.of("mean", meanMicroseconds)
                )
        );
    }

    private static String timerName(String ruleId, String pipelineId, int stage, RuleMetricsListener.Type type) {
        return RuleMetricsListener.getMetricName(name(ruleId, pipelineId, String.valueOf(stage)), type);
    }

    private static PipelineInterpreter.State stateWith(Pipeline... pipelines) {
        final ImmutableMap.Builder<String, Pipeline> builder = ImmutableMap.builder();
        for (Pipeline pipeline : pipelines) {
            builder.put(pipeline.id(), pipeline);
        }
        final PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
        when(state.getCurrentPipelines()).thenReturn(builder.build());
        return state;
    }

    private static PipelineInterpreter.State stateWithSinglePipeline(String pipelineId, int stageNumber, String... ruleIds) {
        final Rule[] rules = new Rule[ruleIds.length];
        for (int i = 0; i < ruleIds.length; i++) {
            rules[i] = mockRule(ruleIds[i]);
        }
        return stateWith(pipeline(pipelineId, List.of(stage(stageNumber, rules))));
    }

    private static Rule mockRule(String id) {
        final Rule rule = mock(Rule.class);
        when(rule.id()).thenReturn(id);
        when(rule.name()).thenReturn(id);
        return rule;
    }

    private static Stage stage(int stageNumber, Rule... rules) {
        final Stage stage = Stage.builder()
                .stage(stageNumber)
                .match(Stage.Match.ALL)
                .ruleReferences(List.of(rules).stream().map(Rule::name).toList())
                .build();
        stage.setRules(List.of(rules));
        return stage;
    }

    private static Pipeline pipeline(String pipelineId, List<Stage> stages) {
        final SortedSet<Stage> sortedStages = new TreeSet<>(Comparator.comparingInt(Stage::stage));
        sortedStages.addAll(stages);
        return Pipeline.builder()
                .id(pipelineId)
                .name(pipelineId)
                .stages(sortedStages)
                .build();
    }
}
