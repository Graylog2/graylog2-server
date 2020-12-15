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

public class ParseUnixMilliseconds extends TimezoneAwareFunction {
    public static final String NAME = "parse_unix_milliseconds";

    private static final String VALUE = "value";

    private final ParameterDescriptor<Long, Long> valueParam;

    public ParseUnixMilliseconds() {
        valueParam = ParameterDescriptor.integer(VALUE).description("UNIX millisecond timestamp to parse").build();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(valueParam);
    }

    @Override
    public DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final Long unixMillis = valueParam.required(args, context);
        return unixMillis == null ? null : new DateTime(unixMillis, timezone);
    }

    @Override
    protected String description() {
        return "Converts a UNIX millisecond timestamp into a date";
    }
}
