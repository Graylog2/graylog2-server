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
package org.graylog.plugins.pipelineprocessor.ast.statements;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

public class VarAssignStatement implements Statement {
    private final String name;
    private final Expression expr;

    public VarAssignStatement(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
    }

    @Override
    public Void evaluate(EvaluationContext context) {
        final Object result = expr.evaluate(context);
        context.define(name, expr.getType(), result);
        return null;
    }

    public String getName() {
        return name;
    }

    public Expression getValueExpression() {
        return expr;
    }

    @Override
    public String toString() {
        return "let " + name + " = " + expr.toString();
    }
}
