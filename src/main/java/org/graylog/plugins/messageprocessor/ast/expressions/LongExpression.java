package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class LongExpression extends ConstantExpression implements NumericExpression {
    private final long value;

    public LongExpression(long value) {
        super(Long.class);
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public boolean isIntegral() {
        return true;
    }

    @Override
    public boolean isFloatingPoint() {
        return false;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
