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
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.primitives.Ints.saturatedCast;

public class Abbreviate extends AbstractFunction<String> {

    public static final String NAME = "abbreviate";
    private static final String VALUE = "value";
    private static final String WIDTH = "width";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<Long, Long> widthParam;

    public Abbreviate() {
        valueParam = ParameterDescriptor.string(VALUE).description("The string to abbreviate").build();
        widthParam = ParameterDescriptor.integer(WIDTH).description("The maximum number of characters including the '...' (at least 4)").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final Long required = widthParam.required(args, context);
        if (required == null) {
            return null;
        }
        final Long maxWidth = Math.max(required, 4L);

        return StringUtils.abbreviate(value, saturatedCast(maxWidth));
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        ImmutableList.Builder<ParameterDescriptor> params = ImmutableList.builder();
        params.add();

        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(ImmutableList.of(
                        valueParam,
                        widthParam
                ))
                .description("Abbreviates a string by appending '...' to fit into a maximum amount of characters")
                .build();
    }
}
