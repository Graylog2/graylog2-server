package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class AndExpression extends BinaryExpression implements LogicalExpression {
    public AndExpression(LogicalExpression left,
                         LogicalExpression right) {
        super(left, right);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return ((LogicalExpression)left).evaluateBool(context) && ((LogicalExpression)right).evaluateBool(context);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public String toString() {
        return left.toString() + " AND " + right.toString();
    }
}
