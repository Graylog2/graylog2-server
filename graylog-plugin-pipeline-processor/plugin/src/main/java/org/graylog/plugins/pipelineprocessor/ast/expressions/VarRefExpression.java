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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class VarRefExpression extends BaseExpression {
    private static final Logger log = LoggerFactory.getLogger(VarRefExpression.class);
    private final String identifier;
    private final Expression varExpr;
    private Class type = Object.class;

    public VarRefExpression(Token start, String identifier, Expression varExpr) {
        super(start);
        this.identifier = identifier;
        this.varExpr = varExpr;
    }

    @Override
    public boolean isConstant() {
        return varExpr != null && varExpr.isConstant();
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final EvaluationContext.TypedValue typedValue = context.get(identifier);
        if (typedValue != null) {
            return typedValue.getValue();
        }
        log.error("Unable to retrieve value for variable {}", identifier);
        return null;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public String toString() {
        return identifier;
    }

    public String varName() {
        return identifier;
    }

    public Expression varExpr() { return varExpr; }

    public void setType(Class type) {
        this.type = type;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
