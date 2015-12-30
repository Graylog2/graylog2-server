package org.graylog.plugins.messageprocessor.ast.expressions;

public class FunctionExpression extends UnaryExpression {
    public FunctionExpression(Expression right) {
        super(right);
    }
}
