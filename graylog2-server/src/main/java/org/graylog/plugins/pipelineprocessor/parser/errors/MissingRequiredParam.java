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

public class MissingRequiredParam extends ParseError {
    private final Function<?> function;
    private final ParameterDescriptor param;

    public MissingRequiredParam(RuleLangParser.FunctionCallContext ctx,
                                Function<?> function,
                                ParameterDescriptor param) {
        super("missing_required_param", ctx);
        this.function = function;
        this.param = param;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Missing required parameter " + param.name() +
                " of type " + param.type().getSimpleName() +
                " in call to function " + function.descriptor().name()
                + positionString();
    }
}
