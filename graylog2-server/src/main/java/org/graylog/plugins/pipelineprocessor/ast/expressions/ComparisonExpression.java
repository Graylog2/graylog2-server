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
import org.joda.time.DateTime;

public class ComparisonExpression extends BinaryExpression implements LogicalExpression {
    private final String operator;

    public ComparisonExpression(Token start, Expression left, Expression right, String operator) {
        super(start, left, right);
        this.operator = operator;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {

        final Object leftValue = this.left.evaluateUnsafe(context);
        final Object rightValue = this.right.evaluateUnsafe(context);
        if (leftValue instanceof DateTime && rightValue instanceof DateTime) {
            return compareDateTimes(operator, (DateTime) leftValue, (DateTime) rightValue);
        }

        if (leftValue instanceof Double || rightValue instanceof Double) {
            return compareDouble(operator, (double) leftValue, (double) rightValue);
        }

        return compareLong(operator, (long) leftValue, (long) rightValue);
    }

    @SuppressWarnings("Duplicates")
    private boolean compareLong(String operator, long left, long right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    @SuppressWarnings("Duplicates")
    private boolean compareDouble(String operator, double left, double right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    private boolean compareDateTimes(String operator, DateTime left, DateTime right) {
        switch (operator) {
            case ">":
                return left.isAfter(right);
            case ">=":
                return !left.isBefore(right);
            case "<":
                return left.isBefore(right);
            case "<=":
                return !left.isAfter(right);
            default:
                return false;
        }
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return left.toString() + " " + operator + " " + right.toString();
    }
}
