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

import org.graylog.plugins.pipelineprocessor.EvaluationContext;

public class BooleanValuedFunctionWrapper implements LogicalExpression {
    private final Expression expr;

    public BooleanValuedFunctionWrapper(Expression expr) {
        this.expr = expr;
        if (!expr.getType().equals(Boolean.class)) {
            throw new IllegalArgumentException("expr must be of boolean type");
        }
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        final Object value = expr.evaluate(context);
        return value != null && (Boolean) value;
    }

    @Override
    public boolean isConstant() {
        return expr.isConstant();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return expr.getType();
    }
}
