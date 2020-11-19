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
package org.graylog.plugins.pipelineprocessor.functions.syslog;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class SyslogPriorityToStringConversion extends AbstractFunction<SyslogPriorityAsString> {
    public static final String NAME = "expand_syslog_priority_as_string";

    private final ParameterDescriptor<Object, Object> valueParam = object("value").description("Value to convert").build();

    @Override
    public SyslogPriorityAsString evaluate(FunctionArgs args, EvaluationContext context) {
        final String s = String.valueOf(valueParam.required(args, context));
        final int priority = Integer.parseInt(s);
        final int facility = SyslogUtils.facilityFromPriority(priority);
        final String facilityString = SyslogUtils.facilityToString(facility);
        final int level = SyslogUtils.levelFromPriority(priority);
        final String levelString = SyslogUtils.levelToString(level);

        return SyslogPriorityAsString.create(levelString, facilityString);
    }

    @Override
    public FunctionDescriptor<SyslogPriorityAsString> descriptor() {
        return FunctionDescriptor.<SyslogPriorityAsString>builder()
                .name(NAME)
                .returnType(SyslogPriorityAsString.class)
                .params(valueParam)
                .description("Converts a syslog priority number to its level and facility string representations")
                .build();
    }
}
