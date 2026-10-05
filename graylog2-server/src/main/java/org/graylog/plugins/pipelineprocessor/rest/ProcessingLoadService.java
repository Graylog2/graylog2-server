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

import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EVALUATE;
import static org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener.Type.EXECUTE;

@Singleton
public class ProcessingLoadService {

    /**
     * Returns one entry per (rule, pipeline). When a rule appears in several stages of one
     * pipeline, only the lowest stage is kept: the metrics listener writes the same data to
     * every stage timer of that rule, so counting each stage separately would double-count it.
     */
    List<ActiveCombination> activeCombinations(PipelineInterpreter.State state) {
        if (state == null) {
            return List.of();
        }
        final Map<RulePipelineKey, ActiveCombination> result = new LinkedHashMap<>();
        for (Pipeline pipeline : state.getCurrentPipelines().values()) {
            final String pipelineId = pipeline.id();
            if (pipelineId == null) {
                continue;
            }
            for (Stage stage : pipeline.stages()) {
                for (Rule rule : stage.getRules()) {
                    final String ruleId = rule.id();
                    if (ruleId == null) {
                        continue;
                    }
                    result.putIfAbsent(
                            new RulePipelineKey(ruleId, pipelineId),
                            ActiveCombination.of(ruleId, pipelineId, stage.stage())
                    );
                }
            }
        }
        return List.copyOf(result.values());
    }

    List<String> expectedTimerNames(List<ActiveCombination> combinations) {
        return combinations.stream()
                .flatMap(c -> Stream.of(c.evaluateTimerName(), c.executeTimerName()))
                .toList();
    }

    ProcessingLoadResponse compute(List<ActiveCombination> combinations,
                                   Map<String, NodeTimerSnapshot> perNodeTimerSnapshots) {
        if (combinations.isEmpty() || perNodeTimerSnapshots.values().stream().allMatch(NodeTimerSnapshot::isEmpty)) {
            return ProcessingLoadResponse.unavailable();
        }

        final List<CombinationCost> costs = new ArrayList<>(combinations.size());
        final Map<String, Double> perPipeline = new LinkedHashMap<>();
        final Map<String, Double> perRule = new LinkedHashMap<>();
        double total = 0.0d;

        for (ActiveCombination combination : combinations) {
            double clusterCost = 0.0d;
            for (NodeTimerSnapshot node : perNodeTimerSnapshots.values()) {
                clusterCost += node.cost(combination.evaluateTimerName()) + node.cost(combination.executeTimerName());
            }
            costs.add(new CombinationCost(combination, clusterCost));
            perPipeline.merge(combination.pipelineId(), clusterCost, Double::sum);
            perRule.merge(combination.ruleId(), clusterCost, Double::sum);
            total += clusterCost;
        }

        if (total <= 0.0d) {
            return ProcessingLoadResponse.unavailable();
        }
        final double clusterTotal = total;

        final List<StageRuleLoad> stageRules = costs.stream()
                .map(c -> StageRuleLoad.create(
                        c.combination.ruleId(),
                        c.combination.pipelineId(),
                        c.combination.stage(),
                        percentage(c.cost, clusterTotal),
                        percentage(c.cost, perPipeline.get(c.combination.pipelineId()))
                ))
                .sorted(Comparator.comparingDouble(StageRuleLoad::loadPercent).reversed())
                .toList();

        return ProcessingLoadResponse.create(
                true,
                clusterTotal,
                stageRules,
                rollup(perPipeline, clusterTotal, PipelineLoad::create, Comparator.comparingDouble(PipelineLoad::loadPercent).reversed()),
                rollup(perRule, clusterTotal, RuleLoad::create, Comparator.comparingDouble(RuleLoad::loadPercent).reversed())
        );
    }

    /**
     * Keeps only the entries the caller can see. The denominator stays cluster-wide so the
     * percentages on visible rows reflect true share of cluster CPU.
     */
    ProcessingLoadResponse filterByPermissions(ProcessingLoadResponse full,
                                               Predicate<String> canReadPipeline,
                                               Predicate<String> canReadRule) {
        if (!full.available()) {
            return full;
        }
        final List<StageRuleLoad> stageRules = full.stageRules().stream()
                .filter(sr -> canReadPipeline.test(sr.pipelineId()) && canReadRule.test(sr.ruleId()))
                .toList();
        final List<PipelineLoad> pipelines = full.pipelines().stream()
                .filter(p -> canReadPipeline.test(p.pipelineId()))
                .toList();
        final List<RuleLoad> rules = full.rules().stream()
                .filter(r -> canReadRule.test(r.ruleId()))
                .toList();
        return ProcessingLoadResponse.create(true, full.totalCostMicrosecondsPerSecond(), stageRules, pipelines, rules);
    }

    private static <T> List<T> rollup(Map<String, Double> aggregated, double total,
                                      BiFunction<String, Double, T> factory, Comparator<T> order) {
        return aggregated.entrySet().stream()
                .map(e -> factory.apply(e.getKey(), percentage(e.getValue(), total)))
                .sorted(order)
                .toList();
    }

    private static double percentage(double cost, double total) {
        return Math.round(100.0d * cost / total * 100.0d) / 100.0d;
    }

    record ActiveCombination(String ruleId, String pipelineId, int stage, String evaluateTimerName,
                             String executeTimerName) {
        static ActiveCombination of(String ruleId, String pipelineId, int stage) {
            final String base = name(ruleId, pipelineId, String.valueOf(stage));
            return new ActiveCombination(
                    ruleId,
                    pipelineId,
                    stage,
                    RuleMetricsListener.getMetricName(base, EVALUATE),
                    RuleMetricsListener.getMetricName(base, EXECUTE)
            );
        }
    }

    private record CombinationCost(ActiveCombination combination, double cost) {
    }

    private record RulePipelineKey(String ruleId, String pipelineId) {
    }
}
