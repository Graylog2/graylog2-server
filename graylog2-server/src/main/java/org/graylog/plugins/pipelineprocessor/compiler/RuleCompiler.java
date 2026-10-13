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
package org.graylog.plugins.pipelineprocessor.compiler;

import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class RuleCompiler {
    private final MetricRegistry metricRegistry;

    @Inject
    public RuleCompiler(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    /** Compiles every distinct rule of the given pipelines and attaches the result to each per-stage rule copy. */
    public void compileAll(Collection<Pipeline> pipelines) {
        // per-stage rule copies share their AST, so compile each AST once
        final Map<LogicalExpression, Optional<CompiledRule>> byCondition = new IdentityHashMap<>();
        for (Pipeline pipeline : pipelines) {
            for (Stage stage : pipeline.stages()) {
                final List<Rule> rules = stage.getRules();
                if (rules == null) {
                    continue;
                }
                for (Rule rule : rules) {
                    rule.setCompiled(byCondition.computeIfAbsent(rule.when(), condition -> compile(rule)).orElse(null));
                }
            }
        }
    }

    public Optional<CompiledRule> compile(Rule rule) {
        return Optional.empty();
    }
}
