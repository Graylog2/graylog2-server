package org.graylog.plugins.messageprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.parser.RuleLangParser;

public class WrongNumberOfArgs extends ParseError {
    private final Function<?> function;
    private final int argCount;

    public WrongNumberOfArgs(RuleLangParser.FunctionCallContext ctx,
                             Function<?> function,
                             int argCount) {
        super("wrong_number_of_arguments", ctx);
        this.function = function;
        this.argCount = argCount;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected " + function.descriptor().params().size() +
                " arguments but found " + argCount +
                " in call to function " + function.descriptor().name()
                + positionString();
    }
}
