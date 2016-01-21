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
import org.graylog.plugins.pipelineprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class IncompatibleTypes extends ParseError {
    private final RuleLangParser.ExpressionContext ctx;
    private final BinaryExpression binaryExpr;

    public IncompatibleTypes(RuleLangParser.ExpressionContext ctx, BinaryExpression binaryExpr) {
        super("incompatible_types", ctx);
        this.ctx = ctx;
        this.binaryExpr = binaryExpr;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Incompatible types " + exprString(binaryExpr.left()) + " <=> " + exprString(binaryExpr.right()) + positionString();
    }

    private String exprString(Expression e) {
        return "(" + e.toString() + ") : " + e.getType().getSimpleName();
    }


}
