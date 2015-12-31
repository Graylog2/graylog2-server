package org.graylog.plugins.messageprocessor.parser;

import org.graylog.plugins.messageprocessor.ast.functions.Function;

import javax.inject.Inject;
import java.util.Map;

public class FunctionRegistry {

    private final Map<String, Function<?>> functions;

    @Inject
    public FunctionRegistry(Map<String, Function<?>> functions) {
        this.functions = functions;
    }


    public Function<?> resolve(String name) {
        return functions.get(name);
    }

    public Function<?> resolveOrError(String name) {
        final Function<?> function = resolve(name);
        if (function == null) {
            return Function.ERROR_FUNCTION;
        }
        return function;
    }
}
