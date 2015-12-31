package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

public class FunctionExpression implements Expression {
    private final String name;
    private final Map<String, Expression> args;
    private final Function function;
    private final FunctionDescriptor descriptor;

    public FunctionExpression(String name, Map<String, Expression> args, Function function) {
        this.name = name;
        this.args = args;
        this.function = function;
        this.descriptor = function.descriptor();
    }

    public Function getFunction() {
        return function;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return descriptor.returnType().cast(function.evaluate(args, context, message));
    }

    @Override
    public Class getType() {
        return descriptor.returnType();
    }

    @Override
    public String toString() {
        String join = "";
        if (args != null) {
            join = Joiner.on(", ").withKeyValueSeparator(": ").join(args); // TODO order arg names
        }
        return descriptor.name()  + "(" + join + ")";
    }
}
