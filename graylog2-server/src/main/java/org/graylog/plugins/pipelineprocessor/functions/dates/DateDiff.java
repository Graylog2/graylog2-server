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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Map;

public class DateDiff extends AbstractFunction<Map<String, Long>> {

    public static final String NAME = "date_diff";

    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String ABSOLUTE = "absolute";

    private final ParameterDescriptor<DateTime, DateTime> left;
    private final ParameterDescriptor<DateTime, DateTime> right;
    private final ParameterDescriptor<Boolean, Boolean> absolute;

    public DateDiff() {
        left = ParameterDescriptor.type(LEFT, DateTime.class)
                .description("The earlier date (start of the interval)")
                .ruleBuilderVariable()
                .build();
        right = ParameterDescriptor.type(RIGHT, DateTime.class)
                .description("The later date (end of the interval)")
                .build();
        absolute = ParameterDescriptor.bool(ABSOLUTE)
                .optional()
                .description("If true, return absolute values; otherwise the result is signed (right - left), defaults to false")
                .build();
    }

    @Override
    public Map<String, Long> evaluate(FunctionArgs args, EvaluationContext context) {
        final DateTime leftValue = left.required(args, context);
        final DateTime rightValue = right.required(args, context);
        if (leftValue == null || rightValue == null) {
            return null;
        }
        final boolean abs = absolute.optional(args, context).orElse(false);

        final long millis = new Duration(leftValue, rightValue).getMillis();
        final long value = (abs && millis < 0) ? -millis : millis;

        return ImmutableMap.<String, Long>builder()
                .put("millis", value)
                .put("seconds", value / 1000L)
                .put("minutes", value / (60L * 1000L))
                .put("hours", value / (60L * 60L * 1000L))
                .put("days", value / (24L * 60L * 60L * 1000L))
                .put("weeks", value / (7L * 24L * 60L * 60L * 1000L))
                .build();
    }

    @Override
    public FunctionDescriptor<Map<String, Long>> descriptor() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Class<? extends Map<String, Long>> returnType = (Class) Map.class;
        return FunctionDescriptor.<Map<String, Long>>builder()
                .name(NAME)
                .returnType(returnType)
                .params(ImmutableList.of(left, right, absolute))
                .description("Computes the difference between two dates and returns it as a map keyed by " +
                        "unit: millis, seconds, minutes, hours, days, weeks. By default the result is signed " +
                        "(right - left); pass absolute=true to get absolute values. Each unit is the full " +
                        "interval converted to that unit (e.g. 2 days = 48 hours = 2880 minutes), truncated " +
                        "toward zero.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Date difference")
                .ruleBuilderTitle("Difference between '${left}' and '${right}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.DATE)
                .build();
    }
}
