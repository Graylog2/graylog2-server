package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class BooleanExpression extends ConstantExpression implements LogicalExpression {
    private final boolean value;

    public BooleanExpression(boolean value) {
        super(Boolean.class);
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }


    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
