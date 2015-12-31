package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class EqualityExpression extends BinaryExpression implements LogicalExpression {
    private final boolean checkEquality;

    public EqualityExpression(Expression left, Expression right, boolean checkEquality) {
        super(left, right);
        this.checkEquality = checkEquality;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return evaluateBool(context, message);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        final boolean equals = left.evaluate(context, message).equals(right.evaluate(context, message));
        if (checkEquality) {
            return equals;
        }
        return !equals;
    }

    @Override
    public String toString() {
        return left.toString() + (checkEquality ? " == " : " != ") + right.toString();
    }
}
