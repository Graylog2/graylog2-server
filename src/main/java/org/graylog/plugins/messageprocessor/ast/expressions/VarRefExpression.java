package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VarRefExpression implements Expression {
    private static final Logger log = LoggerFactory.getLogger(VarRefExpression.class);
    private final String identifier;
    private Class type = Object.class;

    public VarRefExpression(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        final EvaluationContext.TypedValue typedValue = context.get(identifier);
        if (typedValue != null) {
            return typedValue.getValue();
        }
        log.error("Unable to retrieve value for variable {}", identifier);
        return null;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public String toString() {
        return identifier;
    }

    public String varName() {
        return identifier;
    }

    public void setType(Class type) {
        this.type = type;
    }
}
