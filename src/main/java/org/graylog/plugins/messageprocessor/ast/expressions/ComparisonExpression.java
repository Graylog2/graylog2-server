package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class ComparisonExpression extends BinaryExpression implements LogicalExpression {
    private final String operator;
    private final boolean numericArgs;

    public ComparisonExpression(Expression left, Expression right, String operator) {
        super(left, right);
        this.numericArgs = left instanceof NumericExpression && right instanceof NumericExpression;

        this.operator = operator;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return evaluateBool(context, message);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        if (!numericArgs) {
            return false;
        }
        final NumericExpression numericLeft = (NumericExpression) this.left;
        final NumericExpression numericRight = (NumericExpression) this.right;
        if (numericLeft.isFloatingPoint() || numericRight.isFloatingPoint()) {
            return compareDouble(operator, numericLeft.doubleValue(), numericRight.doubleValue());
        } else {
            return compareLong(operator, numericLeft.longValue(), numericRight.longValue());
        }
    }

    @SuppressWarnings("Duplicates")
    private boolean compareLong(String operator, long left, long right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    @SuppressWarnings("Duplicates")
    private boolean compareDouble(String operator, double left, double right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return left.toString() + " " + operator + " " + right.toString();
    }
}
