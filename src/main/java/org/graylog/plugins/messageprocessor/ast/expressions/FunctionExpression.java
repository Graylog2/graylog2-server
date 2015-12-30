package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.FieldSet;
import org.graylog2.plugin.Message;

import java.util.Map;

public class FunctionExpression implements Expression {
    private final String name;
    private final Map<String, Expression> args;

    public FunctionExpression(String name, Map<String, Expression> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return context.invokeFunction(context, message, name, args);
    }

    @Override
    public Class getType() {
        return FieldSet.class;
    }

    @Override
    public String toString() {
        String join = "";
        if (args != null) {
            join = Joiner.on(", ").withKeyValueSeparator(": ").join(args); // TODO order arg names
        }
        return name  + "(" + join + ")";
    }
}
