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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Substring extends AbstractFunction<String> {

    public static final String NAME = "substring";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<Long, Long> startParam;
    private final ParameterDescriptor<Long, Long> endParam;

    public Substring() {
        valueParam = ParameterDescriptor.string("value").description("The string to extract from").build();
        startParam = ParameterDescriptor.integer("start").description("The position to start from, negative means count back from the end of the String by this many characters").build();
        endParam = ParameterDescriptor.integer("end").optional().description("The position to end at (exclusive), negative means count back from the end of the String by this many characters, defaults to length of the input string").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final Long startValue = startParam.required(args, context);
        if (value == null || startValue == null) {
            return null;
        }
        final int start = Ints.saturatedCast(startValue);
        final int end = Ints.saturatedCast(endParam.optional(args, context).orElse((long) value.length()));

        return StringUtils.substring(value, start, end);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(
                        valueParam,
                        startParam,
                        endParam
                ))
                .description("Extract a substring from a string")
                .build();
    }
}
