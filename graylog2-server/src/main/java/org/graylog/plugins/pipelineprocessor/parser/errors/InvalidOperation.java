/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.antlr.v4.runtime.ParserRuleContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

public class InvalidOperation extends ParseError {
    private final Expression expr;

    private final String message;

    public InvalidOperation(ParserRuleContext ctx, Expression expr, String message) {
        super("invalid_operation", ctx);
        this.expr = expr;
        this.message = message;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Invalid operation: " + message;
    }

    public Expression getExpression() {
        return expr;
    }

    public String getMessage() {
        return message;
    }
}
