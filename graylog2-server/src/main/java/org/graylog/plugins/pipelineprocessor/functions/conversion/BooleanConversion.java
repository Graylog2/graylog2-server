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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class BooleanConversion extends AbstractFunction<Boolean> {
    public static final String NAME = "to_bool";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> defaultParam;


    public BooleanConversion() {
        valueParam = object("value").description("Value to convert").build();
        defaultParam = bool("default").optional().description("Used when 'value' is null, defaults to false").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);
        if (value == null) {
            return defaultParam.optional(args, context).orElse(false);
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(valueParam, defaultParam))
                .description("Converts a value to a boolean value using its string representation")
                .build();
    }
}
