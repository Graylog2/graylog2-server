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
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class ListConversion extends AbstractConversion<List> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String NAME = "to_list";
    private static final String VALUE = "value";

    private final ParameterDescriptor<Object, Object> valueParam;


    public ListConversion() {
        this.valueParam = object(VALUE).ruleBuilderVariable().description("List-like value to convert").build();
    }

    @Override
    public List evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);

        return switch (value) {
            case null -> Boolean.TRUE.equals(defaultToNull(args, context)) ? null : Collections.emptyList();
            case List list -> list;
            case JsonNode jsonNode -> MAPPER.convertValue(jsonNode, List.class);
            default -> Collections.emptyList();
        };
    }

    @Override
    public FunctionDescriptor<List> descriptor() {
        return FunctionDescriptor.<List>builder()
                .name(NAME)
                .returnType(List.class)
                .params(of(valueParam, defaultToNullParam))
                .description("Converts a list-like value into a list usable by e.g. list_get()")
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert to list")
                .ruleBuilderTitle("Convert '${value}' to list")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }
}
