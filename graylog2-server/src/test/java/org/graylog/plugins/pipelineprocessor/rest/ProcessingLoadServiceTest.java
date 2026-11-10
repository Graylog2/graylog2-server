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

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EVALUATE;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EXECUTE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessingLoadServiceTest {

    private static final String RULE_A = "rule-A";
    private static final String RULE_B = "rule-B";
    private static final String RULE_C = "rule-C";
    private static final String PIPELINE_P = "pipeline-P";
    private static final int STAGE_0 = 0;

    private final ProcessingLoadService service = new ProcessingLoadService();

    private ProcessingLoadResponse compute(PipelineInterpreter.State state, Map<String, NodeTimerSnapshot> snapshots) {
        return service.compute(service.activeCombinations(state), snapshots);
    }

    private List<String> expectedTimerNames(PipelineInterpreter.State state) {
        return service.expectedTimerNames(service.activeCombinations(state));
    }

    @Test
    void computesPercentagesOnOneNode() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A, RULE_B, RULE_C);

        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 1000.0d, 5.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 1000.0d, 45.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EVALUATE), 100.0d, 10.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EXECUTE), 50.0d, 380.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EVALUATE), 1000.0d, 10.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EXECUTE), 10.0d, 9000.0d)
                .build();

        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));

        assertThat(response.available()).isTrue();
        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(170_000.0d);
        assertThat(response.stageRules())
                .extracting(StageRuleLoad::ruleId, StageRuleLoad::pipelineId, StageRuleLoad::stage, StageRuleLoad::loadPercent)
                .containsExactlyInAnyOrder(
                        tuple(RULE_A, PIPELINE_P, STAGE_0, 29.41d),
                        tuple(RULE_B, PIPELINE_P, STAGE_0, 11.76d),
                        tuple(RULE_C, PIPELINE_P, STAGE_0, 58.82d)
                );
        assertThat(response.pipelines())
                .extracting(PipelineLoad::pipelineId, PipelineLoad::loadPercent)
                .containsExactly(tuple(PIPELINE_P, 100.00d));
        assertThat(response.rules())
                .extracting(RuleLoad::ruleId, RuleLoad::loadPercent)
                .containsExactlyInAnyOrder(
                        tuple(RULE_A, 29.41d),
                        tuple(RULE_B, 11.76d),
                        tuple(RULE_C, 58.82d)
                );
    }

    @Test
    void sumsCostsAcrossNodes() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A, RULE_B, RULE_C);

        // Two nodes split the same workload. Totals match the single-node case.
        final NodeTimerSnapshot node1 = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 500.0d, 5.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 500.0d, 45.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EVALUATE), 50.0d, 10.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EXECUTE), 25.0d, 380.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EVALUATE), 500.0d, 10.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EXECUTE), 5.0d, 9000.0d)
                .build();
        final NodeTimerSnapshot node2 = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 500.0d, 5.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 500.0d, 45.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EVALUATE), 50.0d, 10.0d)
                .timer(timerName(RULE_B, PIPELINE_P, STAGE_0, EXECUTE), 25.0d, 380.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EVALUATE), 500.0d, 10.0d)
                .timer(timerName(RULE_C, PIPELINE_P, STAGE_0, EXECUTE), 5.0d, 9000.0d)
                .build();

        final ProcessingLoadResponse response = compute(state, Map.of("node-1", node1, "node-2", node2));

        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(170_000.0d);
        assertThat(response.stageRules())
                .extracting(StageRuleLoad::ruleId, StageRuleLoad::loadPercent)
                .containsExactlyInAnyOrder(
                        tuple(RULE_A, 29.41d),
                        tuple(RULE_B, 11.76d),
                        tuple(RULE_C, 58.82d)
                );
    }

    @Test
    void aggregatesSharedRuleAcrossPipelines() {
        // Rule X used in two pipelines, per-rule view aggregates both.
        final Rule sharedRule = mockRule("rule-X");
        final Pipeline pipeline1 = pipeline("pipeline-1", List.of(stage(0, sharedRule)));
        final Pipeline pipeline2 = pipeline("pipeline-2", List.of(stage(0, sharedRule)));
        final PipelineInterpreter.State state = stateWith(pipeline1, pipeline2);

        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName("rule-X", "pipeline-1", 0, EVALUATE), 100.0d, 10.0d)
                .timer(timerName("rule-X", "pipeline-1", 0, EXECUTE), 100.0d, 20.0d)
                .timer(timerName("rule-X", "pipeline-2", 0, EVALUATE), 100.0d, 30.0d)
                .timer(timerName("rule-X", "pipeline-2", 0, EXECUTE), 100.0d, 40.0d)
                .build();

        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));

        // Total cost = (100*10 + 100*20) + (100*30 + 100*40) = 3000 + 7000 = 10000
        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(10_000.0d);
        assertThat(response.stageRules())
                .extracting(StageRuleLoad::pipelineId, StageRuleLoad::loadPercent)
                .containsExactlyInAnyOrder(
                        tuple("pipeline-1", 30.00d),
                        tuple("pipeline-2", 70.00d)
                );
        assertThat(response.rules())
                .extracting(RuleLoad::ruleId, RuleLoad::loadPercent)
                .containsExactly(tuple("rule-X", 100.00d));
        assertThat(response.pipelines())
                .extracting(PipelineLoad::pipelineId, PipelineLoad::loadPercent)
                .containsExactlyInAnyOrder(
                        tuple("pipeline-1", 30.00d),
                        tuple("pipeline-2", 70.00d)
                );
    }

    @Test
    void pipelineShareIsScopedToPipeline() {
        // Costs (µs/s): A=30, B=70 → P1=100; C=100 → P2=100; cluster=200.
        final Rule a = mockRule("rule-A");
        final Rule b = mockRule("rule-B");
        final Rule c = mockRule("rule-C");
        final Pipeline p1 = pipeline("P1", List.of(stage(0, a, b)));
        final Pipeline p2 = pipeline("P2", List.of(stage(0, c)));
        final PipelineInterpreter.State state = stateWith(p1, p2);

        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName("rule-A", "P1", 0, EVALUATE), 10.0d, 3.0d)
                .timer(timerName("rule-B", "P1", 0, EVALUATE), 10.0d, 7.0d)
                .timer(timerName("rule-C", "P2", 0, EVALUATE), 10.0d, 10.0d)
                .build();

        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));

        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(200.0d);
        assertThat(response.stageRules())
                .extracting(StageRuleLoad::ruleId, StageRuleLoad::loadPercent, StageRuleLoad::pipelineSharePercent)
                .containsExactlyInAnyOrder(
                        tuple("rule-A", 15.0d, 30.0d),
                        tuple("rule-B", 35.0d, 70.0d),
                        tuple("rule-C", 50.0d, 100.0d)
                );
    }

    @Test
    void unavailableWhenNoPipelines() {
        final PipelineInterpreter.State state = stateWith();
        final ProcessingLoadResponse response = compute(state, Map.of(
                "node-1", NodeTimerSnapshot.builder()
                        .timer("anything", 1.0d, 1.0d)
                        .build()
        ));
        assertThat(response.available()).isFalse();
        assertThat(response.stageRules()).isEmpty();
        assertThat(response.pipelines()).isEmpty();
        assertThat(response.rules()).isEmpty();
    }

    @Test
    void unavailableWhenAllNodesEmpty() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        final ProcessingLoadResponse response = compute(state, Map.of(
                "node-1", NodeTimerSnapshot.empty(),
                "node-2", NodeTimerSnapshot.empty()
        ));
        assertThat(response.available()).isFalse();
    }

    @Test
    void unavailableWhenNoNodeData() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        final ProcessingLoadResponse response = compute(state, Map.of());
        assertThat(response.available()).isFalse();
    }

    @Test
    void unavailableWhenTotalIsZero() {
        // Timers exist but every rate or mean is zero.
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 0.0d, 100.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 0.0d, 100.0d)
                .build();
        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));
        assertThat(response.available()).isFalse();
    }

    @Test
    void worksWithPartialNodeData() {
        // One node has data, the other is empty.
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A);
        final NodeTimerSnapshot withData = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 100.0d, 10.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 100.0d, 90.0d)
                .build();
        final ProcessingLoadResponse response = compute(state, Map.of(
                "node-1", withData,
                "node-2", NodeTimerSnapshot.empty()
        ));
        assertThat(response.available()).isTrue();
        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(10_000.0d);
        assertThat(response.rules()).extracting(RuleLoad::loadPercent).containsExactly(100.00d);
    }

    @Test
    void timerNamesCoverAllRules() {
        final PipelineInterpreter.State state = stateWithSinglePipeline(PIPELINE_P, STAGE_0, RULE_A, RULE_B);
        assertThat(expectedTimerNames(state)).containsExactlyInAnyOrder(
                timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE),
                timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE),
                timerName(RULE_B, PIPELINE_P, STAGE_0, EVALUATE),
                timerName(RULE_B, PIPELINE_P, STAGE_0, EXECUTE)
        );
    }

    @Test
    void dedupesRuleListedTwiceInStage() {
        // Same rule listed twice in a stage maps to one timer, count it once.
        final Rule duplicate = mockRule(RULE_A);
        final Pipeline pipeline = pipeline(PIPELINE_P, List.of(stage(STAGE_0, duplicate, duplicate)));
        final PipelineInterpreter.State state = stateWith(pipeline);

        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE), 100.0d, 10.0d)
                .timer(timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE), 100.0d, 10.0d)
                .build();
        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));

        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(2_000.0d);
        assertThat(response.stageRules()).hasSize(1);
        assertThat(response.rules())
                .extracting(RuleLoad::ruleId, RuleLoad::loadPercent)
                .containsExactly(tuple(RULE_A, 100.00d));
    }

    @Test
    void dedupesRuleAcrossStages() {
        // Listener isn't stage-aware: per-stage timers mirror identical data. Dedupe by
        // (rule, pipeline) and keep the lowest stage.
        final Rule shared = mockRule(RULE_A);
        final Pipeline pipeline = pipeline(PIPELINE_P, List.of(stage(0, shared), stage(2, shared)));
        final PipelineInterpreter.State state = stateWith(pipeline);

        final NodeTimerSnapshot snapshot = NodeTimerSnapshot.builder()
                .timer(timerName(RULE_A, PIPELINE_P, 0, EVALUATE), 100.0d, 10.0d)
                .timer(timerName(RULE_A, PIPELINE_P, 0, EXECUTE), 100.0d, 10.0d)
                // Stage-2 timer mirrors stage-0 data, listener writes to both.
                .timer(timerName(RULE_A, PIPELINE_P, 2, EVALUATE), 100.0d, 10.0d)
                .timer(timerName(RULE_A, PIPELINE_P, 2, EXECUTE), 100.0d, 10.0d)
                .build();
        final ProcessingLoadResponse response = compute(state, Map.of("node-1", snapshot));

        assertThat(response.totalCostMicrosecondsPerSecond()).isEqualTo(2_000.0d);
        assertThat(response.stageRules())
                .extracting(StageRuleLoad::ruleId, StageRuleLoad::stage)
                .containsExactly(tuple(RULE_A, 0));
        assertThat(response.rules())
                .extracting(RuleLoad::ruleId, RuleLoad::loadPercent)
                .containsExactly(tuple(RULE_A, 100.00d));
    }

    @Test
    void skipsRulesWithoutId() {
        // A rule without an id has no timer.
        final Rule withId = mockRule(RULE_A);
        final Rule withoutId = mock(Rule.class);
        when(withoutId.id()).thenReturn(null);
        when(withoutId.name()).thenReturn("synthetic");
        final Pipeline pipeline = pipeline(PIPELINE_P, List.of(stage(STAGE_0, withId, withoutId)));
        final PipelineInterpreter.State state = stateWith(pipeline);

        assertThat(expectedTimerNames(state)).containsExactlyInAnyOrder(
                timerName(RULE_A, PIPELINE_P, STAGE_0, EVALUATE),
                timerName(RULE_A, PIPELINE_P, STAGE_0, EXECUTE)
        );
    }

    @Test
    void filterKeepsAllowedRows() {
        final ProcessingLoadResponse full = ProcessingLoadResponse.create(
                true,
                100.0d,
                List.of(
                        StageRuleLoad.create("rule-a", "pipe-1", 0, 30.0d, 100.0d),
                        StageRuleLoad.create("rule-b", "pipe-2", 0, 70.0d, 100.0d)
                ),
                List.of(
                        PipelineLoad.create("pipe-1", 30.0d),
                        PipelineLoad.create("pipe-2", 70.0d)
                ),
                List.of(
                        RuleLoad.create("rule-a", 30.0d),
                        RuleLoad.create("rule-b", 70.0d)
                )
        );

        final ProcessingLoadResponse filtered = service.filterByPermissions(
                full,
                "pipe-1"::equals,
                "rule-a"::equals
        );

        assertThat(filtered.available()).isTrue();
        assertThat(filtered.totalCostMicrosecondsPerSecond()).isEqualTo(100.0d);
        assertThat(filtered.stageRules())
                .extracting(StageRuleLoad::ruleId)
                .containsExactly("rule-a");
        assertThat(filtered.pipelines())
                .extracting(PipelineLoad::pipelineId)
                .containsExactly("pipe-1");
        assertThat(filtered.rules())
                .extracting(RuleLoad::ruleId)
                .containsExactly("rule-a");
    }

    @Test
    void filterKeepsPipelinesWhenRulePermsMissing() {
        // pipeline-read only: pipelines[] keeps pipe-1, stage_rules[] and rules[] empty.
        final ProcessingLoadResponse full = ProcessingLoadResponse.create(
                true,
                100.0d,
                List.of(StageRuleLoad.create("rule-a", "pipe-1", 0, 100.0d, 100.0d)),
                List.of(PipelineLoad.create("pipe-1", 100.0d)),
                List.of(RuleLoad.create("rule-a", 100.0d))
        );

        final ProcessingLoadResponse filtered = service.filterByPermissions(
                full,
                "pipe-1"::equals,
                id -> false);

        assertThat(filtered.available()).isTrue();
        assertThat(filtered.stageRules()).isEmpty();
        assertThat(filtered.rules()).isEmpty();
        assertThat(filtered.pipelines()).extracting(PipelineLoad::pipelineId).containsExactly("pipe-1");
    }

    @Test
    void filterStaysAvailableWhenEmpty() {
        final ProcessingLoadResponse full = ProcessingLoadResponse.create(
                true,
                100.0d,
                List.of(StageRuleLoad.create("rule-a", "pipe-1", 0, 100.0d, 100.0d)),
                List.of(PipelineLoad.create("pipe-1", 100.0d)),
                List.of(RuleLoad.create("rule-a", 100.0d))
        );

        final ProcessingLoadResponse filtered = service.filterByPermissions(
                full,
                id -> false,
                id -> false
        );

        assertThat(filtered.available()).isTrue();
        assertThat(filtered.totalCostMicrosecondsPerSecond()).isEqualTo(100.0d);
        assertThat(filtered.stageRules()).isEmpty();
        assertThat(filtered.pipelines()).isEmpty();
        assertThat(filtered.rules()).isEmpty();
    }

    @Test
    void filterPassesUnavailableThrough() {
        final ProcessingLoadResponse unavailable = ProcessingLoadResponse.unavailable();
        final ProcessingLoadResponse filtered = service.filterByPermissions(
                unavailable, id -> true, id -> true
        );
        assertThat(filtered).isSameAs(unavailable);
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
