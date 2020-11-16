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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class SetFields extends AbstractFunction<Void> {

    public static final String NAME = "set_fields";

    private final ParameterDescriptor<Map, Map> fieldsParam;
    private final ParameterDescriptor<String, String> prefixParam;
    private final ParameterDescriptor<String, String> suffixParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public SetFields() {
        fieldsParam = type("fields", Map.class).description("The map of new fields to set").build();
        prefixParam = string("prefix").optional().description("The prefix for the field names").build();
        suffixParam = string("suffix").optional().description("The suffix for the field names").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        //noinspection unchecked
        final Map<String, Object> fields = fieldsParam.required(args, context);
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        final Optional<String> prefix = prefixParam.optional(args, context);
        final Optional<String> suffix = suffixParam.optional(args, context);

        if (fields != null) {
            fields.forEach((field, value) -> {
                if (prefix.isPresent()) {
                    field = prefix.get() + field;
                }
                if (suffix.isPresent()) {
                    field = field + suffix.get();
                }
                message.addField(field, value);
            });
        }
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(fieldsParam, prefixParam, suffixParam, messageParam))
                .description("Sets new fields in a message")
                .build();
    }

}
