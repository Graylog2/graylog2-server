package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public class MessageRefExpression implements Expression {
    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return message;
    }

    @Override
    public Class getType() {
        return Message.class;
    }

    @Override
    public String toString() {
        return "$message";
    }
}
