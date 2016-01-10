package org.graylog.plugins.messageprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.parser.RuleLangParser;

public class OptionalParametersMustBeNamed extends ParseError {
    private final Function<?> function;

    public OptionalParametersMustBeNamed(RuleLangParser.FunctionCallContext ctx, Function<?> function) {
        super("must_name_optional_params", ctx);
        this.function = function;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Function " + function.descriptor().name() + " has optional parameters, must use named parameters to call" + positionString();
    }
}
