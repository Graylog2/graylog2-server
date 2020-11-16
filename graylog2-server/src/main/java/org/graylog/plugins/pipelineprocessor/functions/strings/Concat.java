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

import com.google.common.base.Strings;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Concat extends AbstractFunction<String> {
    public static final String NAME = "concat";
    private final ParameterDescriptor<String, String> firstParam;
    private final ParameterDescriptor<String, String> secondParam;

    public Concat() {
        firstParam = ParameterDescriptor.string("first").description("First string").build();
        secondParam = ParameterDescriptor.string("second").description("Second string").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String first = Strings.nullToEmpty(firstParam.required(args, context));
        final String second = Strings.nullToEmpty(secondParam.required(args, context));

        return first.concat(second);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(firstParam, secondParam))
                .description("Concatenates two strings")
                .build();
    }
}
