package org.graylog.plugins.messageprocessor.ast.expressions;

public abstract class UnaryExpression implements Expression {

    protected final Expression right;

    public UnaryExpression(Expression right) {
        this.right = right;
    }

    @Override
    public boolean isConstant() {
        return right.isConstant();
    }

    @Override
    public Class getType() {
        return right.getType();
    }
}
