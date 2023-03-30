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
package org.graylog.plugins.pipelineprocessor.functions.debug;

import com.google.common.annotations.VisibleForTesting;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;

import static com.google.common.collect.ImmutableList.of;

public class Debug extends AbstractFunction<Void> {

    private final ParameterDescriptor<Object, Object> valueParam;
    private final Logger logger;

    public static final String NAME = "debug";

    public Debug() {
        valueParam = ParameterDescriptor.object("value").description("The value to print in the graylog-server log.").build();
        logger = log;
    }

    // Only used in unit tests
    @VisibleForTesting
    public Debug(Logger logger) {
        valueParam = ParameterDescriptor.object("value").description("The value to print in the graylog-server log.").build();
        this.logger = logger;
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);

        if (value instanceof Message) {
            logger.info("PIPELINE DEBUG Message: <{}>", ((Message) value).toDumpString());
        } else {
            logger.info("PIPELINE DEBUG: {}", value);
        }
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(valueParam))
                .description("Print the passed value as string in the graylog-server log." +
                        " You can also pass $message to print the current Message object." +
                        " Note that this will only appear in the log of the graylog-server node" +
                        " that is processing the message you are trying to debug.")
                .build();
    }

}
