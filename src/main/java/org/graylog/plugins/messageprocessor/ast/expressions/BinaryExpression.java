package org.graylog.plugins.messageprocessor.ast.expressions;

public abstract class BinaryExpression extends UnaryExpression {

    protected final Expression left;

    public BinaryExpression(Expression left, Expression right) {
        super(right);
        this.left = left;
    }

    @Override
    public boolean isConstant() {
        return left.isConstant() && right.isConstant();
    }

}
