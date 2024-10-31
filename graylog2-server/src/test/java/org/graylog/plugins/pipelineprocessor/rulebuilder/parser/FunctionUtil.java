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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;

public class FunctionUtil {

    public static Function<Boolean> testFunction(String name,
                                                 ImmutableList<ParameterDescriptor> params,
                                                 Class returnType) {
        return new AbstractFunction<>() {
            @Override
            public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
                return false; // not needed for test
            }

            @Override
            public FunctionDescriptor descriptor() {
                return FunctionDescriptor.builder()
                        .name(name)
                        .params(params)
                        .ruleBuilderEnabled()
                        .returnType(returnType)
                        .build();
            }
        };
    }

    public static RuleFragment testCondition(String name,
                                             String fragment,
                                             ImmutableList<ParameterDescriptor> params) {
        return RuleFragment.builder()
                .isCondition()
                .fragment(fragment)
                .descriptor(FunctionDescriptor.builder()
                        .name(name)
                        .params(params)
                        .returnType(Boolean.class)
                        .build())
                .build();
    }

}
