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

import org.antlr.v4.runtime.ParserRuleContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

public class InvalidOperation extends ParseError {
    private final Expression expr;

    private final String message;

    public InvalidOperation(ParserRuleContext ctx, Expression expr, String message) {
        super("invalid_operation", ctx);
        this.expr = expr;
        this.message = message;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Invalid operation: " + message;
    }

    public Expression getExpression() {
        return expr;
    }

    public String getMessage() {
        return message;
    }
}
