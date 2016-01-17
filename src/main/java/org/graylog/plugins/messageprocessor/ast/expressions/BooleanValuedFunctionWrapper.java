package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class BooleanValuedFunctionWrapper implements LogicalExpression {
    private final Expression expr;

    public BooleanValuedFunctionWrapper(Expression expr) {
        this.expr = expr;
        if (!expr.getType().equals(Boolean.class)) {
            throw new IllegalArgumentException("expr must be of boolean type");
        }
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        final Object value = expr.evaluate(context);
        return value != null && (Boolean) value;
    }

    @Override
    public boolean isConstant() {
        return expr.isConstant();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return expr.getType();
    }
}
