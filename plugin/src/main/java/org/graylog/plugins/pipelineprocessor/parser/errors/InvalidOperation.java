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
