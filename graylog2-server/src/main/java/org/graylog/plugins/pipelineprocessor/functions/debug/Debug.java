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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;

public class Debug extends AbstractFunction<Void> {

    private final ParameterDescriptor<Object, Object> valueParam;

    public static final String NAME = "debug";

    public Debug() {
        valueParam = ParameterDescriptor.object("value")
                .description("The value to print in the graylog-server log.")
                .optional().build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<Object> value = valueParam.optional(args, context);

        if (value.isPresent()) {
            log.info("PIPELINE DEBUG: {}", value.get());
        } else {
            log.info("PIPELINE DEBUG Message: <{}>", context.currentMessage().toDumpString());
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
                        " If no parameter is supplied it will print the current $message." +
                        " Note that this will only appear in the log of the graylog-server node" +
                        " that is processing the message you are trying to debug.")
                .build();
    }

}
