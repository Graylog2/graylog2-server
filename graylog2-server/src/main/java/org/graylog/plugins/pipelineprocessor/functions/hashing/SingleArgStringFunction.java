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
package org.graylog.plugins.pipelineprocessor.functions.hashing;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Locale;

import static com.google.common.collect.ImmutableList.of;

abstract class SingleArgStringFunction extends AbstractFunction<String> {

    private final ParameterDescriptor<String, String> valueParam;

    SingleArgStringFunction() {
        valueParam = ParameterDescriptor.string("value").description("The value to hash").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        return getDigest(value);
    }

    protected abstract String getDigest(String value);

    protected abstract String getName();

    protected String description() {
        return getName().toUpperCase(Locale.ENGLISH) + " hash of the string";
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(getName())
                .returnType(String.class)
                .params(of(
                        valueParam)
                )
                .description(description())
                .build();
    }
}
