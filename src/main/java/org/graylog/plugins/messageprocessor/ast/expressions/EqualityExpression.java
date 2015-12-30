package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class EqualityExpression extends BinaryExpression implements LogicalExpression {
    private final boolean equal;

    public EqualityExpression(Expression left, Expression right, boolean equal) {
        super(left, right);
        this.equal = equal;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return evaluateBool(context, message);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        final boolean equals = left.evaluate(context, message).equals(right.evaluate(context, message));
        if (equal) {
            return equals;
        }
        return !equals;
    }

    @Override
    public String toString() {
        return left.toString() + (equal ? " == " : " != ") + right.toString();
    }
}
