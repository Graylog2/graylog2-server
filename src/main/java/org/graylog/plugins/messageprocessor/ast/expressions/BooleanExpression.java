package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class BooleanExpression extends ConstantExpression implements LogicalExpression {
    private final boolean value;

    public BooleanExpression(boolean value) {
        super(Boolean.class);
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return value;
    }


    @Override
    public boolean evaluateBool(EvaluationContext context, Message message) {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
