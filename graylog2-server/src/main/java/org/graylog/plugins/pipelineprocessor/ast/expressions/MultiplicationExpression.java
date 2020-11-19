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

import javax.annotation.Nullable;

import static com.google.common.base.MoreObjects.firstNonNull;

public class MultiplicationExpression extends BinaryExpression implements NumericExpression  {
    private final char operator;
    private Class type;

    public MultiplicationExpression(Token start, Expression left, Expression right, char operator) {
        super(start, left, right);
        this.operator = operator;
    }

    @Override
    public boolean isIntegral() {
        return getType().equals(Long.class);
    }

    @Override
    public long evaluateLong(EvaluationContext context) {
        return (long) firstNonNull(evaluateUnsafe(context), 0);
    }

    @Override
    public double evaluateDouble(EvaluationContext context) {
        return (double) firstNonNull(evaluateUnsafe(context), 0d);
    }

    @SuppressWarnings("Duplicates")
    @Nullable
    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final Object leftValue = left.evaluateUnsafe(context);
        final Object rightValue = right.evaluateUnsafe(context);

        if (isIntegral()) {
            long l = (long) leftValue;
            long r = (long) rightValue;
            switch (operator) {
                case '*':
                    return l * r;
                case '/':
                    return l / r;
                case '%':
                    return l % r;
                default:
                    throw new IllegalStateException("Invalid operator, this is a bug.");
            }
        } else {
            final double l = (double) leftValue;
            final double r = (double) rightValue;

            switch (operator) {
                case '*':
                    return l * r;
                case '/':
                    return l / r;
                case '%':
                    return l % r;
                default:
                    throw new IllegalStateException("Invalid operator, this is a bug.");
            }
        }
    }

    public char getOperator() {
        return operator;
    }

    @Override
    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return left.toString() + " " + operator + " " + right.toString();
    }
}
