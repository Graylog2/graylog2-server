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
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class CreateMessage extends AbstractFunction<Message> {

    public static final String NAME = "create_message";

    private static final String MESSAGE_ARG = "message";
    private static final String SOURCE_ARG = "source";
    private static final String TIMESTAMP_ARG = "timestamp";
    private final ParameterDescriptor<String, String> messageParam;
    private final ParameterDescriptor<String, String> sourceParam;
    private final ParameterDescriptor<DateTime, DateTime> timestampParam;

    public CreateMessage() {
        messageParam = string(MESSAGE_ARG).optional().description("The 'message' field of the new message, defaults to '$message.message'").build();
        sourceParam = string(SOURCE_ARG).optional().description("The 'source' field of the new message, defaults to '$message.source'").build();
        timestampParam = type(TIMESTAMP_ARG, DateTime.class).optional().description("The 'timestamp' field of the message, defaults to 'now'").build();
    }

    @Override
    public Message evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<String> optMessage = messageParam.optional(args, context);
        final String message = optMessage.isPresent() ? optMessage.get() : context.currentMessage().getMessage();

        final Optional<String> optSource = sourceParam.optional(args, context);
        final String source = optSource.isPresent() ? optSource.get() : context.currentMessage().getSource();

        final Optional<DateTime> optTimestamp = timestampParam.optional(args, context);
        final DateTime timestamp = optTimestamp.isPresent() ? optTimestamp.get() : Tools.nowUTC();

        final Message newMessage = new Message(message, source, timestamp);

        // register in context so the processor can inject it later on
        context.addCreatedMessage(newMessage);
        return newMessage;
    }

    @Override
    public FunctionDescriptor<Message> descriptor() {
        return FunctionDescriptor.<Message>builder()
                .name(NAME)
                .returnType(Message.class)
                .params(of(
                        messageParam,
                        sourceParam,
                        timestampParam
                ))
                .description("Creates a new message")
                .build();
    }
}
