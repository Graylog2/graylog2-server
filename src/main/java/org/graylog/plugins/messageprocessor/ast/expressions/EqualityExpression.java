package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class EqualityExpression extends BinaryExpression implements LogicalExpression {
    private final boolean checkEquality;

    public EqualityExpression(Expression left, Expression right, boolean checkEquality) {
        super(left, right);
        this.checkEquality = checkEquality;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        final boolean equals = left.evaluate(context).equals(right.evaluate(context));
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
