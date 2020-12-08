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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;
import java.util.Optional;

public class ParseDate extends TimezoneAwareFunction {
    public static final String NAME = "parse_date";

    private static final String VALUE = "value";
    private static final String PATTERN = "pattern";
    private static final String LOCALE = "locale";

    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> patternParam;
    private final ParameterDescriptor<String, String> localeParam;

    public ParseDate() {
        valueParam = ParameterDescriptor.string(VALUE).description("Date string to parse").build();
        patternParam = ParameterDescriptor.string(PATTERN).description("The pattern to parse the date with, see http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html").build();
        localeParam = ParameterDescriptor.string(LOCALE).optional().description("The locale to parse the date with, see https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html").build();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(
                valueParam,
                patternParam,
                localeParam
        );
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final String dateString = valueParam.required(args, context);
        final String pattern = patternParam.required(args, context);
        final Optional<String> localeString = localeParam.optional(args, context);

        if (dateString == null || pattern == null) {
            return null;
        }

        final Locale locale = localeString.map(Locale::forLanguageTag).orElse(Locale.getDefault());

        final DateTimeFormatter formatter = DateTimeFormat
                .forPattern(pattern)
                .withLocale(locale)
                .withZone(timezone);

        return formatter.parseDateTime(dateString);
    }

    @Override
    protected String description() {
        return "Parses a date string using the given date format";
    }
}
