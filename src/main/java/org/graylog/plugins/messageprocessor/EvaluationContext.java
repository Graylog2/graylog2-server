package org.graylog.plugins.messageprocessor;

import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.parser.FunctionRegistry;
import org.graylog2.plugin.Message;

import java.util.Map;

public class EvaluationContext {

    private final FunctionRegistry functionRegistry;

    public EvaluationContext(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    public void define(String name, Object result) {
    }

    public Object get(String identifier) {
        return null;
    }

    public Object invokeFunction(EvaluationContext context, Message message, String functionName, Map<String, Expression> args) {
        final Function function = functionRegistry.resolve(functionName);
        // TODO missing function
        return function.evaluate(args, context, message);
    }
}
