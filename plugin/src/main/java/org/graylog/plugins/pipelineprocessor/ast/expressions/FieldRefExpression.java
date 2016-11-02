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

public class FieldRefExpression extends BaseExpression {
    private final String variableName;
    private final Expression fieldExpr;

    public FieldRefExpression(Token start, String variableName, Expression fieldExpr) {
        super(start);
        this.variableName = variableName;
        this.fieldExpr = fieldExpr;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return variableName;
    }

    @Override
    public Class getType() {
        return String.class;
    }

    @Override
    public String toString() {
        return variableName;
    }

    public String fieldName() {
        return variableName;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
