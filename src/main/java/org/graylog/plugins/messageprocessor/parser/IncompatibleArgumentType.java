package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;

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
