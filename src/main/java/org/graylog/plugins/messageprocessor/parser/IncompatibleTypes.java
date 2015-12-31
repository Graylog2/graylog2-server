package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;

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
