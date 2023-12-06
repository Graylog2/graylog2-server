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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class StringEntropy extends AbstractFunction<Double> {
    public static final String NAME = "string_entropy";
    private final ParameterDescriptor<String, String> valueParam;

    public StringEntropy() {
        valueParam = ParameterDescriptor.string("value").description("The string to compute Shannon Entropy for.").build();
    }

    @Override
    public Double evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        return ShannonEntropy.calculateForChars(value);
    }

    @Override
    public FunctionDescriptor<Double> descriptor() {
        return FunctionDescriptor.<Double>builder()
                .name(NAME)
                .returnType(Double.class)
                .params(of(valueParam))
                .description("Compute Shannon Entropy for the characters within a string.")
                .build();
    }
}
