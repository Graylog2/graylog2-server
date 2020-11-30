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

import com.google.common.primitives.Ints;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class SyslogFacilityConversion extends AbstractFunction<String> {
    public static final String NAME = "syslog_facility";

    private final ParameterDescriptor<Object, Object> valueParam = object("value").description("Value to convert").build();

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String s = String.valueOf(valueParam.required(args, context));
        final Integer facility = firstNonNull(Ints.tryParse(s), -1);

        return SyslogUtils.facilityToString(facility);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(valueParam)
                .description("Converts a syslog facility number to its string representation")
                .build();
    }
}
