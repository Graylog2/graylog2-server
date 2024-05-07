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

public class RemoveMultipleFields extends AbstractFunction<Void> {
    public static final String NAME = "remove_multiple_fields";
    private static final String REGEX_PATTERN = "pattern";
    private static final String LIST_OF_NAMES = "names";
    private final ParameterDescriptor<String, String> regexParam;
    private final ParameterDescriptor<List, List> namesParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public RemoveMultipleFields() {
        regexParam = ParameterDescriptor.string(REGEX_PATTERN).optional().description("A regex specifying field names to be removed").build();
        namesParam = type(LIST_OF_NAMES, List.class).optional().description("A list of field names to be removed").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        if (regexParam.optional(args, context).isPresent()) {
            removeRegex(message, regexParam.optional(args, context).get());
        }
        if (namesParam.optional(args, context).isPresent()) {
            removeNames(message, namesParam.optional(args, context).get());
        }
        return null;
    }

    private void removeRegex(Message message, String regex) {
        final var pattern = Pattern.compile(regex);
        message.getFieldNames().stream()
                .filter(name -> pattern.matcher(name).matches())
                .toList() // required to avoid ConcurrentModificationException
                .forEach(message::removeField);
    }

    private void removeNames(Message message, List names) {
        for (Object name : names) {
            message.removeField(String.valueOf(name));
        }
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(regexParam, namesParam, messageParam))
                .description("Removes the specified field(s) from message, unless the field name is reserved. If no specific message is provided, it uses the currently processed message.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Remove field - multiple")
                .ruleBuilderTitle("Remove multiple fields by regex<#if pattern??> '${pattern}'</#if> or name list<#if names??> '${names}'</#if>")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
