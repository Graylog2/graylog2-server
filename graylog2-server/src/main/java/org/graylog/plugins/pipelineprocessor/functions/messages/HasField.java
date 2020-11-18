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
import org.graylog2.plugin.Message;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class HasField extends AbstractFunction<Boolean> {

    public static final String NAME = "has_field";
    public static final String FIELD = "field";
    private final ParameterDescriptor<String, String> fieldParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public HasField() {
        fieldParam = ParameterDescriptor.string(FIELD).description("The field to check").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final String field = fieldParam.required(args, context);
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());

        return message.hasField(field);
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(ImmutableList.of(fieldParam, messageParam))
                .description("Checks whether a message contains a value for a field")
                .build();
    }
}
