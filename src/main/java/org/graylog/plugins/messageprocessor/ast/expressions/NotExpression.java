package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class NotExpression extends UnaryExpression implements LogicalExpression {
    public NotExpression(LogicalExpression right) {
        super(right);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return !evaluateBool(context);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return !((LogicalExpression)right).evaluateBool(context);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public String toString() {
        return "NOT " + right.toString();
    }
}
