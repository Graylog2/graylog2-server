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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.reflect.TypeToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Split extends AbstractFunction<List<String>> {
    @SuppressWarnings("unchecked")
    private static final Class<List<String>> RETURN_TYPE = (Class<List<String>>) new TypeToken<List<String>>() {
    }.getRawType();

    public static final String NAME = "split";

    private final ParameterDescriptor<String, Pattern> pattern;
    private final ParameterDescriptor<String, String> value;
    private final ParameterDescriptor<Long, Integer> limit;

    public Split() {
        pattern = ParameterDescriptor.string("pattern", Pattern.class)
                .transform(Pattern::compile)
                .description("The regular expression to split by, uses Java regex syntax")
                .build();
        value = ParameterDescriptor.string("value")
                .description("The string to be split")
                .build();
        limit = ParameterDescriptor.integer("limit", Integer.class)
                .transform(Ints::saturatedCast)
                .description("The number of times the pattern is applied")
                .optional()
                .build();
    }

    @Override
    public List<String> evaluate(FunctionArgs args, EvaluationContext context) {
        final Pattern regex = requireNonNull(pattern.required(args, context), "Argument 'pattern' cannot be 'null'");
        final String value = requireNonNull(this.value.required(args, context), "Argument 'value' cannot be 'null'");

        final int limit = this.limit.optional(args, context).orElse(0);
        checkArgument(limit >= 0, "Argument 'limit' cannot be negative");
        return ImmutableList.copyOf(regex.split(value, limit));
    }

    @Override
    public FunctionDescriptor<List<String>> descriptor() {
        return FunctionDescriptor.<List<String>>builder()
                .name(NAME)
                .pure(true)
                .returnType(RETURN_TYPE)
                .params(ImmutableList.of(pattern, value, limit))
                .description("Split a string around matches of this pattern (Java syntax)")
                .build();
    }
}
