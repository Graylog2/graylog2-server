package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class StringExpression extends ConstantExpression {

    private final String value;

    public StringExpression(String value) {
        super(String.class);
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return '"' + value + '"';
    }
}
