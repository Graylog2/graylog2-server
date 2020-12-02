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

import java.time.ZonedDateTime;
import java.util.Date;

public class DateConversion extends TimezoneAwareFunction {

    public static final String NAME = "to_date";
    private final ParameterDescriptor<Object, Object> value;

    public DateConversion() {
        value = ParameterDescriptor.object("value").description("The value to convert to a date").build();
    }

    @Override
    protected DateTime evaluate(FunctionArgs args, EvaluationContext context, DateTimeZone timezone) {
        final Object datish = value.required(args, context);
        if (datish instanceof DateTime) {
            return (DateTime) datish;
        }
        if (datish instanceof Date) {
            return new DateTime(datish);
        }
        if (datish instanceof ZonedDateTime) {
            final ZonedDateTime zonedDateTime = (ZonedDateTime) datish;
            final DateTimeZone timeZone = DateTimeZone.forID(zonedDateTime.getZone().getId());
            return new DateTime(zonedDateTime.toInstant().toEpochMilli(), timeZone);
        }
        return null;
    }

    @Override
    protected String description() {
        return "Converts a type to a date, useful for $message.timestamp or related message fields.";
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected ImmutableList<ParameterDescriptor> params() {
        return ImmutableList.of(value);
    }
}
