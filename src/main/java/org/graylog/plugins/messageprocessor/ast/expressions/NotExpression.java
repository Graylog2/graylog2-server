package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class NotExpression extends UnaryExpression implements LogicalExpression {
    public NotExpression(LogicalExpression right) {
        super(right);
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return !evaluateBool(context, message);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        return !((LogicalExpression)right).evaluateBool(context, message);
    }

    @Override
    public String toString() {
        return "NOT " + right.toString();
    }
}
