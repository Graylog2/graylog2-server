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
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Optional;

public class FlexParseDate extends TimezoneAwareFunction {

    public static final String VALUE = "value";
    public static final String NAME = "flex_parse_date";
    public static final String DEFAULT = "default";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<DateTime, DateTime> defaultParam;

    public FlexParseDate() {
        valueParam = ParameterDescriptor.string(VALUE).description("Date string to parse").build();
        defaultParam = ParameterDescriptor.type(DEFAULT, DateTime.class).optional().description("Used when 'value' could not be parsed, 'null' otherwise").build();
    }

    @Override
    protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final String time = valueParam.required(args, context);

        final List<DateGroup> dates = new Parser(timezone.toTimeZone()).parse(time);
        if (dates.size() == 0) {
            final Optional<DateTime> defaultTime = defaultParam.optional(args, context);
            if (defaultTime.isPresent()) {
                return defaultTime.get();
            }
            // TODO really? this should probably throw an exception of some sort to be handled in the interpreter
            return null;
        }
        return new DateTime(dates.get(0).getDates().get(0), timezone);
    }

    @Override
    protected String description() {
        return "Parses a date string using natural language (see http://natty.joestelmach.com/)";
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(
                valueParam,
                defaultParam
        );
    }
}
