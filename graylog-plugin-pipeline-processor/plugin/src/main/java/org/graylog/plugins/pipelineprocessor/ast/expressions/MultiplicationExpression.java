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
