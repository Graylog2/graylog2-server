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
package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class IncompatibleArgumentType extends ParseError {
    private final FunctionExpression functionExpression;
    private final ParameterDescriptor p;
    private final Expression argExpr;

    public IncompatibleArgumentType(RuleLangParser.FunctionCallContext ctx,
                                    FunctionExpression functionExpression,
                                    ParameterDescriptor p,
                                    Expression argExpr) {
        super("incompatible_argument_type", ctx);
        this.functionExpression = functionExpression;
        this.p = p;
        this.argExpr = argExpr;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected type " + p.type().getSimpleName() +
                " for argument " + p.name() +
                " but found " + argExpr.getType().getSimpleName() +
                " in call to function " + functionExpression.getFunction().descriptor().name()
                + positionString();
    }
}
