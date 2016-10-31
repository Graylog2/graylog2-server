/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
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
