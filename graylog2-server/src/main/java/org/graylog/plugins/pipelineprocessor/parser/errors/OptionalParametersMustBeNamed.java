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
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class OptionalParametersMustBeNamed extends ParseError {
    private final Function<?> function;

    public OptionalParametersMustBeNamed(RuleLangParser.FunctionCallContext ctx, Function<?> function) {
        super("must_name_optional_params", ctx);
        this.function = function;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Function " + function.descriptor().name() + " has optional parameters, must use named parameters to call" + positionString();
    }
}
