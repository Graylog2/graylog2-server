package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class MessageRefExpression implements Expression {
    private final Expression fieldExpr;

    public MessageRefExpression(Expression fieldExpr) {
        this.fieldExpr = fieldExpr;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        final Object fieldName = fieldExpr.evaluate(context);
        return context.currentMessage().getField(fieldName.toString());
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return "$message." + fieldExpr.toString();
    }

    public Expression getFieldExpr() {
        return fieldExpr;
    }
}
