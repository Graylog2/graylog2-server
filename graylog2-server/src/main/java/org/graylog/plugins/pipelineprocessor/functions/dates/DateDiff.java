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

public class DateDiff extends AbstractFunction<Map<String, Object>> {

    public static final String NAME = "date_diff";

    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String ABSOLUTE = "absolute";

    private static final long MS_PER_SECOND = 1000L;
    private static final long MS_PER_MINUTE = 60L * MS_PER_SECOND;
    private static final long MS_PER_HOUR   = 60L * MS_PER_MINUTE;
    private static final long MS_PER_DAY    = 24L * MS_PER_HOUR;
    private static final long MS_PER_WEEK   = 7L  * MS_PER_DAY;

    private final ParameterDescriptor<DateTime, DateTime> left;
    private final ParameterDescriptor<DateTime, DateTime> right;
    private final ParameterDescriptor<Boolean, Boolean> absolute;

    public DateDiff() {
        left = ParameterDescriptor.type(LEFT, DateTime.class)
                .description("Start of the interval. May be before or after the end; the result is signed by default (end - start).")
                .ruleBuilderVariable()
                .build();
        right = ParameterDescriptor.type(RIGHT, DateTime.class)
                .description("End of the interval. May be before or after the start.")
                .build();
        absolute = ParameterDescriptor.bool(ABSOLUTE)
                .optional()
                .description("If true, return absolute values; otherwise the result is signed (end - start). Defaults to false.")
                .build();
    }

    @Override
    public Map<String, Object> evaluate(FunctionArgs args, EvaluationContext context) {
        final DateTime leftValue = left.required(args, context);
        final DateTime rightValue = right.required(args, context);
        if (leftValue == null || rightValue == null) {
            return null;
        }
        final boolean abs = absolute.optional(args, context).orElse(false);

        final long signedMillis = new Duration(leftValue, rightValue).getMillis();
        final long value = (abs && signedMillis < 0) ? -signedMillis : signedMillis;

        return ImmutableMap.<String, Object>builder()
                .put("millis", value)
                .put("seconds", value / MS_PER_SECOND)
                .put("minutes", value / MS_PER_MINUTE)
                .put("hours",   value / MS_PER_HOUR)
                .put("days",    value / MS_PER_DAY)
                .put("weeks",   value / MS_PER_WEEK)
                .put("direction", direction(signedMillis))
                .put("friendly",  friendly(value))
                .build();
    }

    /**
     * Describes {@code right} relative to {@code left}. Computed from the signed millis,
     * so direction is preserved even when {@code absolute=true} strips the sign from the
     * numeric components.
     */
    private static String direction(long signedMillis) {
        if (signedMillis > 0) {
            return "ahead";
        }
        if (signedMillis < 0) {
            return "behind";
        }
        return "equal";
    }

    /**
     * Human-readable rendering of the (possibly signed) interval. Zero-valued components are
     * omitted. Sub-second remainder is included as a "ms" component only when the total
     * interval is below one minute, so long intervals aren't cluttered with millisecond noise;
     * the raw {@code millis} field always carries the exact value.
     */
    private static String friendly(long signedMillis) {
        if (signedMillis == 0) {
            return "0 ms";
        }
        final boolean neg = signedMillis < 0;
        final long m = neg ? -signedMillis : signedMillis;
        final StringBuilder sb = new StringBuilder();
        if (neg) {
            sb.append('-');
        }
        final long weeks   = m / MS_PER_WEEK;
        final long days    = (m / MS_PER_DAY)    % 7;
        final long hours   = (m / MS_PER_HOUR)   % 24;
        final long minutes = (m / MS_PER_MINUTE) % 60;
        final long seconds = (m / MS_PER_SECOND) % 60;
        final long millis  = m % MS_PER_SECOND;
        appendPart(sb, weeks,   "week",   "weeks");
        appendPart(sb, days,    "day",    "days");
        appendPart(sb, hours,   "hour",   "hours");
        appendPart(sb, minutes, "minute", "minutes");
        appendPart(sb, seconds, "second", "seconds");
        // Include sub-second remainder when the interval is below a minute, so callers see
        // precision for short deltas without "2 weeks ... 47 ms" noise on long ones.
        if (millis > 0 && m < MS_PER_MINUTE) {
            appendPart(sb, millis, "ms", "ms");
        }
        return sb.toString();
    }

    private static void appendPart(StringBuilder sb, long value, String singular, String plural) {
        if (value == 0) {
            return;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
            sb.append(' ');
        }
        sb.append(value).append(' ').append(value == 1 ? singular : plural);
    }

    @Override
    public FunctionDescriptor<Map<String, Object>> descriptor() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Class<? extends Map<String, Object>> returnType = (Class) Map.class;
        return FunctionDescriptor.<Map<String, Object>>builder()
                .name(NAME)
                .returnType(returnType)
                .params(ImmutableList.of(left, right, absolute))
                .description("Computes the difference between two dates and returns it as a map keyed by " +
                        "unit (millis, seconds, minutes, hours, days, weeks). The map also contains " +
                        "'direction' (\"ahead\" | \"behind\" | \"equal\", describing the end relative to the " +
                        "start) and 'friendly' (a human-readable rendering of the interval). By default the " +
                        "numeric values are signed (end - start); pass absolute=true to get absolute values. " +
                        "'direction' is always derived from the signed result and is preserved when " +
                        "absolute=true.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Date difference")
                .ruleBuilderTitle("Difference between '${left}' and '${right}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.DATE)
                .build();
    }
}
