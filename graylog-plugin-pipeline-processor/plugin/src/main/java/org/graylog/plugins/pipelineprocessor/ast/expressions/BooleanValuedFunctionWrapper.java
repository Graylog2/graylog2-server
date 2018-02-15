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
