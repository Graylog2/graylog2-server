package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class ComparisonExpression extends BinaryExpression implements LogicalExpression {
    private final String operator;

    public ComparisonExpression(Expression left, Expression right, String operator) {
        super(left, right);
        this.operator = operator;
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

        final Object leftValue = this.left.evaluate(context);
        final Object rightValue = this.right.evaluate(context);
        if (leftValue instanceof Double || rightValue instanceof Double) {
            return compareDouble(operator, (double) leftValue, (double) rightValue);
        } else {
            return compareLong(operator, (long) leftValue, (long) rightValue);
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
