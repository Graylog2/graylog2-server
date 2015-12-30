package org.graylog.plugins.messageprocessor.ast.expressions;

public class ComparisonExpression extends BinaryExpression {
    public ComparisonExpression(Expression left, Expression right) {
        super(left, right);
    }
}
