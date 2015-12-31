package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;

import java.util.Map;

public class WrongNumberOfArgs extends ParseError {
    private final Function<?> function;
    private final Map<String, Expression> args;

    public WrongNumberOfArgs(RuleLangParser.FunctionCallContext ctx,
                             Function<?> function,
                             Map<String, Expression> args) {
        super("wrong_number_of_arguments", ctx);
        this.function = function;
        this.args = args;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected " + function.descriptor().params().size() +
                " arguments but found " + args.size() +
                " in call to function " + function.descriptor().name()
                + positionString();
    }
}
