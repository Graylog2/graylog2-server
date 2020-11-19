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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.primitives.Longs.tryParse;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class LongConversion extends AbstractFunction<Long> {

    public static final String NAME = "to_long";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Long, Long> defaultParam;

    public LongConversion() {
        valueParam = object(VALUE).description("Value to convert").build();
        defaultParam = integer(DEFAULT).optional().description("Used when 'value' is null, defaults to 0").build();
    }

    @Override
    public Long evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = valueParam.required(args, context);
        final Long defaultValue = defaultParam.optional(args, context).orElse(0L);

        if (evaluated == null) {
            return defaultValue;
        } else if (evaluated instanceof Number) {
            return ((Number) evaluated).longValue();
        } else {
            final String s = String.valueOf(evaluated);
            return firstNonNull(tryParse(s), defaultValue);
        }
    }

    @Override
    public FunctionDescriptor<Long> descriptor() {
        return FunctionDescriptor.<Long>builder()
                .name(NAME)
                .returnType(Long.class)
                .params(of(
                        valueParam,
                        defaultParam
                ))
                .description("Converts a value to a long value using its string representation")
                .build();
    }
}
