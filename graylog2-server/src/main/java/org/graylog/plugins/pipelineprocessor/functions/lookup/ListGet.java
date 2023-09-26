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
package org.graylog.plugins.pipelineprocessor.functions.lookup;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class ListGet extends AbstractFunction<Object> {
    public static final String NAME = "list_get";
    private static final String LISTARG = "list";
    private static final String IDXARG = "index";
    private final ParameterDescriptor<List, List> listParam;
    private final ParameterDescriptor<Long, Long> indexParam;

    public ListGet() {
        listParam = type(LISTARG, List.class).description("A list").build();
        indexParam = integer(IDXARG).ruleBuilderVariable().description("Get the value for this index in list").build();
    }

    @Override
    public Object evaluate(FunctionArgs args, EvaluationContext context) {

        final List listValue = listParam.required(args, context);
        final Long indexValue = indexParam.required(args, context);
        if (listValue == null || indexValue == null || indexValue >= listValue.size()) {
            return null;
        }

        return listValue.get(indexValue.intValue());
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        return FunctionDescriptor.builder()
                .name(NAME)
                .returnType(Object.class)
                .params(of(listParam, indexParam))
                .description("Get a value from a list")
                .ruleBuilderEnabled()
                .ruleBuilderName("Get value from list")
                .ruleBuilderTitle("Get value at index '${index}' from list")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                .build();
    }
}
