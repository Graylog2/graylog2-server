package org.graylog.plugins.messageprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.parser.RuleLangParser;

public class UndeclaredFunction extends ParseError {
    private final RuleLangParser.FunctionCallContext ctx;

    public UndeclaredFunction(RuleLangParser.FunctionCallContext ctx) {
        super("undeclared_function", ctx);
        this.ctx = ctx;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Unknown function " + ctx.funcName.getText() + positionString();
    }
}
