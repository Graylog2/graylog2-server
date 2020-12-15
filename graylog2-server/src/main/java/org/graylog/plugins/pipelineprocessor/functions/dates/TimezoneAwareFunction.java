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
package org.graylog.plugins.pipelineprocessor.functions.dates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;

public abstract class TimezoneAwareFunction extends AbstractFunction<DateTime> {

    private static final String TIMEZONE = "timezone";
    private static final ImmutableMap<String, String> UPPER_ZONE_MAP = Maps.uniqueIndex(
            DateTimeZone.getAvailableIDs(),
            input -> input != null ? input.toUpperCase(Locale.ENGLISH) : "UTC");
    private final ParameterDescriptor<String, DateTimeZone> timeZoneParam;

    protected TimezoneAwareFunction() {
        timeZoneParam = ParameterDescriptor
                .string(TIMEZONE, DateTimeZone.class)
                .transform(id -> DateTimeZone.forID(UPPER_ZONE_MAP.getOrDefault(id.toUpperCase(Locale.ENGLISH), "UTC")))
                .optional()
                .description("The timezone to apply to the date, defaults to UTC")
                .build();
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context) {
        final DateTimeZone timezone = timeZoneParam.optional(args, context).orElse(DateTimeZone.UTC);

        return evaluate(args, context, timezone);
    }

    protected abstract DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone);

    @Override
    public FunctionDescriptor<DateTime> descriptor() {
        return FunctionDescriptor.<DateTime>builder()
                .name(getName())
                .returnType(DateTime.class)
                .params(ImmutableList.<ParameterDescriptor>builder()
                                .addAll(params())
                                .add(timeZoneParam)
                                .build())
                .description(description())
                .build();
    }

    protected abstract String description();

    protected abstract String getName();

    protected abstract ImmutableList<ParameterDescriptor> params();
}
