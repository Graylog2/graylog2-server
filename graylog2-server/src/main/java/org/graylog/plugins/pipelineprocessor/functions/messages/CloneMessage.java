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
import org.apache.commons.lang3.ObjectUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.plugin.Message;
import org.graylog2.shared.utilities.StringUtils;

import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class CloneMessage extends AbstractFunction<Message> {
    private static final RateLimitedLog LOG = getRateLimitedLog(CloneMessage.class);

    public static final String NAME = "clone_message";

    private static final String CLONE_SOURCE = "gl2_clone_source_rule";
    private static final String CLONE_NUMBER = "gl2_clone_number";
    static final int MAX_CLONES = 100;

    private final ParameterDescriptor<Message, Message> messageParam;
    private final ParameterDescriptor<Boolean, Boolean> loopDetectionParam;

    public CloneMessage() {
        loopDetectionParam = ParameterDescriptor.bool("preventLoops").optional().description("Detects if a cloned message is processed by the same rule again, in order to prevent loops. Defaults to 'false', but will not allow more than " + MAX_CLONES + " clones if not explicitly set.").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Message evaluate(FunctionArgs args, EvaluationContext context) {
        final Message currentMessage = messageParam.optional(args, context).orElse(context.currentMessage());
        final Optional<Boolean> preventLoops = loopDetectionParam.optional(args, context);

        int cloneNumber = (int) currentMessage.getMetadataValue(CLONE_NUMBER, 0);

        final Rule cloneSource = (Rule) currentMessage.getMetadataValue(CLONE_SOURCE);
        final Rule rule = context.getRule();
        if (ObjectUtils.allNotNull(cloneSource, rule) && Objects.equals(cloneSource, rule)) { // message was cloned by the same rule before

            if (preventLoops.orElse(false)) {
                return null;
            }

            if (preventLoops.isEmpty() && cloneNumber >= MAX_CLONES) {
                throw new IllegalStateException(
                        StringUtils.f("Message was cloned more than %d times by rule '%s'. Not allowing any more " +
                                        "clones to prevent a potential endless loop. If this was intentional, please " +
                                        "explicitly set the 'preventLoops' parameter to 'false'.",
                                MAX_CLONES, cloneSource.name()));
            }

        }

        final Message clonedMessage = new Message(currentMessage.getMessage(), currentMessage.getSource(), currentMessage.getTimestamp());
        clonedMessage.addFields(currentMessage.getFields());
        clonedMessage.addStreams(currentMessage.getStreams());
        if (rule != null) {
            clonedMessage.setMetadata(CLONE_SOURCE, rule);
        }
        clonedMessage.setMetadata(CLONE_NUMBER, ++cloneNumber);

        // register in context so the processor can inject it later on
        context.addCreatedMessage(clonedMessage);
        return clonedMessage;
    }

    @Override
    public FunctionDescriptor<Message> descriptor() {
        return FunctionDescriptor.<Message>builder()
                .name(NAME)
                .params(ImmutableList.of(messageParam, loopDetectionParam))
                .returnType(Message.class)
                .description("Clones a message. If no specific message is provided, it clones the currently processed message")
                .ruleBuilderEnabled()
                .ruleBuilderName("Clone message")
                .ruleBuilderTitle("Clone message")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
