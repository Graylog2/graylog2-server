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
package org.graylog2.outputs.filter.functions;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.outputs.filter.PipelineRuleOutputFilter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

/**
 * An internal function that is used for the pipeline processing rule {@link org.graylog2.outputs.filter.OutputFilter}
 * implementation.
 *
 * @see PipelineRuleOutputFilter
 */
public class RemoveFromStreamDestination extends AbstractFunction<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoveFromStreamDestination.class);
    private static final Logger RATE_LIMITED_LOG = RateLimitedLogFactory.createRateLimitedLog(LOG, 1, Duration.ofSeconds(15));

    public static final String NAME = "__remove_from_stream_destination__";
    public static final String STREAM_ID_PARAM = "stream_id";
    public static final String DESTINATION_TYPE_PARAM = "destination_type";

    private final ParameterDescriptor<Message, Message> messageParam;
    private final ParameterDescriptor<String, String> streamIdParam;
    private final ParameterDescriptor<String, String> destinationTypeParam;

    private final StreamCacheService streamCacheService;

    @Inject
    public RemoveFromStreamDestination(StreamCacheService streamCacheService) {
        this.streamCacheService = streamCacheService;
        this.messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'.").build();
        this.streamIdParam = string(STREAM_ID_PARAM).optional().description("The stream to remove the message from.").build();
        this.destinationTypeParam = string(DESTINATION_TYPE_PARAM).optional().description("The destination type to remove the message from.").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final var streamId = streamIdParam.required(args, context);
        final var destinationType = destinationTypeParam.required(args, context);
        final var message = messageParam.optional(args, context).orElse(context.currentMessage());

        if (message.getMetadataValue(PipelineRuleOutputFilter.METADATA_KEY) instanceof PipelineRuleOutputFilter.Metadata metadata) {
            final var stream = streamCacheService.getById(streamId);
            if (stream != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing stream <{}/{}> from destination <{}> for message: {}", stream.getId(), stream.getTitle(), destinationType, message);
                }
                metadata.destinations().remove(destinationType, stream);
            } else {
                RATE_LIMITED_LOG.warn("Couldn't find stream for stream ID <{}> in cache", streamId);
            }
        }

        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(streamIdParam, destinationTypeParam, messageParam))
                .description("[INTERNAL] Removes a message from a stream target.")
                .ruleBuilderEnabled()
                .ruleBuilderName(NAME)
                .ruleBuilderTitle("[INTERNAL] Removes a message from a stream target.")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                .build();
    }
}
