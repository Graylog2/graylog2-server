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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.google.common.collect.ImmutableList.of;

public class FormatDate extends AbstractFunction<String> {

    public static final String NAME = "format_date";

    private final ParameterDescriptor<DateTime, DateTime> value;
    private final ParameterDescriptor<String, DateTimeFormatter> format;
    private final ParameterDescriptor<String, DateTimeZone> timeZoneParam;

    public FormatDate() {
        value = ParameterDescriptor.type("value", DateTime.class).description("The date to format").build();
        format = ParameterDescriptor.string("format", DateTimeFormatter.class)
                .transform(DateTimeFormat::forPattern)
                .description("The format string to use, see http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html")
                .build();
        timeZoneParam = ParameterDescriptor.string("timezone", DateTimeZone.class)
                .transform(DateTimeZone::forID)
                .optional()
                .description("The timezone to apply to the date, defaults to UTC")
                .build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final DateTime dateTime = value.required(args, context);
        final DateTimeFormatter formatter = format.required(args, context);
        if (dateTime == null || formatter == null) {
            return null;
        }
        final DateTimeZone timeZone = timeZoneParam.optional(args, context).orElse(DateTimeZone.UTC);

        return formatter.withZone(timeZone).print(dateTime);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(value, format, timeZoneParam))
                .description("Formats a date using the given format string")
                .build();
    }

}
