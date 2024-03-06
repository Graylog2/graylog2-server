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
package org.graylog.plugins.pipelineprocessor.functions.ips;

import com.google.common.net.InetAddresses;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class IsIp extends AbstractFunction<Boolean> {
    public static final String NAME = "is_ip";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> conversionParam;

    public IsIp() {
        valueParam = object("value").ruleBuilderVariable().description("Value to check").build();
        conversionParam = bool("attemptConversion").optional().description("Try to convert value to an ip from its string representation (default: false)").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);
        final boolean convert = conversionParam.optional(args, context).orElse(false);
        if (!convert) {
            return value instanceof IpAddress;
        }
        if (value instanceof IpAddress) {
            return true;
        }
        try {
            InetAddresses.forString(String.valueOf(value));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(valueParam, conversionParam)
                .description("Checks whether a value is an IP address")
                .ruleBuilderEnabled(false)
                .ruleBuilderName("Check if IP")
                .ruleBuilderTitle("Check if '${value}' is an IP address")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.BOOLEAN)
                .build();
    }
}
