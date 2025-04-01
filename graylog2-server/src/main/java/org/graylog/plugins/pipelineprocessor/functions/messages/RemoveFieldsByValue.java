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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.regex.Pattern;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class RemoveFieldsByValue extends AbstractFunction<Void> {
    public static final String NAME = "remove_fields_by_value";
    private static final String VALUES_ARG = "values";
    private static final String PATTERN_ARG = "pattern";
    private final ParameterDescriptor<String, Pattern> regexParam;
    private final ParameterDescriptor<List, List> valuesParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public RemoveFieldsByValue() {
        regexParam = ParameterDescriptor.string(PATTERN_ARG, Pattern.class)
                .optional()
                .transform(Pattern::compile)
                .description("A regex specifying field values to be removed.").build();
        valuesParam = type(VALUES_ARG, List.class).optional().description("A list of exact values of fields to be removed.").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        if (regexParam.optional(args, context).isPresent()) {
            removeRegex(message, regexParam.optional(args, context).get());
        }
        if (valuesParam.optional(args, context).isPresent()) {
            removeNames(message, valuesParam.optional(args, context).get());
        }
        return null;
    }

    private void removeRegex(Message message, Pattern pattern) {
        message.getFields().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String && pattern.matcher((String) entry.getValue()).matches())
                .toList() // required to avoid ConcurrentModificationException
                .forEach(entry -> message.removeField(entry.getKey()));
    }

    private void removeNames(Message message, List values) {
        message.getFields().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String && values.contains(entry.getValue()))
                .toList() // required to avoid ConcurrentModificationException
                .forEach(entry -> message.removeField(entry.getKey()));
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(regexParam, valuesParam, messageParam))
                .description("Removes fields whose value match a value in the values list or the regex pattern, unless the field name is reserved. If no specific message is provided, it uses the currently processed message.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Remove fields by value")
                .ruleBuilderTitle("Remove multiple fields whose value match<#if pattern??> regex '${pattern}'</#if><#if pattern?? && values??> or</#if><#if values??> values list '${values}'</#if>")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
