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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import java.util.Collections;

public class BooleanValuedFunctionWrapper extends BaseExpression implements LogicalExpression {
    private final Expression expr;

    public BooleanValuedFunctionWrapper(Token start, Expression expr) {
        super(start);
        this.expr = expr;
        if (!expr.getType().equals(Boolean.class)) {
            throw new IllegalArgumentException("expr must be of boolean type");
        }
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        final Object value = expr.evaluateUnsafe(context);
        return value != null && (Boolean) value;
    }

    @Override
    public boolean isConstant() {
        return expr.isConstant();
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return expr.getType();
    }

    public Expression expression() {
        return expr;
    }

    @Override
    public String toString() {
        return expr.toString();
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.singleton(expr);
    }
}
