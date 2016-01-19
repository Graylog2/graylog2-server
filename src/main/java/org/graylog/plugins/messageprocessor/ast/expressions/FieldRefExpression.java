package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public class FieldRefExpression implements Expression {
    private final String variableName;

    public FieldRefExpression(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return variableName;
    }

    @Override
    public Class getType() {
        return String.class;
    }

    @Override
    public String toString() {
        return variableName;
    }
}
