package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class DoubleExpression extends ConstantExpression implements NumericExpression {
    private final double value;

    public DoubleExpression(double value) {
        super(Double.class);
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isFloatingPoint() {
        return true;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
