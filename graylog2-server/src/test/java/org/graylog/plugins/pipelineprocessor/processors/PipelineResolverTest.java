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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSortedSet;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class PipelineResolverTest {

    private MetricRegistry metricRegistry;
    private RuleDao rule1;
    private PipelineDao pipeline1;
    private PipelineConnections connections1;
    private PipelineConnections connections2;

    @BeforeEach
    void setUp() {
        this.rule1 = RuleDao.builder()
                .id("rule-1")
                .title("test-rule-1")
                .description("A test rule")
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .source("""
                        rule "test-rule-1"
                        when true
                        then
                        end
                        """)
                .build();

        this.pipeline1 = PipelineDao.builder()
                .id("pipeline-1")
                .title("test-pipeline-1")
                .description("A test pipeline")
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .source("""
                        pipeline "test-pipeline-1"
                          stage 5 match either
                            rule "test-rule-1"
                        end
                        """)
                .build();

        this.connections1 = PipelineConnections.builder()
                .id("connections-2")
                .streamId("stream-1")
                .pipelineIds(Set.of(requireNonNull(pipeline1.id())))
                .build();
        this.connections2 = PipelineConnections.builder()
                .id("connections-2")
                .streamId("stream-2")
                .pipelineIds(Set.of(requireNonNull(pipeline1.id())))
                .build();

        this.metricRegistry = new MetricRegistry();
    }

    @Test
    void accessConfig() {
        final PipelineResolverConfig config = PipelineResolverConfig.of(
                () -> Stream.of(rule1),
                () -> Stream.of(pipeline1),
                () -> Stream.of(connections1, connections2)
        );
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                config
        );

        assertThat(resolver.config()).isEqualTo(config);
    }

    @Test
    void resolveFunctions() {
        final var registry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                PipelineResolverConfig.of(() -> Stream.of(rule1), Stream::of, Stream::of)
        );

        final var pipelineObj = Pipeline.builder()
                .id("pipeline-999")
                .name("test-pipeline-999")
                .stages(ImmutableSortedSet.of(
                        Stage.builder()
                                .stage(0)
                                .match(Stage.Match.EITHER)
                                .ruleReferences(List.of(rule1.title()))
                                .build()
                ))
                .build();

        final var pipelines = resolver.resolveFunctions(Set.of(pipelineObj), registry);

        assertThat(pipelines).hasSize(1);
        assertThat(pipelines.get("pipeline-999")).satisfies(pipeline -> {
            assertThat(pipeline.id()).isEqualTo("pipeline-999");
            assertThat(pipeline.name()).isEqualTo("test-pipeline-999");
            assertThat(pipeline.stages()).hasSize(1);
            assertThat(pipeline.stages().first()).satisfies(stage -> {
                assertThat(stage.stage()).isEqualTo(0);
                assertThat(stage.match()).isEqualTo(Stage.Match.EITHER);
                assertThat(stage.ruleReferences()).isEqualTo(List.of("test-rule-1"));
                assertThat(stage.getRules()).hasSize(1);
                assertThat(stage.getRules().get(0)).satisfies(rule -> {
                    assertThat(rule.id()).isEqualTo("rule-1");
                    assertThat(rule.name()).isEqualTo("test-rule-1");
                });
            });
        });

        assertThat(metricRegistry.getMetrics().keySet()).containsExactlyInAnyOrder(
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-999.0.not-matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-999.0.matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-999.0.failed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-999.0.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.not-matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.failed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Pipeline.pipeline-999.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Pipeline.pipeline-999.stage.0.executed"
        );
    }

    @Test
    void resolvePipelines() {
        final var registry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                PipelineResolverConfig.of(
                        () -> Stream.of(rule1),
                        () -> Stream.of(pipeline1),
                        () -> Stream.of(connections1, connections2)
                )
        );

        final var pipelines = resolver.resolvePipelines(registry);

        assertThat(pipelines).hasSize(1);
        assertThat(pipelines.get("pipeline-1")).satisfies(pipeline -> {
            assertThat(pipeline.id()).isEqualTo("pipeline-1");
            assertThat(pipeline.name()).isEqualTo("test-pipeline-1");
            assertThat(pipeline.stages()).hasSize(1);
            assertThat(pipeline.stages().first()).satisfies(stage -> {
                assertThat(stage.stage()).isEqualTo(5);
                assertThat(stage.match()).isEqualTo(Stage.Match.EITHER);
                assertThat(stage.ruleReferences()).isEqualTo(List.of("test-rule-1"));
                assertThat(stage.getRules()).hasSize(1);
                assertThat(stage.getRules().get(0)).satisfies(rule -> {
                    assertThat(rule.id()).isEqualTo("rule-1");
                    assertThat(rule.name()).isEqualTo("test-rule-1");
                });
            });
        });

        assertThat(metricRegistry.getMetrics().keySet()).containsExactlyInAnyOrder(
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.5.not-matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.5.matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.5.failed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.pipeline-1.5.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.not-matched",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.failed",
                "org.graylog.plugins.pipelineprocessor.ast.Rule.rule-1.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Pipeline.pipeline-1.executed",
                "org.graylog.plugins.pipelineprocessor.ast.Pipeline.pipeline-1.stage.5.executed"
        );
    }

    @Test
    void resolvePipelinesWithMetricPrefix() {
        final var registry = PipelineMetricRegistry.create(metricRegistry, "PIPELINE", "RULE");
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                PipelineResolverConfig.of(
                        () -> Stream.of(rule1),
                        () -> Stream.of(pipeline1),
                        () -> Stream.of(connections1, connections2)
                )
        );

        resolver.resolvePipelines(registry);

        assertThat(metricRegistry.getMetrics().keySet()).containsExactlyInAnyOrder(
                "RULE.rule-1.pipeline-1.5.not-matched",
                "RULE.rule-1.pipeline-1.5.matched",
                "RULE.rule-1.pipeline-1.5.failed",
                "RULE.rule-1.pipeline-1.5.executed",
                "RULE.rule-1.matched",
                "RULE.rule-1.not-matched",
                "RULE.rule-1.failed",
                "RULE.rule-1.executed",
                "PIPELINE.pipeline-1.executed",
                "PIPELINE.pipeline-1.stage.5.executed"
        );
    }

    @Test
    void resolvePipelinesWithMissingRule() {
        final var registry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                PipelineResolverConfig.of(Stream::of, () -> Stream.of(pipeline1))
        );

        final var pipelines = resolver.resolvePipelines(registry);

        assertThat(pipelines).hasSize(1);
        assertThat(pipelines.get("pipeline-1")).satisfies(pipeline -> {
            assertThat(pipeline.id()).isEqualTo("pipeline-1");
            assertThat(pipeline.name()).isEqualTo("test-pipeline-1");
            assertThat(pipeline.stages()).hasSize(1);
            assertThat(pipeline.stages().first()).satisfies(stage -> {
                assertThat(stage.stage()).isEqualTo(5);
                assertThat(stage.match()).isEqualTo(Stage.Match.EITHER);
                assertThat(stage.ruleReferences()).isEqualTo(List.of("test-rule-1"));
                assertThat(stage.getRules()).hasSize(1);
                assertThat(stage.getRules().get(0)).satisfies(rule -> {
                    assertThat(rule.id()).isNull();
                    assertThat(rule.name())
                            .withFailMessage("Unresolved rules should have a static title")
                            .isEqualTo("Unresolved rule test-rule-1");
                    assertThat(rule.when().evaluateBool(EvaluationContext.emptyContext()))
                            .withFailMessage("Unresolved rules should evaluate to false")
                            .isEqualTo(false);
                });
            });
        });
    }

    @Test
    void resolveStreamConnections() {
        final var registry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        final var resolver = new PipelineResolver(
                new PipelineRuleParser(new FunctionRegistry(Map.of())),
                PipelineResolverConfig.of(
                        () -> Stream.of(rule1),
                        () -> Stream.of(pipeline1),
                        () -> Stream.of(connections1, connections2)
                )
        );

        final var pipelines = resolver.resolvePipelines(registry);
        final var streamConnections = resolver.resolveStreamConnections(pipelines);

        assertThat(streamConnections.size()).isEqualTo(2);
        assertThat(streamConnections.get("stream-1").stream().toList()).satisfies(connections -> {
            assertThat(connections).hasSize(1);
            assertThat(connections.get(0).id()).isEqualTo("pipeline-1");
        });
        assertThat(streamConnections.get("stream-2").stream().toList()).satisfies(connections -> {
            assertThat(connections).hasSize(1);
            assertThat(connections.get(0).id()).isEqualTo("pipeline-1");
        });
    }
}
