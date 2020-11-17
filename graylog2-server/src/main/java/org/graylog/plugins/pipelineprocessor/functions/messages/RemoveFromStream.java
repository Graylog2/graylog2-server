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

import com.google.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class RemoveFromStream extends AbstractFunction<Void> {

    public static final String NAME = "remove_from_stream";
    private static final String ID_ARG = "id";
    private static final String NAME_ARG = "name";
    private final StreamCacheService streamCacheService;
    private final Provider<Stream> defaultStreamProvider;
    private final ParameterDescriptor<Message, Message> messageParam;
    private final ParameterDescriptor<String, String> nameParam;
    private final ParameterDescriptor<String, String> idParam;

    @Inject
    public RemoveFromStream(StreamCacheService streamCacheService, @DefaultStream Provider<Stream> defaultStreamProvider) {
        this.streamCacheService = streamCacheService;
        this.defaultStreamProvider = defaultStreamProvider;

        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
        nameParam = string(NAME_ARG).optional().description("The name of the stream to remove the message from, must match exactly").build();
        idParam = string(ID_ARG).optional().description("The ID of the stream").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        Optional<String> id = idParam.optional(args, context);

        Collection<Stream> streams;
        if (!id.isPresent()) {
            final Optional<Collection<Stream>> foundStreams = nameParam.optional(args, context).map(streamCacheService::getByName);

            if (!foundStreams.isPresent()) {
                // TODO signal error somehow
                return null;
            } else {
                streams = foundStreams.get();
            }
        } else {
            final Stream stream = streamCacheService.getById(id.get());
            if (stream == null) {
                return null;
            }
            streams = Collections.singleton(stream);
        }
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        streams.forEach(stream -> {
            if (!stream.isPaused()) {
                message.removeStream(stream);
            }
        });
        // always leave a message at least on the default stream if we removed the last stream it was on
        if (message.getStreams().isEmpty()) {
            message.addStream(defaultStreamProvider.get());
        }
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(
                        nameParam,
                        idParam,
                        messageParam))
                .description("Removes a message from a stream. Removing the last stream will put the message back onto the default stream. To complete drop a message use the drop_message function.")
                .build();
    }
}
