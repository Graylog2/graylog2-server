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

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class StartsWith extends AbstractFunction<Boolean> {

    public static final String NAME = "starts_with";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> prefixParam;
    private final ParameterDescriptor<Boolean, Boolean> ignoreCaseParam;

    public StartsWith() {
        valueParam = ParameterDescriptor.string("value").description("The string to check").build();
        prefixParam = ParameterDescriptor.string("prefix").description("The prefix to check").build();
        ignoreCaseParam = ParameterDescriptor.bool("ignore_case").optional().description("Whether to search case insensitive, defaults to false").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String prefix = prefixParam.required(args, context);
        final boolean ignoreCase = ignoreCaseParam.optional(args, context).orElse(false);
        if (ignoreCase) {
            return StringUtils.startsWithIgnoreCase(value, prefix);
        } else {
            return StringUtils.startsWith(value, prefix);
        }
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        valueParam,
                        prefixParam,
                        ignoreCaseParam
                ))
                .description("Checks if a string starts with a prefix")
                .build();
    }
}
