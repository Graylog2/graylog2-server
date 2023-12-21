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
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragmentService;

import jakarta.inject.Inject;

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

    public Map<String, RuleFragment> conditions() {
        final Stream<RuleFragment> functionConditions = functionRegistry.all()
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

    public Map<String, RuleFragment> actions() {
        final Stream<RuleFragment> functions = functionRegistry.all()
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
        return Streams.concat(functions, fragmentActions)
                .collect(Collectors.toMap(f -> f.descriptor().name(), function -> function));
    }

}
