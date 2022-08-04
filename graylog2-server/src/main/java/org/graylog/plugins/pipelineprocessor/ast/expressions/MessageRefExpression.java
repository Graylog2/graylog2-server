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
import org.graylog2.plugin.Message;

import java.util.Collections;

public class MessageRefExpression extends BaseExpression {
    private final Expression fieldExpr;

    public MessageRefExpression(Token start, Expression fieldExpr) {
        super(start);
        this.fieldExpr = fieldExpr;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        if (fieldExpr == null) {
            return context.currentMessage();
        }
        final Object fieldName = fieldExpr.evaluateUnsafe(context);
        if (fieldName == null) {
            return null;
        }
        return context.currentMessage().getField(fieldName.toString());
    }

    @Override
    public Class getType() {
        if (fieldExpr == null) {
            return Message.class;
        }
        return Object.class;
    }

    @Override
    public String toString() {
        if (fieldExpr == null) {
            return "$message";
        }
        return "$message." + fieldExpr.toString();
    }

    public Expression getFieldExpr() {
        if (fieldExpr == null) {
            return this;
        }
        return fieldExpr;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.singleton(fieldExpr);
    }
}
