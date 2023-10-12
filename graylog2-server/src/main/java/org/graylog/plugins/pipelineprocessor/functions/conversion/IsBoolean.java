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

import org.apache.commons.lang.BooleanUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Objects;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class IsBoolean extends AbstractFunction<Boolean> {
    public static final String NAME = "is_bool";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> conversionParam;

    public IsBoolean() {
        valueParam = object("value").ruleBuilderVariable().description("Value to check").build();
        conversionParam = bool("attemptConversion").optional().description("Try to convert value to boolean from its string representation (default: false)").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);
        final boolean convert = conversionParam.optional(args, context).orElse(false);
        if (!convert) {
            return value instanceof Boolean;
        }
        return Objects.nonNull(BooleanUtils.toBooleanObject(String.valueOf(value)));
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(valueParam, conversionParam)
                .description("Checks whether a value is a boolean value (true or false)")
                .ruleBuilderEnabled(false)
                .ruleBuilderName("Check if boolean")
                .ruleBuilderTitle("Check if '${value}' is a boolean")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                .build();
    }
}
