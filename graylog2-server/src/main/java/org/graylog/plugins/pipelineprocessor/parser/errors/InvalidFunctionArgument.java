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
package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.PrecomputeFailure;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class InvalidFunctionArgument extends ParseError {
    private final Function<?> function;
    private final PrecomputeFailure failure;

    public InvalidFunctionArgument(RuleLangParser.FunctionCallContext ctx,
                                   Function<?> function,
                                   PrecomputeFailure failure) {
        super("invalid_function_argument", ctx);
        this.function = function;
        this.failure = failure;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        int paramPosition = 1;
        for (ParameterDescriptor descriptor : function.descriptor().params()) {
            if (descriptor.name().equals(failure.getArgumentName())) {
                break;
            }
            paramPosition++;
        }

        return "Unable to pre-compute value for " + ordinal(paramPosition) + " argument " + failure.getArgumentName() + " in call to function " + function.descriptor().name() + ": " + failure.getCause().getMessage();
    }

    private static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];

        }
    }
}
