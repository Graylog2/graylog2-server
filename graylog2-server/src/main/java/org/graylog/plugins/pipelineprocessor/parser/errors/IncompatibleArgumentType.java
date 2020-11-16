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
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class IncompatibleArgumentType extends ParseError {
    private final FunctionExpression functionExpression;
    private final ParameterDescriptor p;
    private final Expression argExpr;

    public IncompatibleArgumentType(RuleLangParser.FunctionCallContext ctx,
                                    FunctionExpression functionExpression,
                                    ParameterDescriptor p,
                                    Expression argExpr) {
        super("incompatible_argument_type", ctx);
        this.functionExpression = functionExpression;
        this.p = p;
        this.argExpr = argExpr;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected type " + p.type().getSimpleName() +
                " for argument " + p.name() +
                " but found " + argExpr.getType().getSimpleName() +
                " in call to function " + functionExpression.getFunction().descriptor().name()
                + positionString();
    }
}
