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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.collect.ImmutableList;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class RemoveStringFieldsByValue extends AbstractFunction<Void> {
    private static final RateLimitedLog LOG = getRateLimitedLog(RemoveStringFieldsByValue.class);
    public static final String NAME = "remove_string_fields_by_value";
    private static final String VALUES_ARG = "values";
    private static final String PATTERN_ARG = "pattern";
    private final ParameterDescriptor<String, Pattern> regexParam;
    private final ParameterDescriptor<List, List> valuesParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public RemoveStringFieldsByValue() {
        regexParam = ParameterDescriptor.string(PATTERN_ARG, Pattern.class)
                .optional()
                .transform(Pattern::compile)
                .description("A regex specifying field values to be removed").build();
        valuesParam = type(VALUES_ARG, List.class).optional().description("A list of exact values of fields to be removed").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        final Predicate<String> removalPredicate = getRemovalPredicate(args, context);
        if (removalPredicate != null) {
            message.removeFieldsByValue(removalPredicate);
        } else {
            final String ruleId = context.getRule() != null ? context.getRule().id() : null;
            final String ruleName = context.getRule() != null ? context.getRule().name() : null;
            LOG.warn("{} called by pipeline rule [{}/{}] with no {} or {} provided. One or both must be provided for the rule to execute.",
                    NAME, ruleId, ruleName, VALUES_ARG, PATTERN_ARG);
        }
        return null;
    }

    private Predicate<String> getRemovalPredicate(FunctionArgs args, EvaluationContext context) {
        final Optional<Pattern> optPattern = regexParam.optional(args, context);
        final Optional<List> optValues = valuesParam.optional(args, context);
        // Return null if neither a pattern nor values is provided
        if (optPattern.isEmpty() && optValues.isEmpty()) {
            return null;
        }
        Predicate<String> removalPredicate = null;
        if (optValues.isPresent()) {
            final List values = optValues.get();
            removalPredicate = values::contains;
        }
        if (optPattern.isPresent()) {
            final Pattern pattern = optPattern.get();
            if (removalPredicate != null) {
                removalPredicate = removalPredicate.or(value -> pattern.matcher(value).matches());
            } else {
                removalPredicate = value -> pattern.matcher(value).matches();
            }
        }
        return removalPredicate;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(regexParam, valuesParam, messageParam))
                .description("Removes fields whose value exists in the values list or matches the regex pattern, unless the field name is reserved. Operates on fields with string values only, other types will be ignored. If no specific message is provided, it uses the currently processed message.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Remove string fields by value")
                .ruleBuilderTitle("Remove multiple fields whose value is a string and matches<#if pattern??> regex '${pattern}'</#if><#if pattern?? && values??> or</#if><#if values??> exists in values list '${values}'</#if>")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
