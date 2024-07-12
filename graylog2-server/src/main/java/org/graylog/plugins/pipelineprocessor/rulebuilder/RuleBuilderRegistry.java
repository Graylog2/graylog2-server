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
package org.graylog.plugins.pipelineprocessor.rulebuilder;

import com.google.common.collect.Streams;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleBuilderRegistry {

    private final FunctionRegistry functionRegistry;
    private final RuleFragmentService ruleFragmentService;

    @Inject
    public RuleBuilderRegistry(FunctionRegistry functionRegistry,
                               RuleFragmentService ruleFragmentService) {
        this.functionRegistry = functionRegistry;
        this.ruleFragmentService = ruleFragmentService;
    }

    /**
     * Returns a map of function conditions that are available for users to use in the rule builder.
     *
     * @return map of condition functions
     */
    public Map<String, RuleFragment> conditions() {
        return collectConditions(functionRegistry.all());
    }

    /**
     * Returns a map of function conditions that includes internal functions. These conditions must not be exposed
     * to users!
     *
     * @return map of condition functions
     */
    public Map<String, RuleFragment> conditionsWithInternal() {
        return collectConditions(functionRegistry.allWithInternal());
    }

    private Map<String, RuleFragment> collectConditions(Collection<Function<?>> functions) {
        final Stream<RuleFragment> functionConditions = functions
                .stream()
                .filter(f -> f.descriptor().ruleBuilderEnabled()
                        && f.descriptor().returnType().equals(Boolean.class))
                .map(f -> RuleFragment.builder()
                        .descriptor(f.descriptor())
                        .build()
                );
        final Stream<RuleFragment> fragmentConditions = ruleFragmentService.all().stream()
                .filter(f -> f.descriptor().ruleBuilderEnabled() && f.isCondition());
        return Streams.concat(functionConditions, fragmentConditions)
                .collect(Collectors.toMap(f -> f.descriptor().name(), function -> function));
    }

    /**
     * Returns a map of function actions that are available for users to use in the rule builder.
     *
     * @return map of action functions
     */
    public Map<String, RuleFragment> actions() {
        return collectActions(functionRegistry.all());
    }

    /**
     * Returns a map of function actions that includes internal functions. These actions must not be exposed
     * to users!
     *
     * @return map of action functions
     */
    public Map<String, RuleFragment> actionsWithInternal() {
        return collectActions(functionRegistry.allWithInternal());
    }

    public Map<String, RuleFragment> collectActions(Collection<Function<?>> functions) {
        final Stream<RuleFragment> functionActions = functions
                .stream()
                .filter(f -> f.descriptor().ruleBuilderEnabled()
                        && !f.descriptor().returnType().equals(Boolean.class))
                .map(f -> RuleFragment.builder()
                        .descriptor(f.descriptor())
                        .build()
                );
        final Stream<RuleFragment> fragmentActions =
                ruleFragmentService.all().stream()
                        .filter(f -> f.descriptor().ruleBuilderEnabled() && !f.isCondition());
        return Streams.concat(functionActions, fragmentActions)
                .collect(Collectors.toMap(f -> f.descriptor().name(), function -> function));
    }

}
