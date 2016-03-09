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

public class BooleanExpression extends ConstantExpression implements LogicalExpression {
    private final boolean value;

    public BooleanExpression(Token start, boolean value) {
        super(start, Boolean.class);
        this.value = value;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return value;
    }


    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
