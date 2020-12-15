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
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.annotation.Nullable;

import static com.google.common.base.MoreObjects.firstNonNull;

public class AdditionExpression extends BinaryExpression implements NumericExpression {
    private final boolean isPlus;
    private Class type = Void.class;

    public AdditionExpression(Token start, Expression left, Expression right, boolean isPlus) {
        super(start, left, right);
        this.isPlus = isPlus;
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

    @Nullable
    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final Object leftValue = left.evaluateUnsafe(context);
        final Object rightValue = right.evaluateUnsafe(context);

        // special case for date arithmetic
        final boolean leftDate = DateTime.class.equals(leftValue.getClass());
        final boolean leftPeriod = Period.class.equals(leftValue.getClass());
        final boolean rightDate = DateTime.class.equals(rightValue.getClass());
        final boolean rightPeriod = Period.class.equals(rightValue.getClass());
        if (leftDate && rightPeriod) {
            final DateTime date = (DateTime) leftValue;
            final Period period = (Period) rightValue;

            return isPlus() ? date.plus(period) : date.minus(period);
        } else if (leftPeriod && rightDate) {
            final DateTime date = (DateTime) rightValue;
            final Period period = (Period) leftValue;

            return isPlus() ? date.plus(period) : date.minus(period);
        } else if (leftPeriod && rightPeriod) {
            final Period period1 = (Period) leftValue;
            final Period period2 = (Period) rightValue;

            return isPlus() ? period1.plus(period2) : period1.minus(period2);
        } else if (leftDate && rightDate) {
            // the most uncommon, this is only defined for - really and means "interval between them"
            // because adding two dates makes no sense
            if (isPlus()) {
                // makes no sense to compute and should be handles in the parser already
                return null;
            }
            final DateTime left = (DateTime) leftValue;
            final DateTime right = (DateTime) rightValue;

            if (left.isBefore(right)) {
                return new Duration(left, right);
            } else {
                return new Duration(right, left);
            }
        } else if(String.class.equals(type)) {
            if (!isPlus) {
                return null;
            }

            final String left = String.valueOf(leftValue);
            final String right = String.valueOf(rightValue);
            return left + right;
        }
        if (isIntegral()) {
            final long l = (long) leftValue;
            final long r = (long) rightValue;
            if (isPlus) {
                return l + r;
            } else {
                return l - r;
            }
        } else {
            final double l = (double) leftValue;
            final double r = (double) rightValue;
            if (isPlus) {
                return l + r;
            } else {
                return l - r;
            }
        }
    }

    public boolean isPlus() {
        return isPlus;
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
        return left.toString() + (isPlus ? " + " : " - ") + right.toString();
    }
}
