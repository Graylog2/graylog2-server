package org.graylog.plugins.messageprocessor.ast.expressions;

public abstract class ConstantExpression implements Expression {

    private final Class type;

    protected ConstantExpression(Class type) {
        this.type = type;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Class getType() {
        return type;
    }

}
