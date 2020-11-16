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
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

import java.util.function.Predicate;

public class WrongNumberOfArgs extends ParseError {
    private final Function<?> function;
    private final int argCount;

    public WrongNumberOfArgs(RuleLangParser.FunctionCallContext ctx,
                             Function<?> function,
                             int argCount) {
        super("wrong_number_of_arguments", ctx);
        this.function = function;
        this.argCount = argCount;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        final Predicate<ParameterDescriptor> optional = ParameterDescriptor::optional;
        return "Expected " + function.descriptor().params().stream().filter(optional.negate()).count() +
                " arguments but found " + argCount +
                " in call to function " + function.descriptor().name()
                + positionString();
    }
}
