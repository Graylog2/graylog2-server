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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayLiteralExpression extends BaseExpression {
    private final List<Expression> elements;

    public ArrayLiteralExpression(Token start, List<Expression> elements) {
        super(start);
        this.elements = elements;
    }

    @Override
    public boolean isConstant() {
        return elements.stream().allMatch(Expression::isConstant);
    }

    @Override
    public List evaluateUnsafe(EvaluationContext context) {
        return  elements.stream()
                .map(expression -> expression.evaluateUnsafe(context))
                .collect(Collectors.toList());
    }

    @Override
    public Class getType() {
        return List.class;
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(", ").join(elements) + "]";
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.copyOf(elements);
    }
}
