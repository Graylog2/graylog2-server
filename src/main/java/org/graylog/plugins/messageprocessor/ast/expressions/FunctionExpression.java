package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

public class FunctionExpression implements Expression {
    private final FunctionArgs args;
    private final Function<?> function;
    private final FunctionDescriptor descriptor;

    public FunctionExpression(Function<?> function, FunctionArgs args) {
        this.args = args;
        this.function = function;
        this.descriptor = function.descriptor();
    }

    public Function<?> getFunction() {
        return function;
    }

    public FunctionArgs getArgs() {
        return args;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return descriptor.returnType().cast(function.evaluate(args, context));
    }

    @Override
    public Class getType() {
        return descriptor.returnType();
    }

    @Override
    public String toString() {
        String argsString = "";
        if (args != null) {
            argsString = Joiner.on(", ").withKeyValueSeparator(": ").join(args.getArgs()); // TODO order arg names
        }
        return descriptor.name() + "(" + argsString + ")";
    }
}
