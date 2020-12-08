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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EqualityExpression extends BinaryExpression implements LogicalExpression {
    private static final Logger log = LoggerFactory.getLogger(EqualityExpression.class);

    private final boolean checkEquality;

    public EqualityExpression(Token start, Expression left, Expression right, boolean checkEquality) {
        super(start, left, right);
        this.checkEquality = checkEquality;
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
        final Object left = this.left.evaluateUnsafe(context);
        final Object right = this.right.evaluateUnsafe(context);
        if (left == null) {
            log.warn("left expression evaluated to null, returning false: {}", this.left);
            return false;
        }
        final boolean equals;
        // sigh: DateTime::equals takes the chronology into account, so identical instants expressed in different timezones are not equal
        if (left instanceof DateTime && right instanceof DateTime) {
            equals = ((DateTime) left).isEqual((DateTime) right);
        } else {
            equals = left.equals(right);
        }

        if (log.isTraceEnabled()) {
            traceEquality(left, right, equals, checkEquality);
        }
        if (checkEquality) {
            return equals;
        }
        return !equals;
    }

    private void traceEquality(Object left,
                               Object right,
                               boolean equals,
                               boolean checkEquality) {
        log.trace(checkEquality
                          ? "[{}] {} == {} : {} == {}"
                          : "[{}] {} != {} : {} != {}",
                  checkEquality == equals, this.left, this.right, left, right);
    }

    public boolean isCheckEquality() {
        return checkEquality;
    }

    @Override
    public String toString() {
        return left.toString() + (checkEquality ? " == " : " != ") + right.toString();
    }
}
