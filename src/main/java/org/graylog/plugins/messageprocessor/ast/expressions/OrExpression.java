package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class OrExpression extends BinaryExpression implements LogicalExpression {
    public OrExpression(LogicalExpression left,
                        LogicalExpression right) {
        super(left, right);
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return evaluateBool(context, message);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        return ((LogicalExpression)left).evaluateBool(context, message) || ((LogicalExpression)right).evaluateBool(context, message);
    }

    @Override
    public String toString() {
        return left.toString() + " OR " + right.toString();
    }
}
