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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Locale;

public abstract class StringUtilsFunction extends AbstractFunction<String> {

    private static final String VALUE = "value";
    private static final String LOCALE = "locale";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, Locale> localeParam;

    public StringUtilsFunction() {
        valueParam = ParameterDescriptor.string(VALUE).description("The input string").build();
        localeParam = ParameterDescriptor.string(LOCALE, Locale.class)
                .optional()
                .transform(Locale::forLanguageTag)
                .description("The locale to use, defaults to English")
                .build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        Locale locale = Locale.ENGLISH;
        if (isLocaleAware()) {
            locale = localeParam.optional(args, context).orElse(Locale.ENGLISH);
        }
        return apply(value, locale);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        ImmutableList.Builder<ParameterDescriptor> params = ImmutableList.builder();
        params.add(valueParam);
        if (isLocaleAware()) {
            params.add(localeParam);
        }
        return FunctionDescriptor.<String>builder()
                .name(getName())
                .returnType(String.class)
                .params(params.build())
                .description(description())
                .build();
    }

    protected abstract String getName();

    protected abstract String description();

    protected abstract boolean isLocaleAware();

    protected abstract String apply(String value, Locale locale);
}
