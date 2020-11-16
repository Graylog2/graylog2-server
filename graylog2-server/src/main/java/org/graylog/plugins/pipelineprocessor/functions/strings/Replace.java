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

public class Replace extends AbstractFunction<String> {
    public static final String NAME = "replace";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> searchParam;
    private final ParameterDescriptor<String, String> replacementParam;
    private final ParameterDescriptor<Long, Integer> maxParam;

    public Replace() {
        valueParam = ParameterDescriptor.string("value").description("The text to search and replace in").build();
        searchParam = ParameterDescriptor.string("search").description("The string to search for").build();
        replacementParam = ParameterDescriptor.string("replacement").optional()
                .description("The string to replace it with. Default: \"\"").build();
        maxParam = ParameterDescriptor.integer("max", Integer.class).optional()
                .transform(Ints::saturatedCast)
                .description("Maximum number of occurrences to replace, or -1 if no maximum. Default: -1").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String text = valueParam.required(args, context);
        final String searchString = searchParam.required(args, context);
        final String replacement = replacementParam.optional(args, context).orElse("");
        final int max = maxParam.optional(args, context).orElse(-1);

        return StringUtils.replace(text, searchString, replacement, max);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(valueParam, searchParam, replacementParam, maxParam))
                .description("Replaces the first \"max\" or all occurrences of a string within another string")
                .build();
    }
}
