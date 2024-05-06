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

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class RemoveField extends AbstractFunction<Void> {
    public static final String NAME = "remove_field";
    public static final String FIELD = "field";
    public static final String INVERT = "invert";
    public static final String IS_REGEX = "is_regex";
    private final ParameterDescriptor<String, String> fieldParam;
    private final ParameterDescriptor<Message, Message> messageParam;
    private final ParameterDescriptor<Boolean, Boolean> invertParam;
    private final ParameterDescriptor<Boolean, Boolean> isRegexParam;

    public RemoveField() {
        fieldParam = ParameterDescriptor.string(FIELD).description("The field(s) to remove (name or regex)").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
        invertParam = ParameterDescriptor.bool(INVERT).optional().description("Keep matching field(s) and remove all others, defaults to false").build();
        isRegexParam = ParameterDescriptor.bool(IS_REGEX).optional().description("Perform regex-matching on the field name pattern, defaults to false").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final String fieldOrPattern = fieldParam.required(args, context);
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        final Boolean invert = invertParam.optional(args, context).orElse(false);
        final Boolean isRegex = isRegexParam.optional(args, context).orElse(false);

        if (Boolean.TRUE.equals(isRegex)) {
            message.getFieldNames().stream()
                .filter(f -> {
                    boolean condition = f.matches(fieldOrPattern);
                    return Boolean.TRUE.equals(invert) ? !condition : condition;
                })
                    .toList() // required to avoid ConcurrentModificationException
                .forEach(message::removeField);
        } else {
            if (Boolean.TRUE.equals(invert)) {
                message.getFieldNames().stream()
                        .filter(f -> !f.equals(fieldOrPattern))
                        .toList() // required to avoid ConcurrentModificationException
                        .forEach(message::removeField);
            } else {
                message.removeField(fieldOrPattern);
            }
        }
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(fieldParam, messageParam, invertParam, isRegexParam))
                .description("Removes one or more fields from message, unless the field is reserved. If no specific message is provided, it removes fields from the currently processed message.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Remove field")
                .ruleBuilderTitle("Remove field '${field}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
