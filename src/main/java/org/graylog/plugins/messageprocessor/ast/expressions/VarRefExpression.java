package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class VarRefExpression implements Expression {
    private final String identifier;

    public VarRefExpression(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return context.get(identifier);
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return identifier;
    }
}
